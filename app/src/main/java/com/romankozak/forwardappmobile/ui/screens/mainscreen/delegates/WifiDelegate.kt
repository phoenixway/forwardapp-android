package com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates

import android.app.Application
import android.net.Uri
import com.romankozak.forwardappmobile.WifiSyncServer
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.GoalListUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WifiDelegate(
    private val syncRepo: SyncRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val uiEventChannel: Channel<GoalListUiEvent>
) {
    private val _desktopAddress = MutableStateFlow("")
    val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

    private val _showWifiServerDialog = MutableStateFlow(false)
    val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

    private val _showWifiImportDialog = MutableStateFlow(false)
    val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

    private val _wifiServerAddress = MutableStateFlow<String?>(null)
    val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

    private val wifiSyncServer = WifiSyncServer(syncRepo, application)

    suspend fun initialize() {
        _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
    }

    suspend fun onShowWifiServerDialog() {
        _wifiServerAddress.value = null
        _showWifiServerDialog.value = true
        startWifiServer()
    }

    suspend fun performWifiImport(address: String) {
        val result = syncRepo.fetchBackupFromWifi(address)
        result.onSuccess { jsonString ->
            uiEventChannel.send(GoalListUiEvent.NavigateToSyncScreenWithData(jsonString))
            onDismissWifiImportDialog()
        }.onFailure {
            uiEventChannel.send(GoalListUiEvent.ShowToast("Error: ${it.message}"))
        }
    }

    suspend fun exportToFile() {
        withContext(Dispatchers.IO) {
            val result = syncRepo.exportFullBackupToFile()
            result.onSuccess { message ->
                uiEventChannel.send(GoalListUiEvent.ShowToast(message))
            }.onFailure { error ->
                uiEventChannel.send(GoalListUiEvent.ShowToast("Export error: ${error.message}"))
            }
        }
    }

    suspend fun onFullImportConfirmed(uri: Uri) {
        withContext(Dispatchers.IO) {
            val result = syncRepo.importFullBackupFromFile(uri)
            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    uiEventChannel.send(GoalListUiEvent.ShowToast(message))
                }.onFailure { error ->
                    uiEventChannel.send(GoalListUiEvent.ShowToast("Import error: ${error.message}"))
                }
            }
        }
    }

    private suspend fun startWifiServer() {
        val result = withContext(Dispatchers.IO) {
            wifiSyncServer.start()
        }
        result.onSuccess { address ->
            _wifiServerAddress.value = address
        }.onFailure { exception ->
            _wifiServerAddress.value = "Error: ${exception.message}"
        }
    }

    private suspend fun stopWifiServer() {
        withContext(Dispatchers.IO) {
            wifiSyncServer.stop()
            withContext(Dispatchers.Main) {
                _wifiServerAddress.value = null
            }
        }
    }

    fun onDesktopAddressChange(newAddress: String) {
        _desktopAddress.value = newAddress
        // Note: You'll need to call settingsRepo.saveDesktopAddress from the main ViewModel
    }

    suspend fun onDismissWifiServerDialog() {
        _showWifiServerDialog.value = false
        stopWifiServer()
    }

    fun onShowWifiImportDialog() { _showWifiImportDialog.value = true }
    fun onDismissWifiImportDialog() { _showWifiImportDialog.value = false }
}