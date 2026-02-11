
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.context.ContextCommand
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogItemActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.CreationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.DirectionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserFlowActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.MarkdownActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.NavigationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ReminderActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.ArtifactHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.BacklogMarkdownHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.BacklogMarkdownHandlerResultListener
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.LogActivityHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.NoteDocumentHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.ProjectNavigationHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.ReminderHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.navigation.ContextRouteResolver
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ContextScreenDataMapper
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ContextScreenDataObserver
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import android.content.Context as AndroidContext

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ContextScreenViewModel
    @Inject
    constructor(
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
        private val directionRepository: DirectionRepository,
        private val noteRepository: LegacyNoteRepository,
        private val inboxRepository: InboxRepository,
        private val contextStructureRepository: ContextStructureRepository,
        private val contextArtifactRepository: ContextArtifactRepository,
        private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
        private val contextSessionStore: ContextSessionStore,
    ) : ViewModel(),
        ItemActionHandler.ResultListener,
        InputHandler.ResultListener,
        SelectionHandler.ResultListener,
        InboxHandlerResultListener,
        InboxMarkdownHandler.ResultListener,
        BacklogMarkdownHandlerResultListener,
        ProjectNavigationHandler.ResultListener,
        NoteDocumentHandler.ResultListener {
        companion object {
            const val HANDLE_LINK_CLICK_ROUTE = "handle_link_click"
            private const val TAG = "BacklogVM_DEBUG"
        }

        val canGoBack: StateFlow<Boolean> get() = enhancedNavigationManager.canGoBack
        val canGoForward: StateFlow<Boolean> get() = enhancedNavigationManager.canGoForward
        lateinit var enhancedNavigationManager: EnhancedNavigationManager
        val contextSessionState: StateFlow<com.romankozak.forwardappmobile.core.context.ContextSessionState> =
            contextSessionStore.state

        // State managers - делегування логіки
        internal val stateManager = ContextStateManager(viewModelScope)
        private val tagManager = TagManager(contextRepository, viewModelScope)
        private val activityManager =
            ActivityManager(
                activityRepository,
                contextRepository,
                settingsRepository,
                viewModelScope,
            )

        // Handlers - делегування дій
        private val contextIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
        private val originContextId: String? = savedStateHandle.get<String>("originContextId")
        private val _listContent = MutableStateFlow<List<BacklogItemContent>>(emptyList())
        val listContent: StateFlow<List<BacklogItemContent>> = _listContent.asStateFlow()
        private val _attachmentItems = MutableStateFlow<List<BacklogItemContent>>(emptyList())
        val attachmentItems: StateFlow<List<BacklogItemContent>> = _attachmentItems.asStateFlow()

        val itemActionHandler =
            ItemActionHandler(
                contextRepository,
                goalRepository,
                recentItemsRepository,
                viewModelScope,
                contextIdFlow,
                this,
            )

        val selectionHandler: SelectionHandler by lazy {
            SelectionHandler(
                contextRepository = contextRepository,
                goalRepository = goalRepository,
                scope = viewModelScope,
                projectIdFlow = contextIdFlow,
                listContentFlow = _listContent,
                resultListener = this,
            )
        }

        val inputHandler =
            InputHandler(
                contextRepository,
                goalRepository,
                listItemRepository,
                viewModelScope,
                contextIdFlow,
                this,
                reminderParser,
                alarmScheduler,
            )

        val inboxHandler =
            InboxHandler(
                contextRepository,
                inboxRepository,
                viewModelScope,
                contextIdFlow,
                this,
            )

    private val inboxMarkdownHandler by lazy {
        InboxMarkdownHandler(
            contextRepository = contextRepository,
            scope = viewModelScope, // Тепер Hilt не свариться, ми передаємо scope самі
            listener = this,
            goalRepository = goalRepository         // ViewModel виступає слухачем
        )
    }

        private val backlogMarkdownHandler =
            BacklogMarkdownHandler(
                contextRepository,
                goalRepository,
                listItemRepository,
                viewModelScope,
                this,
            )

        private var pendingAttachmentShare: BacklogItemContent? = null
        private var pendingDirectionLinkItemId: String? = null
        private var pendingAddDirectionFromContextChooser: Boolean = false
        private var isLinkedNavigationInProgress: Boolean = false
        private var pendingLinkedContextReplace: Boolean = false
        private var lastSyncKey: Triple<String, String?, Int>? = null
        private val routeResolver = ContextRouteResolver(HANDLE_LINK_CLICK_ROUTE)
        private val navigationActions by lazy {
            NavigationActions(
                contextRepository = contextRepository,
                recentItemsRepository = recentItemsRepository,
                settingsRepository = settingsRepository,
                ioDispatcher = ioDispatcher,
            )
        }
        private val backlogActions by lazy {
            BacklogActions(
                listItemRepository = listItemRepository,
                settingsRepository = settingsRepository,
                application = application,
            )
        }
        private val backlogItemActions by lazy {
            BacklogItemActions(
                goalRepository = goalRepository,
                contextRepository = contextRepository,
                noteDocumentRepository = noteDocumentRepository,
                checklistRepository = checklistRepository,
                noteRepository = noteRepository,
                listItemRepository = listItemRepository,
                dayManagementRepository = dayManagementRepository,
                activityRepository = activityRepository,
                contextTimeTrackingRepository = contextTimeTrackingRepository,
            )
        }
        private val listChooserActions by lazy {
            ListChooserActions(
                goalRepository = goalRepository,
                listItemRepository = listItemRepository,
                contextRepository = contextRepository,
            )
        }
        private val listChooserFlowActions by lazy {
            ListChooserFlowActions(
                contextRepository = contextRepository,
                directionRepository = directionRepository,
            )
        }
        private val directionActions by lazy {
            DirectionActions(
                directionRepository = directionRepository,
            )
        }
        private val creationActions by lazy {
            CreationActions(
                noteDocumentRepository = noteDocumentRepository,
            )
        }

        // Exposed StateFlows
        val uiState: StateFlow<ContextUiState> = stateManager.uiState
        val listScrollState = LazyListState()
    
        val allTags: StateFlow<List<String>> = tagManager.allTags
        val allContexts: StateFlow<List<String>> = tagManager.allContexts
        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

        private val _uiEventFlow =
            MutableSharedFlow<UiEvent>(
                extraBufferCapacity = 64,
            )
        val uiEventFlow = _uiEventFlow.asSharedFlow()

        private val _allProjects =
            contextRepository
                .getAllContextsFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val subprojectChildren: StateFlow<Map<String?, List<Context>>> =
            _allProjects
                .map { allProjects -> allProjects.groupBy { it.parentId } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val contextArtifact: StateFlow<ContextArtifact?> =
            contextIdFlow
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf(null)
                    } else {
                        contextArtifactRepository.getContextArtifactStream(contextId)
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val contextTimeMetrics: StateFlow<ContextTimeMetrics?> =
            contextIdFlow
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf<ContextTimeMetrics?>(null)
                    } else {
                        flow {
                            val metrics = contextTimeTrackingRepository.calculateContextTimeMetrics(contextId)
                            emit(metrics)
                        }
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )
        private var batchSaveJob: Job? = null
        private val contextScreenDataMapper = ContextScreenDataMapper()
        private val contextScreenDataObserver =
            ContextScreenDataObserver(
                contextRepository = contextRepository,
                listItemRepository = listItemRepository,
                contextStructureRepository = contextStructureRepository,
                contextLogRepository = contextLogRepository,
                checklistRepository = checklistRepository,
                noteDocumentRepository = noteDocumentRepository,
                directionRepository = directionRepository,
                reminderRepository = reminderRepository,
                recentItemsRepository = recentItemsRepository,
                noteRepository = noteRepository,
                goalRepository = goalRepository,
                mapper = contextScreenDataMapper,
            )

        val project =
            stateManager.uiState
                .map { it.context }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val lastOngoingActivity =
            activityManager.currentActivity
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val autocompleteSuggestions =
            tagManager.allTags
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        internal val artifactHandler by lazy {
            ArtifactHandler(
                contextRepository,
                stateManager,
                viewModelScope,
            )
        }
        internal val logHandler by lazy {
            LogActivityHandler(
                contextLogRepository,
                activityManager,
                stateManager,
                viewModelScope,
            )
        }
        internal val navHandler by lazy {
            ProjectNavigationHandler(
                contextRepository,
                stateManager,
                contextSessionStore,
                this,
                viewModelScope,
            )
        }
        internal val noteDocumentHandler by lazy {
            NoteDocumentHandler(
                contextRepository,
                noteDocumentRepository,
                settingsRepository,
                stateManager,
                this,
                viewModelScope,
            )
        }
        internal val reminderHandler by lazy { ReminderHandler(alarmScheduler, reminderRepository, viewModelScope) }
        private val reminderActions by lazy {
            ReminderActions(
                reminderRepository = reminderRepository,
                stateManager = stateManager,
                uiState = uiState,
                showSnackbar = ::showSnackbar,
                forceRefresh = ::forceRefresh,
            )
        }
        private val markdownActions by lazy {
            MarkdownActions(
                backlogMarkdownHandler = backlogMarkdownHandler,
                inboxMarkdownHandler = inboxMarkdownHandler,
                stateManager = stateManager,
                copyToClipboard = ::copyToClipboard,
                showSnackbar = ::showSnackbar,
            )
        }

        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = "",
                )

        init {
            setupContextObserver()
            observeContextIdChanges()
            tagManager.loadTags()
            activityManager.observeCurrentActivity()
        }

        private fun observeContextIdChanges() {
            viewModelScope.launch {
                contextIdFlow
                    .drop(1)
                    .collect {
                        stateManager.updateState { it.copy(isContextSwitching = true) }
                        _listContent.value = emptyList()
                        _attachmentItems.value = emptyList()
                    }
            }
        }

        private fun setupContextObserver() {
            viewModelScope.launch {
                contextScreenDataObserver
                    .observe(
                        contextIdFlow = contextIdFlow,
                        refreshTriggerFlow = uiState.map { it.refreshTrigger },
                    )
                    .debounce(80)
                    .collect { data ->
                        when (data) {
                            is ContextData.Loaded -> applyLoadedContextData(data)
                            is ContextData.Empty -> applyEmptyContextData()
                        }
                    }
            }
        }

        private fun applyLoadedContextData(data: ContextData.Loaded) {
            _listContent.value = data.items
            _attachmentItems.value = data.attachmentItems
            stateManager.updateContext(data)
            data.context?.let { project ->
                viewModelScope.launch {
                    recentItemsRepository.logProjectAccess(project)
                }
            }
            stateManager.updateState { currentState ->
                val contextId = data.context?.id ?: currentState.context?.id.orEmpty()
                val preferredViewName = data.context?.defaultViewModeName
                val syncKey = Triple(contextId, preferredViewName, data.config.hashCode())
                val session =
                    if (lastSyncKey == syncKey) {
                        contextSessionStore.state.value
                    } else {
                        lastSyncKey = syncKey
                        contextSessionStore.dispatch(
                            ContextCommand.SyncFromConfig(
                                contextId = contextId,
                                config = data.config,
                                preferredViewName = preferredViewName,
                                currentView = currentState.currentViewMode,
                            ),
                        )
                    }

                val enableInbox = session.enabledCapabilities.contains(CapabilityId("inbox"))
                val enableLog = session.enabledCapabilities.contains(CapabilityId("log"))
                val enableArtifact = session.enabledCapabilities.contains(CapabilityId("artifact"))
                val enableBacklog = session.enabledCapabilities.contains(CapabilityId("backlog"))
                val enableDashboard = session.enabledCapabilities.contains(CapabilityId("dashboard"))
                val enableAttachments = session.enabledCapabilities.contains(CapabilityId("attachments"))
                val isProjectManagementEnabled = session.enabledCapabilities.contains(CapabilityId("advanced"))

                if (currentState.enableInbox == enableInbox &&
                    currentState.enableLog == enableLog &&
                    currentState.enableArtifact == enableArtifact &&
                    currentState.enableBacklog == enableBacklog &&
                    currentState.enableDashboard == enableDashboard &&
                    currentState.enableAttachments == enableAttachments &&
                    currentState.isProjectManagementEnabled == isProjectManagementEnabled &&
                    currentState.experimentalCapabilityIds == data.config.experimentalCapabilityIds &&
                    currentState.currentViewMode == session.currentView &&
                    !currentState.isContextSwitching
                ) {
                    currentState
                } else {
                    currentState.copy(
                        enableInbox = enableInbox,
                        enableLog = enableLog,
                        enableArtifact = enableArtifact,
                        enableBacklog = enableBacklog,
                        enableDashboard = enableDashboard,
                        enableAttachments = enableAttachments,
                        isProjectManagementEnabled = isProjectManagementEnabled,
                        experimentalCapabilityIds = data.config.experimentalCapabilityIds,
                        currentViewMode = session.currentView,
                        isContextSwitching = false,
                    )
                }
            }
        }

        private fun applyEmptyContextData() {
            _listContent.value = emptyList()
            _attachmentItems.value = emptyList()
            stateManager.clear()
            stateManager.updateState { it.copy(isContextSwitching = false) }
        }

    override fun onBackPressed(): Boolean {
        val originId = originContextId
        val currentId = contextIdFlow.value
        if (!originId.isNullOrBlank() && originId != currentId) {
            savedStateHandle.remove<String>("originContextId")
            viewModelScope.launch {
                _uiEventFlow.emit(
                    UiEvent.Navigate(
                        NavTarget.ContextDetail(contextId = originId),
                    ),
                )
            }
            return true
        }
        viewModelScope.launch {
            // Прибираємо null, бо це значення за замовчуванням (виправляє WEAK_WARNING)
            _uiEventFlow.emit(UiEvent.ShowSnackbar("Повернення..."))

            // Відправляємо подію на закриття екрана
            _uiEventFlow.emit(UiEvent.NavigateBack)
        }

        // Повертаємо true, щоб повідомити BackHandler:
        // "Ми самі обробимо вихід через UIEvent, нічого більше робити не треба"
        return true
    }
