package com.romankozak.forwardappmobile.features.projectscreen

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository.LegacyNotesRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val itemId: String) : UiEvent()
    data class ScrollTo(val index: Int) : UiEvent()
    data class NavigateBackAndReveal(val projectId: String) : UiEvent()
    data class HandleLinkClick(val link: RelatedLink) : UiEvent()
    data class OpenUri(val uri: String) : UiEvent()
    data object ScrollToLatestInboxRecord : UiEvent()
    data class SwitchViewMode(val viewMode: ProjectViewMode) : UiEvent()
    data class SwitchInputMode(val inputMode: InputMode) : UiEvent()
    data class AddInboxRecord(val text: String) : UiEvent()
}

enum class GoalActionType {
    CreateInstance,
    MoveInstance,
    CopyGoal,
    AddLinkToList,
    ADD_LIST_SHORTCUT,
}

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val itemContent: Any) : GoalActionDialogState()
}

data class UiState(
    val projectName: String? = null,
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedItemId: String? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedItemId: String? = null,
    val showAddWebLinkDialog: Boolean = false,
    val showAddObsidianLinkDialog: Boolean = false,
    val itemToHighlight: String? = null,
    val inboxRecordToHighlight: String? = null,
    val needsStateRefresh: Boolean = false,
    val currentView: ProjectViewMode = ProjectViewMode.Backlog,
    val isViewModePanelVisible: Boolean = false,
    val showRecentProjectsSheet: Boolean = false,
    val showImportFromMarkdownDialog: Boolean = false,
    val showImportBacklogFromMarkdownDialog: Boolean = false,
    val refreshTrigger: Int = 0,
    val detectedReminderSuggestion: String? = null,
    val detectedReminderCalendar: Calendar? = null,
    val nerState: Any = Any(),
    val recordForReminderDialog: ActivityRecord? = null,
    // val projectTimeMetrics: ProjectTimeMetrics? = null,
    val showShareDialog: Boolean = false,
    val showCreateNoteDocumentDialog: Boolean = false,
    val showRemindersDialog: Boolean = false,
    val itemForRemindersDialog: Any? = null,
    val remindersForDialog: List<Reminder> = emptyList(),
    val logEntryToEdit: ProjectExecutionLog? = null,
    val artifactToEdit: ProjectArtifact? = null,
    val selectedDashboardTab: Any = Any(),
    val showNoteDocumentEditor: Boolean = false,
    val showDisplayPropertiesDialog: Boolean = false,
    val showCheckboxes: Boolean = false,
    val inboxItems: List<InboxRecord> = emptyList(),
) {
    val isSelectionModeActive: Boolean get() = selectedItemIds.isNotEmpty()
}

@Inject
class ProjectScreenViewModel(
  private val application: Application,
  private val projectRepository: ProjectRepository,
  // private val settingsRepository: SettingsRepository,
  // private val contextHandler: ContextHandler,
  // private val alarmScheduler: AlarmScheduler,
  // private val nerManager: NerManager,
  // private val reminderParser: ReminderParser,
  private val activityRepository: ActivityRecordsRepository,
  // private val projectMarkdownExporter: ProjectMarkdownExporter,
  private val dayManagementRepository: DayPlanRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val goalRepository: GoalRepository,
  private val listItemRepository: ListItemRepository,
  private val noteDocumentRepository: NoteDocumentsRepository,
  private val checklistRepository: ChecklistRepository,
  private val reminderRepository: RemindersRepository,
  private val recentItemsRepository: RecentItemRepository,
  private val projectLogRepository: ProjectExecutionLogsRepository,
  private val projectArtifactRepository: ProjectArtifactRepository,
  private val legacyNotesRepository: LegacyNotesRepository,
  private val inboxRepository: InboxRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    lateinit var savedStateHandle: SavedStateHandle

    val listContent: StateFlow<List<ListItemContent>> =
        savedStateHandle.getStateFlow<String?>("projectId", null)
            .filterNotNull()
            .flatMapLatest { projectId ->
                listItemRepository.getListItems(projectId)
                    .flatMapLatest { listItems ->
                        val goalIds = listItems.filter { it.itemType == "goal" }.map { it.entityId }
                        val projectIds = listItems.filter { it.itemType == "sublist" }.map { it.entityId }

                        val goalsFlow = if (goalIds.isNotEmpty()) goalRepository.getGoalsByIds(goalIds) else flowOf(emptyList())
                        val projectsFlow = if (projectIds.isNotEmpty()) projectRepository.getProjectsByIds(projectIds) else flowOf(emptyList())

                        combine(goalsFlow, projectsFlow) { goals, projects ->
                            listItems.mapNotNull { listItem ->
                                when (listItem.itemType) {
                                    "goal" -> {
                                        goals.find { it.id == listItem.entityId }?.let { goal ->
                                            ListItemContent.GoalItem(goal, listItem)
                                        }
                                    }
                                    "link" -> {
                                        val link = com.romankozak.forwardappmobile.shared.data.models.RelatedLink(type = com.romankozak.forwardappmobile.shared.data.models.LinkType.URL, target = listItem.entityId)
                                        ListItemContent.LinkItem(link, listItem)
                                    }
                                    "sublist" -> {
                                        projects.find { it.id == listItem.entityId }?.let { project ->
                                            ListItemContent.SublistItem(project, listItem)
                                        }
                                    }
                                    else -> null
                                }
                            }
                        }
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        viewModelScope.launch(ioDispatcher) {
            val projectId = savedStateHandle.get<String>("projectId")
            projectId?.let { id ->
                projectRepository.getProjectById(id)
                    .filterNotNull()
                    .onEach { project ->
                        _uiState.update { it.copy(projectName = project.name) }
                    }
                    .launchIn(viewModelScope)

                inboxRepository.observeInbox(id)
                    .onEach { records ->
                        _uiState.update { it.copy(inboxItems = records) }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.SwitchViewMode -> switchViewMode(event.viewMode)
            is UiEvent.SwitchInputMode -> switchInputMode(event.inputMode)
            is UiEvent.AddInboxRecord -> addInboxRecord(event.text)
            else -> { /* TODO */ }
        }
    }

    private fun addInboxRecord(text: String) {
        viewModelScope.launch(ioDispatcher) {
            val projectId = savedStateHandle.get<String>("projectId") ?: return@launch
            val newRecord = InboxRecord(
                id = "inbox_${System.currentTimeMillis()}",
                projectId = projectId,
                text = text,
                createdAt = System.currentTimeMillis(),
                itemOrder = 0 // TODO: Implement correct ordering
            )
            inboxRepository.upsert(newRecord)
        }
    }

    private fun switchInputMode(inputMode: InputMode) {
        _uiState.update { it.copy(inputMode = inputMode) }
    }

    private fun switchViewMode(viewMode: ProjectViewMode) {
        _uiState.update { it.copy(currentView = viewMode) }
    }
}