package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.app.Application
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.sync.WifiSyncManager
import com.romankozak.forwardappmobile.features.sync.WifiSyncStatus
import com.romankozak.forwardappmobile.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    ) {
        data class SyncUiState(
            val showWifiServerDialog: Boolean = false,
            val wifiServerAddress: String? = null,
            val showWifiImportDialog: Boolean = false,
            val desktopAddress: String = "",
            val syncStatus: WifiSyncStatus = WifiSyncStatus.Disabled,
        )

        private val _syncUiState = MutableStateFlow(SyncUiState())
        val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

        private var wifiSyncManager: WifiSyncManager? = null
        private var isInitialized = false
        private val listeners = LinkedHashSet<Channel<ProjectUiEvent>>()
        private val internalUiEventChannel = Channel<ProjectUiEvent>(Channel.BUFFERED)

        fun initialize(
            scope: CoroutineScope,
            application: Application,
            uiEventChannel: Channel<ProjectUiEvent>,
        ) {
            listeners.add(uiEventChannel)
            if (isInitialized) return

            val manager =
                WifiSyncManager(
                    syncRepository = syncRepository,
                    settingsRepository = settingsRepository,
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
                }.collect { syncState -> _syncUiState.value = syncState }
            }

            isInitialized = true
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
