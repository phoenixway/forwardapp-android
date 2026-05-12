package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
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
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
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
        private val capabilityRegistry: CapabilityRegistry,
        private val capabilitySettingsRegistry: CapabilitySettingsRegistry,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
    ) : ViewModel(), EvaluationTabActions, RemindersTabActions {
        private val projectId: String? = savedStateHandle["projectId"]
        private val allContexts =
            contextRepository.getAllContextsFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
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
                combine(allContexts, allAttachmentOptions, uiState) { contexts, attachments, state ->
                    Triple(contexts, attachments, state.relatedLinks)
                }.collect { (contexts, attachments, relatedLinks) ->
                    val contextOptions =
                        contexts.map { context ->
                            ProjectOption(id = context.id, name = context.name, parentId = context.parentId)
                        }
                    val attachmentOptions = attachments.mapNotNull { it.toAttachmentOption() }.filterNot { it.linkType == LinkType.CONTEXT }
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
            // 1. Отримуємо основні дані проекту та його структуру
            val project = contextRepository.getContextById(projectId)
            val structure = contextStructureRepository.getStructureByContext(projectId)

            if (project != null) {
                // 2. Резолвимо назву пресета (ролі) для відображення в UI
                val presetLabel =
                    structure?.basePresetCode?.let { code ->
                        structurePresetDao.getByCode(code)?.label
                    } ?: "Стандартний (Default)"

                // 3. Формуємо мапу фіч, використовуючи CapabilityGate для перевірки реального стану
                val allKnownCapabilities = ContextRoleRegistry.getAllKnownCapabilities()
                val structureFeatures =
                    allKnownCapabilities.associate { capId ->
                        val key = featureLabelForCapability(capId)
                        key to isEnabledForConfig(capId, structure)
                    }.toSortedMap()

                // 4. Оновлюємо стан UI одним атомарним блоком
                _uiState.update { state ->
                    state.copy(
                        contextId = project.id,
                        // Метадані проекту
                        title = state.title.copy(project.name),
                        description = state.description.copy(project.description ?: ""),
                        relatedLinks = project.relatedLinks ?: emptyList(),
                        tags = project.tags ?: emptyList(),
                        isReady = true,
                        isNewProject = false,
                        showCheckboxes = project.showCheckboxes,
                        // Скоринг та оцінка
                        valueImportance = project.valueImportance,
                        valueImpact = project.valueImpact,
                        effort = project.effort,
                        cost = project.cost,
                        risk = project.risk,
                        weightEffort = project.weightEffort,
                        weightCost = project.weightCost,
                        weightRisk = project.weightRisk,
                        rawScore = project.rawScore,
                        displayScore = project.displayScore,
                        scoringStatus = project.scoringStatus,
                        isScoringEnabled = project.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                        // Системна конфігурація
                        basePresetCode = structure?.basePresetCode,
                        capabilityApplyMode = structure?.applyMode ?: APPLY_MODE_ADDITIVE,
                        enabledCapabilityIds = structureFeatures.filterValues { it }.keys.map(::featureLabelToCapabilityId).toSet(),
                        experimentalCapabilityIds = structure?.experimentalCapabilityIds ?: emptyList(),
                        currentPresetLabel = presetLabel,
                        features = structureFeatures,
                        // Синхронізація ключових прапорців для UI-логіки
                        autoLinkSubprojects = structure?.enableAutoLinkSubprojects ?: true,
                        isProjectManagementEnabled = project.isContextManagementEnabled == true,
                    )
                }
            } else {
                // Якщо проект видалено або не знайдено — повертаємо користувача назад
                _events.send(ContextSettingsEvent.NavigateBack("Проект не знайдено"))
            }
        }

        /**
         * Перевірка стану можливості через конфігурацію (роль + експериментальні ID + legacy прапорці)
         */
        private fun isEnabledForConfig(
            id: CapabilityId,
            config: ContextConfiguration?,
        ): Boolean {
            if (id.raw == "dashboard") return true
            if (config == null) return true

            // Перевірка через роль
            val useRoleDefaults = !config.applyMode.equals(APPLY_MODE_OVERRIDE, ignoreCase = true)
            val roleCapabilities = if (useRoleDefaults) ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode) else emptySet()
            val enabledByRole = roleCapabilities.contains(id)

            // Перевірка через legacy прапорці
            val isLegacy =
                when (id.raw) {
                    "inbox" -> config.enableInbox ?: enabledByRole
                    "log" -> config.enableLog ?: enabledByRole
                    "artifact" -> config.enableArtifact ?: enabledByRole
                    "dashboard" -> config.enableDashboard ?: enabledByRole
                    "backlog" -> config.enableBacklog ?: enabledByRole
                    "attachments" -> config.enableAttachments ?: enabledByRole
                    "connections" -> config.enableAttachments ?: enabledByRole
                    else -> false
                }

            // Перевірка через експериментальні ID або старі прапорці
            return enabledByRole || config.experimentalCapabilityIds.contains(id) || isLegacy
        }

        fun onSave() {
            viewModelScope.launch {
                if (_uiState.value.title.text.isBlank()) {
                    _events.send(ContextSettingsEvent.NavigateBack("Назва проекту не може бути пустою"))
                    return@launch
                }
                saveProject()
                _events.send(ContextSettingsEvent.NavigateBack("Збережено"))
            }
        }

        private suspend fun saveProject() {
            val projectId: String = savedStateHandle["projectId"] ?: return
            val project = contextRepository.getContextById(projectId) ?: return

            val updatedProject =
                project.copy(
                    name = _uiState.value.title.text,
                    description = _uiState.value.description.text.ifEmpty { null },
                    relatedLinks = _uiState.value.relatedLinks,
                    tags = _uiState.value.tags,
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
                )
            contextRepository.updateContext(updatedProject)
            persistFeatureFlags()
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

        fun onAddTag(tag: String) = _uiState.update { it.copy(tags = it.tags + tag) }

        fun onRemoveTag(tag: String) = _uiState.update { it.copy(tags = it.tags - tag) }

        fun onAddContextLink(contextId: String) {
            viewModelScope.launch {
                val context = contextRepository.getContextById(contextId) ?: return@launch
                addRelatedLink(
                    RelatedLink(
                        type = LinkType.CONTEXT,
                        target = context.id,
                        displayName = context.name,
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
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "Новий журнал" },
                            contextId = contextId,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
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
                // 1. Застосовуємо пресет (це оновить basePresetCode та, можливо, структуру)
                structurePresetService.applyPresetToContext(pid, code)

                // 2. Отримуємо можливості, що відповідають цьому пресету
                val presetCapabilities = ContextRoleRegistry.getCapabilitiesForRole(code)

                // 3. Розділяємо можливості на legacy та експериментальні
                val allKnownLegacyCaps =
                    setOf(
                        "inbox",
                        "log",
                        "artifact",
                        "dashboard",
                        "backlog",
                        "attachments",
                        "connections",
                    )
                val experimentalIdsFromPreset = presetCapabilities.filter { it.raw !in allKnownLegacyCaps }

                // 4. Оновлюємо конфігурацію в БД, щоб прапорці відповідали пресету
                val structure = contextStructureRepository.ensureStructure(pid)
                val preset = structurePresetDao.getByCode(code)
                val updatedStructure =
                    structure.copy(
                        basePresetCode = code,
                        applyMode = APPLY_MODE_ADDITIVE,
                        // Оновлення legacy-прапорців
                        enableInbox = presetCapabilities.contains(CapabilityId("inbox")),
                        enableLog = presetCapabilities.contains(CapabilityId("log")),
                        enableArtifact = preset?.enableArtifact ?: presetCapabilities.contains(CapabilityId("artifact")),
                        enableAdvanced = preset?.enableAdvanced,
                        enableDashboard = true,
                        enableBacklog = presetCapabilities.contains(CapabilityId("backlog")),
                        enableAttachments =
                            presetCapabilities.contains(CapabilityId("connections")) ||
                                presetCapabilities.contains(CapabilityId("attachments")),
                        enableAutoLinkSubprojects = structure.enableAutoLinkSubprojects,
                        removeInboxEntryAfterTagAutocopy = structure.removeInboxEntryAfterTagAutocopy,
                        // Оновлення списку експериментальних ID
                        experimentalCapabilityIds = experimentalIdsFromPreset,
                    )
                contextStructureRepository.updateStructure(updatedStructure)

                // 5. Перезавантажуємо дані, щоб UI оновився згідно зі змінами
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
        }

        private fun featureLabelToCapabilityId(label: String): CapabilityId =
            when (label) {
                "Inbox" -> CapabilityId("inbox")
                "Log" -> CapabilityId("log")
                "Artifact" -> CapabilityId("artifact")
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

            // 1. Отримуємо існуючу структуру або створюємо нову
            val structure = contextStructureRepository.ensureStructure(pid)

            // 2. Створюємо оновлений об'єкт структури
            val updated =
                structure.copy(
                    // Зберігаємо код обраної ролі (пресета)
                    basePresetCode = currentState.basePresetCode,
                    applyMode = currentState.capabilityApplyMode,
                    // Зберігаємо список активованих ідентифікаторів можливостей
                    experimentalCapabilityIds = currentState.experimentalCapabilityIds,
                    // Підтримка legacy-колонок (для сумісності)
                    enableInbox = currentState.features["Inbox"] == true,
                    enableLog = currentState.features["Log"] == true,
                    enableArtifact = currentState.features["Artifact"] == true,
                    enableAdvanced = currentState.isProjectManagementEnabled,
                    enableDashboard = currentState.features["Dashboard"] == true,
                    enableBacklog = currentState.features["Backlog"] == true,
                    enableAttachments = currentState.features["Connections"] ?: currentState.features["Attachments"] == true,
                    // Керується окремою вкладкою Direction settings.
                    // Тут не перезаписуємо, щоб не затирати актуальне значення.
                    enableAutoLinkSubprojects = structure.enableAutoLinkSubprojects,
                    removeInboxEntryAfterTagAutocopy = structure.removeInboxEntryAfterTagAutocopy,
                    updatedAt = System.currentTimeMillis(),
                )

            // 3. Записуємо в БД
            contextStructureRepository.updateStructure(updated)

            // 4. Оновлюємо внутрішній стан UI для миттєвої реакції екрана
            _uiState.update { state ->
                state.copy(
                    isProjectManagementEnabled = currentState.isProjectManagementEnabled,
                    autoLinkSubprojects = updated.enableAutoLinkSubprojects == true,
                )
            }
        }

        private companion object {
            private const val APPLY_MODE_ADDITIVE = "ADDITIVE"
            private const val APPLY_MODE_OVERRIDE = "OVERRIDE"
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
                attachmentType == BacklogItemTypeValues.JOURNAL_DOCUMENT && !entityId.isNullOrBlank() ->
                    RelatedLink(type = LinkType.JOURNAL_DOCUMENT, target = entityId, displayName = name)
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