// File: ContextScreenViewModel.kt

    // Додаємо = contextIdFlow.value як значення за замовчуванням
    fun onForwardPressed() {
        onForwardPressed(contextIdFlow.value)
    }

    // 2. Метод для виконання інтерфейсу (обов'язково String, без дефолту)
    override fun onForwardPressed(id: String) {
        viewModelScope.launch {
            enhancedNavigationManager.navigateToProject(id, "Context")
        }
    }

        override fun onHomeClick() {
            viewModelScope.launch {
                _uiEventFlow.tryEmit(UiEvent.Navigate(NavTarget.ContextHierarchy))
            }
        }

    fun deleteCurrentProject() {
        deleteCurrentProject(contextIdFlow.value)
    }

    // 2. Цей метод реалізує інтерфейс (з параметром id)
    override fun deleteCurrentProject(id: String) {
        viewModelScope.launch {
            contextRepository.getContextById(id)?.let { project ->
                contextRepository.deleteContextsAndSubContexts(listOf(project))
            }
            // Використовуємо emit всередині launch для надійності
            _uiEventFlow.emit(UiEvent.NavigateBack)
        }
    }

        // UI Events
        override fun showSnackbar(
            message: String,
            actionLabel: String?,
        ) {
            viewModelScope.launch {
                _uiEventFlow.emit(UiEvent.ShowSnackbar(message, actionLabel))
            }
        }

        override fun showSnackbar(message: String) {
            showSnackbar(message, null)
        }

        override fun scrollToListEnd() {
            viewModelScope.launch { _uiEventFlow.tryEmit(UiEvent.ScrollToLatestInboxRecord) }
        }

        override fun updateInputState(inputValue: TextFieldValue) {
            stateManager.setInputValue(inputValue)
        }

        // Delegated methods
        fun onProjectViewChange(mode: ContextViewMode) {
            val session = contextSessionStore.dispatch(ContextCommand.SelectView(mode))
            val resolved = session.currentView
            stateManager.switchViewMode(resolved)
            if (resolved == ContextViewMode.DIRECTION) {
                stateManager.setInputMode(InputMode.AddDirection)
            }
            val contextId = contextIdFlow.value
            if (contextId.isBlank()) return
            viewModelScope.launch(ioDispatcher) {
                contextRepository.updateContextViewMode(contextId, resolved)
            }
        }

        fun onToggleAttachmentsExpanded() {
            val context = uiState.value.context ?: return
            viewModelScope.launch(ioDispatcher) {
                contextRepository.updateContext(
                    context.copy(isAttachmentsExpanded = !context.isAttachmentsExpanded),
                )
            }
        }

        fun onDashboardTabSelected(tab: ContextManagementTab) = stateManager.switchTab(tab)

        fun onToggleSearchMode() = stateManager.toggleSearchMode()

        fun onSearchQueryChanged(query: String) = stateManager.updateSearchQuery(query)

        fun onDismissDisplayPropertiesDialog() = stateManager.dismissDisplayPropertiesDialog()

        fun onShowDisplayPropertiesDialog() = stateManager.showDisplayPropertiesDialog()

        // Activity tracking
        fun stopOngoingActivity() = activityManager.stopActivity()

        fun setReminderForOngoingActivity() {
            lastOngoingActivity.value?.let {
                stateManager.updateState { uiState ->
                    uiState.copy(recordForReminderDialog = it)
                }
            }
        }

        fun onStartTrackingCurrentProject() {
            project.value?.id?.let {
                activityManager.startActivity(it)
            }
        }

        // Capabilities
        fun hasCapability(capabilityId: CapabilityId) =
            contextSessionStore.state.value.enabledCapabilities.contains(capabilityId)

        // Markdown Export/Import
        fun onExportBacklogToMarkdown() {
            markdownActions.onExportBacklogToMarkdown(_listContent.value)
        }

        fun onImportBacklogFromMarkdown(markdownText: String) {
            markdownActions.onImportBacklogFromMarkdown(markdownText, contextIdFlow.value)
        }

        fun onShowImportBacklogFromMarkdownDialog() {
            markdownActions.onShowImportBacklogFromMarkdownDialog()
        }

        fun onDismissImportBacklogFromMarkdownDialog() {
            markdownActions.onDismissImportBacklogFromMarkdownDialog()
        }

        // Clipboard operations
        override fun copyToClipboard(
            text: String,
            label: String,
        ) {
            val clipboard = application.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        }

        override fun forceRefresh() {
            viewModelScope.launch {
                stateManager.updateState {
                    it.copy(
                        refreshTrigger = it.refreshTrigger + 1,
                        needsStateRefresh = true,
                    )
                }
            }
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
            stateManager.updateState { currentState ->
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
        }

        override fun updateDialogState(
            showAddWebLinkDialog: Boolean?,
            showAddObsidianLinkDialog: Boolean?,
        ) {
            stateManager.updateState {
                it.copy(
                    showAddWebLinkDialog = showAddWebLinkDialog ?: it.showAddWebLinkDialog,
                    showAddObsidianLinkDialog = showAddObsidianLinkDialog ?: it.showAddObsidianLinkDialog,
                )
            }
        }

        override fun showRecentListsSheet(show: Boolean) {
            stateManager.updateState { it.copy(showRecentProjectsSheet = show) }
        }

        override fun addQuickRecord(text: String) {
            inboxHandler.addQuickRecord(text)
        }

        override fun addProjectComment(text: String) {
            logHandler.addProjectComment(text, contextIdFlow.value)
        }

        override fun addMilestone(text: String) {
            logHandler.addMilestone(text, contextIdFlow.value)
        }

        override fun createObsidianNote(noteName: String) {
            noteDocumentHandler.createObsidianNote(noteName)
        }

        override fun openUri(uri: String) {
            viewModelScope.launch {
                _uiEventFlow.tryEmit(UiEvent.OpenUri(uri))
            }
        }

        override fun requestNavigation(route: String) {
            viewModelScope.launch {
                when (val result = routeResolver.resolve(route)) {
                    is ContextRouteResolver.ResolveResult.Back -> {
                        _uiEventFlow.tryEmit(UiEvent.NavigateBack)
                    }

                    is ContextRouteResolver.ResolveResult.GoalDetail -> {
                        val goalDetail = navigationActions.resolveGoalDetail(result.contextId)
                        enhancedNavigationManager.navigateToProject(goalDetail.contextId, goalDetail.contextName)
                    }

                    is ContextRouteResolver.ResolveResult.HandleLinkClick -> {
                        val links =
                            (listContent.value + attachmentItems.value)
                                .filterIsInstance<BacklogItemContent.LinkItem>()
                                .map { it.link.linkData }
                        when (val linkResult = navigationActions.resolveHandleLinkClick(result.rawTarget, links)) {
                            is NavigationActions.HandleLinkClickResult.ExistingLink -> onLinkItemClick(linkResult.link)
                            is NavigationActions.HandleLinkClickResult.OpenObsidianNote -> {
                                when (val noteResult = navigationActions.resolveObsidianNoteOpen(linkResult.noteTarget)) {
                                    is NavigationActions.OpenObsidianNoteResult.OpenUri -> _uiEventFlow.tryEmit(UiEvent.OpenUri(noteResult.uri))
                                    is NavigationActions.OpenObsidianNoteResult.VaultNotConfigured ->
                                        _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Назву Obsidian сховища не встановлено."))
                                }
                            }
                            is NavigationActions.HandleLinkClickResult.OpenUri -> _uiEventFlow.tryEmit(UiEvent.OpenUri(linkResult.uri))
                            is NavigationActions.HandleLinkClickResult.NavigateToContext ->
                                enhancedNavigationManager.navigateToProject(linkResult.contextId, linkResult.contextName)
                            is NavigationActions.HandleLinkClickResult.UnknownTarget -> {
                                Log.w(TAG, "Unknown related link target: ${linkResult.target}")
                                _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Unknown link: ${linkResult.target}"))
                            }
                        }
                    }

                    is ContextRouteResolver.ResolveResult.Navigate -> {
                        _uiEventFlow.tryEmit(UiEvent.Navigate(result.target))
                    }

                    is ContextRouteResolver.ResolveResult.Unknown -> {
                        Log.w(TAG, "Unknown navigation route: ${result.route}")
                    }
                }
            }
        }

        fun onLinkItemClick(link: RelatedLink) {
            Log.d(TAG, "onLinkItemClick: Clicked link with type=${link.type}, target=${link.target}")
            viewModelScope.launch {
                when (val result = navigationActions.resolveLinkItemClick(link)) {
                    is NavigationActions.LinkItemClickResult.NavigateToContext ->
                        enhancedNavigationManager.navigateToProject(result.contextId, result.contextName)
                    is NavigationActions.LinkItemClickResult.OpenUri -> _uiEventFlow.tryEmit(UiEvent.OpenUri(result.uri))
                    is NavigationActions.LinkItemClickResult.VaultNotConfigured ->
                        _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Obsidian vault name is not configured."))
                    is NavigationActions.LinkItemClickResult.DelegateToUi ->
                        _uiEventFlow.tryEmit(UiEvent.HandleLinkClick(result.link))
                }
            }
        }

        private fun navigateToListChooser(title: String) {
            viewModelScope.launch {
                val disabledIds = contextIdFlow.value
                _uiEventFlow.tryEmit(
                    UiEvent.Navigate(
                        NavTarget.ListChooser(
                            title = title,
                            disabledIds = disabledIds.ifBlank { null },
                        ),
                    ),
                )
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
                    GoalActionType.CreateInstance -> "Create link in..."
                    GoalActionType.MoveInstance -> "Move to..."
                    GoalActionType.CopyGoal -> "Copy to..."
                    GoalActionType.AddLinkToList -> "Add link to context..."
                    GoalActionType.ADD_LIST_SHORTCUT -> "Add context shortcut..."
                }
            navigateToListChooser(title)
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
                        swipeResetCounter = it.swipeResetCounter + 1,
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

        override fun isSelectionModeActive(): Boolean = stateManager.uiState.value.isSelectionModeActive

        override fun toggleSelection(itemId: String) {
            stateManager.toggleItemSelection(itemId)
        }

        override fun requestAttachmentShare(item: BacklogItemContent) {
            pendingAttachmentShare = item
            navigateToListChooser("Select context for attachment")
        }

        override fun updateSelectionState(selectedIds: Set<String>) {
            stateManager.updateState { it.copy(selectedItemIds = selectedIds) }
        }

        // public methods for handlers
// File: ContextScreenViewModel.kt

    // 1. Метод для UI (приймає тільки контент)
    fun onSaveArtifact(content: String) {
        onSaveArtifact(contextIdFlow.value, content)
    }

    // 2. Основний метод (виконує роботу)
    fun onSaveArtifact(projectId: String, content: String) {
        artifactHandler.onSaveArtifact(projectId, content)
    }

        fun onAutoSaveArtifact(content: String) = artifactHandler.onAutoSaveArtifact(content)

        fun onDismissArtifactEditor() = artifactHandler.onDismissArtifactEditor()

        fun onDismissNoteDocumentEditor() = noteDocumentHandler.onDismissNoteDocumentEditor()

        fun onToggleProjectManagement(isEnabled: Boolean) {
            viewModelScope.launch {
                contextRepository.toggleContextManagement(contextIdFlow.value, isEnabled)
            }
        }

        fun onDismissEditLogEntryDialog() = logHandler.onDismissEditLogEntryDialog()

        fun onUpdateLogEntry(
            description: String,
            details: String?,
        ) = logHandler.onUpdateLogEntry(
            uiState.value.logEntryToEdit!!,
            description,
            details,
        )

        fun onEditLogEntry(log: ContextLog) = logHandler.onEditLogEntry(log)

        fun onDeleteLogEntry(log: ContextLog) = logHandler.onDeleteLogEntry(log)

        fun onEditArtifact(artifact: ContextArtifact) = artifactHandler.onEditArtifact(artifact)

        fun onCopyToClipboardRequest() {
            markdownActions.onCopyBacklogToClipboardRequest(listContent.value)
        }

// ... (rest of the code)

        fun onTransferBacklogToServerRequest() {
            viewModelScope.launch {
                when (val result = backlogActions.transferBacklogToServer(project.value?.name, listContent.value)) {
                    is BacklogActions.TransferResult.Message -> showSnackbar(result.text, null)
                }
            }
        }

        fun onExportBacklogToMarkdownRequest() = onExportBacklogToMarkdown()

        fun onSetReminderForProject() {
            viewModelScope.launch {
                reminderActions.onSetReminderForProject(project.value)
            }
        }

        fun onImportFromMarkdownRequest() {
            markdownActions.onImportFromMarkdownRequest()
        }

        fun onImportFromMarkdownDismiss() {
            markdownActions.onImportFromMarkdownDismiss()
        }

// File: ContextScreenViewModel.kt

    fun onSaveNoteDocument(content: String, newContextId: String?): Unit =
        noteDocumentHandler.onSaveNoteDocument(
            // Якщо newContextId == null, використовуємо поточний contextIdFlow.value
            newContextId ?: contextIdFlow.value,
            contextIdFlow.value,
            content
        )
        fun onExportProjectStateRequest() {
            contextMarkdownExporter.exportProjectStateToMarkdown(
                project = project.value,
                backlog = listContent.value,
                logs = uiState.value.logs,
                listener = this,
            )
        }

        fun addCurrentProjectToDayPlan() {
            viewModelScope.launch {
                showSnackbar(backlogItemActions.addCurrentProjectToDayPlan(contextIdFlow.value), null)
            }
        }

        fun onCloseSearch() {
            stateManager.updateState { it.copy(localSearchQuery = "") }
        }

        fun onAddMilestone(text: String) = addMilestone(text)

        fun onShowCreateNoteDocumentDialog() {
            viewModelScope.launch {
                when (val result = creationActions.createNoteDocument(contextIdFlow.value)) {
                    is CreationActions.CreateNoteDocumentResult.Navigate ->
                        _uiEventFlow.tryEmit(UiEvent.Navigate(result.target))
                    is CreationActions.CreateNoteDocumentResult.Error ->
                        showSnackbar(result.message, null)
                }
            }
        }

        fun onCreateChecklist() {
            when (val result = creationActions.createChecklist(contextIdFlow.value)) {
                is CreationActions.CreateChecklistResult.Navigate ->
                    viewModelScope.launch { _uiEventFlow.tryEmit(UiEvent.Navigate(result.target)) }
                is CreationActions.CreateChecklistResult.Error ->
                    showSnackbar(result.message, null)
            }
        }

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
            val newText =
                currentText.substring(0, startIndex) +
                    suggestion +
                    " " +
                    currentText.substring(cursorPosition)
            val newCursorPosition = startIndex + suggestion.length + 1

            stateManager.updateState {
                it.copy(
                    inputValue =
                        TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPosition),
                        ),
                )
            }
        }

        fun onMove(
            from: Int,
            to: Int,
        ) {
            viewModelScope.launch {
                _listContent.value = backlogActions.move(_listContent.value, from, to)
            }
        }

        override fun addDirectionItem(text: String) {
            val trimmed = text.trim()
            if (trimmed.isBlank()) {
                showSnackbar("Напрямок не може бути порожнім.", null)
                return
            }
            viewModelScope.launch(ioDispatcher) {
                val parentContextId = contextIdFlow.value
                if (parentContextId.isBlank()) return@launch

                val childContextId = UUID.randomUUID().toString()
                contextRepository.createContextWithId(
                    id = childContextId,
                    name = trimmed,
                    parentId = parentContextId,
                )
                directionRepository.addDirectionItem(
                    contextId = parentContextId,
                    text = trimmed,
                    linkedContextId = childContextId,
                )
            }
        }

        fun onAddDirectionWithLinkedContextRequest() {
            if (!hasCapability(CapabilityId("direction"))) {
                showSnackbar("Можливість direction недоступна.", null)
                return
            }
            pendingAddDirectionFromContextChooser = true
            savedStateHandle["pendingAddDirectionFromContextChooser"] = true
            viewModelScope.launch {
                val disabledIds = contextIdFlow.value.ifBlank { null }
                _uiEventFlow.tryEmit(
                    UiEvent.Navigate(
                        NavTarget.ListChooser(
                            title = "Add direction to...",
                            disabledIds = disabledIds,
                        ),
                    ),
                )
            }
        }

        fun updateDirectionItemText(
            item: DirectionItemEntity,
            text: String,
        ) {
            viewModelScope.launch(ioDispatcher) {
                directionActions
                    .updateDirectionItemText(item, text)
                    ?.let { errorMessage ->
                        withContext(Dispatchers.Main) {
                            showSnackbar(errorMessage, null)
                        }
                    }
            }
        }

        fun deleteDirectionItem(itemId: String) {
            viewModelScope.launch(ioDispatcher) {
                directionActions.deleteDirectionItem(itemId)
            }
        }

        fun onLinkDirectionItemRequest(itemId: String) {
            pendingDirectionLinkItemId = itemId
            savedStateHandle["pendingDirectionLinkItemId"] = itemId
            savedStateHandle["pendingDirectionLink"] = true
            viewModelScope.launch {
                val disabledIds = contextIdFlow.value.ifBlank { null }
                _uiEventFlow.tryEmit(
                    UiEvent.Navigate(
                        NavTarget.ListChooser(
                            title = "Link direction to...",
                            disabledIds = disabledIds,
                        ),
                    ),
                )
            }
        }

        fun onUnlinkDirectionItem(itemId: String) {
            updateDirectionItemLink(itemId, null)
        }

        private fun updateDirectionItemLink(
            itemId: String,
            linkedContextId: String?,
        ) {
            viewModelScope.launch(ioDispatcher) {
                directionActions.updateDirectionItemLink(uiState.value.directionItems, itemId, linkedContextId)
            }
        }

        fun openLinkedContext(contextId: String) {
            val currentId = contextIdFlow.value
            when (val result = directionActions.resolveOpenLinkedContext(contextId, currentId)) {
                is DirectionActions.OpenLinkedContextResult.Error -> {
                    showSnackbar(result.message, null)
                    return
                }

                is DirectionActions.OpenLinkedContextResult.Navigate -> Unit
            }
            if (isLinkedNavigationInProgress) return
            isLinkedNavigationInProgress = true
            viewModelScope.launch {
                pendingLinkedContextReplace = true
                _uiEventFlow.tryEmit(
                    UiEvent.Navigate(
                        NavTarget.ContextDetail(
                            contextId = contextId,
                            originContextId = currentId,
                        ),
                    ),
                )
                kotlinx.coroutines.delay(500)
                isLinkedNavigationInProgress = false
            }
        }

        fun consumeLinkedContextReplace(): Boolean {
            if (!pendingLinkedContextReplace) return false
            pendingLinkedContextReplace = false
            return true
        }

        fun clearPendingDirectionLink() {
            pendingDirectionLinkItemId = null
            savedStateHandle.remove<String>("pendingDirectionLinkItemId")
            savedStateHandle.remove<Boolean>("pendingDirectionLink")
            pendingAddDirectionFromContextChooser = false
            savedStateHandle.remove<Boolean>("pendingAddDirectionFromContextChooser")
        }

        fun onMoveDirectionItem(
            from: Int,
            to: Int,
        ) {
            viewModelScope.launch(ioDispatcher) {
                directionActions
                    .reorderDirectionItems(uiState.value.directionItems, from, to)
                    ?.let { reordered ->
                        withContext(Dispatchers.Main) {
                            stateManager.updateState { it.copy(directionItems = reordered) }
                        }
                    }
            }
        }

        fun onSubprojectCompletedChanged(
            subproject: Context,
            completed: Boolean,
        ) {
            viewModelScope.launch {
                backlogItemActions.updateSubprojectCompleted(subproject, completed)
                forceRefresh()
            }
        }

        fun onDeleteEverywhere(item: BacklogItemContent) {
            viewModelScope.launch {
                val message = backlogItemActions.deleteEverywhere(item)
                showSnackbar(message, null)
                forceRefresh()
            }
        }

        fun onMoveToTop(item: BacklogItemContent) {
            viewModelScope.launch {
                _listContent.value = backlogActions.moveToTop(_listContent.value, item)
            }
        }

        fun addItemToDailyPlan(item: BacklogItemContent) {
            viewModelScope.launch {
                showSnackbar(backlogItemActions.addItemToDailyPlan(item), null)
            }
        }

        fun onStartTrackingRequest(item: BacklogItemContent) {
            viewModelScope.launch {
                showSnackbar(backlogItemActions.startTracking(item), null)
            }
        }

        fun onProjectStatusUpdate(
            newStatus: String,
            statusText: String?,
        ) {
            viewModelScope.launch {
                backlogItemActions.updateProjectStatus(contextIdFlow.value, newStatus, statusText)
            }
        }

        fun onRecalculateTime() {
            viewModelScope.launch {
                val metrics = backlogItemActions.recalculateTime(contextIdFlow.value)
                stateManager.updateState { it.copy(contextTimeMetrics = metrics) }
            }
        }

