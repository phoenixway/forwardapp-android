package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.domain.lifecontext.StartContextTrackingUseCase
import com.romankozak.forwardappmobile.domain.lifecontext.SubmitContextInputUseCase
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeRepository
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase.SyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val COMMAND_DECK_PREFS_NAME = "command_deck_prefs"
private const val SESSION_MODE_CARD_EXPANDED_KEY = "session_mode_card_expanded"

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CommandDeckViewModel
    @Inject
    constructor(
        private val application: Application,
        private val submitContextInputUseCase: SubmitContextInputUseCase,
        private val startContextTrackingUseCase: StartContextTrackingUseCase,
        private val sessionModeRepository: SessionModeRepository,
        private val systemContextEnsurer: SystemContextEnsurer,
        private val contextLogRepository: ContextLogRepository,
        private val importExportHandler: CommandDeckImportExportHandler,
    ) : ViewModel() {
        private val sharedPreferences =
            application.getSharedPreferences(COMMAND_DECK_PREFS_NAME, Context.MODE_PRIVATE)

        private val _isContextInputVisible = MutableStateFlow(false)
        val isContextInputVisible: StateFlow<Boolean> = _isContextInputVisible.asStateFlow()

        private val _contextInputText = MutableStateFlow("")
        val contextInputText: StateFlow<String> = _contextInputText.asStateFlow()

        private val _pendingSessionResultMode = MutableStateFlow<SessionMode?>(null)
        val pendingSessionResultMode: StateFlow<SessionMode?> = _pendingSessionResultMode.asStateFlow()

        private val _pendingSessionChangeReasonMode = MutableStateFlow<SessionMode?>(null)
        val pendingSessionChangeReasonMode: StateFlow<SessionMode?> = _pendingSessionChangeReasonMode.asStateFlow()

        private val _uiEvents = MutableSharedFlow<CommandDeckUiEvent>(extraBufferCapacity = 1)

        val importChoiceUri: StateFlow<Uri?> = importExportHandler.importChoiceUri
        val exportChoiceVisible: StateFlow<Boolean> = importExportHandler.exportChoiceVisible
        val syncUiState: StateFlow<SyncUiState> = importExportHandler.syncUiState
        val showWifiImportDialog: StateFlow<Boolean> = importExportHandler.showWifiImportDialog
        val importExportUiEvents = importExportHandler.uiEvents
        val uiEvents = _uiEvents.asSharedFlow()
        val sessionModeState: StateFlow<SessionModeState> =
            sessionModeRepository.sessionModeState.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SessionModeState(),
            )
        val latestSessionReason: StateFlow<String?> =
            sessionModeState
                .flatMapLatest { state ->
                    val contextId = state.mode.systemContextId
                    if (contextId == null) {
                        emptyFlow()
                    } else {
                        contextLogRepository.getContextLogsStream(contextId)
                    }
                }.map { logs ->
                    logs.firstOrNull { it.type == "SESSION_REASON" }?.description
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null,
                )

        init {
            importExportHandler.initialize(
                scope = viewModelScope,
                application = application,
            )
            viewModelScope.launch {
                systemContextEnsurer.ensureAllSystemContextsExist()
            }
        }

        fun isCategoryExpanded(
            categoryTitle: String,
            defaultValue: Boolean = false,
        ): Boolean {
            return sharedPreferences.getBoolean(categoryTitle, defaultValue)
        }

        fun setCategoryExpanded(
            categoryTitle: String,
            isExpanded: Boolean,
        ) {
            sharedPreferences.edit().putBoolean(categoryTitle, isExpanded).apply()
        }

        fun isSessionModeCardExpanded(): Boolean = sharedPreferences.getBoolean(SESSION_MODE_CARD_EXPANDED_KEY, true)

        fun setSessionModeCardExpanded(isExpanded: Boolean) {
            sharedPreferences.edit().putBoolean(SESSION_MODE_CARD_EXPANDED_KEY, isExpanded).apply()
        }

        fun openContextInput() {
            _isContextInputVisible.value = true
        }

        fun closeContextInput() {
            _isContextInputVisible.value = false
            if (_contextInputText.value.isBlank()) {
                _pendingSessionResultMode.value = null
                _pendingSessionChangeReasonMode.value = null
            }
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
                val pendingMode = _pendingSessionResultMode.value
                val pendingChangeReasonMode = _pendingSessionChangeReasonMode.value
                if (pendingMode != null) {
                    sessionModeRepository.reportModeResults(
                        mode = pendingMode,
                        text = text,
                    )
                    _pendingSessionResultMode.value = null
                    _uiEvents.tryEmit(CommandDeckUiEvent.ShowMessage("Підсумок ${pendingMode.title} збережено"))
                } else if (pendingChangeReasonMode != null) {
                    sessionModeRepository.reportModeChangeReason(
                        mode = pendingChangeReasonMode,
                        text = text,
                    )
                    _pendingSessionChangeReasonMode.value = null
                    _uiEvents.tryEmit(CommandDeckUiEvent.ShowMessage("Причину зміни режиму збережено"))
                } else {
                    submitContextInputUseCase(text)
                }
                clearContextInput()
                closeContextInput()
            }
        }

        fun startContextTracking() {
            if (_pendingSessionResultMode.value != null) {
                submitContextInput()
                return
            }
            if (_pendingSessionChangeReasonMode.value != null) {
                submitContextInput()
                return
            }
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

        fun setSessionMode(mode: SessionMode) {
            viewModelScope.launch {
                val result = sessionModeRepository.setMode(mode)
                if (result.previousMode == null && result.newMode == mode && sessionModeState.value.mode == mode) {
                    return@launch
                }
                val message =
                    if (mode == SessionMode.UNSET) {
                        "Режим сесії вимкнено"
                    } else {
                        "Активний режим: ${mode.title}"
                    }
                _uiEvents.tryEmit(
                    CommandDeckUiEvent.ShowSessionModeChanged(
                        message = message,
                        previousMode = result.previousMode,
                        newMode = result.newMode,
                    ),
                )
            }
        }

        fun openSystemContext(
            contextId: String,
            onOpen: (String) -> Unit,
        ) {
            viewModelScope.launch {
                systemContextEnsurer.ensureAllSystemContextsExist()
                onOpen(contextId)
            }
        }

        fun preparePreviousSessionResult(mode: SessionMode) {
            if (mode == SessionMode.UNSET) return
            _pendingSessionChangeReasonMode.value = null
            _pendingSessionResultMode.value = mode
            _contextInputText.value = "Підсумок ${mode.title}: "
            _isContextInputVisible.value = true
        }

        fun prepareModeChangeReason(mode: SessionMode) {
            if (mode == SessionMode.UNSET) return
            _pendingSessionResultMode.value = null
            _pendingSessionChangeReasonMode.value = mode
            _contextInputText.value = "Чому увімкнено ${mode.title}: "
            _isContextInputVisible.value = true
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
