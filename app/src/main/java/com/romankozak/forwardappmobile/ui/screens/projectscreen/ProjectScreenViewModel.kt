package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
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
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.TagUtils
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments.AttachmentType
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.TransferStatus
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxHandlerResultListener
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.InboxMarkdownHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.ItemActionHandler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.ProjectMarkdownExporter
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.SelectionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "BacklogVM_DEBUG"

sealed class UiEvent {
    data class ShowSnackbar(
        val message: String,
        val action: String? = null,
    ) : UiEvent()

    data class Navigate(
        val route: String,
    ) : UiEvent()

    data class ResetSwipeState(
        val itemId: String,
    ) : UiEvent()

    data class ScrollTo(
        val index: Int,
    ) : UiEvent()

    data class NavigateBackAndReveal(
        val projectId: String,
    ) : UiEvent()

    data class HandleLinkClick(
        val link: RelatedLink,
    ) : UiEvent()

    data class OpenUri(val uri: String) : UiEvent()

    data object ScrollToLatestInboxRecord : UiEvent()
}

enum class GoalActionType { CreateInstance, MoveInstance, CopyGoal, AddLinkToList, ADD_LIST_SHORTCUT }

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()

    data class AwaitingActionChoice(
        val itemContent: ListItemContent,
    ) : GoalActionDialogState()
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
    val projectTimeMetrics: ProjectTimeMetrics? = null,
    val showExportTransferDialog: Boolean = false,
    val transferStatus: TransferStatus = TransferStatus.IDLE,
    val showCreateCustomListDialog: Boolean = false,
)

interface BacklogMarkdownHandlerResultListener {
    fun copyToClipboard(
        text: String,
        label: String,
    )

    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun forceRefresh()
}

