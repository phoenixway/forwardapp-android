package com.romankozak.forwardappmobile.ui.screens.script

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.data.repository.ScriptRepository
import com.romankozak.forwardappmobile.domain.scripts.LuaScriptRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScriptEditorUiState(
    val scriptId: String? = null,
    val projectId: String? = null,
    val name: String = "",
    val description: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val isPreviewRunning: Boolean = false,
    val previewLog: String? = null,
    val error: String? = null,
)

sealed interface ScriptEditorEvent {
    object Close : ScriptEditorEvent
    data class ShowError(val message: String) : ScriptEditorEvent
}

@HiltViewModel
class ScriptEditorViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val luaScriptRunner: LuaScriptRunner,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectIdArg: String? = savedStateHandle["projectId"]
    private val scriptIdArg: String? = savedStateHandle["scriptId"]

    private val _uiState = MutableStateFlow(
        ScriptEditorUiState(
            projectId = projectIdArg,
            scriptId = scriptIdArg,
        ),
    )
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScriptEditorEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (!scriptIdArg.isNullOrBlank()) {
            viewModelScope.launch {
                scriptRepository.getScriptById(scriptIdArg)
                    ?.let { script -> populate(script) }
            }
        }
    }

    private fun populate(script: ScriptEntity) {
        _uiState.update {
            it.copy(
                name = script.name,
                description = script.description.orEmpty(),
                content = script.content,
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onContentChange(value: String) = _uiState.update { it.copy(content = value) }

    fun onPreview() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            viewModelScope.launch { _events.send(ScriptEditorEvent.ShowError("Скрипт не може бути порожнім")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPreviewRunning = true, previewLog = null, error = null) }
            runCatching {
                val context =
                    mapOf(
                        "input" to "Sample user input",
                        "conversation_title" to "Sample Chat",
                    )
                luaScriptRunner.runScript(state.content, context).fold(
                    onSuccess = { value ->
                        _uiState.update { ui ->
                            ui.copy(
                                isPreviewRunning = false,
                                previewLog = "✅ Успіх: $value",
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { ui ->
                            ui.copy(
                                isPreviewRunning = false,
                                previewLog = "❌ Помилка: ${e.message ?: e}",
                            )
                        }
                    },
                )
            }.onFailure { e ->
                _uiState.update { it.copy(isPreviewRunning = false, previewLog = "❌ Помилка: ${e.message ?: e}") }
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _events.send(ScriptEditorEvent.ShowError("Назва не може бути порожньою")) }
            return
        }
        if (state.content.isBlank()) {
            viewModelScope.launch { _events.send(ScriptEditorEvent.ShowError("Скрипт не може бути порожнім")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                if (state.scriptId == null) {
                    scriptRepository.createScript(
                        name = state.name,
                        content = state.content,
                        projectId = state.projectId,
                        description = state.description.ifBlank { null },
                    )
                } else {
                    val existing = scriptRepository.getScriptById(state.scriptId)
                    if (existing != null) {
                        scriptRepository.updateScript(
                            existing.copy(
                                name = state.name,
                                description = state.description.ifBlank { null },
                                content = state.content,
                            ),
                        )
                    }
                }
            }.onSuccess {
                _events.send(ScriptEditorEvent.Close)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
                _events.send(ScriptEditorEvent.ShowError(e.message ?: "Помилка збереження"))
            }
        }
    }
}
