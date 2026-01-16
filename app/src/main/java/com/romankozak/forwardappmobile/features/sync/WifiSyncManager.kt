package com.romankozak.forwardappmobile.features.sync

import android.app.Application
import android.util.Log
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SYNC_LOG_TAG = "FWD_SYNC_TEST"

class WifiSyncManager(
  private val syncRepository: SyncRepository,
  private val settingsRepository: SettingsRepository,
  private val application: Application,
  private val viewModelScope: CoroutineScope,
  private val uiEventChannel: Channel<ProjectUiEvent>,
) {
  private val wifiSyncServer = WifiSyncServer(syncRepository, application)
  private var isServerRunning = false
  private var isServerStarting = false

  private val _showWifiServerDialog = MutableStateFlow(false)
  val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

  private val _showWifiImportDialog = MutableStateFlow(false)
  val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

  private val _wifiServerAddress = MutableStateFlow<String?>(null)
  val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

  private val _desktopAddress = MutableStateFlow("")
  val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

  private val _syncStatus = MutableStateFlow<WifiSyncStatus>(WifiSyncStatus.Disabled)
  val syncStatus: StateFlow<WifiSyncStatus> = _syncStatus.asStateFlow()

  init {
    viewModelScope.launch {
      settingsRepository.desktopSyncAddressFlow.collectLatest { address ->
        _desktopAddress.value = address
      }
    }
    viewModelScope.launch {
      settingsRepository.wifiSyncServerEnabledFlow.collectLatest { enabled ->
        Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Server enabled changed: $enabled")
        if (enabled) {
          startWifiServer()
        } else {
          stopWifiServer(disable = true)
        }
      }
    }
  }

  fun onDesktopAddressChange(address: String) {
    _desktopAddress.value = address
    viewModelScope.launch {
      settingsRepository.saveDesktopSyncAddress(address)
    }
  }


  fun onShowWifiServerDialog() {
    _showWifiServerDialog.value = true
    if (isServerRunning && _wifiServerAddress.value != null) {
      _syncStatus.value = WifiSyncStatus.ServerRunning(_wifiServerAddress.value)
    } else {
      _wifiServerAddress.value = null
      startWifiServer()
    }
  }

  private fun startWifiServer() {
    viewModelScope.launch {
      if (isServerRunning || isServerStarting) {
        _syncStatus.value = WifiSyncStatus.ServerRunning(_wifiServerAddress.value)
        return@launch
      }
      isServerStarting = true
      _syncStatus.value = WifiSyncStatus.Syncing
      val port = settingsRepository.wifiSyncPortFlow.first()
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Starting Wi‑Fi server on port $port")
      val result = withContext(Dispatchers.IO) { wifiSyncServer.start(port) }
      result
        .onSuccess { address ->
          isServerRunning = true
          _wifiServerAddress.value = address
          _syncStatus.value = WifiSyncStatus.ServerRunning(address)
          Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Server started at $address")
        }
        .onFailure { exception ->
          val message = exception.message ?: "Unknown error"
          isServerRunning = false
          _wifiServerAddress.value = "Error: $message"
          _syncStatus.value = WifiSyncStatus.Error(message)
          Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Failed to start server: $message", exception)
        }
      isServerStarting = false
    }
  }

  private fun stopWifiServer(disable: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      wifiSyncServer.stop()
      withContext(Dispatchers.Main) { _wifiServerAddress.value = null }
      isServerRunning = false
      _syncStatus.value = if (disable) WifiSyncStatus.Disabled else WifiSyncStatus.Idle
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Server stopped")
    }
  }

  fun onDismissWifiServerDialog() {
    _showWifiServerDialog.value = false
  }

  fun onShowWifiImportDialog() {
    viewModelScope.launch {
      val serverAddress = settingsRepository.getWifiSyncUrl().first()
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Retrieved wifi sync url: $serverAddress")
      if (serverAddress.isNullOrBlank()) {
        _syncStatus.value = WifiSyncStatus.Offline
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
      _syncStatus.value = WifiSyncStatus.Syncing
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Performing Wi‑Fi import from $address")
      val lastSyncTime = syncRepository.getLastSyncTime()
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] lastSyncTime=$lastSyncTime, requesting delta since then")
      val result = syncRepository.fetchBackupFromWifi(address, lastSyncTime)
      result
        .onSuccess { jsonString ->
          _syncStatus.value = WifiSyncStatus.Idle
          uiEventChannel.send(ProjectUiEvent.NavigateToSyncScreenWithData(jsonString))
          onDismissWifiImportDialog()
        }
        .onFailure {
          val message = it.message ?: "Wi‑Fi import failed"
          _syncStatus.value = WifiSyncStatus.Error(message)
          Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Import error: $message", it)
          uiEventChannel.send(ProjectUiEvent.ShowToast("Error: $message"))
        }
    }
  }

  fun performWifiPush(address: String) {
    viewModelScope.launch {
      _syncStatus.value = WifiSyncStatus.Syncing
      Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Performing Wi‑Fi push to $address")
      val result = syncRepository.pushUnsyncedToWifi(address)
      if (result.isSuccess) {
        _syncStatus.value = WifiSyncStatus.Idle
        uiEventChannel.send(ProjectUiEvent.ShowToast("Synced changes over Wi‑Fi"))
      } else {
        val message = result.exceptionOrNull()?.message ?: "Wi‑Fi sync failed"
        _syncStatus.value = WifiSyncStatus.Error(message)
        Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Push error: $message", result.exceptionOrNull())
        uiEventChannel.send(ProjectUiEvent.ShowToast("Wi‑Fi sync failed: $message"))
      }
    }
  }
}
