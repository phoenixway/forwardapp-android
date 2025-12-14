package com.romankozak.forwardappmobile.ui.screens.projectscreen

import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SearchUseCase

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


import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.domain.reminders.cancelForActivityRecord
import com.romankozak.forwardappmobile.domain.reminders.scheduleForActivityRecord
import com.romankozak.forwardappmobile.domain.wifirestapi.FileDataRequest
import com.romankozak.forwardappmobile.domain.wifirestapi.RetrofitClient
import com.romankozak.forwardappmobile.ui.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.navigation.NavTarget
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.TagUtils
import com.romankozak.forwardappmobile.features.attachments.ui.project.AttachmentType
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.ui.features.backlog.withCompletedAtEnd
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxHandlerResultListener
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxMarkdownHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.ItemActionHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.ProjectMarkdownExporter
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.SelectionHandler
import com.romankozak.forwardappmobile.ui.common.editor.NoteTitleExtractor
import com.romankozak.forwardappmobile.data.repository.ProjectStructureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLEncoder
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectManagementTab
import java.util.UUID
import kotlinx.coroutines.withContext

private const val TAG = "BacklogVM_DEBUG"

sealed class UiEvent {
  data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()

  data class Navigate(val target: NavTarget) : UiEvent()

  data class ResetSwipeState(val itemId: String) : UiEvent()

  data class ScrollTo(val index: Int) : UiEvent()

  data class NavigateBackAndReveal(val projectId: String) : UiEvent()

  data class HandleLinkClick(val link: RelatedLink) : UiEvent()

  data class OpenUri(val uri: String) : UiEvent()

  data object ScrollToLatestInboxRecord : UiEvent()

  data object NavigateBack : UiEvent()
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
  val swipeResetCounter: Int = 0,
  val showAddWebLinkDialog: Boolean = false,
  val showAddObsidianLinkDialog: Boolean = false,
  val itemToHighlight: String? = null,
  val inboxRecordToHighlight: String? = null,
  val needsStateRefresh: Boolean = false,
  val enableInbox: Boolean = true,
  val enableLog: Boolean = true,
  val enableArtifact: Boolean = true,
  val isProjectManagementEnabled: Boolean = false,
  val enableBacklog: Boolean = true,
  val enableDashboard: Boolean = true,
  val enableAttachments: Boolean = true,
  val currentView: ProjectViewMode = ProjectViewMode.BACKLOG,
  val showRecentProjectsSheet: Boolean = false,
  val showImportFromMarkdownDialog: Boolean = false,
  val showImportBacklogFromMarkdownDialog: Boolean = false,
  val refreshTrigger: Int = 0,
  val detectedReminderSuggestion: String? = null,
  val detectedReminderCalendar: Calendar? = null,
  val nerState: NerState = NerState.NotInitialized,
  val recordForReminderDialog: ActivityRecord? = null,
  val projectTimeMetrics: ProjectTimeMetrics? = null,
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
  
  interface BacklogMarkdownHandlerResultListener {  fun copyToClipboard(text: String, label: String)

  fun showSnackbar(message: String, action: String?)

