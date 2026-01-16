package com.romankozak.forwardappmobile.features.contexts.ui.contextstructure

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.StructurePreset
import com.romankozak.forwardappmobile.data.database.models.StructurePresetItem
import com.romankozak.forwardappmobile.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.data.dao.StructurePresetItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PresetEditorItem(
    val id: String = UUID.randomUUID().toString(),
    val entityType: String = "ATTACHMENT",
    val roleCode: String = "",
    val containerType: String? = "NOTE",
    val title: String = "",
    val mandatory: Boolean = false,
)

data class StructurePresetEditorUiState(
    val presetId: String? = null,
    val code: String = "",
    val label: String = "",
    val description: String = "",
    val enableInbox: Boolean = true,
    val enableLog: Boolean = true,
    val enableArtifact: Boolean = true,
    val enableAdvanced: Boolean = false,
    val enableDashboard: Boolean = true,
    val enableBacklog: Boolean = true,
    val enableAttachments: Boolean = true,
    val enableAutoLinkSubprojects: Boolean = true,
    val items: List<PresetEditorItem> = emptyList(),
)

sealed interface StructurePresetEditorEvent {
    object Close : StructurePresetEditorEvent
}

@HiltViewModel
class StructurePresetEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val presetDao: StructurePresetDao,
    private val presetItemDao: StructurePresetItemDao,
) : ViewModel() {

    private val presetIdArg: String? = savedStateHandle["presetId"]
    private val copyFromId: String? = savedStateHandle["copyFromPresetId"]

    private val _uiState = MutableStateFlow(StructurePresetEditorUiState(presetId = presetIdArg))
    val uiState: StateFlow<StructurePresetEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<StructurePresetEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            when {
                presetIdArg != null -> loadPreset(presetIdArg)
                copyFromId != null -> loadPreset(copyFromId, isCopy = true)
            }
        }
    }

    private suspend fun loadPreset(id: String, isCopy: Boolean = false) {
        val preset = presetDao.getById(id) ?: return
        val items = presetItemDao.getItemsByPresetOnce(id)
        _uiState.update {
            it.copy(
                presetId = if (isCopy) null else preset.id,
                code = if (isCopy) "${preset.code}_copy" else preset.code,
                label = preset.label,
                description = preset.description ?: "",
                enableInbox = preset.enableInbox ?: true,
                enableLog = preset.enableLog ?: true,
                enableArtifact = preset.enableArtifact ?: true,
                enableAdvanced = preset.enableAdvanced ?: false,
                enableDashboard = preset.enableDashboard ?: true,
                enableBacklog = preset.enableBacklog ?: true,
                enableAttachments = preset.enableAttachments ?: true,
                enableAutoLinkSubprojects = preset.enableAutoLinkSubprojects ?: true,
                items = items.map { item ->
                    PresetEditorItem(
                        id = if (isCopy) UUID.randomUUID().toString() else item.id,
                        entityType = item.entityType,
                        roleCode = item.roleCode,
                        containerType = item.containerType,
                        title = item.title,
                        mandatory = item.mandatory,
                    )
                }
            )
        }
    }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value) }
    fun onLabelChange(value: String) = _uiState.update { it.copy(label = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onEnableInboxChange(value: Boolean) = _uiState.update { it.copy(enableInbox = value) }
    fun onEnableLogChange(value: Boolean) = _uiState.update { it.copy(enableLog = value) }
    fun onEnableArtifactChange(value: Boolean) = _uiState.update { it.copy(enableArtifact = value) }
    fun onEnableAdvancedChange(value: Boolean) = _uiState.update { it.copy(enableAdvanced = value) }
    fun onEnableDashboardChange(value: Boolean) = _uiState.update { it.copy(enableDashboard = value) }
    fun onEnableBacklogChange(value: Boolean) = _uiState.update { it.copy(enableBacklog = value) }
    fun onEnableAttachmentsChange(value: Boolean) = _uiState.update { it.copy(enableAttachments = value) }
    fun onEnableAutoLinkSubprojectsChange(value: Boolean) = _uiState.update { it.copy(enableAutoLinkSubprojects = value) }

    fun addItem(item: PresetEditorItem) {
        _uiState.update { it.copy(items = it.items + item) }
    }

    fun removeItem(id: String) {
        _uiState.update { it.copy(items = it.items.filterNot { it.id == id }) }
    }

    fun updateItem(updated: PresetEditorItem) {
        _uiState.update {
            it.copy(items = it.items.map { item -> if (item.id == updated.id) updated else item })
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.code.isBlank() || state.label.isBlank()) return@launch
            val presetId = state.presetId ?: UUID.randomUUID().toString()
            val preset = StructurePreset(
                id = presetId,
                code = state.code,
                label = state.label,
                description = state.description.ifBlank { null },
                enableInbox = state.enableInbox,
                enableLog = state.enableLog,
                enableArtifact = state.enableArtifact,
                enableAdvanced = state.enableAdvanced,
                enableDashboard = state.enableDashboard,
                enableBacklog = state.enableBacklog,
                enableAttachments = state.enableAttachments,
                enableAutoLinkSubprojects = state.enableAutoLinkSubprojects,
            )
            presetDao.insertPreset(preset)
            val items = state.items.map {
                StructurePresetItem(
                    id = it.id,
                    presetId = presetId,
                    entityType = it.entityType,
                    roleCode = it.roleCode,
                    containerType = it.containerType,
                    title = it.title,
                    mandatory = it.mandatory,
                )
            }
            presetItemDao.replaceItems(presetId, items)
            _events.send(StructurePresetEditorEvent.Close)
        }
    }
}
