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
import com.romankozak.forwardappmobile.shared.data.models.LinkType
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.romankozak.forwardappmobile.shared.features.settings.logic.ContextHandler
import android.util.Log
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.shared.data.models.ProjectStatusValues
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectType

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
    val contextMarkerToEmojiMap: Map<String, String> = emptyMap(),
    val currentTimeMillis: Long = 0L,
) {
    val isSelectionModeActive: Boolean get() = selectedItemIds.isNotEmpty()
}

sealed class NavigationEvent {
    data class NavigateToProject(val projectId: String) : NavigationEvent()
    // Add other navigation events as needed
}

@Inject
class ProjectScreenViewModel(
  private val application: Application,
  private val projectRepository: ProjectRepository,
  // private val settingsRepository: SettingsRepository,
  private val contextHandler: ContextHandler,
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

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

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
        data class SubprojectClick(val projectId: String) : Event()
        data class SubprojectLongClick(val item: ListItemContent.SublistItem) : Event()
        data class SubprojectChecked(val item: ListItemContent.SublistItem, val checked: Boolean) : Event()
        data class AddNestedProject(val name: String) : Event()
        data class LinkExistingProject(val project: com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project) : Event()
        data class MoveItem(val from: Int, val to: Int) : Event()
        data class DragEnd(val from: Int, val to: Int) : Event()
    }
    
        fun onStart() {
        val projectId = savedStateHandle.get<String>("projectId") ?: return

        viewModelScope.launch(ioDispatcher) {
            contextHandler.initialize()
            _uiState.update {
                it.copy(
                    currentTimeMillis = Clock.System.now().toEpochMilliseconds()
                )
            }

            contextHandler.contextMarkerToEmojiMap.onEach { emojiMap ->
                _uiState.update { it.copy(contextMarkerToEmojiMap = emojiMap) }
            }.launchIn(viewModelScope)

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

                    val childProjectsFlows = projectIds.map { projectRepository.getChildProjects(it) }
                    val childProjectsFlow = if (childProjectsFlows.isNotEmpty()) {
                        combine(childProjectsFlows) { it.toList().flatten() }
                    } else flowOf(emptyList())

                    val remindersFlows = projectIds.map { reminderRepository.observeRemindersForEntity(it) }
                    val remindersFlow = if (remindersFlows.isNotEmpty()) {
                        combine(remindersFlows) { it.toList().flatten() }
                    } else flowOf(emptyList())

                    combine(goalsFlow, projectsFlow, childProjectsFlow, remindersFlow) { goals, projects, allChildProjects, allReminders ->
                        listItems.mapNotNull { listItem ->
                            when (listItem.itemType) {
                                "goal" -> goals.find { it.id == listItem.entityId }
                                    ?.let { ListItemContent.GoalItem(it, listItem) }
                                "link" -> ListItemContent.LinkItem(
                                    RelatedLink(type = LinkType.URL, target = listItem.entityId),
                                    listItem
                                )
                                "sublist" -> projects.find { it.id == listItem.entityId }
                                    ?.let { project ->
                                        val children = allChildProjects.filter { it.parentId == project.id }
                                        val itemReminders = allReminders.filter { it.entityId == project.id }
                                        ListItemContent.SublistItem(project, listItem, children, itemReminders)
                                    }
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
            is Event.SubprojectClick -> {
                viewModelScope.launch {
                    _navigationEvents.emit(NavigationEvent.NavigateToProject(event.projectId))
                }
            }
            is Event.SubprojectLongClick -> {
                Log.d("ProjectScreenViewModel", "SubprojectLongClick: ${event.item.project.name}")
            }
            is Event.SubprojectChecked -> {
                viewModelScope.launch(ioDispatcher) {
                    projectRepository.updateProjectCompleted(event.item.project.id, event.checked)
                }
            }
            is Event.GoalChecked -> TODO()
            is Event.GoalClick -> TODO()
            is Event.GoalLongClick -> TODO()
            is Event.TagClick -> {
                Log.d("ProjectScreenViewModel", "TagClick: ${event.tag}")
            }
            is Event.RelatedLinkClick -> {
                Log.d("ProjectScreenViewModel", "RelatedLinkClick: ${event.link.target}")
            }
            is Event.LinkClick -> TODO()
            is Event.AddNestedProject -> addNestedProject(event.name)
            is Event.LinkExistingProject -> linkExistingProject(event.project)
            is Event.MoveItem -> onBacklogItemMove(event.from, event.to)
            is Event.DragEnd -> onBacklogItemDragEnd(event.from, event.to)
        }
    }

    private fun onBacklogItemMove(from: Int, to: Int) {
        val currentItems = _uiState.value.backlogItems.toMutableList()
        if (from < 0 || from >= currentItems.size || to < 0 || to >= currentItems.size) {
            Log.e("ProjectScreenViewModel", "Invalid move indices: from=$from, to=$to")
            return
        }

        val movedItem = currentItems.removeAt(from)
        currentItems.add(to, movedItem)
        _uiState.update { it.copy(backlogItems = currentItems) }
    }

    private fun onBacklogItemDragEnd(from: Int, to: Int) {
        viewModelScope.launch(ioDispatcher) {
            val currentItems = _uiState.value.backlogItems
            if (from < 0 || from >= currentItems.size || to < 0 || to >= currentItems.size) {
                Log.e("ProjectScreenViewModel", "Invalid drag end indices: from=$from, to=$to")
                return@launch
            }

            // Update itemOrder for all items in the database
            currentItems.forEachIndexed { index, listItemContent ->
                val newOrder = index.toLong()
                if (listItemContent.listItem.itemOrder != newOrder) {
                    listItemRepository.updateListItemOrder(listItemContent.listItem.id, newOrder)
                }
            }
        }
    }

    private fun linkExistingProject(project: com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project) {
        viewModelScope.launch(ioDispatcher) {
            val currentProjectId = savedStateHandle.get<String>("projectId") ?: return@launch

            val currentList = _uiState.value.backlogItems
            val maxOrder = currentList.mapNotNull { it.listItem.itemOrder }.maxOfOrNull { it } ?: 0L

            val newListItem = ListItem(
                id = "listItem_${uuid4()}",
                projectId = currentProjectId,
                entityId = project.id,
                itemType = "sublist",
                itemOrder = maxOrder + 1
            )
            listItemRepository.insertListItem(newListItem)
        }
    }

    private fun addNestedProject(name: String) {
        Log.d("ProjectScreenViewModel", "addNestedProject called with name: $name")
        viewModelScope.launch(ioDispatcher) {
            val currentProjectId = savedStateHandle.get<String>("projectId")
            if (currentProjectId == null) {
                Log.e("ProjectScreenViewModel", "currentProjectId is null, cannot add nested project")
                return@launch
            }
            Log.d("ProjectScreenViewModel", "currentProjectId: $currentProjectId")

            val newProject = Project(
                id = "project_${uuid4()}",
                name = name,
                description = null,
                parentId = currentProjectId,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = null,
                tags = null,
                relatedLinks = null,
                isExpanded = true,
                goalOrder = 0,
                isAttachmentsExpanded = false,
                defaultViewMode = null,
                isCompleted = false,
                isProjectManagementEnabled = false,
                projectStatus = ProjectStatusValues.NO_PLAN,
                projectStatusText = null,
                projectLogLevel = null,
                totalTimeSpentMinutes = 0,
                valueImportance = 0.0,
                valueImpact = 0.0,
                effort = 0.0,
                cost = 0.0,
                risk = 0.0,
                weightEffort = 0.0,
                weightCost = 0.0,
                weightRisk = 0.0,
                rawScore = 0.0,
                displayScore = 0,
                scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                showCheckboxes = false,
                projectType = ProjectType.DEFAULT,
                reservedGroup = null
            )
            Log.d("ProjectScreenViewModel", "newProject created: $newProject")
            projectRepository.upsertProject(newProject)

            val currentList = _uiState.value.backlogItems
            val maxOrder = currentList.mapNotNull { it.listItem.itemOrder }.maxOfOrNull { it } ?: 0L

            val newListItem = ListItem(
                id = "listItem_${uuid4()}",
                projectId = currentProjectId,
                entityId = newProject.id,
                itemType = "sublist",
                itemOrder = maxOrder + 1
            )
            Log.d("ProjectScreenViewModel", "newListItem created: $newListItem")
            listItemRepository.insertListItem(newListItem)
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