  fun forceRefresh()
}

class BacklogMarkdownHandler
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
  private val scope: CoroutineScope,
  private val listener: BacklogMarkdownHandlerResultListener,
) {
  fun exportToMarkdown(content: List<ListItemContent>) {
    if (content.isEmpty()) {
      listener.showSnackbar("Беклог порожній. Нічого експортувати.", null)
      return
    }
    val markdownBuilder = StringBuilder()
    content.forEach { item ->
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
    val markdownText = markdownBuilder.toString()
    listener.copyToClipboard(markdownText, "Backlog Export")
    listener.showSnackbar("Беклог скопійовано у буфер обміну.", null)
  }

  fun importFromMarkdown(markdownText: String, projectId: String) {
    if (markdownText.isBlank()) {
      listener.showSnackbar("Нічого імпортувати.", null)
      return
    }
    scope.launch(Dispatchers.IO) {
      val lines = markdownText.lines().filter { it.isNotBlank() }
      var importedCount = 0
      for (line in lines) {
        try {
          val trimmedLine = line.trim()
          when {
            trimmedLine.startsWith("- [ ]") -> {
              val goalText = trimmedLine.removePrefix("- [ ]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                importedCount++
              }
            }

            trimmedLine.startsWith("- [x]") -> {
              val goalText = trimmedLine.removePrefix("- [x]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToProject(goalText, projectId, completed = true)
                importedCount++
              }
            }
          }
        } catch (e: Exception) {
          Log.e("BacklogMarkdownHandler", "Failed to import line: $line", e)
        }
      }
      withContext(Dispatchers.Main) {
        listener.showSnackbar("Імпортовано $importedCount елементів.", null)
        listener.forceRefresh()
      }
    }
  }
}

private val ActivityRecord.isOngoing: Boolean
  get() = this.startTime != null && this.endTime == null



@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BacklogViewModel
@Inject
constructor(
  private val searchUseCase: SearchUseCase,
  private val application: Application,
  private val projectRepository: ProjectRepository,
  private val settingsRepository: SettingsRepository,
  private val contextHandler: ContextHandler,
  private val alarmScheduler: AlarmScheduler,
  private val nerManager: NerManager,
  private val reminderParser: ReminderParser,
  private val activityRepository: ActivityRepository,
  private val projectMarkdownExporter: ProjectMarkdownExporter,
  private val savedStateHandle: SavedStateHandle,
  private val dayManagementRepository: DayManagementRepository,
  private val clearAndNavigateHomeUseCase: ClearAndNavigateHomeUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
  private val listItemRepository: com.romankozak.forwardappmobile.data.repository.ListItemRepository,
  private val noteDocumentRepository: NoteDocumentRepository,
  private val checklistRepository: ChecklistRepository,
  private val reminderRepository: com.romankozak.forwardappmobile.data.repository.ReminderRepository,
  private val recentItemsRepository: com.romankozak.forwardappmobile.data.repository.RecentItemsRepository,
  private val projectLogRepository: com.romankozak.forwardappmobile.data.repository.ProjectLogRepository,
  private val noteRepository: com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository,
  private val inboxRepository: com.romankozak.forwardappmobile.data.repository.InboxRepository,
  private val projectStructureRepository: ProjectStructureRepository,
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
    private const val TAG = "BacklogVM_DEBUG"
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
          projectRepository.getProjectLogsStream(id)
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val projectArtifact: StateFlow<ProjectArtifact?> =
    projectIdFlow
      .flatMapLatest { id ->
        if (id.isNotEmpty()) {
          projectRepository.getProjectArtifactStream(id)
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
      .getLogStream()
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

  private val databaseContentStream: Flow<List<ListItemContent>> =
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
          projectRepository.getProjectContentStream(id).map { content ->
            if (query.isNotBlank()) {
              content.filter { itemContent ->
                val textToSearch =
                  when (itemContent) {
                    is ListItemContent.GoalItem -> itemContent.goal.text
                    is ListItemContent.SublistItem -> itemContent.project.name
                    is ListItemContent.LinkItem ->
                      itemContent.link.linkData.displayName ?: itemContent.link.linkData.target
                    is ListItemContent.NoteItem -> itemContent.note.title
                    is ListItemContent.NoteDocumentItem -> itemContent.document.name
                    is ListItemContent.ChecklistItem -> itemContent.checklist.name
                  }
                textToSearch.contains(query, ignoreCase = true)
              }
            } else {
              content
            }
          }
        }
      }

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
            projectRepository.ensureChildProjectListItemsExist(projectId)
        }
    }
    viewModelScope.launch {
        projectIdFlow
            .filter { it.isNotEmpty() }
            .flatMapLatest { projectStructureRepository.observeStructure(it) }
            .collect { structureWithItems ->
                val structure = structureWithItems?.structure
                val enableInbox = structure?.enableInbox ?: true
                val enableLog = structure?.enableLog ?: true
                val enableArtifact = structure?.enableArtifact ?: true
                val enableAdvanced = structure?.enableAdvanced ?: false
                val enableDashboard = structure?.enableDashboard ?: true
                val enableBacklog = structure?.enableBacklog ?: true
                val enableAttachments = structure?.enableAttachments ?: true
                _uiState.update {
                    val currentView = it.currentView
                    val availableTabs = listOfNotNull(
                        ProjectManagementTab.Dashboard.takeIf { enableDashboard },
        ProjectManagementTab.Log.takeIf { enableLog },
        ProjectManagementTab.Artifact.takeIf { enableArtifact },
        ProjectManagementTab.Insights,
                    )
                    val safeDashboardTab =
                        if (it.selectedDashboardTab in availableTabs) it.selectedDashboardTab else availableTabs.firstOrNull()
                    val adjustedView =
                        when {
                            !enableBacklog && currentView == ProjectViewMode.BACKLOG && enableDashboard -> ProjectViewMode.DASHBOARD
                            !enableInbox && currentView == ProjectViewMode.INBOX -> if (enableBacklog) ProjectViewMode.BACKLOG else ProjectViewMode.DASHBOARD
                            !enableAttachments && currentView == ProjectViewMode.ATTACHMENTS -> if (enableBacklog) ProjectViewMode.BACKLOG else ProjectViewMode.DASHBOARD
                            (!enableLog || !(it.isProjectManagementEnabled || enableAdvanced)) && currentView == ProjectViewMode.ADVANCED -> if (enableBacklog) ProjectViewMode.BACKLOG else ProjectViewMode.DASHBOARD
                            !enableDashboard && currentView == ProjectViewMode.DASHBOARD && enableBacklog -> ProjectViewMode.BACKLOG
                            else -> currentView
                        }
                    it.copy(
                        enableInbox = enableInbox,
                        enableLog = enableLog,
                        enableArtifact = enableArtifact,
                        enableBacklog = enableBacklog,
                        enableDashboard = enableDashboard,
                        enableAttachments = enableAttachments,
                        isProjectManagementEnabled = it.isProjectManagementEnabled || enableAdvanced,
                        currentView = adjustedView,
                        inputMode = getInputModeForView(adjustedView),
                        selectedDashboardTab = safeDashboardTab ?: ProjectManagementTab.Insights,
                    )
                }
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
          _uiState.update { it.copy(showCheckboxes = proj.showCheckboxes, isProjectManagementEnabled = proj.isProjectManagementEnabled == true || it.isProjectManagementEnabled) }
          val isManagementEnabled = _uiState.value.isProjectManagementEnabled
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
            _listContent.value = dbContent.withCompletedAtEnd()
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
      projectRepository.toggleProjectManagement(projectIdFlow.value, isEnabled)
    }
  }

  fun onProjectStatusUpdate(newStatus: String, statusText: String?) {
    viewModelScope.launch {
      projectRepository.updateProjectStatus(projectIdFlow.value, newStatus, statusText)
    }
  }

  override fun addProjectComment(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) {
      projectRepository.addProjectComment(projectIdFlow.value, text)
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
          projectLogRepository.addProjectLogEntry(
              projectId = projectIdFlow.value,
              type = ProjectLogEntryTypeValues.MILESTONE,
              description = text,
          )
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
      ProjectViewMode.ADVANCED -> InputMode.AddProjectLog
      ProjectViewMode.DASHBOARD -> InputMode.AddGoal
      else -> InputMode.AddGoal
    }

  override fun requestNavigation(route: String) {
    viewModelScope.launch {
      if (route == "back") {
        _uiEventFlow.send(UiEvent.NavigateBack)
        return@launch
      }
      if (route.startsWith("goal_detail_screen/")) {

        val projectId = route.substringAfter("goal_detail_screen/")

        val projectName =
          withContext(ioDispatcher) {
            projectRepository.getProjectById(projectId)?.name ?: "Project"
          }
        enhancedNavigationManager.navigateToProject(projectId, projectName)
        return@launch
      } else if (route.startsWith(HANDLE_LINK_CLICK_ROUTE)) {

        val target = route.substringAfter(HANDLE_LINK_CLICK_ROUTE + "/")
        val link =
          listContent.value
            .filterIsInstance<ListItemContent.LinkItem>()
            .map { it.link.linkData }
            .find { it.target == target }
        if (link != null) {
          onLinkItemClick(link)
        } else {
          val project =
            withContext(ioDispatcher) { projectRepository.getProjectById(target) }
          when {
            project != null -> {
              enhancedNavigationManager.navigateToProject(project.id, project.name)
            }
            target.startsWith("http://") || target.startsWith("https://") -> {
              _uiEventFlow.send(UiEvent.OpenUri(target))
            }
            else -> {
              Log.w(TAG, "Unknown related link target: $target")
              _uiEventFlow.send(UiEvent.ShowSnackbar("Невідоме посилання: $target", null))
            }
          }
        }
      } else {
        val target = parseRouteToNavTarget(route)
        if (target != null) {
          _uiEventFlow.send(UiEvent.Navigate(target))
        } else {
          Log.w(TAG, "Unknown navigation route: $route")
        }
      }
    }
  }

  private fun parseRouteToNavTarget(route: String): NavTarget? {
    return when {
      route.startsWith("global_search_screen/") -> {
        val query = URLDecoder.decode(route.substringAfter("global_search_screen/"), "UTF-8")
        NavTarget.GlobalSearch(query)
      }
      route.startsWith("goal_settings_screen/") -> {
        val goalId = route.substringAfter("goal_settings_screen/")
        NavTarget.GoalSettings(goalId)
      }
      route.startsWith("note_document_screen/") -> {
        val tail = route.substringAfter("note_document_screen/")
        val id = tail.substringBefore("?")
        val startEdit = tail.substringAfter("?", "").contains("startEdit=true")
        NavTarget.NoteDocument(id = id, startEdit = startEdit)
      }
      route.startsWith("note_document_edit_screen") -> {
        val params = route.substringAfter("?", "")
        val paramMap = params.split("&").mapNotNull {
          val parts = it.split("=", limit = 2)
          if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        NavTarget.NoteDocumentEdit(
          projectId = paramMap["projectId"]?.takeIf { it.isNotBlank() },
          documentId = paramMap["documentId"]?.takeIf { it.isNotBlank() },
        )
      }
      route.startsWith("checklist_screen") -> {
        val params = route.substringAfter("?", "")
        val paramMap = params.split("&").mapNotNull {
          val parts = it.split("=", limit = 2)
          if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        NavTarget.Checklist(
          id = paramMap["checklistId"]?.takeIf { it.isNotBlank() },
          projectId = paramMap["projectId"]?.takeIf { it.isNotBlank() },
        )
      }
      route.startsWith("list_chooser_screen/") -> {
        val titleEncoded = route.substringAfter("list_chooser_screen/").substringBefore("?")
        val params = route.substringAfter("?", "")
        val paramMap = params.split("&").mapNotNull {
          val parts = it.split("=", limit = 2)
          if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        NavTarget.ListChooser(
          title = URLDecoder.decode(titleEncoded, "UTF-8"),
          currentParentId = paramMap["currentParentId"]?.takeIf { it.isNotBlank() },
          disabledIds = paramMap["disabledIds"]?.takeIf { it.isNotBlank() },
        )
      }
      route == "activity_tracker_screen" -> NavTarget.Tracker
      route == "reminders_screen" -> NavTarget.Reminders
      route == "settings_screen" -> NavTarget.Settings
      route == "ai_insights_screen" -> NavTarget.AiInsights
      route == "life_state_screen" -> NavTarget.LifeState
      route == "attachments_library_screen" -> NavTarget.AttachmentsLibrary
      route == "scripts_library_screen" -> NavTarget.ScriptsLibrary
      route == "tactical_management_screen" -> NavTarget.TacticalManagement
      else -> null
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
      "BacklogViewModel",
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
      "BacklogViewModel: showRecentListsSheet($show) called. Updating UI state.",
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
      val disabledIds = projectIdFlow.value
      _uiEventFlow.send(
        UiEvent.Navigate(
          NavTarget.ListChooser(
            title = title,
            disabledIds = disabledIds.ifBlank { null },
          )
        )
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

      val attachmentId =
        try {
          projectRepository.ensureAttachmentLinkedToProject(
            attachmentType = attachment.listItem.itemType,
            entityId = attachment.listItem.entityId,
            targetProjectId = targetProjectId,
            ownerProjectId = attachment.listItem.projectId.takeIf { it.isNotBlank() }
              ?: projectIdFlow.value,
          )
        } catch (e: Exception) {
          Log.e(TAG, "Failed to link attachment to project=$targetProjectId", e)
          withContext(Dispatchers.Main) {
            showSnackbar("Не вдалося додати вкладення до проєкту", null)
          }
          return@launch
        }
      withContext(Dispatchers.Main) {
        if (targetProjectId == projectIdFlow.value) {
          _uiState.update { it.copy(newlyAddedItemId = attachmentId) }
          forceRefresh()
        }
        showSnackbar("Вкладення додано до вибраного проєкту", null)
      }
    }
  }

  fun deleteAttachmentEverywhere(attachment: ListItemContent) {
    val isAttachment =
      attachment is ListItemContent.LinkItem ||
        attachment is ListItemContent.NoteDocumentItem ||
        attachment is ListItemContent.ChecklistItem
    if (!isAttachment) return

    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        projectRepository.deleteAttachmentEverywhere(attachment.listItem.id)
      }.onSuccess {
        withContext(Dispatchers.Main) {
          forceRefresh()
          showSnackbar("Вкладення повністю видалено", null)
        }
      }.onFailure { e ->
        Log.e(TAG, "Failed to delete attachment everywhere", e)
        withContext(Dispatchers.Main) {
          showSnackbar("Не вдалося повністю видалити вкладення", null)
        }
      }
    }
  }

  fun onMove(fromIndex: Int, toIndex: Int) {
    Log.d(TAG, "onMove called with fromIndex: $fromIndex, toIndex: $toIndex")
    viewModelScope.launch {
        val currentContent = _listContent.value.toMutableList()
        val movedItem = currentContent.removeAt(fromIndex)
        currentContent.add(toIndex, movedItem)
        val reorderedContent = currentContent.withCompletedAtEnd()
        Log.d(TAG, "onMove before save: " + reorderedContent.mapIndexed { idx, item ->
            "[$idx:${item.listItem.id} order=${item.listItem.order} v=${item.listItem.version} syncedAt=${item.listItem.syncedAt}]"
        }.joinToString(","))
        _listContent.value = reorderedContent
        saveListOrder(reorderedContent)
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
              else -> {
                Log.d(TAG, "[saveListOrder] prepare id=${content.listItem.id} orderOld=${content.listItem.order} orderNew=$order v=${content.listItem.version} syncedAt=${content.listItem.syncedAt}")
                content.listItem.copy(order = order)
              }
            }
          }
        if (updatedItems.isNotEmpty()) {
          listItemRepository.updateListItemsOrder(updatedItems)
        }
        if (attachmentOrders.isNotEmpty()) {
          projectRepository.updateAttachmentOrders(projectIdFlow.value, attachmentOrders)
        }
        Log.d(
          TAG,
          "[saveListOrder] Successfully saved new order to the database. updatedItems=${updatedItems.size} attachments=${attachmentOrders.size}"
        )
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
      currentState.copy(resetTriggers = newTriggers)
    }
  }

  fun resetSwipeStatesExcept(activeItemId: String) {
    _uiState.update { current ->
      current.copy(swipedItemId = activeItemId, swipeResetCounter = current.swipeResetCounter + 1)
    }
  }

  fun onNoteItemClick(note: LegacyNoteEntity) {
    viewModelScope.launch {
      recentItemsRepository.logNoteAccess(note)
          // legacy notes no longer have dedicated editor; no-op
    }
  }

  fun onNoteDocumentItemClick(noteDocument: NoteDocumentEntity) {
    viewModelScope.launch {
      recentItemsRepository.logNoteDocumentAccess(noteDocument)
      _uiEventFlow.send(
        UiEvent.Navigate(
          NavTarget.NoteDocument(id = noteDocument.id)
        )
      )
    }
  }

  fun onChecklistItemClick(checklist: ChecklistEntity) {
    viewModelScope.launch {
      recentItemsRepository.logChecklistAccess(checklist)
      _uiEventFlow.send(
        UiEvent.Navigate(
          NavTarget.Checklist(id = checklist.id)
        )
      )
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
            UiEvent.Navigate(
              NavTarget.NoteDocumentEdit(projectId = projectIdFlow.value)
            )
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
            _uiEventFlow.send(
              UiEvent.Navigate(
                NavTarget.Checklist(projectId = projectId)
              )
            )
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
        projectRepository.deleteProjectsAndSubProjects(listOf(project.value!!))
        withContext(Dispatchers.Main) { requestNavigation("back") }
      }
    }
  }

  override fun scrollToListEnd() {
    viewModelScope.launch { _uiEventFlow.send(UiEvent.ScrollToLatestInboxRecord) }
  }

  fun onProjectViewChange(newView: ProjectViewMode) {
    val flags = uiState.value
    if (newView == ProjectViewMode.INBOX && !flags.enableInbox) return
    if (newView == ProjectViewMode.ADVANCED && (!flags.isProjectManagementEnabled || !flags.enableLog)) return
    if (newView == ProjectViewMode.ATTACHMENTS && !flags.enableAttachments) return
    if (newView == ProjectViewMode.DASHBOARD && !flags.enableDashboard) return
    if (newView == ProjectViewMode.BACKLOG && !flags.enableBacklog) return
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
      projectRepository.updateProject(updatedSubproject)
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
            val record = activityRepository.startGoalActivity(item.goal.id)
            record to "Відстежую ціль"
          }

          is ListItemContent.SublistItem -> {
            val record = activityRepository.startProjectActivity(item.project.id)
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
    _uiState.update {
      it.copy(
        recordForReminderDialog = null,
        remindersForDialog = emptyList(),
        showRemindersDialog = false,
        itemForRemindersDialog = null,
      )
    }
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

  fun onRemoveReminder(reminderId: String) = viewModelScope.launch {
    val record = _uiState.value.recordForReminderDialog ?: return@launch

    // Очищаємо всі нагадування для сутності, щоб точно вимкнути
    reminderRepository.clearRemindersForEntity(record.id)

    val refreshed = reminderRepository.getRemindersForEntityFlow(record.id).firstOrNull().orEmpty()
    val updatedRecord = record.copy(reminderTime = refreshed.firstOrNull()?.reminderTime)

    _uiState.update { it.copy(remindersForDialog = refreshed, recordForReminderDialog = updatedRecord) }

    showSnackbar("Нагадування видалено", null)
    forceRefresh()
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
        when (item) {
            is ListItemContent.GoalItem -> {
                val entityId = item.goal.id
                val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                val record =
                    ActivityRecord(
                        id = entityId,
                        text = item.goal.text,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.goal.createdAt,
                        projectId = item.listItem.projectId,
                        goalId = item.goal.id,
                    )
                _uiState.update { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
            }
            is ListItemContent.SublistItem -> {
                val entityId = item.project.id
                val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                val record =
                    ActivityRecord(
                        id = entityId,
                        text = item.project.name,
                        reminderTime = reminders.firstOrNull()?.reminderTime,
                        createdAt = item.project.createdAt,
                        projectId = item.project.id,
                        goalId = null,
                    )
                _uiState.update { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
            }
            else -> return@launch
        }
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
        activityRepository.endLastActivity(System.currentTimeMillis())
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
      val record = activityRepository.startProjectActivity(currentProjectId)
      if (record != null) {
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

      projectRepository.toggleProjectManagement(proj.id, newState)

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
        val metrics = projectRepository.calculateProjectTimeMetrics(currentProjectId)
        _uiState.update { it.copy(projectTimeMetrics = metrics) }

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

  private fun createClearExecutionContext():
    com.romankozak.forwardappmobile.ui.navigation.ClearExecutionContext {
    return com.romankozak.forwardappmobile.ui.navigation.createClearExecutionContext(
      currentProjects = _allProjects.value,
      subStateStack =
        MutableStateFlow(
          listOf(
            com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenSubState.Hierarchy,
          ),
        ),
      searchUseCase = searchUseCase,
      planningModeManager =
        com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager(),
      enhancedNavigationManager = enhancedNavigationManager,
      uiEventChannel =
        Channel<com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent>(),
    )
  }

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
        RecentItemType.PROJECT -> {
          projectRepository.getProjectById(item.target)?.let { recentItemsRepository.logProjectAccess(it) }
          enhancedNavigationManager.navigateToProject(item.target, item.displayName)
        }
        RecentItemType.NOTE -> _uiEventFlow.send(UiEvent.ShowSnackbar("Застарілі нотатки доступні лише для читання"))
        RecentItemType.NOTE_DOCUMENT -> {
          noteDocumentRepository.getDocumentById(item.target)?.let { recentItemsRepository.logNoteDocumentAccess(it) }
          _uiEventFlow.send(
            UiEvent.Navigate(
              NavTarget.NoteDocument(id = item.target)
            )
          )
        }
        RecentItemType.CHECKLIST -> {
          checklistRepository.getChecklistById(item.target)?.let { recentItemsRepository.logChecklistAccess(it) }
          _uiEventFlow.send(
            UiEvent.Navigate(
              NavTarget.Checklist(id = item.target)
            )
          )
        }
        RecentItemType.OBSIDIAN_LINK -> {
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
      _uiEventFlow.send(
        UiEvent.Navigate(
          NavTarget.Checklist(projectId = projectId)
        )
      )
    }
  }

  fun onCreateNoteDocument(title: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(showCreateNoteDocumentDialog = false) }
      _uiEventFlow.send(
        UiEvent.Navigate(
          NavTarget.NoteDocumentEdit(projectId = projectIdFlow.value)
        )
      )
    }
  }

  fun onCleanupDatabase() {
    viewModelScope.launch {
      projectRepository.cleanupDanglingListItems()
      forceRefresh()
    }
  }

    fun onEditLogEntry(log: ProjectExecutionLog) {
        _uiState.update { it.copy(logEntryToEdit = log) }
    }

    fun onDeleteLogEntry(log: ProjectExecutionLog) {
        viewModelScope.launch {
            projectLogRepository.deleteProjectExecutionLog(log)
        }
    }

    fun onUpdateLogEntry(description: String, details: String?) {
        viewModelScope.launch {
            val logToUpdate = uiState.value.logEntryToEdit ?: return@launch
            val updatedLog = logToUpdate.copy(
                description = description,
                details = details
            )
            projectLogRepository.updateProjectExecutionLog(updatedLog)
            onDismissEditLogEntryDialog()
        }
    }

    fun onDismissEditLogEntryDialog() {
        _uiState.update { it.copy(logEntryToEdit = null) }
    }

  fun onSaveNoteDocument(content: String) {
    viewModelScope.launch {
      Log.d("NoteTitleExtractor", "onSaveNoteDocument called, content length=${content.length}")
      val title = extractTitleFromContent(content)
      Log.d("NoteTitleExtractor", "onSaveNoteDocument extracted title='$title'")
      noteDocumentRepository.createDocument(title, projectIdFlow.value, content)
      onDismissNoteDocumentEditor()
    }
  }

    fun onDismissNoteDocumentEditor() {
        _uiState.update { it.copy(showNoteDocumentEditor = false) }
    }

    fun onAddMilestone() {
        viewModelScope.launch {
            projectLogRepository.addProjectLogEntry(
                projectId = projectIdFlow.value,
                type = ProjectLogEntryTypeValues.MILESTONE,
                description = "New Milestone",
            )
        }
    }

    fun onSaveArtifact(content: String) {
        viewModelScope.launch {
            val currentArtifact = projectArtifact.value
            if (currentArtifact == null) {
                projectRepository.createProjectArtifact(
                    ProjectArtifact(
                        id = UUID.randomUUID().toString(),
                        projectId = projectIdFlow.value,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                projectRepository.updateProjectArtifact(currentArtifact.copy(content = content, updatedAt = System.currentTimeMillis()))
            }
            onDismissArtifactEditor()
        }
    }

    fun onAutoSaveArtifact(content: String) {
        viewModelScope.launch {
            val currentArtifact = projectArtifact.value ?: return@launch
            projectRepository.updateProjectArtifact(
                currentArtifact.copy(
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
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

  private fun extractTitleFromContent(content: String): String {
        return NoteTitleExtractor.extract(content)
    }
}
