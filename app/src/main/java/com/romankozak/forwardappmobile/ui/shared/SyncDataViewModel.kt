package com.romankozak.forwardappmobile.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncDataViewModel
    @Inject
    constructor(
        private val syncRepository: SyncRepository,
    ) : ViewModel() {
        var jsonString: String? = null

        private val _changesCount = MutableStateFlow<Int?>(null)
        val changesCount = _changesCount.asStateFlow()

        private val _isCheckingForChanges = MutableStateFlow(false)
        val isCheckingForChanges = _isCheckingForChanges.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error = _error.asStateFlow()

        private var lastCheckedAddress: String? = null
        private var lastFetchedJson: String? = null

        fun checkForChanges(serverAddress: String) {
            if (serverAddress == lastCheckedAddress) return
            lastCheckedAddress = serverAddress

            viewModelScope.launch {
                _isCheckingForChanges.value = true
                _error.value = null
                _changesCount.value = null
                lastFetchedJson = null

                try {
                    val lastSyncTime = syncRepository.getLastSyncTime()
                    val result = syncRepository.fetchBackupFromWifi(serverAddress, lastSyncTime)
                    result.onSuccess { json ->
                        lastFetchedJson = json
                        runCatching { syncRepository.createSyncReport(json) }
                            .onSuccess { report ->
                                _changesCount.value = report.changes.size
                            }
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                _error.value = error.message ?: "Exception during check"
                            }
                    }.onFailure { e ->
                        _error.value = e.message ?: "Unknown error"
                    }
                } finally {
                    _isCheckingForChanges.value = false
                }
            }
        }

        fun importData(serverAddress: String) {
            viewModelScope.launch {
                val jsonToImport =
                    if (serverAddress == lastCheckedAddress && lastFetchedJson != null) {
                        lastFetchedJson
                    } else {
                        val lastSyncTime = syncRepository.getLastSyncTime()
                        val result = syncRepository.fetchBackupFromWifi(serverAddress, lastSyncTime)
                        result.getOrNull()
                    }

                if (jsonToImport != null) {
                    val report = syncRepository.createSyncReport(jsonToImport)
                    // Наразі ми автоматично схвалюємо всі зміни.
                    // У майбутньому це може перенаправляти на SyncScreen.
                    syncRepository.applyChanges(report.changes)
                } else {
                    _error.value = "Failed to fetch data for import."
                }
            }
        }
    }
