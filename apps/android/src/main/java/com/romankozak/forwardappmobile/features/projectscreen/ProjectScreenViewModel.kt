package com.romankozak.forwardappmobile.features.projectscreen

import com.romankozak.forwardappmobile.features.mainscreen.usecases.SearchUseCase

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.viewModelScope


import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument

import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ListItemTypeValues

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogEntryTypeValues
// TODO: [GM-28] ProjectTimeMetrics seems to be an old model. Needs to be replaced with a new KMP model or refactored.
// import com.romankozak.forwardappmobile.shared.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.features.recent.domain.model.RecentItem
import com.romankozak.forwardappmobile.shared.data.models.LinkType
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder

import com.romankozak.forwardappmobile.shared.data.logic.ContextHandler
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.settings.domain.repository.SettingsRepository
import com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.ReminderRepository
import com.romankozak.forwardappmobile.shared.features.recentitems.domain.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.notes.legacy.domain.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.domain.ner.NerManager
import com.romankozak.forwardappmobile.shared.domain.ner.NerState
import com.romankozak.forwardappmobile.shared.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.shared.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.shared.domain.reminders.cancelForActivityRecord
import com.romankozak.forwardappmobile.shared.domain.reminders.scheduleForActivityRecord
import com.romankozak.forwardappmobile.shared.domain.wifirestapi.FileDataRequest
import com.romankozak.forwardappmobile.shared.domain.wifirestapi.RetrofitClient
import com.romankozak.forwardappmobile.features.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.features.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.projectscreen.components.TagUtils
import com.romankozak.forwardappmobile.features.attachments.models.AttachmentType
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.BacklogMarkdownHandler
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.InboxHandler
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.InboxHandlerResultListener
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.InboxMarkdownHandler
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.ItemActionHandler
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.ProjectMarkdownExporter
import com.romankozak.forwardappmobile.features.projectscreen.viewmodel.SelectionHandler
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.features.projectscreen.components.projectrealization.ProjectManagementTab
import java.util.UUID
import kotlinx.coroutines.withContext

private const val TAG = "ProjectScreenVM_DEBUG"

sealed class UiEvent {
  data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()

  data class Navigate(val route: String) : UiEvent()

  data class ResetSwipeState(val itemId: String) : UiEvent()

  data class ScrollTo(val index: Int) : UiEvent()

  data class NavigateBackAndReveal(val projectId: String) : UiEvent()

  data class HandleLinkClick(val link: RelatedLink) : UiEvent()

  data class OpenUri(val uri: String) : UiEvent()

  data object ScrollToLatestInboxRecord : UiEvent()
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

  data class AwaitingActionChoice(val itemContent: ListItemContent) : GoalActionDialogState()
}

data class UiState(
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
  val currentView: ProjectViewMode = ProjectViewMode.BACKLOG,
  val isViewModePanelVisible: Boolean = false,
  val showRecentProjectsSheet: Boolean = false,
  val showImportFromMarkdownDialog: Boolean = false,
  val showImportBacklogFromMarkdownDialog: Boolean = false,
  val refreshTrigger: Int = 0,
  val detectedReminderSuggestion: String? = null,
  val detectedReminderCalendar: Calendar? = null,
  val nerState: NerState = NerState.NotInitialized,
  val recordForReminderDialog: ActivityRecord? = null,
  // TODO: [GM-28] ProjectTimeMetrics seems to be an old model. Needs to be replaced with a new KMP model or refactored.
  // val projectTimeMetrics: ProjectTimeMetrics? = null,
  val showShareDialog: Boolean = false,
  val showCreateNoteDocumentDialog: Boolean = false,
  val showRemindersDialog: Boolean = false,
  val itemForRemindersDialog: ListItemContent? = null,
      val remindersForDialog: List<Reminder> = emptyList(),
      val logEntryToEdit: ProjectExecutionLog? = null,
      val artifactToEdit: ProjectArtifact? = null,
      val selectedDashboardTab: ProjectManagementTab = ProjectManagementTab.Dashboard,
      val showNoteDocumentEditor: Boolean = false,
      val showDisplayPropertiesDialog: Boolean = false,
      val showCheckboxes: Boolean = false,
  ) {
      val isSelectionModeActive: Boolean get() = selectedItemIds.isNotEmpty()
  }
  


private val ActivityRecord.isOngoing: Boolean
  get() = this.startTime != null && this.endTime == null



