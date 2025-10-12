package com.romankozak.forwardappmobile.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncDataViewModel @Inject constructor(
    private val syncRepository: SyncRepository
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
                val result = syncRepository.fetchBackupFromWifi(serverAddress)
                result.onSuccess { json ->
                    lastFetchedJson = json
                    val report = syncRepository.createSyncReport(json)
                    _changesCount.value = report.changes.size
                }.onFailure { e ->
                    _error.value = e.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Exception during check"
            } finally {
                _isCheckingForChanges.value = false
            }
        }
    }

    fun importData(serverAddress: String) {
        viewModelScope.launch {
            val jsonToImport = if (serverAddress == lastCheckedAddress && lastFetchedJson != null) {
                lastFetchedJson
            } else {
                val result = syncRepository.fetchBackupFromWifi(serverAddress)
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