package com.romankozak.forwardappmobile.features.sync

import android.app.Application
import android.util.Log
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket

private const val SYNC_LOG_TAG = "FWD_SYNC_TEST"

class WifiSyncManager(
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val dayManagementRepository: DayManagementRepository,
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private val uiEventChannel: Channel<ProjectUiEvent>,
) {
    private val wifiSyncServer =
        WifiSyncServer(
            syncRepository,
            application,
            settingsRepository,
            dayManagementRepository,
        )
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
        if (isServerRunning || isServerStarting) {
            _syncStatus.value = WifiSyncStatus.ServerRunning(_wifiServerAddress.value)
            return
        }
        isServerStarting = true
        viewModelScope.launch {
            try {
                _syncStatus.value = WifiSyncStatus.Syncing
                val port = settingsRepository.wifiSyncPortFlow.first()
                Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Starting Wi‑Fi server on port $port")
                val result = startServerOnPort(port)
                result
                    .onSuccess { address ->
                        isServerRunning = true
                        _wifiServerAddress.value = address
                        _syncStatus.value = WifiSyncStatus.ServerRunning(address)
                        Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Server started at $address")
                    }
                    .onFailure { exception ->
                        val message = exception.message ?: "Unknown error"
                        val isPortBusy = exception is BindException || message.contains("Address already in use")
                        if (isPortBusy) {
                            val fallbackPort = findAvailablePort(port + 1, 20)
                            if (fallbackPort != null) {
                                settingsRepository.saveWifiSyncPort(fallbackPort)
                                val fallbackResult = startServerOnPort(fallbackPort)
                                fallbackResult
                                    .onSuccess { address ->
                                        isServerRunning = true
                                        _wifiServerAddress.value = address
                                        _syncStatus.value = WifiSyncStatus.ServerRunning(address)
                                        val notice = "Port $port busy. Started on $fallbackPort."
                                        Log.w(SYNC_LOG_TAG, "[WifiSyncManager] $notice")
                                        uiEventChannel.send(ProjectUiEvent.ShowToast(notice))
                                    }
                                    .onFailure { fallbackException ->
                                        val fallbackMessage = fallbackException.message ?: "Unknown error"
                                        isServerRunning = false
                                        _wifiServerAddress.value = "Error: $fallbackMessage"
                                        _syncStatus.value = WifiSyncStatus.Error(fallbackMessage)
                                        Log.e(
                                            SYNC_LOG_TAG,
                                            "[WifiSyncManager] Failed to start fallback server: $fallbackMessage",
                                            fallbackException,
                                        )
                                    }
                            } else {
                                isServerRunning = false
                                _wifiServerAddress.value = "Error: $message"
                                _syncStatus.value = WifiSyncStatus.Error(message)
                                Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Failed to start server: $message", exception)
                            }
                        } else {
                            isServerRunning = false
                            _wifiServerAddress.value = "Error: $message"
                            _syncStatus.value = WifiSyncStatus.Error(message)
                            Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Failed to start server: $message", exception)
                        }
                    }
            } finally {
                isServerStarting = false
            }
        }
    }

    private suspend fun startServerOnPort(port: Int): Result<String> {
        return runCatching {
            withContext(Dispatchers.IO) { wifiSyncServer.start(port) }
        }.getOrElse { exception ->
            Result.failure(exception)
        }
    }

    private fun findAvailablePort(
        startPort: Int,
        maxAttempts: Int,
    ): Int? {
        val endPort = startPort + maxAttempts
        for (port in startPort..endPort) {
            val available =
                runCatching {
                    ServerSocket().use { socket ->
                        socket.bind(InetSocketAddress("0.0.0.0", port))
                        true
                    }
                }.getOrDefault(false)
            if (available) {
                return port
            }
        }
        return null
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
            Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Performing full Wi‑Fi import from $address")
            val result = syncRepository.fetchBackupFromWifi(address, deltaSince = null)
            result
                .onSuccess { jsonString ->
                    syncRepository.importBackupJsonString(jsonString)
                        .onSuccess { importedCount ->
                            _syncStatus.value = WifiSyncStatus.Idle
                            uiEventChannel.send(
                                ProjectUiEvent.ShowToast("Wi‑Fi import applied: $importedCount items"),
                            )
                            onDismissWifiImportDialog()
                        }
                        .onFailure {
                            val message = it.message ?: "Wi‑Fi import failed"
                            _syncStatus.value = WifiSyncStatus.Error(message)
                            Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Apply import error: $message", it)
                            uiEventChannel.send(ProjectUiEvent.ShowToast("Error: $message"))
                        }
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
