package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.goalproperties

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.ContextSettingsEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.TagUtils
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GoalSettingsViewModel
    @Inject
    constructor(
        private val goalRepository: GoalRepository,
        private val contextRepository: ContextRepository,
        private val contextMarkerHandler: ContextMarkerHandler,
        private val reminderRepository: ReminderRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val settingsRepository: SettingsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel(), EvaluationTabActions, RemindersTabActions {
        private val goalId: String? = savedStateHandle["goalId"]
        private val initialProjectId: String? = savedStateHandle["projectId"]
        private val allContexts =
            contextRepository.getAllContextsFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private val allAttachmentOptions =
            attachmentsRepository.getAttachmentLibraryItems()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        private val _uiState = MutableStateFlow(GoalSettingsUiState())
        val uiState: StateFlow<GoalSettingsUiState> = _uiState.asStateFlow()

        private val _events = Channel<ContextSettingsEvent>()
        val events = _events.receiveAsFlow()

        private var currentGoal: Goal? = null

        val allContextMarkerNames: StateFlow<List<String>> = contextMarkerHandler.contextMarkerNamesFlow

        private val _allTags = MutableStateFlow<List<String>>(emptyList())
        val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

        init {
            viewModelScope.launch {
                val goalId: String? = savedStateHandle["goalId"]

                contextMarkerHandler.initialize()
                loadAvailableTags()

                if (goalId != null) {
                    loadExistingGoal(goalId)
                    reminderRepository.getRemindersForEntityFlow(goalId).collect { reminders ->
                        _uiState.update { it.copy(reminderTime = reminders.firstOrNull()?.reminderTime) }
                    }
                } else {
                    createNewGoal()
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

            viewModelScope.launch {
                settingsRepository.goalBeaconProgressExpandedFlow.collect { isExpanded ->
                    _uiState.update { it.copy(isBeaconProgressExpanded = isExpanded) }
                }
            }

            viewModelScope.launch {
                settingsRepository.goalRelativeSizeExpandedFlow.collect { isExpanded ->
                    _uiState.update { it.copy(isRelativeSizeExpanded = isExpanded) }
                }
            }
        }

        private suspend fun loadAvailableTags() {
            try {
                val allGoals = goalRepository.getAllGoals()
                val allTagsFromGoals =
                    allGoals.flatMap { goal ->
                        TagUtils.extractTags(goal.text).map { it.fullTag }
                    }.distinct().sorted()

                _allTags.value = allTagsFromGoals
            } catch (e: Exception) {
                Log.e("GoalSettingsVM", "Error loading tags", e)
                _allTags.value = emptyList()
            }
        }

        fun onListChooserResult(projectId: String) {
            if (projectId.isBlank() || projectId == "root") return
            onAddProjectAssociation(projectId)
        }

        private suspend fun loadExistingGoal(goalId: String) {
            val goal = goalRepository.getGoalById(goalId)
            if (goal != null) {
                currentGoal = goal
                _uiState.update {
                    it.copy(
                        title = it.title.copy(goal.text),
                        description = it.description.copy(goal.description ?: ""),
                        relatedLinks = goal.relatedLinks ?: emptyList(),
                        isReady = true,
                        isNewGoal = false,
                        createdAt = goal.createdAt,
                        updatedAt = goal.updatedAt,
                        valueImportance = goal.valueImportance,
                        valueImpact = goal.valueImpact,
                        effort = goal.effort,
                        cost = goal.cost,
                        risk = goal.risk,
                        weightEffort = goal.weightEffort,
                        weightCost = goal.weightCost,
                        weightRisk = goal.weightRisk,
                        rawScore = goal.rawScore,
                        displayScore = goal.displayScore,
                        scoringStatus = goal.scoringStatus,
                        isScoringEnabled = goal.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                        relativeSize = goal.relativeSize,
                        goalStatus = GoalStatusValues.normalize(goal.goalStatus),
                    )
                }
            } else {
                // TODO: _events.send(ProjectSettingsEvent.NavigateBack("Ціль не знайдено"))
            }
        }

        private fun createNewGoal() {
            _uiState.update {
                it.copy(
                    isReady = true,
                    isNewGoal = true,
                    scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                    isScoringEnabled = true,
                    goalStatus = GoalStatusValues.ACTIVE,
                )
            }
            updateScores()
        }

        fun onSave() {
            viewModelScope.launch {
                if (_uiState.value.title.text.isBlank()) {
                    // TODO: _events.send(ProjectSettingsEvent.NavigateBack("Назва цілі не може бути пустою"))
                    return@launch
                }

                saveGoal()

                loadAvailableTags()

                _events.send(ContextSettingsEvent.NavigateBack("Збережено"))
            }
        }

        private suspend fun saveGoal() {
            val goalFromState = buildGoalFromState(_uiState.value)
            val goalToSave = GoalScoringManager.calculateScores(goalFromState)

            if (currentGoal != null) {
                goalRepository.updateGoal(goalToSave)
                contextMarkerHandler.syncContextsOnUpdate(oldGoal = currentGoal!!, newGoal = goalToSave)
            } else {
                initialProjectId ?: return
                goalRepository.addGoalToContext(goalToSave, initialProjectId)
            }
        }

        private fun buildGoalFromState(state: GoalSettingsUiState): Goal {
            val currentTime = System.currentTimeMillis()
            val descriptionToSave = state.description.text.ifEmpty { null }

            val baseGoal =
                currentGoal ?: Goal(
                    id = UUID.randomUUID().toString(),
                    text = "",
                    completed = false,
                    goalStatus = GoalStatusValues.ACTIVE,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                )

            return baseGoal.copy(
                text = state.title.text,
                description = descriptionToSave,
                completed = GoalStatusValues.isTerminal(state.goalStatus),
                goalStatus = GoalStatusValues.normalize(state.goalStatus),
                updatedAt = currentTime,
                relatedLinks = state.relatedLinks,
                valueImportance = state.valueImportance,
                valueImpact = state.valueImpact,
                effort = state.effort,
                cost = state.cost,
                risk = state.risk,
                weightEffort = state.weightEffort,
                weightCost = state.weightCost,
                weightRisk = state.weightRisk,
                scoringStatus = state.scoringStatus,
                relativeSize = state.relativeSize,
            )
        }

        fun onTextChange(newValue: TextFieldValue) = _uiState.update { it.copy(title = newValue) }

        fun onDescriptionChange(newValue: TextFieldValue) = _uiState.update { it.copy(description = newValue) }

        fun onGoalStatusChange(status: String) {
            _uiState.update { it.copy(goalStatus = GoalStatusValues.normalize(status)) }
        }

        override fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }

        override fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }

        override fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }

        override fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }

        override fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }

        override fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }

        override fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }

        override fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

        override fun onScoringStatusChange(newStatus: String) {
            _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS) }
            updateScores()
        }

        override fun onRelativeSizeChange(value: Int) {
            _uiState.update { it.copy(relativeSize = value.coerceIn(0, 5)) }
        }

        override fun onBeaconProgressExpandedChange(isExpanded: Boolean) {
            _uiState.update { it.copy(isBeaconProgressExpanded = isExpanded) }
            viewModelScope.launch {
                settingsRepository.saveGoalBeaconProgressExpanded(isExpanded)
            }
        }

        override fun onRelativeSizeExpandedChange(isExpanded: Boolean) {
            _uiState.update { it.copy(isRelativeSizeExpanded = isExpanded) }
            viewModelScope.launch {
                settingsRepository.saveGoalRelativeSizeExpanded(isExpanded)
            }
        }

        private fun onScoringParameterChange(update: (GoalSettingsUiState) -> GoalSettingsUiState) {
            _uiState.update(update)
            if (_uiState.value.scoringStatus == ScoringStatusValues.NOT_ASSESSED) {
                _uiState.update { it.copy(scoringStatus = ScoringStatusValues.ASSESSED) }
            }
            updateScores()
        }

        private fun updateScores() {
            val tempGoal = buildGoalFromState(_uiState.value)
            val updatedGoal = GoalScoringManager.calculateScores(tempGoal)
            _uiState.update { it.copy(rawScore = updatedGoal.rawScore, displayScore = updatedGoal.displayScore) }
        }

        fun onAddLinkRequest() {
            viewModelScope.launch {
                val disabledIds =
                    _uiState.value.relatedLinks
                        .filter { it.type == LinkType.CONTEXT }
                        .joinToString(",") { it.target }
                _events.send(
                    ContextSettingsEvent.Navigate(
                        NavTarget.ListChooser(
                            title = "Додати посилання на проект",
                            disabledIds = disabledIds.ifBlank { null },
                        ),
                    ),
                )
            }
        }

        private fun onAddProjectAssociation(projectId: String) {
            viewModelScope.launch {
                val projectName = contextRepository.getContextById(projectId)?.name
                val newLink =
                    RelatedLink(
                        type = LinkType.CONTEXT,
                        target = projectId,
                        displayName = projectName,
                    )

                _uiState.update {
                    if (it.relatedLinks.any { link -> link.target == projectId && link.type == LinkType.CONTEXT }) {
                        it
                    } else {
                        it.copy(relatedLinks = it.relatedLinks + newLink)
                    }
                }
            }
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

        fun onAttachmentSelected(attachmentId: String) {
            val option = _uiState.value.availableAttachments.firstOrNull { it.id == attachmentId } ?: return
            val newLink = option.toRelatedLink() ?: return
            _uiState.update { state ->
                if (state.relatedLinks.any { relatedLinkIdentity(it) == relatedLinkIdentity(newLink) }) {
                    state
                } else {
                    state.copy(relatedLinks = state.relatedLinks + newLink)
                }
            }
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

        fun onAddWebLinkRequest() {
        }

        suspend fun createAttachmentForPicker(request: NewDocumentDraft): String? {
            val contextId = resolveOwnerContextId() ?: return null
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

        fun onAddObsidianLinkRequest() {
        }

        fun onTabSelected(index: Int) {
            _uiState.update { it.copy(selectedTabIndex = index) }
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

            goalId?.let {
                viewModelScope.launch {
                    reminderRepository.createReminder(it, "GOAL", newReminderTime)
                }
            }
        }

        override fun onClearReminder() {
            _uiState.update { it.copy(reminderTime = null) }
            goalId?.let {
                viewModelScope.launch {
                    reminderRepository.clearRemindersForEntity(it)
                }
            }
        }

        private suspend fun resolveOwnerContextId(): String? =
            initialProjectId ?: goalId?.let { contextRepository.findContextIdForGoal(it) }

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