// ... (rest of the imports)

        fun onListChooserResult(targetcontextId: String) {
            pendingDirectionLinkItemId?.let { itemId ->
                pendingDirectionLinkItemId = null
                savedStateHandle.remove<String>("pendingDirectionLinkItemId")
                savedStateHandle.remove<Boolean>("pendingDirectionLink")
                val resolved = targetcontextId.takeIf { it != "root" }
                updateDirectionItemLink(itemId, resolved)
                return
            }

            val pendingDirectionAdd =
                pendingAddDirectionFromContextChooser ||
                    (savedStateHandle.get<Boolean>("pendingAddDirectionFromContextChooser") == true)
            if (pendingDirectionAdd) {
                pendingAddDirectionFromContextChooser = false
                savedStateHandle.remove<Boolean>("pendingAddDirectionFromContextChooser")
                viewModelScope.launch(ioDispatcher) {
                    val result =
                        listChooserFlowActions.addDirectionLinkedToContext(
                            targetContextId = targetcontextId,
                            currentContextId = contextIdFlow.value,
                        )
                    result.errorMessage?.let { message ->
                        withContext(Dispatchers.Main) {
                            showSnackbar(message, null)
                        }
                    }
                }
                return
            }

            pendingAttachmentShare?.let { attachment ->
                pendingAttachmentShare = null
                viewModelScope.launch(ioDispatcher) {
                    val result =
                        listChooserFlowActions.shareAttachmentToProject(
                            attachment = attachment,
                            targetContextId = targetcontextId,
                            currentContextId = contextIdFlow.value,
                        )
                    withContext(Dispatchers.Main) {
                        result.newlyAddedItemId?.let { newItemId ->
                            stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                        }
                        if (result.shouldRefreshCurrentContext) {
                            forceRefresh()
                        }
                        showSnackbar(result.message, null)
                    }
                }
                return
            }

            if (inboxHandler.recordForPromotion.value != null) {
                inboxHandler.onListSelectedForInboxPromotion(targetcontextId)
                return
            }

            val actionTypeName = savedStateHandle.get<String>("pendingAction") ?: return
            val actionType = GoalActionType.valueOf(actionTypeName)
            val itemIds = savedStateHandle.get<List<String>>("pendingSourceItemIds") ?: emptyList()
            val goalIds = savedStateHandle.get<List<String>>("pendingSourceGoalIds") ?: emptyList()

            viewModelScope.launch(ioDispatcher) { // Use ioDispatcher for repository operations
                val result =
                    listChooserActions.executePendingAction(
                        actionType = actionType,
                        targetContextId = targetcontextId,
                        currentContextId = contextIdFlow.value,
                        itemIds = itemIds,
                        goalIds = goalIds,
                    )
                withContext(Dispatchers.Main) {
                    result.newlyAddedItemId?.let { newItemId ->
                        stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                    }
                    forceRefresh()
                }
            }
            savedStateHandle.remove<String>("pendingAction")
            savedStateHandle.remove<List<String>>("pendingSourceItemIds")
            savedStateHandle.remove<List<String>>("pendingSourceGoalIds")
            selectionHandler.clearSelection()
        }

        fun onScrolledToNewItem() {
            stateManager.updateState { it.copy(newlyAddedItemId = null) }
        }

        fun onHighlightShown() {
            stateManager.updateState { it.copy(goalToHighlight = null, itemToHighlight = null) }
        }

        fun onInboxHighlightShown() {
            Log.d(TAG, "Clearing inbox highlight state.")
            stateManager.updateState { it.copy(inboxRecordToHighlight = null) }
        }

        fun onLimitLastActivityRequested() {
            // not implemented
        }

        fun resetSwipeStatesExcept(itemId: String) {
            // This should be handled in the UI, not in the viewmodel
        }

        fun onSetReminderForItem(item: BacklogItemContent) {
            viewModelScope.launch {
                reminderActions.onSetReminderForItem(item)
            }
        }

        fun onOpenRemindersDialog(itemContent: BacklogItemContent) {
            reminderActions.onOpenRemindersDialog(itemContent)
        }

        fun onDismissRemindersDialog() {
            reminderActions.onDismissRemindersDialog()
        }

        fun onRecentItemClick(item: RecentItem) {
            viewModelScope.launch {
                when (item.type) {
                    RecentItemType.PROJECT -> enhancedNavigationManager.navigateToProject(item.target, item.displayName ?: "Context")
                    RecentItemType.NOTE -> { /* legacy notes no longer have dedicated editor; no-op */ }
                    RecentItemType.NOTE_DOCUMENT -> _uiEventFlow.tryEmit(UiEvent.Navigate(NavTarget.NoteDocument(id = item.target)))
                    RecentItemType.CHECKLIST -> _uiEventFlow.tryEmit(UiEvent.Navigate(NavTarget.Checklist(id = item.target)))
                    RecentItemType.OBSIDIAN_LINK -> {
                        // Assuming toRelatedLink() exists or is created
                        // recentItemsRepository.logObsidianLinkAccess(item.toRelatedLink())
                        val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                        if (vaultName.isNotBlank()) {
                            val encodedNoteName = URLEncoder.encode(item.target, "UTF-8")
                            val uri = "obsidian://open?vault=$vaultName&file=$encodedNoteName"
                            _uiEventFlow.tryEmit(UiEvent.OpenUri(uri))
                        } else {
                            _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Obsidian vault name is not configured."))
                        }
                    }
                }
            }
        }

        fun onPinRecentItem(item: RecentItem) {
            // Not implemented yet
        }