class BacklogMarkdownHandler
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
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
                        is ListItemContent.CustomListItem -> "- [К] ${item.customList.name}"
                    }
                markdownBuilder.appendLine(line)
            }
            val markdownText = markdownBuilder.toString()
            listener.copyToClipboard(markdownText, "Backlog Export")
            listener.showSnackbar("Беклог скопійовано у буфер обміну.", null)
        }

        fun importFromMarkdown(
            markdownText: String,
            projectId: String,
        ) {
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
                                    projectRepository.addGoalToProject(goalText, projectId, completed = false)
                                    importedCount++
                                }
                            }

                            trimmedLine.startsWith("- [x]") -> {
                                val goalText = trimmedLine.removePrefix("- [x]").trim()
                                if (goalText.isNotEmpty()) {
                                    projectRepository.addGoalToProject(goalText, projectId, completed = true)
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
    ) : ViewModel(),
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

        val canGoBack: StateFlow<Boolean> get() = enhancedNavigationManager.canGoBack
        val canGoForward: StateFlow<Boolean> get() = enhancedNavigationManager.canGoForward

        
        private val _isProcessingHome = MutableStateFlow(false)

        private val _allTags = MutableStateFlow<List<String>>(emptyList())
        val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

        private val _allContexts = MutableStateFlow<List<String>>(emptyList())
        val allContexts: StateFlow<List<String>> = _allContexts.asStateFlow()

        
        private val _allProjects =
            projectRepository.getAllProjectsFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val subprojectChildren: StateFlow<Map<String?, List<Project>>> =
            _allProjects.map { allProjects ->
                allProjects.groupBy { it.parentId }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        
        private var batchSaveJob: Job? = null
private val projectIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
private val _listContent = MutableStateFlow<List<ListItemContent>>(emptyList())
        val listContent: StateFlow<List<ListItemContent>> = _listContent.asStateFlow()

        val itemActionHandler = ItemActionHandler(projectRepository, viewModelScope, projectIdFlow, this)
        val selectionHandler = SelectionHandler(projectRepository, viewModelScope, _listContent, this)
        val inboxHandler = InboxHandler(projectRepository, viewModelScope, projectIdFlow, this)
        val inboxMarkdownHandler = InboxMarkdownHandler(projectRepository, viewModelScope, this)
        val backlogMarkdownHandler = BacklogMarkdownHandler(projectRepository, viewModelScope, this)

        private val _uiState =
            MutableStateFlow(
                UiState(
                    goalToHighlight = savedStateHandle.get<String>("goalId"),
                    itemToHighlight = savedStateHandle.get<String>("itemIdToHighlight"),
                    inboxRecordToHighlight = savedStateHandle.get<String>("inboxRecordIdToHighlight"),
                ),
            )
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading = _isLoading.asStateFlow()

        val projectLogs: StateFlow<List<ProjectExecutionLog>> =
            projectIdFlow.flatMapLatest { id ->
                if (id.isNotEmpty()) {
                    projectRepository.getProjectLogsStream(id)
                } else {
                    flowOf(emptyList())
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val inputHandler =
            InputHandler(
                projectRepository,
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
            projectRepository.getRecentItems()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> =
            contextHandler.contextMarkerToEmojiMap

        val project: StateFlow<Project?> =
            combine(projectIdFlow, _refreshTrigger) { id, _ -> id }
                .flatMapLatest { id ->
                    if (id.isNotEmpty()) projectRepository.getProjectByIdFlow(id) else flowOf(null)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val tagToContextNameMap: StateFlow<Map<String, String>> =
            contextHandler.tagToContextNameMap
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val lastOngoingActivity: StateFlow<ActivityRecord?> =
            activityRepository
                .getLogStream()
                .map { log ->
                    log.firstOrNull { it.isOngoing }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val currentProjectContextMarker: StateFlow<String?> =
            combine(project, tagToContextNameMap) { proj, tagMap ->
                val projectTags = proj?.tags ?: emptyList()
                if (projectTags.isEmpty() || tagMap.isEmpty()) return@combine null
                val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in projectTags }?.value
                contextName?.let { contextHandler.getContextMarker(it) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val currentProjectContextEmojiToHide: StateFlow<String?> =
            combine(currentProjectContextMarker, contextMarkerToEmojiMap) { marker, emojiMap ->
                marker?.let { emojiMap[it] }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        private val databaseContentStream: Flow<List<ListItemContent>> =
            combine(
                projectIdFlow,
                _uiState.map { it.localSearchQuery }.distinctUntilChanged(),
                _refreshTrigger,
            ) { id, query, _ -> Pair(id, query) }
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
                                            is ListItemContent.CustomListItem -> itemContent.customList.name
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
            _uiState.map { it.selectedItemIds.isNotEmpty() }
                .onEach { isActive -> Log.d(TAG, "СТАН: isSelectionModeActive змінився на: $isActive") }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")


        private var pendingActivityForReminder: ActivityRecord? = null

        val desktopAddress: StateFlow<String> =
            settingsRepository.desktopAddressFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        
        init {
            Log.d(TAG, "ViewModel instance created: ${this.hashCode()}")

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
                        val isManagementEnabled = proj.isProjectManagementEnabled ?: false
                        val currentView = uiState.value.currentView
                        if (!isManagementEnabled && currentView == ProjectViewMode.DASHBOARD) {
                            Log.d(TAG, "Inconsistency detected: Project management is OFF but view is DASHBOARD. Switching to BACKLOG.")
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
                project.filterNotNull()
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
                databaseContentStream.collect { dbContent -> _listContent.value = dbContent }
            }

            viewModelScope.launch {
                projectIdFlow.filter { it.isNotEmpty() }.collect { id -> projectRepository.logProjectAccess(id) }
            }

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    contextHandler.initialize()
                }
            }
            loadAllTags()
            loadAllContexts()
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
                withContext(Dispatchers.Main) {
                    _allTags.value = (projectTags + goalTags).distinct()
                }
            }
        }

        private fun loadAllContexts() {
            viewModelScope.launch {
                _allContexts.value = contextHandler.contextNamesFlow.first()
            }
        }

        val autocompleteSuggestions =
            uiState.map { it.inputValue }.debounce(150).flatMapLatest { inputValue ->
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
                                _allTags.value.filter { tag ->
                                    tag.removePrefix("#").startsWith(word, ignoreCase = true)
                                }.map { tag ->
                                    if (tag.startsWith("#")) tag else "#$tag"
                                }
                            flowOf(filtered)
                        }
                        "@" -> {
                            val filtered = _allContexts.value.filter { it.startsWith(word, ignoreCase = true) }.map { "@$it" }
                            flowOf(filtered)
                        }
                        else -> flowOf(emptyList())
                    }
                } else {
                    flowOf(emptyList())
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private fun getCurrentWordInfo(
            text: String,
            cursorPosition: Int,
        ): Pair<String, String>? {
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
            val newText = currentText.substring(0, startIndex) + suggestion + " " + currentText.substring(cursorPosition)
            val newCursorPosition = startIndex + suggestion.length + 1

            _uiState.update {
                it.copy(
                    inputValue =
                        TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newCursorPosition),
                        ),
                )
            }
        }

        fun onToggleProjectManagement(isEnabled: Boolean) {
            viewModelScope.launch {
                projectRepository.toggleProjectManagement(projectIdFlow.value, isEnabled)
            }
        }

        fun onProjectStatusUpdate(
            newStatus: String,
            statusText: String?,
        ) {
            viewModelScope.launch {
                projectRepository.updateProjectStatus(projectIdFlow.value, newStatus, statusText)
            }
        }

        override fun addProjectComment(text: String) {
            if (text.isBlank()) return
            viewModelScope.launch(Dispatchers.IO) {
                projectRepository.addProjectComment(projectIdFlow.value, text)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(inputValue = TextFieldValue("")) }
                }
            }
        }

        private fun getInputModeForView(viewMode: ProjectViewMode): InputMode =
            when (viewMode) {
                ProjectViewMode.INBOX -> InputMode.AddQuickRecord
                ProjectViewMode.DASHBOARD -> InputMode.AddProjectLog
                else -> InputMode.AddGoal
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

        override fun showSnackbar(
            message: String,
            action: String?,
        ) {
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
                    showAddObsidianLinkDialog =
                        showAddObsidianLinkDialog
                            ?: it.showAddObsidianLinkDialog,
                )
            }
        }

        override fun showRecentListsSheet(show: Boolean) {
            Log.d("Recents_Debug", "BacklogViewModel: showRecentListsSheet($show) called. Updating UI state.")
            _uiState.update { it.copy(showRecentProjectsSheet = show) }
        }

        override fun updateSelectionState(selectedIds: Set<String>) {
            Log.d(TAG, "ВИКЛИК: updateSelectionState з ${selectedIds.size} елементами.")
            _uiState.update { it.copy(selectedItemIds = selectedIds) }
        }

        fun onListChooserResult(targetProjectId: String) {
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
                    GoalActionType.CreateInstance ->
                        projectRepository.createGoalLinks(
                            goalIds,
                            targetProjectId,
                        )

                    GoalActionType.MoveInstance -> projectRepository.moveListItems(itemIds, targetProjectId)
                    GoalActionType.CopyGoal -> projectRepository.copyGoalsToProject(goalIds, targetProjectId)
                    GoalActionType.AddLinkToList -> {
                        val targetProject = projectRepository.getProjectById(targetProjectId)
                        val link =
                            RelatedLink(
                                type = LinkType.PROJECT,
                                target = targetProjectId,
                                displayName = targetProject?.name ?: "Проект без назви",
                            )
                        val newItemId = projectRepository.addLinkItemToProject(projectIdFlow.value, link)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                        }
                    }

                    GoalActionType.ADD_LIST_SHORTCUT -> {
                        if (goalIds.isNotEmpty()) {
                            val subprojectToLinkId = goalIds.first()
                            val newItemId = projectRepository.addProjectLinkToProject(subprojectToLinkId, targetProjectId)
                            withContext(Dispatchers.Main) {
                                _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                            }
                        } else {
                            val newItemId = projectRepository.addProjectLinkToProject(targetProjectId, projectIdFlow.value)
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
                _uiEventFlow.send(UiEvent.Navigate("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds"))
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

        fun moveItem(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val currentContent = _listContent.value
            val draggableItems =
                currentContent.filterNot { it is ListItemContent.LinkItem }.toMutableList()
            if (fromIndex !in draggableItems.indices || toIndex !in draggableItems.indices) return
            if (fromIndex == toIndex) return
            val movedItem = draggableItems.removeAt(fromIndex)
            draggableItems.add(toIndex, movedItem)
            val newFullList = mutableListOf<ListItemContent>()
            val reorderedDraggablesIterator = draggableItems.iterator()
            currentContent.forEach { originalItem ->
                if (originalItem is ListItemContent.LinkItem) {
                    newFullList.add(originalItem)
                } else if (reorderedDraggablesIterator.hasNext()) {
                    newFullList.add(
                        reorderedDraggablesIterator.next(),
                    )
                }
            }
            _listContent.value = newFullList
            viewModelScope.launch { saveListOrder(newFullList) }
        }

        private suspend fun saveListOrder(listToSave: List<ListItemContent>) =
            withContext(Dispatchers.IO) {
                try {
                    val updatedItems =
                        listToSave.mapIndexed { index, content -> content.listItem.copy(order = index.toLong()) }
                    projectRepository.updateListItemsOrder(updatedItems)
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

        fun onNoteItemClick(note: NoteEntity) {
            viewModelScope.launch {
                projectRepository.logNoteAccess(note)
                _uiEventFlow.send(UiEvent.Navigate("note_edit_screen?noteId=${note.id}"))
            }
        }

        fun onCustomListItemClick(customList: CustomListEntity) {
            viewModelScope.launch {
                projectRepository.logCustomListAccess(customList)
                _uiEventFlow.send(UiEvent.Navigate("custom_list_screen/${customList.id}"))
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
                        projectRepository.logObsidianLinkAccess(link)
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
                AttachmentType.NOTE -> {
                    viewModelScope.launch {
                        _uiEventFlow.send(UiEvent.Navigate("note_edit_screen?projectId=${projectIdFlow.value}"))
                    }
                }
            AttachmentType.CUSTOM_LIST -> {
                viewModelScope.launch {
                    _uiEventFlow.send(UiEvent.Navigate("custom_list_edit_screen?projectId=${projectIdFlow.value}"))
                }
            }                AttachmentType.WEB_LINK -> inputHandler.onShowAddWebLinkDialog()
                AttachmentType.OBSIDIAN_LINK -> inputHandler.onShowAddObsidianLinkDialog()
                AttachmentType.LIST_LINK -> inputHandler.onAddListLinkRequest()
                AttachmentType.SHORTCUT -> inputHandler.onAddListShortcutRequest()
            }
        }

        override fun copyToClipboard(
            text: String,
            label: String,
        ) {
            val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        }

        fun deleteCurrentProject() {
            viewModelScope.launch(Dispatchers.IO) {
                val projectId = projectIdFlow.value
                if (projectId.isNotEmpty()) {
                    projectRepository.deleteProject(projectId)
                    withContext(Dispatchers.Main) {
                        requestNavigation("back")
                    }
                }
            }
        }

        override fun scrollToListEnd() {
            viewModelScope.launch {
                _uiEventFlow.send(UiEvent.ScrollToLatestInboxRecord)
            }
        }

        fun onProjectViewChange(newView: ProjectViewMode) {
            Log.d("ATTACHMENT_DEBUG", "VM: onProjectViewChange(newView = $newView) called.")
            _uiState.update {
                Log.d("ATTACHMENT_DEBUG", "VM: Updating uiState.currentView to $newView.")
                it.copy(currentView = newView, inputMode = getInputModeForView(newView))
            }
            viewModelScope.launch {
                projectRepository.updateProjectViewMode(projectIdFlow.value, newView)
            }
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

        fun onSubprojectCompletedChanged(
            subproject: Project,
            isCompleted: Boolean,
        ) {
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
                        is ListItemContent.CustomListItem,
                        -> null to null
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
            _uiState.update { it.copy(recordForReminderDialog = null) }
        }

        fun onSetReminder(timestamp: Long) =
            viewModelScope.launch {
                val record = _uiState.value.recordForReminderDialog ?: return@launch

                alarmScheduler.cancelForActivityRecord(record)

                val updatedRecord = record.copy(reminderTime = timestamp)
                activityRepository.updateRecord(updatedRecord)
                alarmScheduler.scheduleForActivityRecord(updatedRecord)
                onReminderDialogDismiss()
                showSnackbar(
                    "Нагадування встановлено на ${
                        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(
                            Date(timestamp),
                        )
                    }",
                    null,
                )
            }

        fun onClearReminder() =
            viewModelScope.launch {
                val record = _uiState.value.recordForReminderDialog ?: return@launch
                val updatedRecord = record.copy(reminderTime = null)
                activityRepository.updateRecord(updatedRecord)
                alarmScheduler.cancelForActivityRecord(record)
                onReminderDialogDismiss()
                showSnackbar("Нагадування скасовано", null)
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
                    onProjectViewChange(ProjectViewMode.DASHBOARD)
                } else if (currentView == ProjectViewMode.DASHBOARD) {
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
                        is ListItemContent.CustomListItem -> null
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
            _uiState.update { it.copy(showExportTransferDialog = true, transferStatus = TransferStatus.IDLE) }
        }

        fun onExportTransferDialogDismiss() {
            _uiState.update { it.copy(showExportTransferDialog = false) }
        }

        fun onCopyToClipboardRequest() {
            backlogMarkdownHandler.exportToMarkdown(listContent.value)
            showSnackbar("Беклог скопійовано", null)
            onExportTransferDialogDismiss()
        }

        fun onTransferBacklogViaWifi(url: String) {
            Log.d(TAG, "onTransferBacklogViaWifi: Ініційовано передачу на URL: $url")
            executeBacklogTransfer(url)
        }

        private fun executeBacklogTransfer(url: String) {
            Log.d(TAG, "executeBacklogTransfer: Початок підготовки даних для відправки.")
            _uiState.update { it.copy(transferStatus = TransferStatus.IN_PROGRESS) }

            viewModelScope.launch(Dispatchers.IO) {
                try {
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
                                is ListItemContent.CustomListItem -> "- [К] ${item.customList.name}"
                            }
                        markdownBuilder.appendLine(line)
                    }
                    val markdownContent = markdownBuilder.toString()

                    if (markdownContent.isBlank()) {
                        withContext(Dispatchers.Main) {
                            showSnackbar("Беклог порожній. Нічого передавати.", null)
                            _uiState.update { it.copy(transferStatus = TransferStatus.IDLE) }
                        }
                        return@launch
                    }

                    val filename = project.value?.name ?: "backlog_export"

                    val requestBody =
                        FileDataRequest(
                            filename = filename,
                            content = markdownContent,
                        )

                    Log.d(TAG, "executeBacklogTransfer: Дані підготовлено. Відправка на: $url")

                    val response = RetrofitClient.getInstance(application, url).uploadFileAsJson(requestBody)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "executeBacklogTransfer: Успішна відповідь від сервера. Код: ${response.code()}")
                            _uiState.update { it.copy(transferStatus = TransferStatus.SUCCESS) }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Невідома помилка"
                            Log.e(TAG, "executeBacklogTransfer: Помилка від сервера. Код: ${response.code()}, Повідомлення: $errorMsg")
                            _uiState.update { it.copy(transferStatus = TransferStatus.ERROR) }
                            showSnackbar("Помилка: ${response.code()} - $errorMsg", null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "executeBacklogTransfer: Критична помилка мережі.", e)
                        _uiState.update { it.copy(transferStatus = TransferStatus.ERROR) }
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
                        context = createClearExecutionContext()
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

        private fun createClearExecutionContext(): com.romankozak.forwardappmobile.ui.navigation.ClearExecutionContext {
            return com.romankozak.forwardappmobile.ui.navigation.createClearExecutionContext(
                currentProjects = _allProjects.value,
                subStateStack = MutableStateFlow(listOf(com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState.Hierarchy)),
                searchAndNavigationManager = com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager(projectRepository, viewModelScope, savedStateHandle, Channel<com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent>(), _allProjects),
                planningModeManager = com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager(),
                enhancedNavigationManager = enhancedNavigationManager,
                uiEventChannel = Channel<com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent>()
            )
        }

        fun onForwardPressed() {
            enhancedNavigationManager.goForward()
        }

        private fun updateProjectNameInHistory(newName: String) {
            
            
            
        }

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

            
            if (state.showExportTransferDialog) {
                onExportTransferDialogDismiss()
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

        fun onSubprojectClick(subproject: ListItemContent.SublistItem) {
            viewModelScope.launch {
                enhancedNavigationManager.navigateToProject(subproject.project.id, subproject.project.name)
            }
        }

        fun onRecentItemClick(item: RecentItem) {
            viewModelScope.launch {
                when (item.type) {
                    RecentItemType.PROJECT -> {
                        enhancedNavigationManager.navigateToProject(item.target, item.displayName)
                    }
                    RecentItemType.NOTE -> {
                        _uiEventFlow.send(UiEvent.Navigate("note_edit_screen?noteId=${item.target}"))
                    }
                    RecentItemType.CUSTOM_LIST -> {
                        _uiEventFlow.send(UiEvent.Navigate("custom_list_screen/${item.target}"))
                    }
                    RecentItemType.OBSIDIAN_LINK -> {
                        val link = RelatedLink(type = LinkType.OBSIDIAN, target = item.target, displayName = item.displayName)
                        onLinkItemClick(link)
                    }
                }
            }
        }

        fun onPinRecentItem(item: RecentItem) {
            viewModelScope.launch {
                projectRepository.updateRecentItem(item.copy(isPinned = !item.isPinned))
            }
        }

            fun onChildProjectClick(childProject: Project) {
                viewModelScope.launch {
                    enhancedNavigationManager.navigateToProject(childProject.id, childProject.name)
                }
            }
        
            fun onDismissCreateCustomListDialog() {
                _uiState.update { it.copy(showCreateCustomListDialog = false) }
            }
        
            fun onCreateCustomList(title: String) {
                viewModelScope.launch {
                    _uiState.update { it.copy(showCreateCustomListDialog = false) }
                    _uiEventFlow.send(UiEvent.Navigate("custom_list_edit_screen?projectId=${projectIdFlow.value}"))
                }
            }    }
