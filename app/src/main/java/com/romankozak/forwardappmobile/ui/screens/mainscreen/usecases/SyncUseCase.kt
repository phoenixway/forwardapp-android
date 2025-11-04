package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import android.app.Application
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.sync.WifiSyncManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ViewModelScoped
class SyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
) {

    data class SyncUiState(
        val showWifiServerDialog: Boolean = false,
        val wifiServerAddress: String? = null,
        val showWifiImportDialog: Boolean = false,
        val desktopAddress: String = "",
    )

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    private var wifiSyncManager: WifiSyncManager? = null
    private var isInitialized = false

    fun initialize(
        scope: CoroutineScope,
        application: Application,
        uiEventChannel: Channel<ProjectUiEvent>,
    ) {
        if (isInitialized) return

        val manager =
            WifiSyncManager(
                syncRepository = syncRepository,
                settingsRepository = settingsRepository,
                application = application,
                viewModelScope = scope,
                uiEventChannel = uiEventChannel,
            )
        wifiSyncManager = manager

        scope.launch {
            combine(
                manager.showWifiServerDialog,
                manager.wifiServerAddress,
                manager.showWifiImportDialog,
                manager.desktopAddress,
            ) { showServerDialog, wifiServerAddress, showImportDialog, desktopAddress ->
                SyncUiState(
                    showWifiServerDialog = showServerDialog,
                    wifiServerAddress = wifiServerAddress,
                    showWifiImportDialog = showImportDialog,
                    desktopAddress = desktopAddress,
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
}
