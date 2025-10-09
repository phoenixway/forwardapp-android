package com.romankozak.forwardappmobile.ui.screens.mainscreen.sync

import android.app.Application
import android.util.Log
import com.romankozak.forwardappmobile.WifiSyncServer
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WifiSyncManager(
  private val syncRepository: SyncRepository,
  private val settingsRepository: SettingsRepository,
  private val application: Application,
  private val viewModelScope: CoroutineScope,
  private val uiEventChannel: Channel<ProjectUiEvent>,
) {
  private val wifiSyncServer = WifiSyncServer(syncRepository, application)

  private val _showWifiServerDialog = MutableStateFlow(false)
  val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

  private val _showWifiImportDialog = MutableStateFlow(false)
  val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

  private val _wifiServerAddress = MutableStateFlow<String?>(null)
  val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

  private val _desktopAddress = MutableStateFlow("")
  val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

  fun onDesktopAddressChange(address: String) {
    _desktopAddress.value = address
  }


  fun onShowWifiServerDialog() {
    _wifiServerAddress.value = null
    _showWifiServerDialog.value = true
    startWifiServer()
  }

  private fun startWifiServer() {
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) { wifiSyncServer.start() }
      result
        .onSuccess { address -> _wifiServerAddress.value = address }
        .onFailure { exception -> _wifiServerAddress.value = "Error: ${exception.message}" }
    }
  }

  private fun stopWifiServer() {
    viewModelScope.launch(Dispatchers.IO) {
      wifiSyncServer.stop()
      withContext(Dispatchers.Main) { _wifiServerAddress.value = null }
    }
  }

  fun onDismissWifiServerDialog() {
    _showWifiServerDialog.value = false
    stopWifiServer()
  }

  fun onShowWifiImportDialog() {
    viewModelScope.launch {
      val serverAddress = settingsRepository.getWifiSyncUrl().first()
      Log.e("WifiSyncManager", "Retrieved wifi sync url: $serverAddress")
      if (serverAddress.isNullOrBlank()) {
        uiEventChannel.send(ProjectUiEvent.ShowToast("Server address not configured in settings"))
      } else {
        performWifiImport(serverAddress)
      }
    }
  }

  fun onDismissWifiImportDialog() {
    _showWifiImportDialog.value = false
  }

  fun performWifiImport(address: String) {
    viewModelScope.launch {
      val result = syncRepository.fetchBackupFromWifi(address)
      result
        .onSuccess { jsonString ->
          uiEventChannel.send(ProjectUiEvent.NavigateToSyncScreenWithData(jsonString))
          onDismissWifiImportDialog()
        }
        .onFailure { uiEventChannel.send(ProjectUiEvent.ShowToast("Error: ${it.message}")) }
    }
  }
}
