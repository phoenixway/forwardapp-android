package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import android.net.Uri
import android.util.Log
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.sync.SyncRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class CommandDeckImportExportHandler
    @Inject
    constructor(
        private val syncUseCase: SyncUseCase,
        private val syncRepository: SyncRepository,
    ) {
        private val _importChoiceUri = MutableStateFlow<Uri?>(null)
        val importChoiceUri: StateFlow<Uri?> = _importChoiceUri.asStateFlow()

        private val _exportChoiceVisible = MutableStateFlow(false)
        val exportChoiceVisible: StateFlow<Boolean> = _exportChoiceVisible.asStateFlow()

        private val _uiEvents = MutableSharedFlow<CommandDeckUiEvent>(extraBufferCapacity = 1)
        val uiEvents: SharedFlow<CommandDeckUiEvent> = _uiEvents.asSharedFlow()

        val syncUiState: StateFlow<SyncUseCase.SyncUiState> = syncUseCase.syncUiState

        private val _showWifiImportDialog = MutableStateFlow(false)
        val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

        private val uiEventChannel = Channel<ProjectUiEvent>(Channel.BUFFERED)
        private var isInitialized = false

        fun initialize(
            scope: CoroutineScope,
            application: Application,
        ) {
            if (isInitialized) return
            syncUseCase.initialize(
                scope = scope,
                application = application,
                uiEventChannel = uiEventChannel,
            )
            scope.launch {
                uiEventChannel.receiveAsFlow().collect { event ->
                    when (event) {
                        is ProjectUiEvent.ShowToast ->
                            _uiEvents.tryEmit(CommandDeckUiEvent.ShowMessage(event.message))

                        is ProjectUiEvent.NavigateToSyncScreenWithData ->
                            _uiEvents.tryEmit(CommandDeckUiEvent.NavigateToSyncScreenWithData(event.json))

                        else -> Unit
                    }
                }
            }
            isInitialized = true
        }

        fun requestExportToFile() {
            _exportChoiceVisible.value = true
        }

        fun dismissExportChoice() {
            _exportChoiceVisible.value = false
        }

        fun requestImportFromFile(uri: Uri) {
            _importChoiceUri.value = uri
        }

        fun dismissImportChoice() {
            _importChoiceUri.value = null
        }

        suspend fun confirmImportV1(uri: Uri) {
            Log.e("FullJsonImport", "confirmImportV1 start uri=$uri")
            val result = syncRepository.importFullBackupFromFile(uri)
            Log.e("FullJsonImport", "confirmImportV1 result isSuccess=${result.isSuccess} message=${result.getOrNull()} error=${result.exceptionOrNull()?.message}")
            emitResult(result, "Import successful", "Import error")
        }

        suspend fun confirmImportV2(uri: Uri) {
            Log.e("FullJsonImport", "confirmImportV2 start uri=$uri")
            val result = syncRepository.importFullBackupFromFileV2(uri)
            Log.e("FullJsonImport", "confirmImportV2 result isSuccess=${result.isSuccess} message=${result.getOrNull()} error=${result.exceptionOrNull()?.message}")
            emitResult(result, "Import successful", "Import error")
        }

        suspend fun confirmExportV1() {
            val result = syncRepository.exportFullBackupToFile()
            emitResult(result, "Export successful", "Export error")
            _exportChoiceVisible.value = false
        }

        suspend fun confirmExportV2() {
            val result = syncRepository.exportFullBackupToFileV2()
            emitResult(result, "Export successful", "Export error")
            _exportChoiceVisible.value = false
        }

        suspend fun exportAttachments() {
            val result = syncRepository.exportAttachmentsToFile()
            emitResult(result, "Attachments exported", "Attachments export error")
        }

        suspend fun importAttachments(uri: Uri) {
            val result = syncRepository.importAttachmentsFromFile(uri)
            emitResult(result, "Attachments imported", "Attachments import error")
        }

        fun showWifiServerDialog() {
            syncUseCase.onShowWifiServerDialog()
        }

        fun dismissWifiServerDialog() {
            syncUseCase.onDismissWifiServerDialog()
        }

        fun showWifiImportDialog() {
            _showWifiImportDialog.value = true
        }

        fun dismissWifiImportDialog() {
            _showWifiImportDialog.value = false
        }

        fun updateWifiImportAddress(address: String) {
            syncUseCase.onDesktopAddressChange(address)
        }

        fun performWifiImport(address: String) {
            _showWifiImportDialog.value = false
            syncUseCase.performWifiImport(address)
        }

        suspend fun wifiPush(host: String) {
            syncUseCase.performWifiPush(host)
        }

        private fun emitResult(
            result: Result<String>,
            fallbackSuccess: String,
            fallbackError: String,
        ) {
            val message = result.getOrNull() ?: fallbackSuccess
            if (result.isSuccess) {
                val emitted = _uiEvents.tryEmit(CommandDeckUiEvent.ShowMessage(message))
                Log.e("FullJsonImport", "emitResult success emitted=$emitted message=$message")
            } else {
                val reason = result.exceptionOrNull()?.message
                val fullMessage =
                    if (reason.isNullOrBlank()) {
                        fallbackError
                    } else {
                        "$fallbackError: $reason"
                    }
                val emitted = _uiEvents.tryEmit(CommandDeckUiEvent.ShowMessage(fullMessage))
                Log.e("FullJsonImport", "emitResult failure emitted=$emitted message=$fullMessage")
            }
            _importChoiceUri.value = null
        }
    }
