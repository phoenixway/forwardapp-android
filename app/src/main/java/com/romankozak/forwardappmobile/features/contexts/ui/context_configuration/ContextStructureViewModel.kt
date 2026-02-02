package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProjectStructureUiState(
    val projectId: String,
    val basePresetCode: String? = null,
    val items: List<ContextStructureItem> = emptyList(),
    val presets: List<ContextRoleProfile> = emptyList(),
    val featureFlags: Map<String, Boolean> =
        mapOf(
            "Inbox" to true,
            "Log" to true,
            "Artifact" to true,
            "Advanced" to false,
            "Dashboard" to true,
            "Backlog" to true,
            "Attachments" to true,
            "Auto link subprojects" to true,
        ),
    val isLoading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ProjectStructureViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val contextStructureRepository: ContextStructureRepository,
        private val structurePresetService: StructurePresetService,
        private val structurePresetDao: StructurePresetDao,
    ) : ViewModel() {
        private val projectId: String = checkNotNull(savedStateHandle["projectId"])

        private val _uiState = MutableStateFlow(ProjectStructureUiState(projectId = projectId))
        val uiState: StateFlow<ProjectStructureUiState> = _uiState.asStateFlow()

        init {
            observePresets()
            observeStructure()
        }

        private fun observePresets() {
            viewModelScope.launch {
                structurePresetDao.getAll().collect { presets ->
                    _uiState.update { it.copy(presets = presets) }
                }
            }
        }

        private fun observeStructure() {
            viewModelScope.launch {
                contextStructureRepository.observeStructure(projectId).collect { structure ->
                    if (structure != null) {
                        val flags =
                            mapOf(
                                "Inbox" to (structure.structure.enableInbox ?: _uiState.value.featureFlags["Inbox"] ?: true),
                                "Log" to (structure.structure.enableLog ?: _uiState.value.featureFlags["Log"] ?: true),
                                "Artifact" to (structure.structure.enableArtifact ?: _uiState.value.featureFlags["Artifact"] ?: true),
                                "Advanced" to (structure.structure.enableAdvanced ?: _uiState.value.featureFlags["Advanced"] ?: false),
                                "Dashboard" to (structure.structure.enableDashboard ?: _uiState.value.featureFlags["Dashboard"] ?: true),
                                "Backlog" to (structure.structure.enableBacklog ?: _uiState.value.featureFlags["Backlog"] ?: true),
                                "Attachments" to (structure.structure.enableAttachments ?: _uiState.value.featureFlags["Attachments"] ?: true),
                                "Auto link subprojects" to (structure.structure.enableAutoLinkSubprojects ?: _uiState.value.featureFlags["Auto link subprojects"] ?: true),
                            )
                        _uiState.update {
                            it.copy(
                                basePresetCode = structure.structure.basePresetCode,
                                items = structure.items,
                                featureFlags = flags,
                            )
                        }
                    } else {
                        // ensure structure exists lazily
                        contextStructureRepository.ensureStructure(projectId)
                    }
                }
            }
        }

        fun applyPreset(code: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, message = null) }
                structurePresetService.applyPresetToContext(projectId, code)
                _uiState.update { it.copy(isLoading = false, basePresetCode = code) }
            }
        }

        fun applyStructure() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, message = null) }
                structurePresetService.applyContextStructure(projectId)
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        fun toggleItem(
            item: ContextStructureItem,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                contextStructureRepository.setItemEnabled(item, enabled)
                if (enabled) {
                    structurePresetService.applyContextStructure(projectId)
                }
            }
        }

        fun addItem(
            entityType: String,
            roleCode: String,
            containerType: String?,
            title: String,
            mandatory: Boolean,
        ) {
            viewModelScope.launch {
                val structure = contextStructureRepository.ensureStructure(projectId)
                val newItem =
                    ContextStructureItem(
                        id = UUID.randomUUID().toString(),
                        contextStructureId = structure.id,
                        entityType = entityType,
                        roleCode = roleCode,
                        containerType = containerType,
                        title = title,
                        mandatory = mandatory,
                        isEnabled = true,
                    )
                contextStructureRepository.addOrUpdateItem(structure.id, newItem)
                structurePresetService.applyContextStructure(projectId)
            }
        }

        fun onToggleFeatureFlag(
            key: String,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                val updatedFlags = _uiState.value.featureFlags + (key to enabled)
                _uiState.update { it.copy(featureFlags = updatedFlags) }
                val structure = contextStructureRepository.ensureStructure(projectId)
                contextStructureRepository.updateStructure(
                    structure.copy(
                        enableInbox = updatedFlags["Inbox"],
                        enableLog = updatedFlags["Log"],
                        enableArtifact = updatedFlags["Artifact"],
                        enableAdvanced = updatedFlags["Advanced"],
                        enableDashboard = updatedFlags["Dashboard"],
                        enableBacklog = updatedFlags["Backlog"],
                        enableAttachments = updatedFlags["Attachments"],
                        enableAutoLinkSubprojects = updatedFlags["Auto link subprojects"],
                    ),
                )
            }
        }
    }