@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class ProjectScreenViewModel(
  // TODO: [GM-27] Old navigation/search use case. Needs to be migrated or replaced with new KMP architecture.
  // private val searchUseCase: SearchUseCase,
  private val application: Application,
  private val projectRepository: ProjectRepository,
  private val settingsRepository: SettingsRepository,
  private val contextHandler: ContextHandler,
  private val alarmScheduler: AlarmScheduler,
  private val nerManager: NerManager,
  private val reminderParser: ReminderParser,
  private val activityRepository: ActivityRecordsRepository,
  private val projectMarkdownExporter: ProjectMarkdownExporter,
  private val savedStateHandle: SavedStateHandle,
  private val dayManagementRepository: DayPlanRepository,
  // TODO: [GM-27] Old navigation/search use case. Needs to be migrated or replaced with new KMP architecture.
  // private val clearAndNavigateHomeUseCase: ClearAndNavigateHomeUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val goalRepository: GoalRepository,
  private val listItemRepository: ListItemRepository,
  private val noteDocumentRepository: NoteDocumentRepository,
  private val checklistRepository: ChecklistRepository,
  private val reminderRepository: ReminderRepository,
  private val recentItemsRepository: RecentItemsRepository,
  private val projectLogRepository: ProjectExecutionLogsRepository,
  private val projectArtifactRepository: ProjectArtifactRepository,
  private val noteRepository: LegacyNoteRepository,
  private val inboxRepository: InboxRepository,
) :
  ViewModel(),
  ItemActionHandler.ResultListener,
  InputHandler.ResultListener,
  SelectionHandler.ResultListener,
  InboxHandlerResultListener,
  InboxMarkdownHandler.ResultListener,
  BacklogMarkdownHandlerResultListener {
  companion object {
    const val HANDLE_LINK_CLICK_ROUTE = "handle_link_click"
    private const val TAG = "ProjectScreenVM_DEBUG"
  }

  lateinit var enhancedNavigationManager: EnhancedNavigationManager

  val canGoBack: StateFlow<Boolean>
    get() = enhancedNavigationManager.canGoBack

  val canGoForward: StateFlow<Boolean>
    get() = enhancedNavigationManager.canGoForward

  private val _isProcessingHome = MutableStateFlow(false)

  private val _allTags = MutableStateFlow<List<String>>(emptyList())
  val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

  private val _allContexts = MutableStateFlow<List<String>>(emptyList())
  val allContexts: StateFlow<List<String>> = _allContexts.asStateFlow()

  private val _allProjects =
    projectRepository
      .getAllProjectsFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val subprojectChildren: StateFlow<Map<String?, List<Project>>> =
    _allProjects
      .map { allProjects -> allProjects.groupBy { it.parentId } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  private var batchSaveJob: Job? = null
  private val projectIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
  private val _listContent = MutableStateFlow<List<ListItemContent>>(emptyList())
  val listContent: StateFlow<List<ListItemContent>> = _listContent.asStateFlow()

  val itemActionHandler = ItemActionHandler(projectRepository, goalRepository, recentItemsRepository, viewModelScope, projectIdFlow, this)
  val selectionHandler = SelectionHandler(projectRepository, goalRepository, viewModelScope, projectIdFlow, _listContent, this)
  val inboxHandler = InboxHandler(projectRepository, inboxRepository, viewModelScope, projectIdFlow, this)
  val inboxMarkdownHandler = InboxMarkdownHandler(projectRepository, goalRepository, viewModelScope, this)
  val backlogMarkdownHandler = BacklogMarkdownHandler(projectRepository, goalRepository, viewModelScope, this)

  override fun copyToClipboard(text: String, label: String) {
    val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
  }

  private lateinit var lazyListState: LazyListState

  private var pendingAttachmentShare: ListItemContent? = null




  private val _uiState =
    MutableStateFlow(
      UiState(
        goalToHighlight = savedStateHandle.get<String>("goalId"),
        itemToHighlight = savedStateHandle.get<String>("itemIdToHighlight"),
        inboxRecordToHighlight = savedStateHandle.get<String>("inboxRecordIdToHighlight"),
      )
    )
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  val projectLogs: StateFlow<List<ProjectExecutionLog>> =
    projectIdFlow
      .flatMapLatest { id ->
        if (id.isNotEmpty()) {
          projectLogRepository.observeLogs(id)
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val projectArtifact: StateFlow<ProjectArtifact?> =
    projectIdFlow
      .flatMapLatest { id ->
        if (id.isNotEmpty()) {
          projectArtifactRepository.getProjectArtifactStream(id)
        } else {
          flowOf(null)
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val inputHandler =
    InputHandler(
      projectRepository,
      goalRepository,
      listItemRepository,
      viewModelScope,
      projectIdFlow,
      this,
      reminderParser,
      alarmScheduler,
    )

  private val _refreshTrigger = MutableStateFlow(0)

  private val _uiEventFlow = Channel<UiEvent>()
  val uiEventFlow = _uiEventFlow.receiveAsFlow()

  val recentItems: StateFlow<List<RecentItem>> =
    recentItemsRepository
      .getRecentItems()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val contextMarkerToEmojiMap: StateFlow<Map<String, String>> =
    contextHandler.contextMarkerToEmojiMap

  val project: StateFlow<Project?> =
    combine(projectIdFlow, _refreshTrigger) { id, _ -> id }
      .flatMapLatest { id ->
        if (id.isNotEmpty()) projectRepository.getProjectByIdFlow(id) else flowOf(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val tagToContextNameMap: StateFlow<Map<String, String>> =
    contextHandler.tagToContextNameMap.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyMap(),
    )

  val lastOngoingActivity: StateFlow<ActivityRecord?> =
    activityRepository
      .observeActivityRecords()
      .map { log -> log.firstOrNull { it.isOngoing } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentProjectContextMarker: StateFlow<String?> =
    combine(project, tagToContextNameMap) { proj, tagMap ->
        val projectTags = proj?.tags ?: emptyList()
        if (projectTags.isEmpty() || tagMap.isEmpty()) return@combine null
        val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in projectTags }?.value
        contextName?.let { contextHandler.getContextMarker(it) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val currentProjectContextEmojiToHide: StateFlow<String?> =
    combine(currentProjectContextMarker, contextMarkerToEmojiMap) { marker, emojiMap ->
        marker?.let { emojiMap[it] }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val databaseContentStream: StateFlow<List<ListItemContent>> =
    combine(
        projectIdFlow,
        _uiState.map { it.localSearchQuery }.distinctUntilChanged(),
        _refreshTrigger,
      ) { id, query, _ ->
        Pair(id, query)
      }
      .flatMapLatest { (id, query) ->
        if (id.isEmpty()) {
          flowOf(emptyList())
        } else {
          listItemRepository.getListItems(id).map { listItems ->
            listItems.mapNotNull { listItem ->
              when (listItem.itemType) {
                ListItemTypeValues.GOAL -> {
                  goalRepository.getGoalById(listItem.entityId).firstOrNull()?.let { goal ->
                    ListItemContent.GoalItem(goal, listItem)
                  }
                }
                ListItemTypeValues.PROJECT -> {
                  projectRepository.getProjectById(listItem.entityId).firstOrNull()?.let { project ->
                    ListItemContent.SublistItem(project, listItem)
                  }
                }
                ListItemTypeValues.LINK_ITEM -> {
                  // This is a bit tricky, as we don't have a direct way to get a RelatedLink by its ID.
                  // For now, I will leave this as a TODO.
                  null
                }
                ListItemTypeValues.NOTE -> {
                  noteRepository.getNoteById(listItem.entityId).firstOrNull()?.let { note ->
                    ListItemContent.NoteItem(note, listItem)
                  }
                }
                ListItemTypeValues.NOTE_DOCUMENT -> {
                  noteDocumentRepository.getDocumentById(listItem.entityId).firstOrNull()?.let { document ->
                    ListItemContent.NoteDocumentItem(document, listItem)
                  }
                }
                ListItemTypeValues.CHECKLIST -> {
                  checklistRepository.getChecklistById(listItem.entityId).firstOrNull()?.let { checklist ->
                    ListItemContent.ChecklistItem(checklist, listItem)
                  }
                }
                else -> null
              }
            }
          }
        }
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val isSelectionModeActive: StateFlow<Boolean> =
    _uiState
      .map { it.selectedItemIds.isNotEmpty() }
      .onEach { isActive -> Log.d(TAG, "СТАН: isSelectionModeActive змінився на: $isActive") }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val obsidianVaultName: StateFlow<String> =
    settingsRepository.obsidianVaultNameFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      "",
    )

  private var pendingActivityForReminder: ActivityRecord? = null



  init {
    Log.d(TAG, "ViewModel instance created: ${this.hashCode()}")

    viewModelScope.launch {
        projectIdFlow.filter { it.isNotEmpty() }.collect { projectId ->
        // TODO: [GM-30] This method needs to be re-implemented or replaced with a new mechanism for ensuring data consistency.
        // projectRepository.ensureChildProjectListItemsExist(projectId)
        }
    }

    savedStateHandle.get<String>("initialViewMode")?.let { modeName ->
      try {
        val viewMode = ProjectViewMode.valueOf(modeName)
        _uiState.update { it.copy(currentView = viewMode) }
        Log.d(TAG, "Initial view mode set to $viewMode from navigation argument.")
      } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Invalid initialViewMode provided: $modeName")
      }
    }

    viewModelScope.launch {
      project.collect { proj ->
        if (proj != null) {
          _uiState.update { it.copy(showCheckboxes = proj.showCheckboxes) }
          val isManagementEnabled = proj.isProjectManagementEnabled ?: false
          val currentView = uiState.value.currentView
          if (!isManagementEnabled && currentView == ProjectViewMode.ADVANCED) {
            Log.d(
              TAG,
              "Inconsistency detected: Project management is OFF but view is DASHBOARD. Switching to BACKLOG.",
            )
            onProjectViewChange(ProjectViewMode.BACKLOG)
          }

          val currentInputMode = uiState.value.inputMode
          if (!isManagementEnabled && currentInputMode == InputMode.AddProjectLog) {
            _uiState.update { it.copy(inputMode = InputMode.AddGoal) }
          }
        }
      }
    }

    val inboxIdToHighlight = savedStateHandle.get<String>("inboxRecordIdToHighlight")
    Log.d(TAG, "Received 'inboxRecordIdToHighlight' from SavedStateHandle: $inboxIdToHighlight")

    viewModelScope.launch {
      nerManager.nerState.collect { state ->
        Log.i(TAG, "NER State Changed -> $state")
        _uiState.update { it.copy(nerState = state) }
      }
    }

    viewModelScope.launch {
      project
        .filterNotNull()
        .map { it.defaultViewModeName }
        .distinctUntilChanged()
        .collect { savedModeName ->
          val viewMode =
            try {
              ProjectViewMode.valueOf(savedModeName ?: ProjectViewMode.BACKLOG.name)
            } catch (e: Exception) {
              ProjectViewMode.BACKLOG
            }
          _uiState.update {
            it.copy(currentView = viewMode, inputMode = getInputModeForView(viewMode))
          }
        }
    }

    viewModelScope.launch {
        databaseContentStream.collect { dbContent ->
            Log.d(TAG, "databaseContentStream collected, list size: ${dbContent.size}")
            _listContent.value = dbContent
        }
    }

    viewModelScope.launch {
      projectIdFlow
        .filter { it.isNotEmpty() }
        .collect { id ->
            projectRepository.getProjectById(id)?.let {
                recentItemsRepository.logProjectAccess(it)
            }
        }
    }

    viewModelScope.launch { withContext(Dispatchers.IO) { contextHandler.initialize() } }
    loadAllTags()
        loadAllContexts()
    
        lazyListState = LazyListState(0, 0)



      }

  private fun loadAllTags() {
    viewModelScope.launch(Dispatchers.IO) {
      val projects = projectRepository.getAllProjectsFlow().first()
      val projectTags = projects.flatMap { it.tags ?: emptyList() }
      val goalTags = mutableListOf<String>()
      for (project in projects) {
        val content = projectRepository.getProjectContentStream(project.id).first()
        content.forEach { item ->
          if (item is ListItemContent.GoalItem) {
            goalTags.addAll(TagUtils.extractTags(item.goal.text).map { it.fullTag })
          }
        }
      }
      withContext(Dispatchers.Main) { _allTags.value = (projectTags + goalTags).distinct() }
    }
  }

  private fun loadAllContexts() {
    viewModelScope.launch { _allContexts.value = contextHandler.contextNamesFlow.first() }
  }

  val autocompleteSuggestions =
    uiState
      .map { it.inputValue }
      .debounce(150)
      .flatMapLatest { inputValue ->
        val text = inputValue.text
        val cursorPosition = inputValue.selection.start
        if (text.isEmpty()) {
          return@flatMapLatest flowOf(emptyList())
        }

        val wordInfo = getCurrentWordInfo(text, cursorPosition)
        if (wordInfo != null) {
          val (word, type) = wordInfo
          if (word.length < 2) {
            return@flatMapLatest flowOf(emptyList())
          }
          when (type) {
            "#" -> {
              val filtered =
                _allTags.value
                  .filter { tag -> tag.removePrefix("#").startsWith(word, ignoreCase = true) }
                  .map { tag -> if (tag.startsWith("#")) tag else "#$tag" }
              flowOf(filtered)
            }
            "@" -> {
              val filtered =
                _allContexts.value.filter { it.startsWith(word, ignoreCase = true) }.map { "@$it" }
              flowOf(filtered)
            }
            else -> flowOf(emptyList())
          }
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private fun getCurrentWordInfo(text: String, cursorPosition: Int): Pair<String, String>? {
    val textUpToCursor = text.substring(0, cursorPosition)
    val lastAt = textUpToCursor.lastIndexOf('@')
    val lastHash = textUpToCursor.lastIndexOf('#')

    if (lastAt == -1 && lastHash == -1) {
      return null
    }

    val (startIndex, prefix) =
      if (lastAt > lastHash) {
        lastAt to "@"
      } else {
        lastHash to "#"
      }

    val word = textUpToCursor.substring(startIndex + 1)
    if (word.contains(" ")) {
      return null
    }

    return word to prefix
  }

  fun onSuggestionClick(suggestion: String) {
    val currentText = uiState.value.inputValue.text
    val cursorPosition = uiState.value.inputValue.selection.start
    val (word, prefix) = getCurrentWordInfo(currentText, cursorPosition) ?: return

    val startIndex = currentText.substring(0, cursorPosition).lastIndexOf(prefix)
    val newText =
      currentText.substring(0, startIndex) +
        suggestion +
        " " +
        currentText.substring(cursorPosition)
    val newCursorPosition = startIndex + suggestion.length + 1

    _uiState.update {
      it.copy(
        inputValue =
          TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursorPosition),
          )
      )
    }
  }

  fun onToggleProjectManagement(isEnabled: Boolean) {
    viewModelScope.launch {
      project.value?.let { currentProject ->
        val updatedProject = currentProject.copy(isProjectManagementEnabled = isEnabled)
        projectRepository.upsertProject(updatedProject)
      }
    }
  }

  fun onProjectStatusUpdate(newStatus: String, statusText: String?) {
    viewModelScope.launch {
      project.value?.let { currentProject ->
        val updatedProject = currentProject.copy(projectStatus = newStatus, projectStatusText = statusText)
        projectRepository.upsertProject(updatedProject)
      }
    }
  }

  override fun addProjectComment(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) {
      val log = ProjectExecutionLog(
        id = UUID.randomUUID().toString(),
        projectId = projectIdFlow.value,
        type = ProjectLogEntryTypeValues.COMMENT,
        description = text,
        createdAt = System.currentTimeMillis()
      )
      projectLogRepository.upsertLog(log)
      withContext(Dispatchers.Main) {
          _uiState.update { it.copy(
              inputValue = TextFieldValue(""),
              currentView = ProjectViewMode.ADVANCED,
              selectedDashboardTab = ProjectManagementTab.Log
          ) }
      }
    }
  }

      override fun addMilestone(text: String) {
      if (text.isBlank()) return
      viewModelScope.launch(Dispatchers.IO) {
          val log = ProjectExecutionLog(
              id = UUID.randomUUID().toString(),
              projectId = projectIdFlow.value,
              type = ProjectLogEntryTypeValues.MILESTONE,
              description = text,
              createdAt = System.currentTimeMillis()
          )
          projectLogRepository.upsertLog(log)
          withContext(Dispatchers.Main) {
              _uiState.update { it.copy(
                  inputValue = TextFieldValue(""),
                  selectedDashboardTab = ProjectManagementTab.Log
              ) }
          }
      }
    }
  private fun getInputModeForView(viewMode: ProjectViewMode): InputMode =
    when (viewMode) {
      ProjectViewMode.INBOX -> InputMode.AddQuickRecord
                  ProjectViewMode.ADVANCED -> InputMode.AddProjectLog      else -> InputMode.AddGoal
    }

  override fun requestNavigation(route: String) {
    viewModelScope.launch {
      if (route.startsWith("goal_detail_screen/")) {

        val projectId = route.substringAfter("goal_detail_screen/")

        val projectName =
          withContext(ioDispatcher) {
            projectRepository.getProjectById(projectId)?.name ?: "Project"
          }
        enhancedNavigationManager.navigateToProject(projectId, projectName)
      } else if (route.startsWith(HANDLE_LINK_CLICK_ROUTE)) {

        val target = route.substringAfter(HANDLE_LINK_CLICK_ROUTE + "/")
        val link =
          listContent.value
            .filterIsInstance<ListItemContent.LinkItem>()
            .map { it.link.linkData }
            .find { it.target == target }
        if (link != null) {
          onLinkItemClick(link)
        }
      } else {

        _uiEventFlow.send(UiEvent.Navigate(route))
      }
    }
  }

  override fun showSnackbar(message: String, action: String?) {
    viewModelScope.launch { _uiEventFlow.send(UiEvent.ShowSnackbar(message, action)) }
  }

  override fun forceRefresh() {
    viewModelScope.launch { _refreshTrigger.value++ }
  }

  override fun isSelectionModeActive(): Boolean = isSelectionModeActive.value

  override fun toggleSelection(itemId: String) {
    _uiState.update {
      val currentSelection = it.selectedItemIds.toMutableSet()
      if (itemId in currentSelection) {
        currentSelection.remove(itemId)
      } else {
        currentSelection.add(itemId)
      }
      it.copy(selectedItemIds = currentSelection)
    }
  }

  override fun requestAttachmentShare(item: ListItemContent) {
    pendingAttachmentShare = item
    navigateToListChooser("Виберіть проект для вкладення")
  }

  override fun setPendingAction(
    actionType: GoalActionType,
    itemIds: Set<String>,
    goalIds: Set<String>,
  ) {
    savedStateHandle["pendingAction"] = actionType.name
    savedStateHandle["pendingSourceItemIds"] = itemIds.toList()
    savedStateHandle["pendingSourceGoalIds"] = goalIds.toList()

    val title =
      when (actionType) {
        GoalActionType.CreateInstance -> "Створити посилання у..."
        GoalActionType.MoveInstance -> "Перемістити до..."
        GoalActionType.CopyGoal -> "Копіювати до..."
        GoalActionType.AddLinkToList -> "Додати посилання на проект..."
        GoalActionType.ADD_LIST_SHORTCUT -> "Додати ярлик на проект..."
      }
    navigateToListChooser(title)
  }

  override fun updateInputState(
    inputValue: TextFieldValue?,
    inputMode: InputMode?,
    localSearchQuery: String?,
    newlyAddedItemId: String?,
    detectedReminderSuggestion: String?,
    detectedReminderCalendar: Calendar?,
    clearDetectedReminder: Boolean,
  ) {
    _uiState.update { currentState ->
      currentState.copy(
        inputValue = inputValue ?: currentState.inputValue,
        inputMode = inputMode ?: currentState.inputMode,
        localSearchQuery = localSearchQuery ?: currentState.localSearchQuery,
        newlyAddedItemId = newlyAddedItemId,
        detectedReminderSuggestion =
          when {
            clearDetectedReminder -> null
            detectedReminderSuggestion != null -> detectedReminderSuggestion
            else -> currentState.detectedReminderSuggestion
          },
        detectedReminderCalendar =
          when {
            clearDetectedReminder -> null
            detectedReminderCalendar != null -> detectedReminderCalendar
            else -> currentState.detectedReminderCalendar
          },
      )
    }

    Log.d(
      "ProjectScreenViewModel",
      "updateInputState: clearReminder=$clearDetectedReminder, " +
        "suggestion=$detectedReminderSuggestion, " +
        "calendar=${detectedReminderCalendar?.time}",
    )
  }

  override fun updateInputState(inputValue: TextFieldValue) {
    _uiState.update { it.copy(inputValue = inputValue) }
  }

  override fun updateDialogState(
    showAddWebLinkDialog: Boolean?,
    showAddObsidianLinkDialog: Boolean?,
  ) {
    _uiState.update {
      it.copy(
        showAddWebLinkDialog = showAddWebLinkDialog ?: it.showAddWebLinkDialog,
        showAddObsidianLinkDialog = showAddObsidianLinkDialog ?: it.showAddObsidianLinkDialog,
      )
    }
  }

  override fun showRecentListsSheet(show: Boolean) {
    Log.d(
      "Recents_Debug",
      "ProjectScreenViewModel: showRecentListsSheet($show) called. Updating UI state.",
    )
    _uiState.update { it.copy(showRecentProjectsSheet = show) }
  }

  override fun updateSelectionState(selectedIds: Set<String>) {
    Log.d(TAG, "ВИКЛИК: updateSelectionState з ${selectedIds.size} елементами.")
    _uiState.update { it.copy(selectedItemIds = selectedIds) }
  }

  fun onListChooserResult(targetProjectId: String) {
    pendingAttachmentShare?.let { attachment ->
      pendingAttachmentShare = null
      shareAttachmentToProject(attachment, targetProjectId)
      return
    }

    if (inboxHandler.recordForPromotion.value != null) {
      inboxHandler.onListSelectedForInboxPromotion(targetProjectId)
      return
    }

    val actionTypeName = savedStateHandle.get<String>("pendingAction") ?: return
    val actionType = GoalActionType.valueOf(actionTypeName)
    val itemIds = savedStateHandle.get<List<String>>("pendingSourceItemIds") ?: emptyList()
    val goalIds = savedStateHandle.get<List<String>>("pendingSourceGoalIds") ?: emptyList()

    viewModelScope.launch(Dispatchers.IO) {
      when (actionType) {
        GoalActionType.CreateInstance -> goalRepository.createGoalLinks(goalIds, targetProjectId)

        GoalActionType.MoveInstance -> listItemRepository.moveListItems(itemIds, targetProjectId)
        GoalActionType.CopyGoal -> goalRepository.copyGoalsToProject(goalIds, targetProjectId)
        GoalActionType.AddLinkToList -> {
          val targetProject = projectRepository.getProjectById(targetProjectId)
          val link =
            RelatedLink(
              type = LinkType.PROJECT,
              target = targetProjectId,
              displayName = targetProject?.name ?: "Проект без назви",
            )
          val newItemId = projectRepository.addLinkItemToProjectFromLink(projectIdFlow.value, link)
          withContext(Dispatchers.Main) {
            _uiState.update { it.copy(newlyAddedItemId = newItemId) }
          }
        }

        GoalActionType.ADD_LIST_SHORTCUT -> {
          if (goalIds.isNotEmpty()) {
            val subprojectToLinkId = goalIds.first()
            val newItemId =
              listItemRepository.addProjectLinkToProject(subprojectToLinkId, targetProjectId)
            withContext(Dispatchers.Main) {
              _uiState.update { it.copy(newlyAddedItemId = newItemId) }
            }
          } else {
            val newItemId =
              listItemRepository.addProjectLinkToProject(targetProjectId, projectIdFlow.value)
            withContext(Dispatchers.Main) {
              _uiState.update { it.copy(newlyAddedItemId = newItemId) }
            }
          }
        }
      }
      withContext(Dispatchers.Main) { forceRefresh() }
    }
    savedStateHandle.remove<String>("pendingAction")
    savedStateHandle.remove<List<String>>("pendingSourceItemIds")
    savedStateHandle.remove<List<String>>("pendingSourceGoalIds")
    selectionHandler.clearSelection()
  }

  private fun navigateToListChooser(title: String) {
    viewModelScope.launch {
      val encodedTitle = URLEncoder.encode(title, "UTF-8")
      val disabledIds = projectIdFlow.value
      _uiEventFlow.send(
        UiEvent.Navigate("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds")
      )
    }
  }

  fun onHighlightShown() {
    _uiState.update { it.copy(goalToHighlight = null, itemToHighlight = null) }
  }

  fun onInboxHighlightShown() {
    Log.d(TAG, "Clearing inbox highlight state.")

    _uiState.update { it.copy(inboxRecordToHighlight = null) }
  }

  fun onScrolledToNewItem() {
    _uiState.update { it.copy(newlyAddedItemId = null) }
  }

  private fun shareAttachmentToProject(
    attachment: ListItemContent,
    targetProjectId: String,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val isAttachmentSupported =
        attachment is ListItemContent.LinkItem ||
          attachment is ListItemContent.NoteDocumentItem ||
          attachment is ListItemContent.ChecklistItem
      if (!isAttachmentSupported) {
        withContext(Dispatchers.Main) {
          showSnackbar("Цей тип вкладення не підтримує копіювання", null)
        }
        return@launch
      }

      projectRepository.linkAttachmentToProject(attachment.listItem.id, targetProjectId)
      withContext(Dispatchers.Main) {
        if (targetProjectId == projectIdFlow.value) {
          _uiState.update { it.copy(newlyAddedItemId = attachment.listItem.id) }
          forceRefresh()
        }
        showSnackbar("Вкладення додано до вибраного проєкту", null)
      }
    }
  }

  fun onMove(fromIndex: Int, toIndex: Int) {
    Log.d(TAG, "onMove called with fromIndex: $fromIndex, toIndex: $toIndex")
    viewModelScope.launch {
        val currentContent = _listContent.value.toMutableList()
        val movedItem = currentContent.removeAt(fromIndex)
        currentContent.add(toIndex, movedItem)
        _listContent.value = currentContent
        saveListOrder(currentContent)
    }
  }

  fun onMoveToTop(item: ListItemContent) {
    val fromIndex = _listContent.value.indexOf(item)
    if (fromIndex != -1) {
        onMove(fromIndex, 0)
        viewModelScope.launch { _uiEventFlow.send(UiEvent.ScrollTo(0)) }
    }
  }

  private suspend fun saveListOrder(listToSave: List<ListItemContent>) =
    withContext(Dispatchers.IO) {
      Log.d(TAG, "[saveListOrder] Starting to save order for ${listToSave.size} items.")
      try {
        val attachmentOrders = mutableListOf<Pair<String, Long>>()
        val updatedItems =
          listToSave.mapIndexedNotNull { index, content ->
            val order = index.toLong()
            when (content.listItem.itemType) {
              ListItemTypeValues.LINK_ITEM,
              ListItemTypeValues.NOTE_DOCUMENT,
              ListItemTypeValues.CHECKLIST -> {
                attachmentOrders += content.listItem.id to order
                null
              }
              else -> content.listItem.copy(order = order)
            }
          }
        if (updatedItems.isNotEmpty()) {
          listItemRepository.updateListItemsOrder(updatedItems)
        }
        if (attachmentOrders.isNotEmpty()) {
          projectRepository.updateAttachmentOrders(projectIdFlow.value, attachmentOrders)
        }
        Log.d(TAG, "[saveListOrder] Successfully saved new order to the database.")
      } catch (e: Exception) {
        Log.e(TAG, "[saveListOrder] Failed to save list order", e)
      }
    }

  fun onStateRefreshed() {
    _uiState.update { it.copy(needsStateRefresh = false) }
  }

  fun onTagClicked(tag: String) {
    viewModelScope.launch {
      val encodedTag = URLEncoder.encode(tag, "UTF-8")
      requestNavigation("global_search_screen/$encodedTag")
    }
  }

  fun onSwipeStart(itemId: String) {
    if (_uiState.value.swipedItemId != itemId) {
      _uiState.update { it.copy(swipedItemId = itemId) }
    }
  }

  fun onSwipeStateReset(itemId: String) {
    _uiState.update { currentState ->
      val newTriggers = currentState.resetTriggers.toMutableMap()
      newTriggers[itemId] = (newTriggers[itemId] ?: 0) + 1
      currentState.copy(resetTriggers = newTriggers, swipedItemId = null)
    }
  }

  fun onNoteItemClick(note: LegacyNote) {
    viewModelScope.launch {
      recentItemsRepository.logNoteAccess(note)
          // legacy notes no longer have dedicated editor; no-op
    }
  }

  fun onNoteDocumentItemClick(noteDocument: NoteDocument) {
    viewModelScope.launch {
      recentItemsRepository.logNoteDocumentAccess(noteDocument)
      _uiEventFlow.send(UiEvent.Navigate("note_document_screen/${noteDocument.id}"))
    }
  }

  fun onChecklistItemClick(checklist: Checklist) {
    viewModelScope.launch {
      recentItemsRepository.logChecklistAccess(checklist)
      _uiEventFlow.send(UiEvent.Navigate("checklist_screen?checklistId=${checklist.id}"))
    }
  }

  fun onLinkItemClick(link: RelatedLink) {
    Log.d(TAG, "onLinkItemClick: Clicked link with type=${link.type}, target=${link.target}")
    viewModelScope.launch {
      when (link.type) {
        LinkType.PROJECT -> {
          val projectName = link.displayName ?: "Project"
          enhancedNavigationManager.navigateToProject(link.target, projectName)
        }
        LinkType.OBSIDIAN -> {
          recentItemsRepository.logObsidianLinkAccess(link)
          val vaultName = settingsRepository.obsidianVaultNameFlow.first()
          if (vaultName.isNotBlank()) {
            val encodedNoteName = URLEncoder.encode(link.target, "UTF-8")
            val uri = "obsidian://open?vault=$vaultName&file=$encodedNoteName"
            _uiEventFlow.send(UiEvent.OpenUri(uri))
          } else {
            _uiEventFlow.send(UiEvent.ShowSnackbar("Obsidian vault name is not configured."))
          }
        }
        else -> {
          _uiEventFlow.send(UiEvent.HandleLinkClick(link))
        }
      }
    }
  }

  fun flushPendingMoves() {
    batchSaveJob?.cancel()
  }

  override fun onCleared() {
    super.onCleared()
    batchSaveJob?.cancel()
    inputHandler.cleanup()
  }

  override fun createObsidianNote(noteName: String) {
    viewModelScope.launch {
      val vaultName = settingsRepository.obsidianVaultNameFlow.first()
      val encodedNoteName = URLEncoder.encode(noteName, "UTF-8")
      val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
      _uiEventFlow.send(UiEvent.OpenUri(uri))
    }
  }

  fun onAddAttachment(type: AttachmentType) {
    when (type) {
      AttachmentType.NOTES -> {
        viewModelScope.launch {
          _uiEventFlow.send(
            UiEvent.Navigate("note_document_edit_screen?projectId=${projectIdFlow.value}")
          )
        }
      }
      AttachmentType.WEB_LINK -> inputHandler.onShowAddWebLinkDialog()
      AttachmentType.OBSIDIAN_LINK -> inputHandler.onShowAddObsidianLinkDialog()
      AttachmentType.PROJECT_LINK -> inputHandler.onAddListLinkRequest()
      AttachmentType.PROJECT_SHORTCUT -> inputHandler.onAddListShortcutRequest()
      AttachmentType.CHECKLIST -> {
        val projectId = projectIdFlow.value
        if (projectId.isNotBlank()) {
          viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("checklist_screen?projectId=$projectId"))
          }
        } else {
          showSnackbar("Не вдалося визначити проект для створення чекліста", null)
        }
      }
    }
  }

  override fun copyToClipboard(text: String, label: String) {
    val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
  }

  fun deleteCurrentProject() {
    viewModelScope.launch(Dispatchers.IO) {
      val projectId = projectIdFlow.value
      if (projectId.isNotEmpty()) {
        // TODO: [GM-29] Implement recursive deletion for subprojects. For now, only deletes the current project.
        projectRepository.deleteProject(projectId)
        withContext(Dispatchers.Main) { requestNavigation("back") }
      }
    }
  }

  override fun scrollToListEnd() {
    viewModelScope.launch { _uiEventFlow.send(UiEvent.ScrollToLatestInboxRecord) }
  }

  fun onProjectViewChange(newView: ProjectViewMode) {
    Log.d("ATTACHMENT_DEBUG", "VM: onProjectViewChange(newView = $newView) called.")
    _uiState.update {
      Log.d("ATTACHMENT_DEBUG", "VM: Updating uiState.currentView to $newView.")
      it.copy(currentView = newView, inputMode = getInputModeForView(newView))
    }
    viewModelScope.launch { projectRepository.updateProjectViewMode(projectIdFlow.value, newView) }
  }

  fun onDashboardTabSelected(tab: ProjectManagementTab) {
    _uiState.update { it.copy(selectedDashboardTab = tab) }
  }

  override fun addQuickRecord(text: String) {
    inboxHandler.addQuickRecord(text)
  }

  fun copyInboxRecordText(text: String) {
    copyToClipboard(text, "Inbox Record")
  }

  fun onImportFromMarkdownRequest() {
    _uiState.update { it.copy(showImportFromMarkdownDialog = true) }
  }

  fun onImportFromMarkdownDismiss() {
    _uiState.update { it.copy(showImportFromMarkdownDialog = false) }
  }

  fun onImportFromMarkdownConfirm(markdownText: String) {
    inboxMarkdownHandler.importFromMarkdown(markdownText, projectIdFlow.value)
    onImportFromMarkdownDismiss()
  }

  fun onExportToMarkdownRequest() {
    inboxMarkdownHandler.exportToMarkdown(inboxHandler.inboxRecords.value)
  }

  fun onImportBacklogFromMarkdownRequest() {
    _uiState.update { it.copy(showImportBacklogFromMarkdownDialog = true) }
  }

  fun onImportBacklogFromMarkdownDismiss() {
    _uiState.update { it.copy(showImportBacklogFromMarkdownDialog = false) }
  }

  fun onImportBacklogFromMarkdownConfirm(markdownText: String) {
    backlogMarkdownHandler.importFromMarkdown(markdownText, projectIdFlow.value)
    onImportBacklogFromMarkdownDismiss()
  }

  fun onExportBacklogToMarkdownRequest() {
    backlogMarkdownHandler.exportToMarkdown(listContent.value)
  }

  fun onExportProjectStateRequest() {
    projectMarkdownExporter.exportProjectStateToMarkdown(
      project = project.value,
      backlog = listContent.value,
      logs = projectLogs.value,
      listener = this,
    )
  }

  fun onSubprojectCompletedChanged(subproject: Project, isCompleted: Boolean) {
    viewModelScope.launch {
      val updatedSubproject = subproject.copy(isCompleted = isCompleted)
      projectRepository.upsertProject(updatedSubproject)
      forceRefresh()
    }
  }

  fun onClearDetectedReminder() {
    inputHandler.onClearDetectedReminder()
  }

  fun onStartTrackingRequest(item: ListItemContent) {
    viewModelScope.launch {
      val result =
        when (item) {
          is ListItemContent.GoalItem -> {
            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                goalId = item.goal.id,
                startTime = System.currentTimeMillis(),
                name = item.goal.text,
                description = null,
                endTime = null,
                totalTimeSpentMinutes = 0,
                tags = emptyList(),
                relatedLinks = emptyList(),
                isCompleted = false,
                activityType = "GOAL",
                parentProjectId = item.listItem.projectId,
                createdAt = System.currentTimeMillis()
            )
            activityRepository.upsertActivityRecord(record)
            record to "Відстежую ціль"
          }

          is ListItemContent.SublistItem -> {
            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                projectId = item.project.id,
                startTime = System.currentTimeMillis(),
                name = item.project.name,
                description = null,
                endTime = null,
                totalTimeSpentMinutes = 0,
                tags = emptyList(),
                relatedLinks = emptyList(),
                isCompleted = false,
                activityType = "PROJECT",
                parentProjectId = item.project.parentId,
                createdAt = System.currentTimeMillis()
            )
            activityRepository.upsertActivityRecord(record)
            record to "Відстежую проєкт"
          }

          is ListItemContent.LinkItem,
          is ListItemContent.NoteItem,
          is ListItemContent.NoteDocumentItem,
          is ListItemContent.ChecklistItem -> null to null
        }

      val (newRecord, message) = result
      if (newRecord != null && message != null) {
        pendingActivityForReminder = newRecord
        showSnackbar(message, "Обмежити в часі")
      }
    }
  }

  fun onLimitLastActivityRequested() {
    _uiState.update { it.copy(recordForReminderDialog = pendingActivityForReminder) }
    pendingActivityForReminder = null
  }

  fun onReminderDialogDismiss() {
    _uiState.update { it.copy(recordForReminderDialog = null, remindersForDialog = emptyList()) }
  }

  fun onSetReminder(timestamp: Long) =
    viewModelScope.launch {
      val record = _uiState.value.recordForReminderDialog ?: return@launch

      val entityType = when {
          record.goalId != null -> "GOAL"
          record.projectId != null -> "PROJECT"
          else -> "TASK" // Assuming ActivityRecord can also be a task
      }
      val entityId = record.goalId ?: record.projectId ?: record.id

      reminderRepository.createReminder(entityId, entityType, timestamp)

      // onReminderDialogDismiss() // Don't dismiss, so user can add more
      showSnackbar(
        "Нагадування додано на ${
                    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(
                        Date(timestamp)
                    )
                }",
        null,
      )
      forceRefresh()
    }

  fun onRemoveReminder(time: Long) = viewModelScope.launch {
      val reminderToRemove = _uiState.value.remindersForDialog.find { it.reminderTime == time }
      if (reminderToRemove != null) {
          reminderRepository.removeReminder(reminderToRemove)
          showSnackbar("Нагадування видалено", null)
          forceRefresh()
      }
  }

  fun onClearReminder() =
    viewModelScope.launch {
      val record = _uiState.value.recordForReminderDialog ?: return@launch

      val entityId = record.goalId ?: record.projectId ?: record.id
      reminderRepository.clearRemindersForEntity(entityId)

      onReminderDialogDismiss()
      showSnackbar("Нагадування скасовано", null)
      forceRefresh()
    }

  fun onSetReminderForItem(item: ListItemContent) {
    viewModelScope.launch {
        val reminders = when (item) {
            is ListItemContent.GoalItem -> item.reminders
            is ListItemContent.SublistItem -> item.reminders
            else -> emptyList()
        }

        val record =
            when (item) {
                is ListItemContent.GoalItem -> {
                    ActivityRecord(
                        id = item.goal.id,
                        text = item.goal.text,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.goal.createdAt,
                        projectId = item.listItem.projectId,
                        goalId = item.goal.id,
                    )
                }
                is ListItemContent.SublistItem -> {
                    ActivityRecord(
                        id = item.project.id,
                        text = item.project.name,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.project.createdAt,
                        projectId = item.project.id,
                        goalId = null,
                    )
                }
                else -> null
            }
        _uiState.update { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
    }
  }

  fun onSetReminderForProject() {
    viewModelScope.launch {
        project.value?.let { proj ->
            val reminders = reminderRepository.getRemindersForEntityFlow(proj.id).firstOrNull()
            val record = ActivityRecord(
                id = proj.id,
                text = proj.name,
                reminderTime = reminders?.firstOrNull()?.reminderTime,
                createdAt = proj.createdAt,
                projectId = proj.id,
                goalId = null,
            )
            _uiState.update { it.copy(recordForReminderDialog = record) }
        }
    }
  }

  fun stopOngoingActivity() {
    viewModelScope.launch {
      lastOngoingActivity.value?.let {
        val updatedRecord = it.copy(endTime = System.currentTimeMillis())
        activityRepository.upsertActivityRecord(updatedRecord)
      }
    }
  }

  fun setReminderForOngoingActivity() {
    viewModelScope.launch {
      lastOngoingActivity.value?.let {
        _uiState.update { it.copy(recordForReminderDialog = lastOngoingActivity.value) }
      }
    }
  }

  fun onStartTrackingCurrentProject() {
    val currentProjectId = projectIdFlow.value
    if (currentProjectId.isBlank()) return

    viewModelScope.launch {
        project.value?.let {
            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                projectId = it.id,
                startTime = System.currentTimeMillis(),
                name = it.name,
                description = null,
                endTime = null,
                totalTimeSpentMinutes = 0,
                tags = emptyList(),
                relatedLinks = emptyList(),
                isCompleted = false,
                activityType = "PROJECT",
                parentProjectId = it.parentId,
                createdAt = System.currentTimeMillis()
            )
            activityRepository.upsertActivityRecord(record)
            showSnackbar("Відстежую проєкт", "Обмежити в часі")
            pendingActivityForReminder = record
        }
    }
  }

  fun onToggleProjectManagement() {
    viewModelScope.launch {
      val proj = project.value ?: return@launch
      val currentState = proj.isProjectManagementEnabled ?: false
      val newState = !currentState
      val currentView = uiState.value.currentView

      val updatedProject = proj.copy(isProjectManagementEnabled = newState)
      projectRepository.upsertProject(updatedProject)

      if (newState) {
                    onProjectViewChange(ProjectViewMode.ADVANCED)        } else if (currentView == ProjectViewMode.ADVANCED) {
        onProjectViewChange(ProjectViewMode.BACKLOG)
      }
    }
  }

  fun onRecalculateTime() {
    val currentProjectId = projectIdFlow.value
    if (currentProjectId.isNotBlank()) {
      viewModelScope.launch {
        // TODO: [GM-28] ProjectTimeMetrics seems to be an old model. Needs to be replaced with a new KMP model or refactored.
        // val metrics = projectRepository.calculateProjectTimeMetrics(currentProjectId)
        // _uiState.update { it.copy(projectTimeMetrics = metrics) }

        projectRepository.recalculateAndLogProjectTime(currentProjectId)
      }
    }
  }

  fun onDeleteEverywhere(item: ListItemContent) {
    viewModelScope.launch {
        if (item is ListItemContent.GoalItem) {
            goalRepository.deleteGoal(item.goal.id)
        }
    }
  }


  fun addItemToDailyPlan(itemContent: ListItemContent) {
    viewModelScope.launch {
      val today = System.currentTimeMillis()
      val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)

      val task =
        when (itemContent) {
          is ListItemContent.GoalItem -> {
            dayManagementRepository.addGoalToDayPlan(dayPlan.id, itemContent.goal.id)
          }
          is ListItemContent.SublistItem -> {
            dayManagementRepository.addProjectToDayPlan(dayPlan.id, itemContent.project.id)
          }
          is ListItemContent.LinkItem -> null
          is ListItemContent.NoteItem -> null
          is ListItemContent.NoteDocumentItem -> null
          is ListItemContent.ChecklistItem -> null
        }

      if (task != null) {
        showSnackbar("Додано до плану на сьогодні", null)
      } else {
        showSnackbar("Цей тип елемента неможливо додати до плану", null)
      }
    }
  }

  fun addCurrentProjectToDayPlan() {
    val currentProjectId = projectIdFlow.value
    if (currentProjectId.isBlank()) {
      showSnackbar("Неможливо додати, проект не визначено", null)
      return
    }

    viewModelScope.launch {
      val today = System.currentTimeMillis()
      val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
      dayManagementRepository.addProjectToDayPlan(dayPlan.id, currentProjectId)
      showSnackbar("Проект додано до плану на сьогодні", null)
    }
  }

  fun onExportBacklogRequest() {
    _uiState.update { it.copy(showShareDialog = true) }
  }

  fun onShareDialogDismiss() {
    _uiState.update { it.copy(showShareDialog = false) }
  }

  fun getBacklogAsMarkdown(): String {
    val markdownBuilder = StringBuilder()
    listContent.value.forEach { item ->
        val line =
            when (item) {
                is ListItemContent.GoalItem -> {
                    val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                    "$checkbox ${item.goal.text}"
                }
                is ListItemContent.SublistItem -> "- [С] ${item.project.name}"
                is ListItemContent.LinkItem -> {
                    val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                    "- [Л] [$displayName](${item.link.linkData.target})"
                }
                is ListItemContent.NoteItem -> "- [Н] ${item.note.title}"
                is ListItemContent.NoteDocumentItem -> "- [К] ${item.document.name}"
                is ListItemContent.ChecklistItem -> "- [Ч] ${item.checklist.name}"
            }
        markdownBuilder.appendLine(line)
    }
    return markdownBuilder.toString()
  }

  fun onCopyToClipboardRequest() {
    val markdownText = getBacklogAsMarkdown()
    copyToClipboard(markdownText, "Backlog Export")
    showSnackbar("Беклог скопійовано", null)
    onShareDialogDismiss()
  }

  fun onTransferBacklogToServerRequest() {
    viewModelScope.launch {
        val url = settingsRepository.getFastApiUrl().first()
        if (url.isNullOrBlank()) {
            showSnackbar("Server address is not available. Check settings.", null)
            return@launch
        }
        Log.d(TAG, "onTransferBacklogViaWifi: Ініційовано передачу на URL: $url")
        executeBacklogTransfer(url)
    }
  }

  private fun executeBacklogTransfer(url: String) {
    Log.d(TAG, "executeBacklogTransfer: Початок підготовки даних для відправки.")
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val markdownContent = getBacklogAsMarkdown()

        if (markdownContent.isBlank()) {
          withContext(Dispatchers.Main) {
            showSnackbar("Беклог порожній. Нічого передавати.", null)
          }
          return@launch
        }

        val filename = project.value?.name ?: "backlog_export"

        val requestBody = FileDataRequest(filename = filename, content = markdownContent)

        Log.d(TAG, "executeBacklogTransfer: Дані підготовлено. Відправка на: $url")

        val response = RetrofitClient.getInstance(application, url).uploadFileAsJson(requestBody)

        withContext(Dispatchers.Main) {
          if (response.isSuccessful) {
            Log.d(
              TAG,
              "executeBacklogTransfer: Успішна відповідь від сервера. Код: ${response.code()}",
            )
            showSnackbar("Беклог успішно передано", null)
          } else {
            val errorMsg = response.errorBody()?.string() ?: "Невідома помилка"
            Log.e(
              TAG,
              "executeBacklogTransfer: Помилка від сервера. Код: ${response.code()}, Повідомлення: $errorMsg",
            )
            showSnackbar("Помилка: ${response.code()} - $errorMsg", null)
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Log.e(TAG, "executeBacklogTransfer: Критична помилка мережі.", e)
          showSnackbar("Помилка мережі: ${e.message}", null)
        }
      }
    }
  }

  // TODO: [GM-27] This method is dependent on old navigation/search use cases and needs to be refactored.
  /*
  fun onHomeClick() {
    if (_isProcessingHome.value) {
      Log.w(TAG, "Home click ignored - already processing")
      return
    }

    viewModelScope.launch {
      _isProcessingHome.value = true

      try {
        Log.d(TAG, "Starting home navigation with UseCase")

        clearAndNavigateHomeUseCase.execute(
          command = com.romankozak.forwardappmobile.ui.navigation.ClearCommand.Home,
          context = createClearExecutionContext(),
        )

        Log.d(TAG, "Home navigation completed successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Error during home navigation", e)
        viewModelScope.launch {
          _uiEventFlow.send(UiEvent.ShowSnackbar("Navigation error: ${e.message}"))
        }
      } finally {
        _isProcessingHome.value = false
      }
    }
  }
  */

  // TODO: [GM-27] This method is dependent on old navigation/search use cases and needs to be refactored.
  /*
  private fun createClearExecutionContext():
    com.romankozak.forwardappmobile.ui.navigation.ClearExecutionContext {
    return com.romankozak.forwardappmobile.ui.navigation.createClearExecutionContext(
      currentProjects = _allProjects.value,
      subStateStack =
        MutableStateFlow(
          listOf(
            com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState.Hierarchy
          )
        ),
      searchUseCase = searchUseCase, // This will be injected
      planningModeManager =
        com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager(),
      enhancedNavigationManager = enhancedNavigationManager,
      uiEventChannel =
        Channel<com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent>(),
    )
  }
  */

  fun onForwardPressed() {
    enhancedNavigationManager.goForward()
  }

  private fun updateProjectNameInHistory(newName: String) {}

  fun onRevealInExplorer(currentProjectId: String) {
    if (currentProjectId.isEmpty()) return

    enhancedNavigationManager.navigateHomeWithResult(
      key = "project_to_reveal",
      value = currentProjectId,
    )
  }

  fun onBackPressed(): Boolean {
    Log.d(TAG, "onBackPressed TRIGGERED")
    val state = uiState.value

    if (inboxHandler.recordToEdit.value != null) {
      inboxHandler.onInboxRecordEditDismiss()
      return true
    }

    if (state.showShareDialog) {
      onShareDialogDismiss()
      return true
    }
    if (state.recordForReminderDialog != null) {
      onReminderDialogDismiss()
      return true
    }
    if (state.showImportBacklogFromMarkdownDialog) {
      onImportBacklogFromMarkdownDismiss()
      return true
    }
    if (state.showImportFromMarkdownDialog) {
      onImportFromMarkdownDismiss()
      return true
    }
    if (state.showRecentProjectsSheet) {
      inputHandler.onDismissRecentLists()
      return true
    }
    if (state.showAddWebLinkDialog || state.showAddObsidianLinkDialog) {
      inputHandler.onDismissLinkDialogs()
      return true
    }

    if (state.inputMode == InputMode.SearchInList && state.inputValue.text.isNotEmpty()) {
      Log.d(TAG, "Action: Clearing input field because we are in search mode.")
      inputHandler.onInputTextChanged(TextFieldValue(""), state.inputMode)
      return true
    }

    if (state.inputMode == InputMode.SearchInList) {
      onCloseSearch()
      return true
    }

    if (isSelectionModeActive.value) {
      Log.d(TAG, "Action: Clearing selection.")
      selectionHandler.clearSelection()
      return true
    }

    if (enhancedNavigationManager.canGoBack.value) {
      Log.d(TAG, "Action: Navigating back via EnhancedNavigationManager.")
      flushPendingMoves()
      enhancedNavigationManager.goBack()
      return true
    }

    Log.d(TAG, "Action: No local actions or history, letting system handle back press.")
    flushPendingMoves()
    return false
  }

  fun onCloseSearch() {
    Log.d(TAG, "Action: Closing local search.")

    updateInputState(
      localSearchQuery = "",
      inputValue = TextFieldValue(""),
      inputMode = getInputModeForView(uiState.value.currentView),
    )
  }

  fun onToggleNavPanelMode() {
    _uiState.update { it.copy(isViewModePanelVisible = !it.isViewModePanelVisible) }
  }

  fun onOpenRemindersDialog(item: ListItemContent) {
    _uiState.update { it.copy(showRemindersDialog = true, itemForRemindersDialog = item) }
  }

  fun onDismissRemindersDialog() {
    _uiState.update { it.copy(showRemindersDialog = false, itemForRemindersDialog = null) }
  }

  fun onSubprojectClick(subproject: ListItemContent.SublistItem) {
    viewModelScope.launch {
      enhancedNavigationManager.navigateToProject(subproject.project.id, subproject.project.name)
    }
  }

  fun onRecentItemClick(item: RecentItem) {
    _uiState.update { it.copy(showRecentProjectsSheet = false) }
    viewModelScope.launch {
      when (item.type) {
        "PROJECT" -> {
          projectRepository.getProjectById(item.target)?.let { recentItemsRepository.logProjectAccess(it) }
          enhancedNavigationManager.navigateToProject(item.target, item.displayName)
        }
        "NOTE" -> _uiEventFlow.send(UiEvent.ShowSnackbar("Застарілі нотатки доступні лише для читання"))
        "NOTE_DOCUMENT" -> {
          noteDocumentRepository.getDocumentById(item.target)?.let { recentItemsRepository.logNoteDocumentAccess(it) }
          _uiEventFlow.send(UiEvent.Navigate("note_document_screen/${item.target}"))
        }
        "CHECKLIST" -> {
          checklistRepository.getChecklistById(item.target)?.let { recentItemsRepository.logChecklistAccess(it) }
          _uiEventFlow.send(UiEvent.Navigate("checklist_screen?checklistId=${item.target}"))
        }
        "OBSIDIAN_LINK" -> {
          val link =
            RelatedLink(
              type = LinkType.OBSIDIAN,
              target = item.target,
              displayName = item.displayName,
            )
          recentItemsRepository.logObsidianLinkAccess(link)
          onLinkItemClick(link)
        }
      }
    }
  }

  fun onPinRecentItem(item: RecentItem) {
    viewModelScope.launch {
      recentItemsRepository.updateRecentItem(item.copy(isPinned = !item.isPinned))
    }
  }

  fun onChildProjectClick(childProject: Project) {
    viewModelScope.launch {
      enhancedNavigationManager.navigateToProject(childProject.id, childProject.name)
    }
  }

  fun onDismissCreateNoteDocumentDialog() {
    _uiState.update { it.copy(showCreateNoteDocumentDialog = false) }
  }

  fun onShowCreateNoteDocumentDialog() {
    _uiState.update { it.copy(showNoteDocumentEditor = true) }
  }

  fun onCreateChecklist() {
    val projectId = projectIdFlow.value
    if (projectId.isBlank()) {
      showSnackbar("Не вдалося створити чекліст для невідомого проєкту", null)
      return
    }
    viewModelScope.launch {
      _uiEventFlow.send(UiEvent.Navigate("checklist_screen?projectId=$projectId"))
    }
  }

  fun onCreateNoteDocument(title: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(showCreateNoteDocumentDialog = false) }
      _uiEventFlow.send(
        UiEvent.Navigate("note_document_edit_screen?projectId=${projectIdFlow.value}")
      )
    }
  }

  fun onCleanupDatabase() {
    viewModelScope.launch {
      // TODO: [GM-30] This method needs to be re-implemented or replaced with a new mechanism for cleaning up dangling list items.
      // projectRepository.cleanupDanglingListItems()
      forceRefresh()
    }
  }

    fun onEditLogEntry(log: ProjectExecutionLog) {
        _uiState.update { it.copy(logEntryToEdit = log) }
    }

    fun onDeleteLogEntry(log: ProjectExecutionLog) {
        viewModelScope.launch {
            projectLogRepository.deleteLog(log.id)
        }
    }

    fun onUpdateLogEntry(description: String, details: String?) {
        viewModelScope.launch {
            val logToUpdate = uiState.value.logEntryToEdit ?: return@launch
            val updatedLog = logToUpdate.copy(
                description = description,
                details = details
            )
            projectLogRepository.updateLogDetails(updatedLog.id, updatedLog.description, updatedLog.details)
            onDismissEditLogEntryDialog()
        }
    }

    fun onDismissEditLogEntryDialog() {
        _uiState.update { it.copy(logEntryToEdit = null) }
    }

    fun onSaveNoteDocument(content: String) {
        viewModelScope.launch {
            val title = content.lines().firstOrNull()?.trim() ?: "Новий документ"
            noteDocumentRepository.createDocument(title, projectIdFlow.value, content)
            onDismissNoteDocumentEditor()
        }
    }

    fun onDismissNoteDocumentEditor() {
        _uiState.update { it.copy(showNoteDocumentEditor = false) }
    }

    fun onAddMilestone() {
        viewModelScope.launch {
            val log = ProjectExecutionLog(
                id = UUID.randomUUID().toString(),
                projectId = projectIdFlow.value,
                type = ProjectLogEntryTypeValues.MILESTONE,
                description = "New Milestone",
                createdAt = System.currentTimeMillis()
            )
            projectLogRepository.upsertLog(log)
        }
    }

    fun onSaveArtifact(content: String) {
        viewModelScope.launch {
            val currentArtifact = projectArtifact.value
            if (currentArtifact == null) {
                projectArtifactRepository.createProjectArtifact(
                    ProjectArtifact(
                        id = UUID.randomUUID().toString(),
                        projectId = projectIdFlow.value,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                projectArtifactRepository.updateProjectArtifact(currentArtifact.copy(content = content, updatedAt = System.currentTimeMillis()))
            }
            onDismissArtifactEditor()
        }
    }

    fun onEditArtifact(artifact: ProjectArtifact) {
        _uiState.update { it.copy(artifactToEdit = artifact) }
    }

    fun onDismissArtifactEditor() {
        _uiState.update { it.copy(artifactToEdit = null) }
    }

    fun onShowDisplayPropertiesDialog() {
        _uiState.update { it.copy(showDisplayPropertiesDialog = true) }
    }

    fun onDismissDisplayPropertiesDialog() {
        _uiState.update { it.copy(showDisplayPropertiesDialog = false) }
    }






}
