package com.romankozak.forwardappmobile.desktop.features.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.romankozak.forwardappmobile.desktop.data.contexts.DesktopWorkspaceFileStore
import com.romankozak.forwardappmobile.desktop.data.sync.DesktopAndroidSyncClient
import com.romankozak.forwardappmobile.desktop.data.sync.DesktopAndroidSyncSettings
import com.romankozak.forwardappmobile.desktop.data.sync.DesktopAndroidSyncSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun rememberDesktopAndroidSyncController(
    fileStore: DesktopWorkspaceFileStore,
): DesktopAndroidSyncController {
    val scope = rememberCoroutineScope()
    return remember(fileStore, scope) {
        DesktopAndroidSyncController(
            client = DesktopAndroidSyncClient(fileStore),
            settingsStore = DesktopAndroidSyncSettingsStore(),
            scope = scope,
        )
    }
}

class DesktopAndroidSyncController(
    private val client: DesktopAndroidSyncClient,
    private val settingsStore: DesktopAndroidSyncSettingsStore,
    private val scope: CoroutineScope,
) {
    private val syncMutex = Mutex()
    private val mutableState = MutableStateFlow(DesktopAndroidSyncState(settings = settingsStore.read()))
    private var loopJob: Job? = null

    val state: StateFlow<DesktopAndroidSyncState> = mutableState.asStateFlow()

    init {
        startLoop()
    }

    fun onAddressChanged(address: String) {
        updateSettings { settings -> settings.copy(androidAddress = address.trim()) }
    }

    fun onAutoSyncChanged(enabled: Boolean) {
        updateSettings { settings -> settings.copy(autoSyncEnabled = enabled) }
    }

    fun syncNow() {
        val settings = mutableState.value.settings
        if (settings.androidAddress.isBlank()) {
            mutableState.update { state -> state.copy(lastError = "Android server address is empty.") }
            return
        }
        scope.launch {
            performSync(settings)
        }
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob =
            scope.launch {
                var wasConnected = false
                var retryDelayMillis = QUICK_RETRY_DELAY_MS
                while (true) {
                    val settings = mutableState.value.settings
                    if (!settings.autoSyncEnabled || settings.androidAddress.isBlank()) {
                        mutableState.update { state ->
                            state.copy(connection = DesktopAndroidConnectionState.Idle, isSyncing = false)
                        }
                        wasConnected = false
                        delay(IDLE_DELAY_MS)
                        continue
                    }

                    mutableState.update { state -> state.copy(connection = DesktopAndroidConnectionState.Checking) }
                    val pingResult = client.ping(settings.androidAddress)
                    if (pingResult.isSuccess) {
                        mutableState.update { state -> state.copy(connection = DesktopAndroidConnectionState.Connected, lastError = null) }
                        retryDelayMillis = QUICK_RETRY_DELAY_MS
                        val lastSyncAt = settings.lastSyncAt
                        val syncDue =
                            !wasConnected ||
                                lastSyncAt == null ||
                                System.currentTimeMillis() - lastSyncAt >= CONNECTED_SYNC_INTERVAL_MS
                        if (syncDue) {
                            performSync(settings)
                        }
                        wasConnected = true
                        delay(CONNECTED_POLL_DELAY_MS)
                    } else {
                        wasConnected = false
                        mutableState.update { state ->
                            state.copy(
                                connection = DesktopAndroidConnectionState.Disconnected,
                                lastError = pingResult.exceptionOrNull()?.message,
                            )
                        }
                        delay(retryDelayMillis)
                        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                    }
                }
            }
    }

    private suspend fun performSync(settings: DesktopAndroidSyncSettings) {
        syncMutex.withLock {
            mutableState.update { state -> state.copy(isSyncing = true, lastError = null) }
            client.syncOnce(address = settings.androidAddress, lastSyncAt = settings.lastSyncAt)
                .onSuccess { result ->
                    val nextSettings = settings.copy(lastSyncAt = result.syncedAt)
                    settingsStore.write(nextSettings)
                    mutableState.update { state ->
                        state.copy(
                            settings = nextSettings,
                            connection = DesktopAndroidConnectionState.Connected,
                            isSyncing = false,
                            lastError = null,
                            lastSyncMessage = result.toMessage(),
                            workspaceRevision = state.workspaceRevision + 1,
                        )
                    }
                }.onFailure { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isSyncing = false,
                            lastError = throwable.message ?: "Sync failed.",
                        )
                    }
                }
        }
    }

    private fun updateSettings(transform: (DesktopAndroidSyncSettings) -> DesktopAndroidSyncSettings) {
        val next = transform(mutableState.value.settings)
        settingsStore.write(next)
        mutableState.update { state -> state.copy(settings = next, lastError = null) }
    }

    private companion object {
        const val QUICK_RETRY_DELAY_MS = 1_000L
        const val MAX_RETRY_DELAY_MS = 10_000L
        const val CONNECTED_POLL_DELAY_MS = 15_000L
        const val CONNECTED_SYNC_INTERVAL_MS = 30_000L
        const val IDLE_DELAY_MS = 2_000L
    }
}

data class DesktopAndroidSyncState(
    val settings: DesktopAndroidSyncSettings,
    val connection: DesktopAndroidConnectionState = DesktopAndroidConnectionState.Idle,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val lastSyncMessage: String = "No sync yet.",
    val workspaceRevision: Long = 0L,
)

enum class DesktopAndroidConnectionState {
    Idle,
    Checking,
    Connected,
    Disconnected,
}

private fun com.romankozak.forwardappmobile.desktop.data.sync.DesktopAndroidSyncResult.toMessage(): String =
    buildString {
        append("Synced")
        if (pushedLocalDelta) append(", pushed desktop changes")
        if (importedRemoteDelta) append(", imported Android changes")
        append(", day tasks $incomingDayTasks/$mergedDayTasks")
        append(".")
    }
