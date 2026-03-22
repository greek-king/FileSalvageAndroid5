package com.filesalvage.models

import android.net.Uri
import java.util.UUID

// ─── File type ───────────────────────────────────────────────────────────────

enum class FileType(
    val label: String,
    val iconRes: String,       // Material icon name for UI
    val colorHex: String,
    val extensions: List<String>
) {
    PHOTO(
        label      = "Photos",
        iconRes    = "photo",
        colorHex   = "#4ECDC4",
        extensions = listOf("jpg","jpeg","png","heic","gif","webp","bmp","tiff","raw","cr2","nef","dng")
    ),
    VIDEO(
        label      = "Videos",
        iconRes    = "video",
        colorHex   = "#FF6B6B",
        extensions = listOf("mp4","mov","avi","mkv","m4v","3gp","wmv","flv","ts","webm")
    ),
    AUDIO(
        label      = "Audio",
        iconRes    = "audio",
        colorHex   = "#A855F7",
        extensions = listOf("mp3","m4a","wav","aac","flac","ogg","wma","aiff","opus","amr")
    ),
    DOCUMENT(
        label      = "Documents",
        iconRes    = "document",
        colorHex   = "#3B82F6",
        extensions = listOf("pdf","doc","docx","txt","xls","xlsx","ppt","pptx","csv","rtf","zip","rar","7z","apk")
    ),
    UNKNOWN(
        label      = "Other",
        iconRes    = "unknown",
        colorHex   = "#6B7280",
        extensions = emptyList()
    );

    companion object {
        fun fromExtension(ext: String): FileType =
            values().firstOrNull { ext.lowercase() in it.extensions } ?: UNKNOWN

        fun fromMimeType(mime: String?): FileType = when {
            mime == null            -> UNKNOWN
            mime.startsWith("image") -> PHOTO
            mime.startsWith("video") -> VIDEO
            mime.startsWith("audio") -> AUDIO
            mime.contains("pdf") || mime.contains("document") ||
            mime.contains("text") || mime.contains("zip")      -> DOCUMENT
            else                    -> UNKNOWN
        }
    }
}

// ─── Recovery status ─────────────────────────────────────────────────────────

enum class RecoveryStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, PARTIAL }

// ─── Scan depth ──────────────────────────────────────────────────────────────

enum class ScanDepth(
    val label: String,
    val description: String,
    val estimatedSeconds: Int
) {
    QUICK(
        label            = "Quick Scan",
        description      = "Scans trashed & recently deleted media (~1–2 min)",
        estimatedSeconds = 90
    ),
    DEEP(
        label            = "Deep Scan",
        description      = "Full MediaStore + orphaned files scan (~5–8 min)",
        estimatedSeconds = 360
    ),
    FULL(
        label            = "Full Recovery Scan",
        description      = "Maximum depth — all storage volumes (~15–20 min)",
        estimatedSeconds = 1000
    )
}

// ─── Recoverable file ────────────────────────────────────────────────────────

data class RecoverableFile(
    val id: String           = UUID.randomUUID().toString(),
    val name: String,
    val fileType: FileType,
    val size: Long,                    // bytes
    val deletedDate: Long?   = null,   // epoch ms (DATE_MODIFIED / DATE_EXPIRES)
    val originalPath: String = "",
    val contentUri: Uri?     = null,   // MediaStore URI — usable for recovery
    val thumbnailUri: Uri?   = null,
    var isSelected: Boolean  = false,
    val recoveryChance: Double = 1.0,  // 0.0 – 1.0
    val fragmentCount: Int   = 1,
    var isRecovered: Boolean = false,
    val isTrashed: Boolean   = false,  // true = in Android Recycle Bin (IS_TRASHED=1)
    val mediaStoreId: Long   = -1L,
    val mimeType: String?    = null
) {
    val formattedSize: String get() {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0  -> "%.1f GB".format(gb)
            mb >= 1.0  -> "%.1f MB".format(mb)
            kb >= 1.0  -> "%.0f KB".format(kb)
            else       -> "$size B"
        }
    }

    val recoveryChanceLabel: String get() = when {
        recoveryChance >= 0.8 -> "Excellent"
        recoveryChance >= 0.5 -> "Good"
        recoveryChance >= 0.2 -> "Fair"
        else                  -> "Low"
    }

    val recoveryChanceColor: String get() = when {
        recoveryChance >= 0.8 -> "#10B981"
        recoveryChance >= 0.5 -> "#F59E0B"
        recoveryChance >= 0.2 -> "#FF6B6B"
        else                  -> "#6B7280"
    }

    val formattedDeletedDate: String get() {
        deletedDate ?: return "Unknown"
        val diffMs = System.currentTimeMillis() - deletedDate
        val diffMin  = diffMs / 60_000
        val diffHour = diffMs / 3_600_000
        val diffDay  = diffMs / 86_400_000
        return when {
            diffMin  < 60  -> "$diffMin minutes ago"
            diffHour < 24  -> "$diffHour hours ago"
            diffDay  < 30  -> "$diffDay days ago"
            else           -> "${diffDay / 30} months ago"
        }
    }
}

// ─── Scan result ─────────────────────────────────────────────────────────────

data class ScanResult(
    val files: List<RecoverableFile>,
    val totalScanned: Int,
    val durationMs: Long,
    val scanDepth: ScanDepth
) {
    val byType: Map<FileType, List<RecoverableFile>>
        get() = files.groupBy { it.fileType }

    val totalSize: Long
        get() = files.sumOf { it.size }
}

// ─── Progress events ─────────────────────────────────────────────────────────

data class ScanProgress(
    val step: String,
    val percentage: Float,
    val filesFound: Int
)

data class RecoveryProgress(
    val currentFileName: String,
    val completed: Int,
    val total: Int,
    val percentage: Float,
    val failed: List<String>
)
