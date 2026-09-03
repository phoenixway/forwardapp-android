package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
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
            "Dashboard" to true,
            "Backlog" to true,
            "Connections" to true,
            "Auto add child context in context hierarchy to direction front" to true,
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
        private val contextRepository: ContextRepository,
        private val structurePresetService: StructurePresetService,
        private val structurePresetDao: StructurePresetDao,
        private val canonicalDashboardCapabilityRepository: CanonicalDashboardCapabilityRepository,
        private val canonicalExecutionLogRepository: CanonicalExecutionLogRepository,
    ) : ViewModel() {
        private val autoLinkLabel = "Auto add child context in context hierarchy to direction front"
        private val projectId: String = checkNotNull(savedStateHandle["projectId"])

        private val _uiState = MutableStateFlow(ProjectStructureUiState(projectId = projectId))
        val uiState: StateFlow<ProjectStructureUiState> = _uiState.asStateFlow()

        init {
            observePresets()
            observeStructure()
        }

        private fun observePresets() {
            viewModelScope.launch {
                contextStructureRepository.ensureReservedBaseRolePresets()
                structurePresetDao.getAll().collect { presets ->
                    _uiState.update { it.copy(presets = presets) }
                }
            }
        }

        private fun observeStructure() {
            viewModelScope.launch {
                contextStructureRepository.observeStructure(projectId).collect { structure ->
                    if (structure != null) {
                        val dashboardEnabled = canonicalDashboardCapabilityRepository.isEnabled(projectId)
                        val executionLogEnabled = canonicalExecutionLogRepository.isEnabled(projectId)
                        val flags =
                            mapOf(
                                "Inbox" to (structure.structure.enableInbox ?: _uiState.value.featureFlags["Inbox"] ?: true),
                                "Log" to executionLogEnabled,
                                "Dashboard" to dashboardEnabled,
                                "Backlog" to (structure.structure.enableBacklog ?: _uiState.value.featureFlags["Backlog"] ?: true),
                                "Connections" to
                                    (
                                        structure.structure.enableAttachments
                                            ?: _uiState.value.featureFlags["Connections"]
                                            ?: _uiState.value.featureFlags["Attachments"]
                                            ?: true
                                    ),
                                autoLinkLabel to (structure.structure.enableAutoLinkSubprojects ?: _uiState.value.featureFlags[autoLinkLabel] ?: true),
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
                val applied = contextStructureRepository.getStructureByContext(projectId)
                canonicalDashboardCapabilityRepository.setEnabled(
                    workspaceId = projectId,
                    enabled = applied?.enableDashboard == true,
                )
                canonicalExecutionLogRepository.setEnabled(
                    workspaceId = projectId,
                    enabled = applied?.enableLog == true,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        basePresetCode = code,
                        featureFlags =
                            it.featureFlags +
                                ("Dashboard" to (applied?.enableDashboard == true)) +
                                ("Log" to (applied?.enableLog == true)),
                    )
                }
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
                when (key) {
                    "Dashboard" ->
                        canonicalDashboardCapabilityRepository.setEnabled(
                            workspaceId = projectId,
                            enabled = enabled,
                        )
                    "Log" ->
                        canonicalExecutionLogRepository.setEnabled(
                            workspaceId = projectId,
                            enabled = enabled,
                        )
                }
                contextStructureRepository.updateStructure(
                    structure.copy(
                        enableInbox = updatedFlags["Inbox"],
                        enableAdvanced = false,
                        enableBacklog = updatedFlags["Backlog"],
                        enableAttachments = updatedFlags["Connections"] ?: updatedFlags["Attachments"],
                        enableAutoLinkSubprojects = updatedFlags[autoLinkLabel],
                    ),
                )
                if (key == autoLinkLabel && enabled) {
                    contextRepository.ensureDirectionFrontLinksForExistingChildren(projectId)
                }
            }
        }
    }
