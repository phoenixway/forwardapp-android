package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.app.Application
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncSettingsStore
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.sync.WifiSyncManager
import com.romankozak.forwardappmobile.features.sync.WifiSyncStatus
import com.romankozak.forwardappmobile.features.sync.WifiAutoSyncStatus
import com.romankozak.forwardappmobile.features.sync.WifiSyncEventJournal
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUseCase
    @Inject
    constructor(
        private val syncRepository: SyncRepository,
        private val settingsRepository: SettingsRepository,
        private val dayManagementRepository: DayManagementRepository,
        private val autoSyncSettingsStore: WifiAutoSyncSettingsStore,
        private val wifiSyncEventJournal: WifiSyncEventJournal,
    ) {
        data class SyncUiState(
            val showWifiServerDialog: Boolean = false,
            val wifiServerAddress: String? = null,
            val showWifiImportDialog: Boolean = false,
            val desktopAddress: String = "",
            val syncStatus: WifiSyncStatus = WifiSyncStatus.Disabled,
            val autoSyncStatus: WifiAutoSyncStatus = WifiAutoSyncStatus.Off,
        )

        private val _syncUiState = MutableStateFlow(SyncUiState())
        val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

        private var wifiSyncManager: WifiSyncManager? = null
        private var isInitialized = false
        private val listeners = java.util.concurrent.ConcurrentHashMap.newKeySet<Channel<ProjectUiEvent>>()
        private val internalUiEventChannel = Channel<ProjectUiEvent>(Channel.BUFFERED)

        fun initialize(
            scope: CoroutineScope,
            application: Application,
            uiEventChannel: Channel<ProjectUiEvent>? = null,
        ) {
            uiEventChannel?.let { channel ->
                listeners.add(channel)
                scope.coroutineContext[Job]?.invokeOnCompletion { listeners.remove(channel) }
            }
            if (isInitialized) return

            val manager =
                WifiSyncManager(
                    syncRepository = syncRepository,
                    settingsRepository = settingsRepository,
                    autoSyncSettingsStore = autoSyncSettingsStore,
                    eventJournal = wifiSyncEventJournal,
                    dayManagementRepository = dayManagementRepository,
                    application = application,
                    viewModelScope = scope,
                    uiEventChannel = internalUiEventChannel,
                )
            wifiSyncManager = manager

            scope.launch {
                internalUiEventChannel.receiveAsFlow().collect { event ->
                    listeners.forEach { listener ->
                        listener.trySend(event)
                    }
                }
            }
            scope.launch {
                combine(
                    manager.showWifiServerDialog,
                    manager.wifiServerAddress,
                    manager.showWifiImportDialog,
                    manager.desktopAddress,
                    manager.syncStatus,
                ) {
                        showServerDialog,
                        wifiServerAddress,
                        showImportDialog,
                        desktopAddress,
                        syncStatus,
                    ->
                    SyncUiState(
                        showWifiServerDialog = showServerDialog,
                        wifiServerAddress = wifiServerAddress,
                        showWifiImportDialog = showImportDialog,
                        desktopAddress = desktopAddress,
                        syncStatus = syncStatus,
                    )
                }.collect { syncState ->
                    _syncUiState.update { current -> syncState.copy(autoSyncStatus = current.autoSyncStatus) }
                }
            }

            scope.launch {
                manager.autoSyncStatus.collect { status ->
                    _syncUiState.update { it.copy(autoSyncStatus = status) }
                }
            }

            isInitialized = true
        }

        fun setForeground(foreground: Boolean) {
            wifiSyncManager?.setForeground(foreground)
        }

        private fun manager(): WifiSyncManager =
            wifiSyncManager ?: error("SyncUseCase.initialize must be called before use.")

        fun onShowWifiServerDialog() = manager().onShowWifiServerDialog()

        fun onShowWifiImportDialog() = manager().onShowWifiImportDialog()

        fun onDismissWifiServerDialog() = manager().onDismissWifiServerDialog()

        fun onDismissWifiImportDialog() = manager().onDismissWifiImportDialog()

        fun onDesktopAddressChange(address: String) = manager().onDesktopAddressChange(address)

        fun performWifiImport(address: String) = manager().performWifiImport(address)

        fun performWifiPush(address: String) = manager().performWifiPush(address)
    }
