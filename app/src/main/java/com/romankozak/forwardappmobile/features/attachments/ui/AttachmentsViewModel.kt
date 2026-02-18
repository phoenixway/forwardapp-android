package com.romankozak.forwardappmobile.features.attachments.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.attachments.ui.context.AttachmentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

enum class AddAttachmentDialogType {
    NONE,
    WEB_LINK,
    OBSIDIAN_LINK,
}

enum class PendingAttachmentType {
    NONE,
    PROJECT_LINK,
    PROJECT_SHORTCUT,
}

data class AttachmentsUiState(
    val project: Context? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: Long? = null,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val isScoringEnabled: Boolean = true,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val showAddAttachmentDialog: AddAttachmentDialogType = AddAttachmentDialogType.NONE,
    val pendingAttachmentType: PendingAttachmentType = PendingAttachmentType.NONE,
)

@HiltViewModel
class AttachmentsViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val settingsRepository: SettingsRepository,
        private val alarmScheduler: AlarmScheduler,
        private val recentItemsRepository: RecentItemsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contextId: StateFlow<String> = savedStateHandle.getStateFlow("contextId", "")

        private val _uiState = MutableStateFlow(AttachmentsUiState())
        val uiState: StateFlow<AttachmentsUiState> = _uiState.asStateFlow()

        private val _uiEventFlow = Channel<UiEvent>()
        val uiEventFlow = _uiEventFlow.receiveAsFlow()

        private var originalProject: Context? = null

        init {
            viewModelScope.launch {
                savedStateHandle.getLiveData<String>("list_chooser_result").asFlow().collect { result ->
                    if (result != null) {
                        Log.d("AttachmentsViewModel", "Result received: $result")
                        when (uiState.value.pendingAttachmentType) {
                            PendingAttachmentType.PROJECT_LINK -> onAddProjectLink(result)
                            PendingAttachmentType.PROJECT_SHORTCUT -> onAddProjectShortcut(result)
                            PendingAttachmentType.NONE -> {
                                Log.w("AttachmentsViewModel", "Received a list chooser result but no pending attachment type.")
                            }
                        }
                        savedStateHandle.remove<String>("list_chooser_result")
                        Log.d("AttachmentsViewModel", "Result removed from SavedStateHandle.")
                    }
                }
            }

            viewModelScope.launch {
                val loadedProject = contextRepository.getContextById(contextId.value)
                originalProject = loadedProject
                _uiState.update {
                    if (loadedProject != null) {
                        it.copy(
                            project = loadedProject,
                            name = loadedProject.name,
                            tags = loadedProject.tags?.filter { it.isNotBlank() } ?: emptyList(),
                            scoringStatus = loadedProject.scoringStatus,
                            isScoringEnabled = loadedProject.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                            valueImportance = loadedProject.valueImportance,
                            valueImpact = loadedProject.valueImpact,
                            effort = loadedProject.effort,
                            cost = loadedProject.cost,
                            risk = loadedProject.risk,
                            weightEffort = loadedProject.weightEffort,
                            weightCost = loadedProject.weightCost,
                            weightRisk = loadedProject.weightRisk,
                            rawScore = loadedProject.rawScore,
                        )
                    } else {
                        it
                    }
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val attachments: StateFlow<List<BacklogItemContent>> =
            contextId.flatMapLatest { contextId ->
                if (contextId.isNotEmpty()) {
                    contextRepository.getContextContentStream(contextId).map { content ->
                        content.filter { item ->
                            item is BacklogItemContent.LinkItem ||
                                item is BacklogItemContent.NoteDocumentItem ||
                                item is BacklogItemContent.MusicNoteItem ||
                                item is BacklogItemContent.ChecklistItem
                        }
                    }
                } else {
                    flowOf(emptyList())
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun deleteAttachment(attachment: BacklogItemContent) {
            viewModelScope.launch {
                val currentcontextId = contextId.value
                if (currentcontextId.isNotEmpty()) {
                    contextRepository.deleteListItemsFromContext(currentcontextId, listOf(attachment.backlogItem.id))
                }
            }
        }

        fun onAddAttachment(type: AttachmentType) {
            when (type) {
                AttachmentType.NOTES -> {
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.NoteDocumentEdit(contextId = contextId.value),
                            ),
                        )
                    }
                }
                AttachmentType.CHECKLIST -> {
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.Checklist(contextId = contextId.value),
                            ),
                        )
                    }
                }
                AttachmentType.MUSIC_NOTES -> {
                    viewModelScope.launch {
                        val musicNoteId =
                            musicNoteRepository.create(
                                name = "Нові ноти",
                                contextId = contextId.value,
                                content = "",
                            )
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.MusicNote(
                                    id = musicNoteId,
                                    startEdit = true,
                                ),
                            ),
                        )
                    }
                }
                AttachmentType.WEB_LINK -> {
                    _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.WEB_LINK) }
                }
                AttachmentType.OBSIDIAN_LINK -> {
                    _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.OBSIDIAN_LINK) }
                }
                AttachmentType.CONTEXT_LINK -> {
                    _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.PROJECT_LINK) }
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.ListChooser(
                                    title = "Add link to another project",
                                    currentParentId = contextId.value,
                                ),
                            ),
                        )
                    }
                }
                AttachmentType.CONTEXT_SHORTCUT -> {
                    _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.PROJECT_SHORTCUT) }
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.ListChooser(
                                    title = "Add shortcut to another project",
                                    currentParentId = contextId.value,
                                ),
                            ),
                        )
                    }
                }
            }
        }

        fun onLinkClick(link: RelatedLink) {
            viewModelScope.launch {
                when (link.type) {
                    LinkType.CONTEXT -> {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.ContextDetail(contextId = link.target),
                            ),
                        )
                    }
                    LinkType.URL -> {
                        _uiEventFlow.send(UiEvent.OpenUri(link.target))
                    }
                    LinkType.OBSIDIAN -> {
                        recentItemsRepository.logObsidianLinkAccess(link)
                        val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                        val encodedNoteName = URLEncoder.encode(link.target, "UTF-8")
                        val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
                        _uiEventFlow.send(UiEvent.OpenUri(uri))
                    }
                    null -> {
                        // Do nothing
                    }
                }
            }
        }

        fun onDismissAddAttachmentDialog() {
            _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.NONE) }
        }

        fun onAddWebLink(
            url: String,
            name: String,
        ) {
            viewModelScope.launch {
                val link =
                    RelatedLink(
                        type = LinkType.URL,
                        target = url,
                        displayName = name,
                    )
                if (contextId.value.isNotEmpty()) {
                    contextRepository.addLinkItemToContextFromLink(contextId.value, link)
                }
                onDismissAddAttachmentDialog()
            }
        }

        fun onAddObsidianLink(
            url: String,
            name: String,
        ) {
            viewModelScope.launch {
                val link =
                    RelatedLink(
                        type = LinkType.OBSIDIAN,
                        target = url,
                        displayName = name,
                    )
                if (contextId.value.isNotEmpty()) {
                    contextRepository.addLinkItemToContextFromLink(contextId.value, link)
                }
                onDismissAddAttachmentDialog()
            }
        }

        fun onAddProjectLink(contextId: String) {
            Log.d("AttachmentsViewModel", "onAddProjectLink called with contextId: $contextId")
            viewModelScope.launch {
                val project = contextRepository.getContextById(contextId)
                Log.d("AttachmentsViewModel", "project: $project")
                if (project != null) {
                    val link =
                        RelatedLink(
                            type = LinkType.CONTEXT,
                            target = contextId,
                            displayName = project.name,
                        )
                    if (this@AttachmentsViewModel.contextId.value.isNotEmpty()) {
                        contextRepository.addLinkItemToContextFromLink(this@AttachmentsViewModel.contextId.value, link)
                    }
                }
                _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.NONE) }
            }
        }

        fun onAddProjectShortcut(contextId: String) {
            viewModelScope.launch {
                contextRepository.addContextLinkToContext(contextId, this@AttachmentsViewModel.contextId.value)
                _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.NONE) }
            }
        }

        fun onNameChange(newName: String) {
            _uiState.update { it.copy(name = newName) }
        }

        fun onTagsChange(newTags: List<String>) {
            _uiState.update { it.copy(tags = newTags.filter { it.isNotBlank() }) }
        }

        fun onSave(): Context? {
            if (_uiState.value.name.isBlank()) return null

            val state = _uiState.value
            val currentProject = originalProject ?: return null

            val tempProject =
                currentProject.copy(
                    name = state.name,
                    tags = state.tags.filter { it.isNotBlank() }.map { it.trim() },
                    updatedAt = System.currentTimeMillis(),
                    scoringStatus = state.scoringStatus,
                    valueImportance = state.valueImportance,
                    valueImpact = state.valueImpact,
                    effort = state.effort,
                    cost = state.cost,
                    risk = state.risk,
                    weightEffort = state.weightEffort,
                    weightCost = state.weightCost,
                    weightRisk = state.weightRisk,
                )

            val updatedProject = GoalScoringManager.calculateScoresForContext(tempProject)

            viewModelScope.launch {
                contextRepository.updateContext(updatedProject)
            }
            return updatedProject
        }

        fun onScoringStatusChange(newStatus: String) {
            _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS) }
            updateScores()
        }

        fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }

        fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }

        fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }

        fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }

        fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }

        fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }

        fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }

        fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

        private fun onScoringParameterChange(update: (AttachmentsUiState) -> AttachmentsUiState) {
            _uiState.update(update)
            if (_uiState.value.scoringStatus == ScoringStatusValues.NOT_ASSESSED) {
                _uiState.update { it.copy(scoringStatus = ScoringStatusValues.ASSESSED) }
            }
            updateScores()
        }

        private fun updateScores() {
            val state = _uiState.value
            val tempProject =
                (state.project ?: return).copy(
                    scoringStatus = state.scoringStatus,
                    valueImportance = state.valueImportance,
                    valueImpact = state.valueImpact,
                    effort = state.effort,
                    cost = state.cost,
                    risk = state.risk,
                    weightEffort = state.weightEffort,
                    weightCost = state.weightCost,
                    weightRisk = state.weightRisk,
                )
            val updatedProject = GoalScoringManager.calculateScoresForContext(tempProject)
            _uiState.update { it.copy(rawScore = updatedProject.rawScore) }
        }
    }
