package com.romankozak.forwardappmobile.features.sync

import android.app.Application
import android.util.Log
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncSettingsStore
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    private val autoSyncSettingsStore: WifiAutoSyncSettingsStore,
    private val eventJournal: WifiSyncEventJournal,
    private val dayManagementRepository: DayManagementRepository,
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private val uiEventChannel: Channel<ProjectUiEvent>,
) {
    private val wifiSyncServer =
        WifiSyncServer(
            syncRepository,
            application,
            dayManagementRepository,
            settingsRepository,
            eventJournal,
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

    private val _isForeground = MutableStateFlow(false)
    private val _autoSyncStatus = MutableStateFlow<WifiAutoSyncStatus>(WifiAutoSyncStatus.Off)
    val autoSyncStatus: StateFlow<WifiAutoSyncStatus> = _autoSyncStatus.asStateFlow()
    private var lastAutoSyncSuccessAt: Long? = null

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
    }

    init {
        viewModelScope.launch {
            settingsRepository.desktopSyncAddressFlow.collectLatest { address ->
                _desktopAddress.value = address
            }
        }
        viewModelScope.launch {
            combine(settingsRepository.wifiSyncServerEnabledFlow, settingsRepository.featureTogglesFlow) { enabled, toggles ->
                enabled && toggles[FeatureFlag.WifiSync] == true && FeatureToggles.isEnabled(FeatureFlag.WifiSync)
            }.collectLatest { enabled ->
                Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Server enabled changed: $enabled")
                if (enabled) {
                    startWifiServer()
                } else {
                    stopWifiServer(disable = true)
                }
            }
        }
        viewModelScope.launch {
            combine(
                autoSyncSettingsStore.enabledFlow,
                autoSyncSettingsStore.peersFlow,
                settingsRepository.featureTogglesFlow,
                _isForeground,
                settingsRepository.wifiSyncServerEnabledFlow,
            ) { enabled, peers, toggles, foreground, serverEnabled ->
                AutoSyncConfig(enabled && toggles[FeatureFlag.WifiSync] == true && FeatureToggles.isEnabled(FeatureFlag.WifiSync), peers, foreground, serverEnabled)
            }.collectLatest { config ->
                when {
                    !config.enabled -> _autoSyncStatus.value = WifiAutoSyncStatus.Off
                    !config.foreground -> _autoSyncStatus.value = WifiAutoSyncStatus.Paused
                    !config.serverEnabled -> _autoSyncStatus.value = WifiAutoSyncStatus.NeedsServer
                    config.peers.isEmpty() -> _autoSyncStatus.value = WifiAutoSyncStatus.NeedsSetup
                    else -> while (true) {
                        runAutoSyncRound(config.peers)
                        delay(5 * 60 * 1000L)
                    }
                }
            }
        }
    }

    private suspend fun runAutoSyncRound(peers: List<WifiAutoSyncPeer>) {
        val wifiHost = currentWifiIpv4Address(application)
        if (wifiHost != null && isServerRunning) {
            val port = settingsRepository.wifiSyncPortFlow.first()
            if (_wifiServerAddress.value != "$wifiHost:$port") {
                withContext(Dispatchers.IO) { wifiSyncServer.stop() }
                isServerRunning = false
                _wifiServerAddress.value = null
                startWifiServer()
            }
        }
        var successful = 0
        var firstError: String? = null
        peers.forEach { peer ->
            _autoSyncStatus.value = WifiAutoSyncStatus.Syncing(peer.name.ifBlank { peer.address }, peers.size)
            val eventId = eventJournal.begin(peer, "Автоматично")
            try {
                check(currentWifiIpv4Address(application) != null) { "Wi‑Fi недоступний" }
                withTimeout(90_000L) {
                    exchangeFullSnapshotWithPeer(syncRepository, peer) { eventJournal.stage(eventId, it) }
                }
                eventJournal.finish(eventId)
                successful++
                lastAutoSyncSuccessAt = System.currentTimeMillis()
            } catch (timeout: TimeoutCancellationException) {
                eventJournal.finish(eventId, timeout)
                if (firstError == null) firstError = "${peer.name.ifBlank { peer.address }}: перевищено час очікування"
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) { eventJournal.finish(eventId, cancelled) }
                throw cancelled
            } catch (error: Exception) {
                eventJournal.finish(eventId, error)
                Log.w(SYNC_LOG_TAG, "Auto sync with ${peer.address} failed: ${error.message}", error)
                if (firstError == null) firstError = "${peer.name.ifBlank { peer.address }}: ${wifiSyncErrorMessage(error)}"
            }
        }
        _autoSyncStatus.value = WifiAutoSyncStatus.Result(successful, peers.size, lastAutoSyncSuccessAt, firstError)
    }

    private data class AutoSyncConfig(
        val enabled: Boolean,
        val peers: List<WifiAutoSyncPeer>,
        val foreground: Boolean,
        val serverEnabled: Boolean,
    )

    fun onDesktopAddressChange(address: String) {
        _desktopAddress.value = address
        viewModelScope.launch {
            if (autoSyncSettingsStore.enabledFlow.first()) {
                autoSyncSettingsStore.disable()
                uiEventChannel.send(ProjectUiEvent.ShowToast("Автосинк вимкнено: перевірте адресу й ключ у налаштуваннях"))
            }
            settingsRepository.saveDesktopSyncAddress(address)
        }
    }

    fun onShowWifiServerDialog() {
        if (!FeatureToggles.isEnabled(FeatureFlag.WifiSync)) return
        _showWifiServerDialog.value = true
        refreshServerAddress(requireEnabled = false)
    }

    fun refreshServerAddress(requireEnabled: Boolean = true) {
        if (!FeatureToggles.isEnabled(FeatureFlag.WifiSync)) return
        viewModelScope.launch {
            if (requireEnabled && !settingsRepository.wifiSyncServerEnabledFlow.first()) return@launch
            val currentHost = currentWifiIpv4Address(application)
            val currentPort = settingsRepository.wifiSyncPortFlow.first()
            if (isServerRunning && _wifiServerAddress.value == "$currentHost:$currentPort") {
                _syncStatus.value = WifiSyncStatus.ServerRunning(_wifiServerAddress.value)
            } else if (isServerRunning) {
                _wifiServerAddress.value = null
                withContext(Dispatchers.IO) { wifiSyncServer.stop() }
                isServerRunning = false
                startWifiServer()
            } else {
                _wifiServerAddress.value = null
                startWifiServer()
            }
        }
    }

    private fun startWifiServer() {
        if (isServerRunning) {
            _syncStatus.value = WifiSyncStatus.ServerRunning(_wifiServerAddress.value)
            return
        }
        if (isServerStarting) return
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
                            val fallbackPort = currentWifiIpv4Address(application)
                                ?.let { host -> findAvailablePort(port + 1, 20, host) }
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
            settingsRepository.ensureWifiSyncServerKey()
            withContext(Dispatchers.IO) { wifiSyncServer.start(port) }
        }.getOrElse { exception ->
            Result.failure(exception)
        }
    }

    private fun findAvailablePort(
        startPort: Int,
        maxAttempts: Int,
        host: String,
    ): Int? {
        val endPort = startPort + maxAttempts
        for (port in startPort..endPort) {
            val available =
                runCatching {
                    ServerSocket().use { socket ->
                        socket.bind(InetSocketAddress(host, port))
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
        _showWifiImportDialog.value = true
    }

    fun onDismissWifiImportDialog() {
        _showWifiImportDialog.value = false
    }

    fun performWifiImport(address: String) {
        viewModelScope.launch {
            _syncStatus.value = WifiSyncStatus.Syncing
            Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Performing full Wi‑Fi import from $address")
            val eventId = eventJournal.begin(WifiAutoSyncPeer("manual-import", "Ручний імпорт", address, ""), "Імпорт")
            eventJournal.stage(eventId, "Отримання даних")
            val result = syncRepository.fetchBackupFromWifi(address, deltaSince = null)
            result
                .onSuccess { jsonString ->
                    eventJournal.stage(eventId, "Злиття даних")
                    syncRepository.importBackupJsonString(jsonString, restoreSettings = false)
                        .onSuccess { importedCount ->
                            eventJournal.finish(eventId)
                            _syncStatus.value = WifiSyncStatus.Idle
                            uiEventChannel.send(
                                ProjectUiEvent.ShowToast("Wi‑Fi import applied: $importedCount items"),
                            )
                            onDismissWifiImportDialog()
                        }
                        .onFailure {
                            eventJournal.finish(eventId, it)
                            val message = it.message ?: "Wi‑Fi import failed"
                            _syncStatus.value = WifiSyncStatus.Error(message)
                            Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Apply import error: $message", it)
                            uiEventChannel.send(ProjectUiEvent.ShowToast("Error: $message"))
                        }
                }
                .onFailure {
                    eventJournal.finish(eventId, it)
                    val message = wifiSyncErrorMessage(it)
                    _syncStatus.value = WifiSyncStatus.Error(message)
                    Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Import error: $message", it)
                    uiEventChannel.send(ProjectUiEvent.ShowToast("Error: $message"))
                }
        }
    }

    fun performWifiPush(address: String) {
        viewModelScope.launch {
            val targetAddress = address.trim().ifEmpty { settingsRepository.desktopSyncAddressFlow.first().trim() }
            if (targetAddress.isEmpty()) {
                val message = "Вкажіть адресу іншого пристрою в Налаштуваннях → Інтеграції → Wi-Fi Sync"
                _syncStatus.value = WifiSyncStatus.Error(message)
                uiEventChannel.send(ProjectUiEvent.ShowToast(message))
                return@launch
            }
            _syncStatus.value = WifiSyncStatus.Syncing
            Log.d(SYNC_LOG_TAG, "[WifiSyncManager] Performing Wi‑Fi push to $targetAddress")
            val eventId = eventJournal.begin(WifiAutoSyncPeer("manual-push", "Ручне надсилання", targetAddress, ""), "Надсилання")
            eventJournal.stage(eventId, "Надсилання даних")
            val result = syncRepository.pushUnsyncedToWifi(targetAddress)
            if (result.isSuccess) {
                eventJournal.finish(eventId)
                _syncStatus.value = WifiSyncStatus.Idle
                uiEventChannel.send(ProjectUiEvent.ShowToast("Synced changes over Wi‑Fi"))
            } else {
                eventJournal.finish(eventId, result.exceptionOrNull())
                val message = result.exceptionOrNull()?.let(::wifiSyncErrorMessage) ?: "Не вдалося синхронізуватися"
                _syncStatus.value = WifiSyncStatus.Error(message)
                Log.e(SYNC_LOG_TAG, "[WifiSyncManager] Push error: $message", result.exceptionOrNull())
                uiEventChannel.send(ProjectUiEvent.ShowToast("Wi‑Fi sync failed: $message"))
            }
        }
    }
}
