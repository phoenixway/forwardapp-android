package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.domain.lifecontext.StartContextTrackingUseCase
import com.romankozak.forwardappmobile.domain.lifecontext.SubmitContextInputUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase.SyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val COMMAND_DECK_PREFS_NAME = "command_deck_prefs"

@HiltViewModel
class CommandDeckViewModel
    @Inject
    constructor(
        private val application: Application,
        private val submitContextInputUseCase: SubmitContextInputUseCase,
        private val startContextTrackingUseCase: StartContextTrackingUseCase,
        private val importExportHandler: CommandDeckImportExportHandler,
    ) : ViewModel() {
        private val sharedPreferences =
            application.getSharedPreferences(COMMAND_DECK_PREFS_NAME, Context.MODE_PRIVATE)

        private val _isContextInputVisible = MutableStateFlow(false)
        val isContextInputVisible: StateFlow<Boolean> = _isContextInputVisible.asStateFlow()

        private val _contextInputText = MutableStateFlow("")
        val contextInputText: StateFlow<String> = _contextInputText.asStateFlow()

        val importChoiceUri: StateFlow<Uri?> = importExportHandler.importChoiceUri
        val exportChoiceVisible: StateFlow<Boolean> = importExportHandler.exportChoiceVisible
        val syncUiState: StateFlow<SyncUiState> = importExportHandler.syncUiState
        val showWifiImportDialog: StateFlow<Boolean> = importExportHandler.showWifiImportDialog
        val uiEvents = importExportHandler.uiEvents

        init {
            importExportHandler.initialize(
                scope = viewModelScope,
                application = application,
            )
        }

        fun isCategoryExpanded(categoryTitle: String): Boolean {
            return sharedPreferences.getBoolean(categoryTitle, false)
        }

        fun setCategoryExpanded(
            categoryTitle: String,
            isExpanded: Boolean,
        ) {
            sharedPreferences.edit().putBoolean(categoryTitle, isExpanded).apply()
        }

        fun openContextInput() {
            _isContextInputVisible.value = true
        }

        fun closeContextInput() {
            _isContextInputVisible.value = false
        }

        fun onContextInputChange(text: String) {
            _contextInputText.value = text
        }

        fun clearContextInput() {
            _contextInputText.value = ""
        }

        fun submitContextInput() {
            val text = _contextInputText.value.trim()
            if (text.isEmpty()) {
                closeContextInput()
                return
            }
            viewModelScope.launch {
                submitContextInputUseCase(text)
                clearContextInput()
                closeContextInput()
            }
        }

        fun startContextTracking() {
            val text = _contextInputText.value.trim()
            if (text.isEmpty()) {
                closeContextInput()
                return
            }
            viewModelScope.launch {
                startContextTrackingUseCase(text)
                clearContextInput()
                closeContextInput()
            }
        }

        fun onImportChoiceDismiss() {
            importExportHandler.dismissImportChoice()
        }

        fun onExportChoiceDismiss() {
            importExportHandler.dismissExportChoice()
        }

        fun confirmImportV1(uri: Uri) {
            viewModelScope.launch {
                importExportHandler.confirmImportV1(uri)
            }
        }

        fun confirmImportV2(uri: Uri) {
            viewModelScope.launch {
                importExportHandler.confirmImportV2(uri)
            }
        }

        fun confirmExportV1() {
            viewModelScope.launch {
                importExportHandler.confirmExportV1()
            }
        }

        fun confirmExportV2() {
            viewModelScope.launch {
                importExportHandler.confirmExportV2()
            }
        }

        fun onShowWifiServerDialog() {
            importExportHandler.showWifiServerDialog()
        }

        fun onDismissWifiServerDialog() {
            importExportHandler.dismissWifiServerDialog()
        }

        fun onShowWifiImportDialog() {
            importExportHandler.showWifiImportDialog()
        }

        fun onDismissWifiImportDialog() {
            importExportHandler.dismissWifiImportDialog()
        }

        fun onWifiImportAddressChange(address: String) {
            importExportHandler.updateWifiImportAddress(address)
        }

        fun onWifiImportConfirm(address: String) {
            importExportHandler.performWifiImport(address)
        }

        fun onEvent(event: CommandDeckEvent) {
            when (event) {
                CommandDeckEvent.ExportToFile -> {
                    importExportHandler.requestExportToFile()
                }
                is CommandDeckEvent.ImportFromFileRequest -> {
                    importExportHandler.requestImportFromFile(Uri.parse(event.fileUri))
                }
                CommandDeckEvent.ExportAttachments -> {
                    viewModelScope.launch {
                        importExportHandler.exportAttachments()
                    }
                }
                is CommandDeckEvent.ImportAttachmentsFromFile -> {
                    viewModelScope.launch {
                        importExportHandler.importAttachments(Uri.parse(event.fileUri))
                    }
                }
                is CommandDeckEvent.WifiPush -> {
                    viewModelScope.launch {
                        importExportHandler.wifiPush(event.host)
                    }
                }
            }
        }
    }

sealed interface CommandDeckEvent {
    object ExportToFile : CommandDeckEvent

    data class ImportFromFileRequest(val fileUri: String) : CommandDeckEvent

    object ExportAttachments : CommandDeckEvent

    data class ImportAttachmentsFromFile(val fileUri: String) : CommandDeckEvent

    data class WifiPush(val host: String) : CommandDeckEvent
}
