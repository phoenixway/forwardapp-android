
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context as AndroidContext
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.gate.CapabilityGate
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.BacklogMarkdownHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.BacklogMarkdownHandlerResultListener
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BacklogViewModel
@Inject
constructor(
    private val capabilityGate: CapabilityGate,
    private val searchUseCase: SearchUseCase,
    private val application: Application,
    private val contextRepository: ContextRepository,
    private val settingsRepository: SettingsRepository,
    private val contextHandler: ContextHandler,
    private val alarmScheduler: AlarmScheduler,
    private val nerManager: NerManager,
    private val reminderParser: ReminderParser,
    private val activityRepository: ActivityRepository,
    private val contextMarkdownExporter: ContextMarkdownExporter,
    private val savedStateHandle: SavedStateHandle,
    private val dayManagementRepository: DayManagementRepository,
    private val clearAndNavigateHomeUseCase: ClearAndNavigateHomeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val goalRepository: GoalRepository,
    private val listItemRepository: ListItemRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val checklistRepository: ChecklistRepository,
    private val reminderRepository: ReminderRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val contextLogRepository: ContextLogRepository,
    private val noteRepository: LegacyNoteRepository,
    private val inboxRepository: InboxRepository,
    private val contextStructureRepository: ContextStructureRepository,
    private val capabilityHandler: ContextCapabilityHandler = ContextCapabilityHandler()
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

    // State managers - делегування логіки
    private val stateManager = ContextStateManager(viewModelScope)
    private val capabilityManager = ContextCapabilityManager(capabilityGate, capabilityHandler)
    private val tagManager = TagManager(contextRepository, viewModelScope)
    private val activityManager = ActivityManager(
        activityRepository,
        contextRepository,
        settingsRepository,
        viewModelScope
    )
    
    // Handlers - делегування дій
    private val contextIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
    private val _listContent = MutableStateFlow<List<BacklogItemContent>>(emptyList())
    val listContent: StateFlow<List<BacklogItemContent>> = _listContent.asStateFlow()

    val itemActionHandler = ItemActionHandler(
        contextRepository,
        goalRepository,
        recentItemsRepository,
        viewModelScope,
        contextIdFlow,
        this
    )
    
    val selectionHandler = SelectionHandler(
        contextRepository,
        goalRepository,
        viewModelScope,
        contextIdFlow,
        _listContent,
        this
    )
    
    val inputHandler = InputHandler(
        contextRepository,
        goalRepository,
        listItemRepository,
        viewModelScope,
        contextIdFlow,
        nerManager,
        reminderParser,
        alarmScheduler,
        this
    )
    
    private val inboxHandler = InboxHandler(
        inboxRepository,
        recentItemsRepository,
        this
    )
    
    private val backlogMarkdownHandler = BacklogMarkdownHandler(
        contextRepository,
        goalRepository,
        listItemRepository,
        viewModelScope,
        this
    )

    // Exposed StateFlows
    val uiState: StateFlow<ContextUiState> = stateManager.uiState
    val listScrollState = LazyListState()
    val allTags: StateFlow<List<String>> = tagManager.allTags
    val allContexts: StateFlow<List<String>> = tagManager.allContexts
    
    private val _uiEventFlow = Channel<UiEvent>(Channel.BUFFERED)
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    private val _allProjects = contextRepository
        .getAllContextsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subprojectChildren: StateFlow<Map<String?, List<Context>>> = _allProjects
        .map { allProjects -> allProjects.groupBy { it.parentId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val contextArtifact: StateFlow<ContextArtifact?> = contextIdFlow
        .flatMapLatest { contextId ->
            if (contextId.isBlank()) flowOf(null)
            else contextRepository.getContextArtifactFlow(contextId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contextTimeMetrics: StateFlow<ContextTimeMetrics?> = contextIdFlow
        .flatMapLatest { contextId ->
            if (contextId.isBlank()) flowOf(null)
            else contextRepository.getContextTimeMetricsFlow(contextId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var batchSaveJob: Job? = null

    init {
        setupContextObserver()
        tagManager.loadTags()
        activityManager.observeCurrentActivity()
    }

    private fun setupContextObserver() {
        viewModelScope.launch {
            contextIdFlow
                .distinctUntilChanged()
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf(ContextData.Empty)
                    } else {
                        combine(
                            contextRepository.getContextFlow(contextId),
                            contextRepository.getListItemsWithRelationshipsFlow(contextId),
                            contextRepository.getContextConfigurationFlow(contextId),
                            contextRepository.getContextLogsFlow(contextId),
                            checklistRepository.getChecklistsFlow(contextId),
                            noteDocumentRepository.getDocumentsFlow(contextId),
                            reminderRepository.getRemindersFlow(contextId),
                            recentItemsRepository.getRecentItemsFlow(contextId),
                            noteRepository.getNotesForContextFlow(contextId)
                        ) { context, items, config, logs, checklists, noteDocuments, reminders, recentItems, notes ->
                            ContextData.Loaded(
                                context = context,
                                items = items,
                                config = config ?: ContextConfiguration.default(contextId),
                                logs = logs,
                                checklists = checklists,
                                noteDocuments = noteDocuments,
                                reminders = reminders,
                                recentItems = recentItems,
                                notes = notes
                            )
                        }
                    }
                }
                .collect { data ->
                    when (data) {
                        is ContextData.Loaded -> {
                            _listContent.value = data.items
                            stateManager.updateContext(data)
                            capabilityManager.updateCapabilities(data.config)
                        }
                        is ContextData.Empty -> {
                            _listContent.value = emptyList()
                            stateManager.clear()
                        }
                    }
                }
        }
    }

    // Navigation
    fun navigateBack() {
        viewModelScope.launch {
            enhancedNavigationManager.navigateBack()
        }
    }

    fun navigateForward() {
        viewModelScope.launch {
            enhancedNavigationManager.navigateForward()
        }
    }

    fun navigateHome() {
        viewModelScope.launch {
            if (stateManager.isProcessingHome()) return@launch
            stateManager.setProcessingHome(true)
            
            try {
                clearAndNavigateHomeUseCase(
                    ClearCommand.CLEAR_WITH_NAVIGATE,
                    ClearExecutionContext.PROJECT_SCREEN
                )
            } finally {
                stateManager.setProcessingHome(false)
            }
        }
    }

    // UI Events
    fun showSnackbar(message: String, actionLabel: String?) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.ShowSnackbar(message, actionLabel))
        }
    }

    // Delegated methods
    fun onSwitchViewMode(mode: ContextViewMode) = stateManager.switchViewMode(mode)
    fun onSwitchTab(tab: ContextManagementTab) = stateManager.switchTab(tab)
    fun onToggleSearchMode() = stateManager.toggleSearchMode()
    fun onSearchQueryChanged(query: String) = stateManager.updateSearchQuery(query)
    fun onDismissDisplayPropertiesDialog() = stateManager.dismissDisplayPropertiesDialog()
    fun onShowDisplayPropertiesDialog() = stateManager.showDisplayPropertiesDialog()
    
    // Activity tracking
    fun onStartActivity() = activityManager.startActivity(contextIdFlow.value)
    fun onStopActivity() = activityManager.stopActivity()
    fun getCurrentActivity() = activityManager.currentActivity.value

    // Capabilities
    fun hasCapability(capabilityId: CapabilityId) = capabilityManager.hasCapability(capabilityId)
    fun getEnabledCapabilities() = capabilityManager.getEnabledCapabilities()

    // Markdown Export/Import
    fun onExportBacklogToMarkdown() {
        backlogMarkdownHandler.exportToMarkdown(_listContent.value)
    }

    fun onImportBacklogFromMarkdown(markdownText: String) {
        backlogMarkdownHandler.importFromMarkdown(markdownText, contextIdFlow.value)
    }

    fun onShowImportBacklogFromMarkdownDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = true) }
        }
    }

    fun onDismissImportBacklogFromMarkdownDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = false) }
        }
    }

    // Clipboard operations
    fun copyToClipboard(text: String, label: String) {
        val clipboard = application.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    // Force refresh
    fun forceRefresh() {
        viewModelScope.launch {
            stateManager.updateState { 
                it.copy(
                    refreshTrigger = it.refreshTrigger + 1,
                    needsStateRefresh = true
                ) 
            }
        }
    }

    // Dialog management
    fun onShowRecentProjectsSheet() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showRecentProjectsSheet = true) }
        }
    }

    fun onDismissRecentProjectsSheet() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showRecentProjectsSheet = false) }
        }
    }

    fun onShowShareDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showShareDialog = true) }
        }
    }

    fun onDismissShareDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showShareDialog = false) }
        }
    }

    // Web & Obsidian Links
    fun onShowAddWebLinkDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showAddWebLinkDialog = true) }
        }
    }

    fun onDismissAddWebLinkDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showAddWebLinkDialog = false) }
        }
    }

    fun onShowAddObsidianLinkDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showAddObsidianLinkDialog = true) }
        }
    }

    fun onDismissAddObsidianLinkDialog() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showAddObsidianLinkDialog = false) }
        }
    }

    // Selection & Highlight
    fun onHighlightItem(itemId: String?) {
        viewModelScope.launch {
            stateManager.updateState { it.copy(itemToHighlight = itemId) }
        }
    }

    fun onHighlightGoal(goalId: String?) {
        viewModelScope.launch {
            stateManager.updateState { it.copy(goalToHighlight = goalId) }
        }
    }

    fun onHighlightInboxRecord(recordId: String?) {
        viewModelScope.launch {
            stateManager.updateState { it.copy(inboxRecordToHighlight = recordId) }
        }
    }

    // Swipe actions
    fun onItemSwiped(itemId: String?) {
        viewModelScope.launch {
            stateManager.updateState { it.copy(swipedItemId = itemId) }
        }
    }

    fun onResetSwipeState() {
        viewModelScope.launch {
            stateManager.updateState { 
                it.copy(
                    swipedItemId = null,
                    swipeResetCounter = it.swipeResetCounter + 1
                ) 
            }
        }
    }

    // Checkboxes
    fun onToggleCheckboxes() {
        viewModelScope.launch {
            stateManager.updateState { it.copy(showCheckboxes = !it.showCheckboxes) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batchSaveJob?.cancel()
    }

    // Interface implementations будуть тут, але делеговані до відповідних handlers
    override fun onItemActionCompleted() = Unit
    override fun onSelectionCompleted() = Unit
    override fun onInputCompleted() = Unit
    override fun onInboxProcessed() = Unit
    override fun onInboxMarkdownProcessed() = Unit
    override fun onBacklogMarkdownProcessed() = Unit
}
