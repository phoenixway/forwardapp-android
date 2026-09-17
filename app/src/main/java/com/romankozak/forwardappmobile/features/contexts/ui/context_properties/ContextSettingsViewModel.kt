package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsRegistry
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextSettingsUpdate
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.canonicalSystemBacklogLifecycleOverrides
import com.romankozak.forwardappmobile.data.workspace.canonicalSystemRemainingCapabilityOverrides
import com.romankozak.forwardappmobile.domain.structure.StructurePresetService
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabActions
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private data class SettingsOptionsSources(
    val contexts: List<ContextPresentation>,
    val attachments: List<AttachmentLibraryQueryResult>,
    val relatedLinks: List<RelatedLink>,
)

@HiltViewModel
class ContextSettingsViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val reminderRepository: ReminderRepository,
        private val savedStateHandle: SavedStateHandle,
        private val structurePresetDao: StructurePresetDao,
        private val contextStructureRepository: ContextStructureRepository,
        private val structurePresetService: StructurePresetService,
        private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
        private val systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority,
        private val capabilityRegistry: CapabilityRegistry,
        private val contextCapabilitiesResolver: ContextCapabilitiesResolver,
        private val capabilitySettingsRegistry: CapabilitySettingsRegistry,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val canonicalDashboardCapabilityRepository: CanonicalDashboardCapabilityRepository,
        private val canonicalExecutionLogRepository: CanonicalExecutionLogRepository,
        private val systemCapabilityAccess: SystemContextCanonicalInboxDirectionAccess,
        private val systemRemainingCapabilityAccess: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
        private val systemBacklogLifecycleAccess: SystemContextCanonicalBacklogLifecycleAccess,
    ) : ViewModel(), EvaluationTabActions, RemindersTabActions {
        private val projectId: String? = savedStateHandle["projectId"]
        private val allContexts =
            systemWorkspacePresentationContextProjector.observePresentationUniverse(
                contextRepository.getAllContextsFlow(),
            ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private val allAttachmentOptions =
            attachmentsRepository.getAttachmentLibraryItems()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        private val _uiState = MutableStateFlow(ContextSettingsUiState())
        val uiState: StateFlow<ContextSettingsUiState> = _uiState.asStateFlow()

        private val _events = Channel<ContextSettingsEvent>()
        val events = _events.receiveAsFlow()

        fun getAvailableCapabilitySettingsTabs(enabledCapabilityIds: Set<CapabilityId>): List<CapabilitySettingsEntry> =
            capabilitySettingsRegistry.forCapabilities(enabledCapabilityIds)

        init {
            viewModelScope.launch {
                if (projectId != null) {
                    loadExistingProject(projectId)
                    reminderRepository.getRemindersForEntityFlow(projectId).collect { reminders ->
                        _uiState.update { it.copy(reminderTime = reminders.firstOrNull()?.reminderTime) }
                    }
                }
            }

            viewModelScope.launch {
                contextStructureRepository.ensureReservedBaseRolePresets()
                structurePresetDao.getAll().collect { presets ->
                    _uiState.update { it.copy(availablePresets = presets) }
                }
            }

            viewModelScope.launch {
                combine(
                    allContexts,
                    allAttachmentOptions,
                    uiState,
                ) { contexts, attachments, state ->
                    SettingsOptionsSources(
                        contexts = contexts,
                        attachments = attachments,
                        relatedLinks = state.relatedLinks,
                    )
                }.collect { sources ->
                    val contextOptions =
                        sources.contexts.map { context ->
                            ProjectOption(
                                id = context.id,
                                name = context.name,
                                parentId = context.parentId,
                            )
                        }
                    val attachmentOptions =
                        sources.attachments
                            .mapNotNull { it.toAttachmentOption() }
                            .filterNot { it.linkType == LinkType.CONTEXT }
                    val relatedLinks = sources.relatedLinks
                    _uiState.update {
                        it.copy(
                            availableContexts = contextOptions,
                            availableAttachments = attachmentOptions,
                            selectedAttachmentIds = resolveSelectedAttachmentIds(relatedLinks, attachmentOptions),
                        )
                    }
                }
            }
        }

        /**
         * Завантаження існуючого проекту з повною синхронізацією конфігурації
         */
        private suspend fun loadExistingProject(projectId: String) {
            val project = contextRepository.getContextById(projectId)
            val presentedProject =
                systemWorkspacePresentationContextProjector.resolvePresentation(
                    contextId = projectId,
                    context = project,
                )
            if (presentedProject == null) {
                _events.send(ContextSettingsEvent.NavigateBack("Проект недоступний"))
                return
            }

            val structure = contextStructureRepository.getStructureByContext(projectId)
            val resolvedConfig = structure ?: ContextConfiguration.default(projectId)
            val presetLabel =
                structure?.basePresetCode?.let { code ->
                    structurePresetDao.getByCode(code)?.label
                } ?: "Стандартний (Default)"

            val canonicalSystemState = systemCapabilityAccess.getState(projectId)
            val canonicalRemainingState = systemRemainingCapabilityAccess.getState(projectId)
            val canonicalBacklogState = systemBacklogLifecycleAccess.getState(projectId)
            val hasPromotedSystemCapabilityAuthority =
                canonicalSystemState?.isCanonicalOwnerAvailable == true &&
                    canonicalRemainingState?.isCanonicalOwnerAvailable == true &&
                    canonicalBacklogState?.isCanonicalOwnerAvailable == true

            val legacyEnabledCapabilities =
                contextCapabilitiesResolver.resolve(
                    config = resolvedConfig,
                    includePresetCapabilities = !hasPromotedSystemCapabilityAuthority,
                )
            val withCanonicalSystemCapabilities =
                canonicalSystemState?.let { canonical ->
                    legacyEnabledCapabilities
                        .let { if (canonical.inboxEnabled) it + CapabilityId("inbox") else it - CapabilityId("inbox") }
                        .let { if (canonical.directionEnabled) it + CapabilityId("direction") else it - CapabilityId("direction") }
                } ?: legacyEnabledCapabilities
            val withAllCanonicalSystemCapabilities =
                canonicalSystemRemainingCapabilityOverrides(projectId, canonicalRemainingState)
                    .entries
                    .fold(withCanonicalSystemCapabilities) { capabilities, (id, enabled) ->
                        if (enabled) capabilities + id else capabilities - id
                    }
            val withCanonicalBacklog =
                canonicalSystemBacklogLifecycleOverrides(projectId, canonicalBacklogState)
                    .entries
                    .fold(withAllCanonicalSystemCapabilities) { capabilities, (id, enabled) ->
                        if (enabled) capabilities + id else capabilities - id
                    }

            val dashboardCapability = CapabilityId("dashboard")
            val executionLogCapability = CapabilityId("log")
            val malformedSystemOwner =
                canonicalSystemState?.isCanonicalOwnerAvailable == false
            val withCanonicalDashboard =
                if (
                    !malformedSystemOwner &&
                    canonicalDashboardCapabilityRepository.isEnabled(projectId)
                ) {
                    withCanonicalBacklog + dashboardCapability
                } else {
                    withCanonicalBacklog - dashboardCapability
                }
            val enabledCapabilities =
                if (
                    !malformedSystemOwner &&
                    canonicalExecutionLogRepository.isEnabled(projectId)
                ) {
                    withCanonicalDashboard + executionLogCapability
                } else {
                    withCanonicalDashboard - executionLogCapability
                }

            val allKnownCapabilities = ContextRoleRegistry.getAllKnownCapabilities() + enabledCapabilities
            val structureFeatures =
                allKnownCapabilities.associate { capId ->
                    featureLabelForCapability(capId) to enabledCapabilities.contains(capId)
                }.toSortedMap()

            _uiState.update { state ->
                state.copy(
                    contextId = projectId,
                    title = state.title.copy(presentedProject.name),
                    description = state.description.copy(presentedProject.description ?: ""),
                    relatedLinks = project?.relatedLinks ?: emptyList(),
                    tags = sanitizeTags(presentedProject.tags),
                    isReady = true,
                    isNewProject = false,
                    showCheckboxes = project?.showCheckboxes ?: state.showCheckboxes,
                    valueImportance = project?.valueImportance ?: 0f,
                    valueImpact = project?.valueImpact ?: 0f,
                    effort = project?.effort ?: 0f,
                    cost = project?.cost ?: 0f,
                    risk = project?.risk ?: 0f,
                    weightEffort = project?.weightEffort ?: 1f,
                    weightCost = project?.weightCost ?: 1f,
                    weightRisk = project?.weightRisk ?: 1f,
                    rawScore = project?.rawScore ?: 0f,
                    displayScore = project?.displayScore ?: 0,
                    scoringStatus = project?.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
                    isScoringEnabled =
                        project?.scoringStatus?.let {
                            it != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS
                        } ?: true,
                    basePresetCode = resolvedConfig.basePresetCode,
                    capabilityApplyMode = resolvedConfig.applyMode,
                    enabledCapabilityIds = enabledCapabilities,
                    experimentalCapabilityIds = resolvedConfig.experimentalCapabilityIds,
                    currentPresetLabel = presetLabel,
                    features = structureFeatures,
                    autoLinkSubprojects =
                        canonicalSystemState?.let {
                            it.direction?.configuration?.autoLinkChildWorkspaces ?: false
                        } ?: (structure?.enableAutoLinkSubprojects ?: true),
                    isProjectManagementEnabled = project?.isContextManagementEnabled == true,
                )
            }
        }

        fun onSave() {
            viewModelScope.launch {
                if (_uiState.value.title.text.isBlank()) {
                    _events.send(ContextSettingsEvent.NavigateBack("Назва проекту не може бути пустою"))
                    return@launch
                }
                if (saveProject()) {
                    _events.send(ContextSettingsEvent.NavigateBack("Збережено"))
                } else {
                    _events.send(ContextSettingsEvent.NavigateBack("Проект недоступний"))
                }
            }
        }

        private suspend fun saveProject(): Boolean {
            val projectId: String = savedStateHandle["projectId"] ?: return false
            val project = contextRepository.getContextById(projectId)
            val isReservedSystem = SystemContexts.isSystem(ContextId(projectId))

            if (isReservedSystem) {
                val presentation =
                    systemWorkspacePresentationContextProjector.resolvePresentation(
                        contextId = projectId,
                        context = project,
                    ) ?: return false

                contextRepository.updateContextPresentation(
                    contextId = presentation.id,
                    name = _uiState.value.title.text,
                    description = _uiState.value.description.text.ifEmpty { null },
                )
                contextRepository.updateContextTags(
                    contextId = projectId,
                    tags = sanitizeTags(_uiState.value.tags),
                )
                persistFeatureFlags()
                return true
            }

            project ?: return false
            contextRepository.updateContextSettings(
                contextId = projectId,
                update =
                    ContextSettingsUpdate(
                        name = _uiState.value.title.text,
                        description = _uiState.value.description.text.ifEmpty { null },
                        relatedLinks = _uiState.value.relatedLinks,
                        showCheckboxes = _uiState.value.showCheckboxes,
                        isContextManagementEnabled = _uiState.value.isProjectManagementEnabled,
                        valueImportance = _uiState.value.valueImportance,
                        valueImpact = _uiState.value.valueImpact,
                        effort = _uiState.value.effort,
                        cost = _uiState.value.cost,
                        risk = _uiState.value.risk,
                        weightEffort = _uiState.value.weightEffort,
                        weightCost = _uiState.value.weightCost,
                        weightRisk = _uiState.value.weightRisk,
                        rawScore = _uiState.value.rawScore,
                        displayScore = _uiState.value.displayScore,
                        scoringStatus = _uiState.value.scoringStatus,
                    ),
            )
            contextRepository.updateContextTags(
                contextId = projectId,
                tags = sanitizeTags(_uiState.value.tags),
            )
            persistFeatureFlags()
            return true
        }

        fun onTextChange(newValue: TextFieldValue) = _uiState.update { it.copy(title = newValue) }

        fun onDescriptionChange(newValue: TextFieldValue) = _uiState.update { it.copy(description = newValue) }

        override fun onValueImportanceChange(value: Float) = _uiState.update { it.copy(valueImportance = value) }

        override fun onValueImpactChange(value: Float) = _uiState.update { it.copy(valueImpact = value) }

        override fun onEffortChange(value: Float) = _uiState.update { it.copy(effort = value) }

        override fun onCostChange(value: Float) = _uiState.update { it.copy(cost = value) }

        override fun onRiskChange(value: Float) = _uiState.update { it.copy(risk = value) }

        override fun onWeightEffortChange(value: Float) = _uiState.update { it.copy(weightEffort = value) }

        override fun onWeightCostChange(value: Float) = _uiState.update { it.copy(weightCost = value) }

        override fun onWeightRiskChange(value: Float) = _uiState.update { it.copy(weightRisk = value) }

        override fun onScoringStatusChange(newStatus: String) {
            _uiState.update {
                it.copy(
                    scoringStatus = newStatus,
                    isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                )
            }
        }

        override fun onRelativeSizeChange(value: Int) {
        }

        override fun onBeaconProgressExpandedChange(isExpanded: Boolean) {
            _uiState.update { it.copy(isBeaconProgressExpanded = isExpanded) }
        }

        override fun onRelativeSizeExpandedChange(isExpanded: Boolean) {
            _uiState.update { it.copy(isRelativeSizeExpanded = isExpanded) }
        }

        fun openDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = true) }

        fun closeDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = false) }

        fun onDescriptionChangeAndCloseEditor(newDescription: String) {
            _uiState.update {
                it.copy(
                    description = it.description.copy(text = newDescription),
                    isDescriptionEditorOpen = false,
                )
            }
        }

        fun onTabSelected(index: Int) = _uiState.update { it.copy(selectedTabIndex = index) }

        fun onShowCheckboxesChange(show: Boolean) = _uiState.update { it.copy(showCheckboxes = show) }

        fun onAddTag(tag: String) {
            val normalizedTag = tag.trim()
            if (normalizedTag.isBlank()) return
            _uiState.update { state ->
                val updatedTags = (state.tags + normalizedTag).distinct()
                state.copy(tags = sanitizeTags(updatedTags))
            }
        }

        fun onRemoveTag(tag: String) = _uiState.update { it.copy(tags = sanitizeTags(it.tags - tag)) }

        fun onAddContextLink(contextId: String) {
            viewModelScope.launch {
                val context = contextRepository.getContextById(contextId)
                val presented =
                    systemWorkspacePresentationContextProjector.resolvePresentation(
                        contextId = contextId,
                        context = context,
                    ) ?: return@launch
                addRelatedLink(
                    RelatedLink(
                        type = LinkType.CONTEXT,
                        target = presented.id,
                        displayName = presented.name,
                    ),
                )
            }
        }

        fun onAttachmentSelected(attachmentId: String) {
            val option = _uiState.value.availableAttachments.firstOrNull { it.id == attachmentId } ?: return
            option.toRelatedLink()?.let(::addRelatedLink)
        }

        fun onRemoveLinkAssociation(targetToRemove: String) {
            _uiState.update {
                it.copy(
                    relatedLinks =
                        it.relatedLinks.filterNot { link ->
                            link.target == targetToRemove || relatedLinkIdentity(link) == targetToRemove
                        },
                )
            }
        }

        suspend fun createAttachmentForPicker(request: NewDocumentDraft): String? {
            val contextId = projectId ?: return null
            return when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId = noteDocumentRepository.createDocument(name = request.name.ifBlank { "Нова нотатка" }, contextId = contextId)
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId = musicNoteRepository.create(name = request.name.ifBlank { "Нові ноти" }, contextId = contextId)
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId = checklistRepository.createChecklist(name = request.name.ifBlank { "Новий чекліст" }, contextId = contextId)
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    target.takeIf { it.isNotBlank() }?.let {
                        val link = RelatedLink(type = LinkType.URL, target = it, displayName = request.name.trim().ifBlank { it })
                        addRelatedLink(link)
                        relatedLinkIdentity(link)
                    }
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    target.takeIf { it.isNotBlank() }?.let {
                        val link =
                            RelatedLink(
                                type = LinkType.OBSIDIAN,
                                target = it,
                                displayName = request.displayName.trim().ifBlank { it },
                                vault = request.vault,
                            )
                        addRelatedLink(link)
                        relatedLinkIdentity(link)
                    }
                }
            }
        }

        fun onProjectManagementChange(enabled: Boolean) {
            _uiState.update { it.copy(isProjectManagementEnabled = enabled) }
        }

        fun onAutoLinkSubprojectsChange(enabled: Boolean) {
            _uiState.update {
                it.copy(
                    autoLinkSubprojects = enabled,
                )
            }
        }

        fun onApplyPreset(code: String) {
            val pid = projectId ?: return
            viewModelScope.launch {
                structurePresetService.applyPresetToContext(pid, code)
                loadExistingProject(pid)
            }
        }

        /**
         * Перемикання стану окремої можливості (фічі)
         */
        fun onToggleFeature(
            key: String,
            enabled: Boolean,
        ) {
            if (key == "Dashboard") return

            // 1. Мапимо текстовий ключ UI на системний CapabilityId
            val capabilityId = featureLabelToCapabilityId(key)

            _uiState.update { state ->
                // 2. Оновлюємо список експериментальних можливостей для збереження в БД
                val updatedExperimentalIds =
                    state.experimentalCapabilityIds.toMutableList().apply {
                        if (enabled) {
                            if (!contains(capabilityId)) add(capabilityId)
                        } else {
                            remove(capabilityId)
                        }
                    }

                // 3. Копіюємо стан із синхронізацією всіх залежних полів
                state.copy(
                    // Оновлюємо мапу для UI списку
                    features = state.features + (key to enabled),
                    capabilityApplyMode = APPLY_MODE_OVERRIDE,
                    enabledCapabilityIds =
                        state.enabledCapabilityIds.toMutableSet().apply {
                            if (enabled) add(capabilityId) else remove(capabilityId)
                        },
                    // Оновлюємо список для майбутнього persistFeatureFlags()
                    experimentalCapabilityIds = updatedExperimentalIds,
                    // Синхронізуємо спеціальні прапорці стану
                    isProjectManagementEnabled = state.isProjectManagementEnabled,
                )
            }
            if (
                systemCapabilityAccess.handles(projectId.orEmpty()) &&
                capabilityId.raw in
                    setOf("backlog", "inbox", "direction", "connections", "inbox_sorting", "key_problems")
            ) {
                viewModelScope.launch {
                    val id = projectId.orEmpty()
                    try {
                        when (capabilityId.raw) {
                            "backlog" -> systemBacklogLifecycleAccess.setEnabled(id, enabled)
                            "inbox" -> systemCapabilityAccess.setInboxEnabled(id, enabled)
                            "direction" -> systemCapabilityAccess.setDirectionEnabled(id, enabled)
                            "connections" -> systemRemainingCapabilityAccess.setConnectionsEnabled(id, enabled)
                            "inbox_sorting" -> systemRemainingCapabilityAccess.setInboxSortingEnabled(id, enabled)
                            "key_problems" -> systemRemainingCapabilityAccess.setKeyProblemsEnabled(id, enabled)
                        }
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        loadExistingProject(id)
                    }
                }
            }
        }

        private fun featureLabelToCapabilityId(label: String): CapabilityId =
            when (label) {
                "Inbox" -> CapabilityId("inbox")
                "Log" -> CapabilityId("log")
                "Dashboard" -> CapabilityId("dashboard")
                "Backlog" -> CapabilityId("backlog")
                "Attachments", "Connections" -> CapabilityId("connections")
                "Issues" -> CapabilityId("key_problems")
                "Directions" -> CapabilityId("direction")
                else -> CapabilityId(label.lowercase().replace(" ", "_"))
            }

        private fun featureLabelForCapability(capabilityId: CapabilityId): String {
            return capabilityRegistry.get(capabilityId)?.label ?: capabilityId.raw
                .replace("_", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        /**
         * Збереження стану можливостей у БД
         */
        private suspend fun persistFeatureFlags() {
            val pid = projectId ?: return
            val currentState = _uiState.value

            if (SystemContexts.isSystem(ContextId(pid))) {
                if (systemCapabilityAccess.handles(pid)) {
                    systemCapabilityAccess.setInboxEnabled(
                        pid,
                        currentState.features["Inbox"] == true,
                    )
                    systemCapabilityAccess.setDirectionEnabled(
                        pid,
                        currentState.features["Direction"] == true,
                    )
                }
                if (systemRemainingCapabilityAccess.handles(pid)) {
                    systemRemainingCapabilityAccess.setConnectionsEnabled(
                        pid,
                        currentState.features["Connections"] == true ||
                            currentState.features["Attachments"] == true,
                    )
                    systemRemainingCapabilityAccess.setInboxSortingEnabled(
                        pid,
                        currentState.features["Inbox Sorting"] == true,
                    )
                    systemRemainingCapabilityAccess.setKeyProblemsEnabled(
                        pid,
                        currentState.features["Key Problems"] == true,
                    )
                }
                if (systemBacklogLifecycleAccess.handles(pid)) {
                    systemBacklogLifecycleAccess.setEnabled(
                        pid,
                        currentState.features["Backlog"] == true,
                    )
                }
                canonicalDashboardCapabilityRepository.setEnabled(
                    workspaceId = pid,
                    enabled = currentState.features["Dashboard"] == true,
                )
                canonicalExecutionLogRepository.setEnabled(
                    workspaceId = pid,
                    enabled = currentState.features["Log"] == true,
                )
                return
            }

            // Ordinary Context configuration remains Context-owned.
            val structure = contextStructureRepository.ensureStructure(pid)
            val isCanonicalSystem = systemCapabilityAccess.handles(pid)
            val canonicalCompatibilityIds =
                setOf(
                    CapabilityId("direction"),
                    CapabilityId("inbox_sorting"),
                    CapabilityId("key_problems"),
                )
            val experimentalCapabilityIds =
                if (isCanonicalSystem) {
                    (currentState.experimentalCapabilityIds - canonicalCompatibilityIds) +
                        structure.experimentalCapabilityIds.filter { it in canonicalCompatibilityIds }
                } else {
                    currentState.experimentalCapabilityIds
                }

            // 2. Створюємо оновлений об'єкт структури
            val updated =
                structure.copy(
                    // Зберігаємо код обраної ролі (пресета)
                    basePresetCode = currentState.basePresetCode,
                    applyMode = currentState.capabilityApplyMode,
                    // Зберігаємо список активованих ідентифікаторів можливостей
                    experimentalCapabilityIds = experimentalCapabilityIds,
                    // Підтримка legacy-колонок (для сумісності)
                    enableInbox =
                        if (isCanonicalSystem) structure.enableInbox else currentState.features["Inbox"] == true,
                    enableAdvanced = structure.enableAdvanced,
                    enableBacklog =
                        if (systemBacklogLifecycleAccess.handles(pid)) {
                            structure.enableBacklog
                        } else {
                            currentState.features["Backlog"] == true
                        },
                    enableAttachments =
                        if (isCanonicalSystem) {
                            structure.enableAttachments
                        } else {
                            currentState.features["Connections"] ?: currentState.features["Attachments"] == true
                        },
                    // Керується окремою вкладкою Direction settings.
                    // Тут не перезаписуємо, щоб не затирати актуальне значення.
                    enableAutoLinkSubprojects = structure.enableAutoLinkSubprojects,
                    removeInboxEntryAfterTagAutocopy = structure.removeInboxEntryAfterTagAutocopy,
                    removeBacklogEntryAfterTagAutocopy = structure.removeBacklogEntryAfterTagAutocopy,
                    updatedAt = System.currentTimeMillis(),
                )

            // 3. Записуємо в БД
            contextStructureRepository.updateStructure(updated)
            canonicalDashboardCapabilityRepository.setEnabled(
                workspaceId = pid,
                enabled = currentState.features["Dashboard"] == true,
            )
            canonicalExecutionLogRepository.setEnabled(
                workspaceId = pid,
                enabled = currentState.features["Log"] == true,
            )

            // 4. Оновлюємо внутрішній стан UI для миттєвої реакції екрана
            _uiState.update { state ->
                state.copy(
                    isProjectManagementEnabled = currentState.isProjectManagementEnabled,
                    autoLinkSubprojects = updated.enableAutoLinkSubprojects == true,
                )
            }
        }

        private companion object {
            private const val APPLY_MODE_OVERRIDE = "OVERRIDE"

            private fun sanitizeTags(tags: List<String>?): List<String> =
                tags.orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
        }

        override fun onSetReminder(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ) {
            val calendar =
                Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                }
            val newReminderTime = calendar.timeInMillis
            _uiState.update { it.copy(reminderTime = newReminderTime) }

            projectId?.let { id ->
                viewModelScope.launch {
                    reminderRepository.createReminder(id, "PROJECT", newReminderTime)
                }
            }
        }

        override fun onClearReminder() {
            _uiState.update { it.copy(reminderTime = null) }
            projectId?.let { id ->
                viewModelScope.launch {
                    reminderRepository.clearRemindersForEntity(id)
                }
            }
        }

        fun onOpenStructure() {
            projectId?.let { id ->
                viewModelScope.launch {
                    _events.send(ContextSettingsEvent.Navigate(NavTarget.ContextStructure(id)))
                }
            }
        }

        private fun addRelatedLink(link: RelatedLink) {
            _uiState.update { state ->
                if (state.relatedLinks.any { relatedLinkIdentity(it) == relatedLinkIdentity(link) }) state else state.copy(relatedLinks = state.relatedLinks + link)
            }
        }

        private fun resolveSelectedAttachmentIds(
            relatedLinks: List<RelatedLink>,
            options: List<AttachmentOption>,
        ): Set<String> {
            val linkKeys = relatedLinks.map(::relatedLinkIdentity).toSet()
            return options.filter { option ->
                option.toRelatedLink()?.let(::relatedLinkIdentity) in linkKeys
            }.mapTo(mutableSetOf()) { it.id }
        }

        private fun AttachmentOption.toRelatedLink(): RelatedLink? =
            when {
                linkType == LinkType.URL && !target.isNullOrBlank() ->
                    RelatedLink(type = LinkType.URL, target = target, displayName = name)
                linkType == LinkType.OBSIDIAN && !target.isNullOrBlank() ->
                    RelatedLink(type = LinkType.OBSIDIAN, target = target, displayName = name, vault = vault)
                attachmentType == BacklogItemTypeValues.NOTE_DOCUMENT && !entityId.isNullOrBlank() ->
                    RelatedLink(type = LinkType.NOTE_DOCUMENT, target = entityId, displayName = name)
                attachmentType == BacklogItemTypeValues.CHECKLIST && !entityId.isNullOrBlank() ->
                    RelatedLink(type = LinkType.CHECKLIST, target = entityId, displayName = name)
                attachmentType == BacklogItemTypeValues.MUSIC_NOTE && !entityId.isNullOrBlank() ->
                    RelatedLink(type = LinkType.MUSIC_NOTE, target = entityId, displayName = name)
                else -> null
            }
    }

private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption {
    val relatedLink =
        linkDisplayName?.let { json ->
            runCatching { Gson().fromJson(json, RelatedLink::class.java) }.getOrNull()
        }
    val linkLabel =
        relatedLink?.displayName?.takeIf { it.isNotBlank() }
            ?: relatedLink?.target?.takeIf { it.isNotBlank() }
    val label =
        noteName?.takeIf { it.isNotBlank() }
            ?: musicNoteName?.takeIf { it.isNotBlank() }
            ?: checklistName?.takeIf { it.isNotBlank() }
            ?: scriptName?.takeIf { it.isNotBlank() }
            ?: linkLabel
            ?: contextName
            ?: "Attachment ${id.takeLast(4)}"

    return AttachmentOption(
        id = id,
        name = label,
        linkType = relatedLink?.type,
        attachmentType = attachmentType,
        entityId = entityId,
        target = relatedLink?.target,
        vault = relatedLink?.vault,
    )
}

private fun relatedLinkIdentity(link: RelatedLink): String = "${link.type}:${link.target}:${link.vault.orEmpty()}"