// ... (rest of the code)

        fun onClearReminder() =
            viewModelScope.launch {
                reminderActions.onClearReminder()
            }

        fun onSetReminder(timestamp: Long) =
            viewModelScope.launch {
                reminderActions.onSetReminder(timestamp)
            }

            fun onRemoveReminder(reminderId: String) =
                viewModelScope.launch {
                    reminderActions.onRemoveReminder(reminderId)
                }
    fun getBacklogAsMarkdown(): String = backlogActions.getBacklogAsMarkdown(listContent.value)

        // Will be removed



// У файлі ContextScreenViewModel.kt

        fun onSwipeStateReset(itemId: String) {
            // 1. Використовуємо stateManager замість uiState
            stateManager.updateState { currentState ->
                // 2. Створюємо копію карти тригерів
                val newTriggers = currentState.resetTriggers.toMutableMap()

                // 3. Збільшуємо лічильник для конкретного ID (це змушує Compose скинути стан Swipe)
                newTriggers[itemId] = (newTriggers[itemId] ?: 0) + 1

                // 4. Повертаємо оновлений стан через copy
                currentState.copy(resetTriggers = newTriggers)
            }
        }

    fun onExportToMarkdownRequest() {
        markdownActions.onExportInboxToMarkdown(inboxHandler.inboxRecords.value)
    }

    // File: ContextScreenViewModel.kt

    fun onImportBacklogFromMarkdownRequest() {
        markdownActions.onShowImportBacklogFromMarkdownDialog()
    }

    fun onImportBacklogFromMarkdownDismiss() {
        markdownActions.onDismissImportBacklogFromMarkdownDialog()
    }

    fun onImportBacklogFromMarkdownConfirm(markdownText: String) {
        markdownActions.onImportBacklogFromMarkdownConfirm(markdownText, contextIdFlow.value)
    }

    fun onReminderDialogDismiss() {
        onDismissRemindersDialog()
    }

    fun onImportFromMarkdownConfirm(markdownText: String) {
        markdownActions.onImportFromMarkdownConfirm(markdownText, contextIdFlow.value)
    }

    fun copyInboxRecordText(text: String) {
        markdownActions.copyInboxRecordText(text)
    }

    }
