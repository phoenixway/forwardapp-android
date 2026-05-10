package com.romankozak.forwardappmobile.features.attachments.ui.context

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar
import javax.inject.Inject

enum class AddAttachmentDialogType {
    NONE,
    WEB_LINK,
    OBSIDIAN_LINK,
}

enum class PendingAttachmentType {
    NONE,
    CONTEXT_LINK,
    CONTEXT_SHORTCUT,
}

data class AttachmentsUiState(
    val context: Context? = null,
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
        private val settingsRepository: SettingsRepository,
        private val alarmScheduler: AlarmScheduler,
        private val recentItemsRepository: RecentItemsRepository,
        private val listItemRepository: ListItemRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val goalScoringManager: GoalScoringManager,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contextId: StateFlow<String> = savedStateHandle.getStateFlow("contextId", "")

        private val _uiState = MutableStateFlow(AttachmentsUiState())
        val uiState: StateFlow<AttachmentsUiState> = _uiState.asStateFlow()

        private val _uiEventFlow = Channel<UiEvent>()
        val uiEventFlow = _uiEventFlow.receiveAsFlow()

        private var originalContext: Context? = null

        init {
            viewModelScope.launch {
                savedStateHandle.getLiveData<String>("list_chooser_result").asFlow().collect { result ->
                    if (result != null) {
                        android.util.Log.d("AttachmentsViewModel", "Result received: $result")
                        when (uiState.value.pendingAttachmentType) {
                            PendingAttachmentType.CONTEXT_LINK -> onAddContextLink(result)
                            PendingAttachmentType.CONTEXT_SHORTCUT -> onAddContextShortcut(result)
                            PendingAttachmentType.NONE -> {
                                android.util.Log.w("AttachmentsViewModel", "Received a list chooser result but no pending attachment type.")
                            }
                        }
                        savedStateHandle.remove<String>("list_chooser_result")
                        android.util.Log.d("AttachmentsViewModel", "Result removed from SavedStateHandle.")
                    }
                }
            }

            viewModelScope.launch {
                val loadedContext = contextRepository.getContextById(contextId.value)
                originalContext = loadedContext
                _uiState.update {
                    if (loadedContext != null) {
                        it.copy(
                            context = loadedContext,
                            name = loadedContext.name,
                            tags = loadedContext.tags?.filter { it.isNotBlank() } ?: emptyList(),
                            scoringStatus = loadedContext.scoringStatus,
                            isScoringEnabled = loadedContext.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                            valueImportance = loadedContext.valueImportance,
                            valueImpact = loadedContext.valueImpact,
                            effort = loadedContext.effort,
                            cost = loadedContext.cost,
                            risk = loadedContext.risk,
                            weightEffort = loadedContext.weightEffort,
                            weightCost = loadedContext.weightCost,
                            weightRisk = loadedContext.weightRisk,
                            rawScore = loadedContext.rawScore,
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
                                item is BacklogItemContent.JournalDocumentItem ||
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
                val currentContextId = contextId.value
                if (currentContextId.isNotEmpty()) {
                    contextRepository.deleteListItemsFromContext(currentContextId, listOf(attachment.backlogItem.id))
                }
            }
        }

        fun onAddAttachment(type: AttachmentType) {
            when (type) {
                AttachmentType.NOTES -> {
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.NoteDocumentEdit(contextId = contextId.value, documentId = null),
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
                AttachmentType.CHECKLIST -> {
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.Checklist(contextId = contextId.value, id = null),
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
                    _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.CONTEXT_LINK) }
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.ListChooser(
                                    title = "Add link to another context",
                                    currentParentId = contextId.value,
                                ),
                            ),
                        )
                    }
                }
                AttachmentType.CONTEXT_SHORTCUT -> {
                    _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.CONTEXT_SHORTCUT) }
                    viewModelScope.launch {
                        _uiEventFlow.send(
                            UiEvent.Navigate(
                                NavTarget.ListChooser(
                                    title = "Add shortcut to another context",
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
                    LinkType.NOTE_DOCUMENT,
                    -> {
                        _uiEventFlow.send(UiEvent.Navigate(NavTarget.NoteDocument(id = link.target, startEdit = false)))
                    }
                    LinkType.JOURNAL_DOCUMENT -> {
                        _uiEventFlow.send(UiEvent.Navigate(NavTarget.JournalDocument(id = link.target, startEdit = false)))
                    }
                    LinkType.CHECKLIST -> {
                        _uiEventFlow.send(UiEvent.Navigate(NavTarget.Checklist(id = link.target)))
                    }
                    LinkType.MUSIC_NOTE -> {
                        _uiEventFlow.send(UiEvent.Navigate(NavTarget.MusicNote(id = link.target, startEdit = false)))
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

        fun onAddWebLink(link: RelatedLink) {
            viewModelScope.launch {
                contextRepository.addLinkItemToContextFromLink(contextId.value, link)
            }
        }

        fun onAddContextLink(targetContextId: String) {
            viewModelScope.launch {
                contextRepository.addContextLinkToContext(targetContextId, contextId.value)
            }
        }

        fun onAddContextShortcut(targetContextId: String) {
            viewModelScope.launch {
                contextRepository.addContextLinkToContext(targetContextId, contextId.value)
            }
        }

        fun onPendingAttachmentTypeResolved() {
            _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.NONE) }
        }

        fun onReminderTimeSelected(calendar: Calendar) {
            _uiState.update { it.copy(reminderTime = calendar.timeInMillis) }
        }

        fun onReminderCleared() {
            _uiState.update { it.copy(reminderTime = null) }
        }

        fun onSetReminder() {
            viewModelScope.launch {
                uiState.value.context?.let { context ->
                    uiState.value.reminderTime?.let { reminderTime ->
                        val reminder =
                            Reminder(
                                entityId = context.id,
                                entityType = "CONTEXT",
                                reminderTime = reminderTime,
                                status = "SCHEDULED",
                                creationTime = System.currentTimeMillis(),
                            )
                        alarmScheduler.schedule(reminder)
                    }
                }
            }
        }

        fun onScoringEnabledChange(isEnabled: Boolean) {
            _uiState.update { it.copy(isScoringEnabled = isEnabled) }
        }

        fun onScoringStatusChange(status: String) {
            _uiState.update { it.copy(scoringStatus = status) }
        }

        fun onValueImportanceChange(value: Float) {
            _uiState.update { it.copy(valueImportance = value) }
        }

        fun onValueImpactChange(value: Float) {
            _uiState.update { it.copy(valueImpact = value) }
        }

        fun onEffortChange(value: Float) {
            _uiState.update { it.copy(effort = value) }
        }

        fun onCostChange(value: Float) {
            _uiState.update { it.copy(cost = value) }
        }

        fun onRiskChange(value: Float) {
            _uiState.update { it.copy(risk = value) }
        }

        fun onWeightEffortChange(value: Float) {
            _uiState.update { it.copy(weightEffort = value) }
        }

        fun onWeightCostChange(value: Float) {
            _uiState.update { it.copy(weightCost = value) }
        }

        fun onWeightRiskChange(value: Float) {
            _uiState.update { it.copy(weightRisk = value) }
        }

        fun onRawScoreChange(value: Float) {
            _uiState.update { it.copy(rawScore = value) }
        }

        fun onSaveScoring() {
            viewModelScope.launch {
                uiState.value.context?.let { context ->
                    val updatedContext =
                        context.copy(
                            scoringStatus = uiState.value.scoringStatus,
                            valueImportance = uiState.value.valueImportance,
                            valueImpact = uiState.value.valueImpact,
                            effort = uiState.value.effort,
                            cost = uiState.value.cost,
                            risk = uiState.value.risk,
                            weightEffort = uiState.value.weightEffort,
                            weightCost = uiState.value.weightCost,
                            weightRisk = uiState.value.weightRisk,
                            rawScore = uiState.value.rawScore,
                        )
                    val scoredContext = goalScoringManager.calculateScoresForContext(updatedContext)
                    contextRepository.updateContext(scoredContext)
                }
            }
        }

        sealed interface UiEvent {
            data class Navigate(val target: NavTarget) : UiEvent

            data class OpenUri(val uri: String) : UiEvent
        }
    }
