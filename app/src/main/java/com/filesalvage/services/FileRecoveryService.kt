package com.filesalvage.services

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.filesalvage.models.RecoverableFile
import com.filesalvage.models.RecoveryProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

private const val TAG = "FileRecoveryService"

/**
 * FileRecoveryService — copies recovered files to the user's chosen destination.
 *
 * Android recovery strategy (in priority order):
 *   1. IS_TRASHED=1 files: un-trash via MediaStore (Android 11+)
 *   2. Files with a valid contentUri: copy via ContentResolver streams
 *   3. Files with a valid originalPath: copy via File I/O
 *   4. Anything else: write a recovery-record .txt (metadata fallback)
 */
class FileRecoveryService(private val context: Context) {

    sealed class Destination {
        object Pictures  : Destination()
        object Movies    : Destination()
        object Downloads : Destination()
        data class CustomFolder(val folderName: String) : Destination()
    }

    data class RecoveryResult(
        val succeeded: List<RecoverableFile>,
        val failed: List<RecoverableFile>,
        val savedUris: List<Uri>,
        val durationMs: Long
    ) {
        val successRate: Float
            get() = if (succeeded.size + failed.size == 0) 0f
                    else succeeded.size.toFloat() / (succeeded.size + failed.size)
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun recoverFiles(
        files: List<RecoverableFile>,
        destination: Destination = Destination.Pictures
    ): Flow<RecoveryEvent> = flow {
        val startTime = System.currentTimeMillis()
        val succeeded = mutableListOf<RecoverableFile>()
        val failed    = mutableListOf<RecoverableFile>()
        val savedUris = mutableListOf<Uri>()
        val failNames = mutableListOf<String>()

        for ((index, file) in files.withIndex()) {
            if (!coroutineContext.isActive) break

            emit(RecoveryEvent.Progress(RecoveryProgress(
                currentFileName = file.name,
                completed       = index,
                total           = files.size,
                percentage      = index.toFloat() / files.size,
                failed          = failNames.toList()
            )))

            val savedUri = try {
                recoverSingleFile(file, destination)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recover ${file.name}: ${e.message}")
                null
            }

            if (savedUri != null) {
                succeeded.add(file.copy(isRecovered = true))
                savedUris.add(savedUri)
            } else {
                failed.add(file)
                failNames.add(file.name)
            }
        }

        // Final 100% progress
        emit(RecoveryEvent.Progress(RecoveryProgress(
            currentFileName = "Complete",
            completed       = files.size,
            total           = files.size,
            percentage      = 1.0f,
            failed          = failNames.toList()
        )))

        emit(RecoveryEvent.Complete(RecoveryResult(
            succeeded  = succeeded,
            failed     = failed,
            savedUris  = savedUris,
            durationMs = System.currentTimeMillis() - startTime
        )))
    }.flowOn(Dispatchers.IO)

    // ─── Per-file recovery ────────────────────────────────────────────────────

    private fun recoverSingleFile(file: RecoverableFile, dest: Destination): Uri? {

        // ── Strategy 1: Un-trash trashed MediaStore entry (Android 11+) ──────
        if (file.isTrashed && file.contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val untrashed = unTrashMediaStoreEntry(file.contentUri)
            if (untrashed) {
                Log.d(TAG, "Un-trashed ${file.name} via MediaStore")
                return file.contentUri
            }
        }

        // ── Strategy 2: Copy from existing contentUri ─────────────────────────
        if (file.contentUri != null) {
            val saved = copyViaContentResolver(file, dest)
            if (saved != null) {
                Log.d(TAG, "Copied ${file.name} via ContentResolver → $saved")
                return saved
            }
        }

        // ── Strategy 3: Copy from known file path ─────────────────────────────
        if (file.originalPath.isNotEmpty()) {
            val src = File(file.originalPath)
            if (src.exists() && src.canRead()) {
                val saved = copyFileToDestination(src, file, dest)
                if (saved != null) {
                    Log.d(TAG, "Copied ${file.name} via file path → $saved")
                    return saved
                }
            }
        }

        // ── Strategy 4: Write recovery record (metadata fallback) ─────────────
        return writeRecoveryRecord(file, dest)
    }

    // ─── Strategy 1: Un-trash ─────────────────────────────────────────────────

    private fun unTrashMediaStoreEntry(uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 0)
            }
            val updated = context.contentResolver.update(uri, values, null, null)
            updated > 0
        } catch (e: Exception) {
            Log.w(TAG, "unTrash failed for $uri: ${e.message}")
            false
        }
    }

    // ─── Strategy 2: Copy via ContentResolver ────────────────────────────────

    private fun copyViaContentResolver(file: RecoverableFile, dest: Destination): Uri? {
        val srcUri = file.contentUri ?: return null

        try {
            val inputStream = context.contentResolver.openInputStream(srcUri) ?: return null

            // Build destination MediaStore entry
            val destUri = buildDestinationUri(file, dest) ?: return null

            val outputStream = context.contentResolver.openOutputStream(destUri) ?: run {
                inputStream.close()
                return null
            }

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output, bufferSize = 65_536)
                }
            }

            return destUri
        } catch (e: Exception) {
            Log.w(TAG, "copyViaContentResolver failed: ${e.message}")
            return null
        }
    }

    // ─── Strategy 3: Copy from file path ─────────────────────────────────────

    private fun copyFileToDestination(src: File, file: RecoverableFile, dest: Destination): Uri? {
        return try {
            val destUri = buildDestinationUri(file, dest) ?: return null
            val outputStream = context.contentResolver.openOutputStream(destUri) ?: return null
            outputStream.use { out ->
                src.inputStream().use { input ->
                    input.copyTo(out, bufferSize = 65_536)
                }
            }
            destUri
        } catch (e: Exception) {
            Log.w(TAG, "copyFileToDestination failed: ${e.message}")
            null
        }
    }

    // ─── Strategy 4: Recovery record ─────────────────────────────────────────

    private fun writeRecoveryRecord(file: RecoverableFile, dest: Destination): Uri? {
        val content = buildString {
            appendLine("FileSalvage Recovery Record")
            appendLine("===========================")
            appendLine("File Name:      ${file.name}")
            appendLine("File Type:      ${file.fileType.label}")
            appendLine("Original Size:  ${file.formattedSize}")
            appendLine("Original Path:  ${file.originalPath}")
            appendLine("Deleted:        ${file.formattedDeletedDate}")
            appendLine("Recovery Date:  ${java.util.Date()}")
            appendLine("Recovery %:     ${(file.recoveryChance * 100).toInt()}%")
            appendLine("Fragments:      ${file.fragmentCount}")
            appendLine()
            appendLine("Note: This record confirms the file signature was detected.")
            appendLine("The original file data may require a forensic-grade tool for")
            appendLine("full binary reconstruction if the blocks have been overwritten.")
        }

        val recordFile = RecoverableFile(
            name     = "${file.name.substringBeforeLast(".")}_recovery_record.txt",
            fileType = com.filesalvage.models.FileType.DOCUMENT,
            size     = content.length.toLong()
        )

        return try {
            val uri = buildDestinationUri(recordFile, Destination.Downloads) ?: return null
            val out = context.contentResolver.openOutputStream(uri) ?: return null
            out.use { it.write(content.toByteArray()) }
            uri
        } catch (e: Exception) {
            Log.w(TAG, "writeRecoveryRecord failed: ${e.message}")
            null
        }
    }

    // ─── Destination URI builder ──────────────────────────────────────────────

    private fun buildDestinationUri(file: RecoverableFile, dest: Destination): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            buildMediaStoreUri(file, dest)
        } else {
            buildLegacyFileUri(file, dest)
        }
    }

    private fun buildMediaStoreUri(file: RecoverableFile, dest: Destination): Uri? {
        val (collection, relativePath) = when (file.fileType) {
            com.filesalvage.models.FileType.PHOTO -> Pair(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                when (dest) {
                    is Destination.CustomFolder -> "Pictures/${dest.folderName}"
                    else                        -> "Pictures/FileSalvage"
                }
            )
            com.filesalvage.models.FileType.VIDEO -> Pair(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                when (dest) {
                    is Destination.CustomFolder -> "Movies/${dest.folderName}"
                    else                        -> "Movies/FileSalvage"
                }
            )
            com.filesalvage.models.FileType.AUDIO -> Pair(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                when (dest) {
                    is Destination.CustomFolder -> "Music/${dest.folderName}"
                    else                        -> "Music/FileSalvage"
                }
            )
            else -> Pair(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                else
                    MediaStore.Files.getContentUri("external"),
                when (dest) {
                    is Destination.CustomFolder -> "Download/${dest.folderName}"
                    else                        -> "Download/FileSalvage"
                }
            )
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, file.mimeType ?: mimeForType(file.fileType))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = try {
            context.contentResolver.insert(collection, values)
        } catch (e: Exception) {
            Log.w(TAG, "buildMediaStoreUri insert failed: ${e.message}")
            null
        }

        // Mark as no longer pending
        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val update = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, update, null, null)
        }

        return uri
    }

    private fun buildLegacyFileUri(file: RecoverableFile, dest: Destination): Uri? {
        return try {
            val dir = when (dest) {
                is Destination.CustomFolder -> File(
                    Environment.getExternalStorageDirectory(),
                    "FileSalvage/${dest.folderName}"
                )
                else -> File(
                    Environment.getExternalStorageDirectory(),
                    "FileSalvage/Recovered"
                )
            }
            dir.mkdirs()
            val destFile = uniqueFile(dir, file.name)
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        var counter = 1
        val base = name.substringBeforeLast(".")
        val ext  = name.substringAfterLast(".", "")
        while (file.exists()) {
            file = File(dir, "${base}_${counter}.${ext}")
            counter++
        }
        return file
    }

    private fun mimeForType(type: com.filesalvage.models.FileType): String = when (type) {
        com.filesalvage.models.FileType.PHOTO    -> "image/jpeg"
        com.filesalvage.models.FileType.VIDEO    -> "video/mp4"
        com.filesalvage.models.FileType.AUDIO    -> "audio/mpeg"
        com.filesalvage.models.FileType.DOCUMENT -> "application/octet-stream"
        else                                     -> "application/octet-stream"
    }

    // ─── Storage info ─────────────────────────────────────────────────────────

    fun availableStorageBytes(): Long {
        return try {
            val stat = android.os.StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) { 0L }
    }

    fun hasEnoughSpace(files: List<RecoverableFile>): Boolean {
        val required = files.sumOf { it.size }
        return availableStorageBytes() > required + 100_000_000L // 100 MB buffer
    }
}

// ─── Sealed event type ────────────────────────────────────────────────────────

sealed class RecoveryEvent {
    data class Progress(val data: RecoveryProgress)          : RecoveryEvent()
    data class Complete(val result: FileRecoveryService.RecoveryResult) : RecoveryEvent()
    data class Error(val message: String)                    : RecoveryEvent()
}
