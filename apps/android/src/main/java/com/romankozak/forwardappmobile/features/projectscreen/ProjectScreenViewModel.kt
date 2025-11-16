package com.romankozak.forwardappmobile.features.projectscreen

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.di.IoDispatcher
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
import com.romankozak.forwardappmobile.shared.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.

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
    val backlogItems: List<ListItemContent> = emptyList(),
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

    sealed class Event {
        data class GoalChecked(val item: ListItemContent.GoalItem, val checked: Boolean) : Event()
        data class GoalClick(val item: ListItemContent.GoalItem) : Event()
        data class GoalLongClick(val item: ListItemContent.GoalItem) : Event()
        data class TagClick(val tag: String) : Event()
        data class RelatedLinkClick(val link: RelatedLink) : Event()
        data class LinkClick(val item: ListItemContent.LinkItem) : Event()
                data class SwitchViewMode(val viewMode: ProjectViewMode) : Event()
                data class SwitchInputMode(val inputMode: InputMode) : Event()
                data class AddInboxRecord(val text: String) : Event()
                data class AddBacklogGoal(val text: String) : Event()
            }
            
                fun onStart() {
                val projectId = savedStateHandle.get<String>("projectId") ?: return
        
                viewModelScope.launch(ioDispatcher) {
        
                    projectRepository.getProjectById(projectId)
                        .filterNotNull()
                        .onEach { project ->
                            _uiState.update { it.copy(projectName = project.name) }
                        }
                        .launchIn(viewModelScope)
        
                    inboxRepository.observeInbox(projectId)
                        .onEach { records ->
                            _uiState.update { it.copy(inboxItems = records) }
                        }
                        .launchIn(viewModelScope)
        
                    listItemRepository.getListItems(projectId)
                        .flatMapLatest { listItems ->
                            val goalIds = listItems.filter { it.itemType == "goal" }.map { it.entityId }
                            val projectIds = listItems.filter { it.itemType == "sublist" }.map { it.entityId }
        
                            val goalsFlow =
                                if (goalIds.isNotEmpty()) goalRepository.getGoalsByIds(goalIds)
                                else flowOf(emptyList())
        
                            val projectsFlow =
                                if (projectIds.isNotEmpty()) {
                                    combine(projectIds.map { id ->
                                        projectRepository.getProjectById(id).filterNotNull()
                                    }) { it.toList() }
                                } else flowOf(emptyList())
        
                            combine(goalsFlow, projectsFlow) { goals, projects ->
                                listItems.mapNotNull { listItem ->
                                    when (listItem.itemType) {
                                        "goal" -> goals.find { it.id == listItem.entityId }
                                            ?.let { ListItemContent.GoalItem(it, listItem) }
                                        "link" -> ListItemContent.LinkItem(
                                            RelatedLink(type = LinkType.URL, target = listItem.entityId),
                                            listItem
                                        )
                                        "sublist" -> projects.find { it.id == listItem.entityId }
                                            ?.let { ListItemContent.SublistItem(it, listItem) }
                                        else -> null
                                    }
                                }
                            }
                        }
                        .onEach { backlogItems ->
                            _uiState.update { it.copy(backlogItems = backlogItems) }
                        }
                        .launchIn(viewModelScope)
                }
            }
        
        
        
        /*    init {
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
        
                        listItemRepository.getListItems(id).flatMapLatest { listItems ->
                            val goalIds = listItems.filter { it.itemType == "goal" }.map { it.entityId }
                            val projectIds = listItems.filter { it.itemType == "sublist" }.map { it.entityId }
        
                            val goalsFlow = if (goalIds.isNotEmpty()) goalRepository.getGoalsByIds(goalIds) else flowOf(emptyList())
                            val projectsFlow = if (projectIds.isNotEmpty()) {
                                combine(projectIds.map { projectId -> projectRepository.getProjectById(projectId).filterNotNull() }) { it.toList() }
                            } else {
                                flowOf(emptyList())
                            }
        
                            combine(goalsFlow, projectsFlow) { goals, projects ->
                                listItems.mapNotNull { listItem ->
                                    when (listItem.itemType) {
                                        "goal" -> {
                                            goals.find { it.id == listItem.entityId }?.let { goal ->
                                                ListItemContent.GoalItem(goal, listItem)
                                            }
                                        }
                                        "link" -> {
                                            val link = RelatedLink(type = LinkType.URL, target = listItem.entityId)
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
                        }.onEach { backlogItems ->
                            _uiState.update { it.copy(backlogItems = backlogItems) }
                        }.launchIn(viewModelScope)
                    }
                }
            }*/
        
            fun onEvent(event: Event) {
                when (event) {
                    is Event.SwitchViewMode -> switchViewMode(event.viewMode)
                    is Event.SwitchInputMode -> switchInputMode(event.inputMode)
                    is Event.AddInboxRecord -> addInboxRecord(event.text)
                    is Event.AddBacklogGoal -> addBacklogGoal(event.text)
                    is Event.GoalChecked -> TODO()
                    is Event.GoalClick -> TODO()
                    is Event.GoalLongClick -> TODO()
                    is Event.TagClick -> TODO()
                    is Event.RelatedLinkClick -> TODO()
                    is Event.LinkClick -> TODO()
                }
            }
        
            private fun addBacklogGoal(text: String) {
                viewModelScope.launch(ioDispatcher) {
                    val projectId = savedStateHandle.get<String>("projectId") ?: return@launch
        
                    val newGoal = Goal(
                        id = "goal_${uuid4()}",
                        text = text,
                        completed = false,
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        updatedAt = null
                    )
                    goalRepository.insertGoal(newGoal)
        
                    val currentList = _uiState.value.backlogItems
                    val maxOrder = currentList.mapNotNull { it.listItem.itemOrder }.maxOfOrNull { it } ?: 0L
        
                    val newListItem = ListItem(
                        id = "listItem_${uuid4()}",
                        projectId = projectId,
                        entityId = newGoal.id,
                        itemType = "goal",
                        itemOrder = maxOrder + 1
                    )
                    listItemRepository.insertListItem(newListItem)
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
