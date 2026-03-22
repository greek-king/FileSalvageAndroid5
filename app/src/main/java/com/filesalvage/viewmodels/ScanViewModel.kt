package com.filesalvage.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filesalvage.models.*
import com.filesalvage.services.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class Screen {
    object Home     : Screen()
    object Scanning : Screen()
    object Results  : Screen()
    object Recovery : Screen()
}

data class ScanUiState(
    val progress: ScanProgress = ScanProgress("", 0f, 0),
    val result: ScanResult?    = null,
    val error: String?         = null
)

data class RecoveryUiState(
    val progress: RecoveryProgress = RecoveryProgress("", 0, 0, 0f, emptyList()),
    val result: FileRecoveryService.RecoveryResult? = null,
    val isRunning: Boolean = false,
    val error: String?     = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner  = FileScanner(application)
    private val recovery = FileRecoveryService(application)

    // Navigation
    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    // Scan state
    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Recovery state
    private val _recoveryState = MutableStateFlow(RecoveryUiState())
    val recoveryState: StateFlow<RecoveryUiState> = _recoveryState.asStateFlow()

    // File selection
    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    // Active type filter in results
    private val _activeFilter = MutableStateFlow<FileType?>(null)
    val activeFilter: StateFlow<FileType?> = _activeFilter.asStateFlow()

    // Scan depth choice
    private val _scanDepth = MutableStateFlow(ScanDepth.QUICK)
    val scanDepth: StateFlow<ScanDepth> = _scanDepth.asStateFlow()

    private var scanJob: Job? = null
    private var recoveryJob: Job? = null

    // ─── Navigation ───────────────────────────────────────────────────────────

    fun navigate(screen: Screen) { _screen.value = screen }

    // ─── Scan ─────────────────────────────────────────────────────────────────

    fun setScanDepth(depth: ScanDepth) { _scanDepth.value = depth }

    fun startScan() {
        _scanState.value = ScanUiState()
        _selectedFiles.value = emptySet()
        _screen.value = Screen.Scanning

        scanJob = viewModelScope.launch {
            scanner.scan(_scanDepth.value).collect { event ->
                when (event) {
                    is ScanEvent.Progress -> {
                        _scanState.update { it.copy(progress = event.data) }
                    }
                    is ScanEvent.Complete -> {
                        _scanState.update { it.copy(result = event.result) }
                        _screen.value = Screen.Results
                    }
                    is ScanEvent.Error -> {
                        _scanState.update { it.copy(error = event.message) }
                    }
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _screen.value = Screen.Home
    }

    // ─── File selection ───────────────────────────────────────────────────────

    fun toggleFileSelection(fileId: String) {
        _selectedFiles.update { current ->
            if (fileId in current) current - fileId else current + fileId
        }
    }

    fun selectAll() {
        val ids = filteredFiles().map { it.id }.toSet()
        _selectedFiles.value = ids
    }

    fun deselectAll() { _selectedFiles.value = emptySet() }

    fun setFilter(type: FileType?) { _activeFilter.value = type }

    fun filteredFiles(): List<RecoverableFile> {
        val files = _scanState.value.result?.files ?: emptyList()
        val filter = _activeFilter.value
        return if (filter == null) files else files.filter { it.fileType == filter }
    }

    val selectedCount: Int get() = _selectedFiles.value.size
    val selectedSize: Long get() {
        val ids = _selectedFiles.value
        return _scanState.value.result?.files
            ?.filter { it.id in ids }
            ?.sumOf { it.size } ?: 0L
    }

    // ─── Recovery ─────────────────────────────────────────────────────────────

    fun startRecovery(destination: FileRecoveryService.Destination = FileRecoveryService.Destination.Pictures) {
        val toRecover = _scanState.value.result?.files
            ?.filter { it.id in _selectedFiles.value }
            ?: return

        if (toRecover.isEmpty()) return

        _recoveryState.value = RecoveryUiState(isRunning = true)
        _screen.value = Screen.Recovery

        recoveryJob = viewModelScope.launch {
            recovery.recoverFiles(toRecover, destination).collect { event ->
                when (event) {
                    is RecoveryEvent.Progress -> {
                        _recoveryState.update { it.copy(progress = event.data) }
                    }
                    is RecoveryEvent.Complete -> {
                        _recoveryState.update {
                            it.copy(result = event.result, isRunning = false)
                        }
                    }
                    is RecoveryEvent.Error -> {
                        _recoveryState.update {
                            it.copy(error = event.message, isRunning = false)
                        }
                    }
                }
            }
        }
    }

    fun cancelRecovery() {
        recoveryJob?.cancel()
        _recoveryState.update { it.copy(isRunning = false) }
    }

    // ─── Storage check ────────────────────────────────────────────────────────

    fun hasEnoughSpace(): Boolean {
        val files = _scanState.value.result?.files
            ?.filter { it.id in _selectedFiles.value } ?: emptyList()
        return recovery.hasEnoughSpace(files)
    }

    fun formattedFreeSpace(): String {
        val bytes = recovery.availableStorageBytes()
        val gb = bytes / 1_073_741_824.0
        val mb = bytes / 1_048_576.0
        return if (gb >= 1.0) "%.1f GB free".format(gb) else "%.0f MB free".format(mb)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        recoveryJob?.cancel()
    }
}
