package com.romankozak.forwardappmobile.features.contexts.ui.context_screen
import android.app.Application
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionIds
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionRegistry
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogDndCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogItemActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.BacklogItemRepositories
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ClipboardActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextDataApplyActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextPickerActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextPickerRepositories
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextSettingsActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ContextViewActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.CreationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.CreationResultActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.CurrentContextActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.DirectionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.DirectionChooserActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputStateUpdate
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserFlowActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserOrchestrationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserPendingStateActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserResultActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ListChooserResultCoordinatorActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.MarkdownActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.NavigationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.NavigationEffectDispatcherActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.NavigationEventActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.RecentItemActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.ReminderActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.TopNavigationActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.UiControlActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.UiEventDispatcherActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.UiStateActions
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
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ContextScreenDataMapper
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ContextScreenDataObserver
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ContextScreenDataObserverDependencies
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.*
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ContextScreenViewModel
    @Inject
    constructor(
        private val searchUseCase: SearchUseCase,
        private val application: Application,
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val contextHandler: ContextMarkerHandler,
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
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val reminderRepository: ReminderRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val contextLogRepository: ContextLogRepository,
        private val directionRepository: DirectionRepository,
        private val noteRepository: LegacyNoteRepository,
        private val inboxRepository: InboxRepository,
        private val contextStructureRepository: ContextStructureRepository,
        private val contextArtifactRepository: ContextArtifactRepository,
        private val contextKeyProblemsRepository: ContextKeyProblemsRepository,
        private val focusContextRepository: FocusContextRepository,
        private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
        private val contextSessionStore: ContextSessionStore,
        private val backlogClipboardUseCase: BacklogClipboardUseCase,
        private val capabilityViewActionRegistry: CapabilityViewActionRegistry,
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

        private var _enhancedNavigationManager: EnhancedNavigationManager =
            EnhancedNavigationManager(
                savedStateHandle = SavedStateHandle(),
                scope = viewModelScope,
            )
        var enhancedNavigationManager: EnhancedNavigationManager
            get() = _enhancedNavigationManager
            set(value) {
                _enhancedNavigationManager = value
            }
        val canGoBack: StateFlow<Boolean> get() = enhancedNavigationManager.canGoBack
        val canGoForward: StateFlow<Boolean> get() = enhancedNavigationManager.canGoForward
        val contextSessionState: StateFlow<com.romankozak.forwardappmobile.core.context.ContextSessionState> =
            contextSessionStore.state
        internal val stateManager = ContextStateManager(viewModelScope)
        private val tagManager = TagManager(contextRepository, viewModelScope)
        private val activityManager = ActivityManager(activityRepository, viewModelScope)
        private val contextIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
        private val originContextId: String? = savedStateHandle.get<String>("originContextId")
        private val _listContent = MutableStateFlow<List<BacklogItemContent>>(emptyList())
        val listContent: StateFlow<List<BacklogItemContent>> = _listContent.asStateFlow()
        private val _attachmentItems = MutableStateFlow<List<BacklogItemContent>>(emptyList())
        val attachmentItems: StateFlow<List<BacklogItemContent>> = _attachmentItems.asStateFlow()
        val itemActionHandler =
            ItemActionHandler(
                contextRepository = contextRepository,
                goalRepository = goalRepository,
                recentItemsRepository = recentItemsRepository,
                backlogClipboardUseCase = backlogClipboardUseCase,
                scope = viewModelScope,
                projectIdFlow = contextIdFlow,
                resultListener = this,
            )
        val selectionHandler: SelectionHandler by lazy {
            SelectionHandler(
                contextRepository = contextRepository,
                goalRepository = goalRepository,
                backlogClipboardUseCase = backlogClipboardUseCase,
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
        val inboxHandler = InboxHandler(contextRepository, inboxRepository, viewModelScope, contextIdFlow, this)
        private val inboxMarkdownHandler by lazy {
            InboxMarkdownHandler(
                scope = viewModelScope, // Тепер Hilt не свариться, ми передаємо scope самі
                listener = this,
                goalRepository = goalRepository, // ViewModel виступає слухачем
            )
        }
        private val backlogMarkdownHandler =
            BacklogMarkdownHandler(contextRepository, goalRepository, listItemRepository, viewModelScope, this)
        private var listChooserPendingState = ListChooserPendingStateActions.VmPendingState()
        private var pendingLinkedContextReplace: Boolean = false
        private val navigationActions by lazy {
            NavigationActions(
                contextRepository = contextRepository,
                recentItemsRepository = recentItemsRepository,
                settingsRepository = settingsRepository,
                ioDispatcher = ioDispatcher,
                handleLinkClickRoute = HANDLE_LINK_CLICK_ROUTE,
            )
        }
        private val navigationEventActions = NavigationEventActions()
        private val navigationEffectDispatcherActions = NavigationEffectDispatcherActions(navigationEventActions)
        private val topNavigationActions = TopNavigationActions()
        private val uiEventActions by lazy { UiEventDispatcherActions(_uiEventFlow) }
        private val uiControlActions by lazy { UiControlActions(stateManager = stateManager, contextSessionStore = contextSessionStore) }
        private val clipboardActions by lazy { ClipboardActions(application) }
        private val backlogActions by lazy {
            BacklogActions(
                listItemRepository = listItemRepository,
                settingsRepository = settingsRepository,
            )
        }
        private val backlogDndCoordinator by lazy {
            BacklogDndCoordinator(backlogActions) { message, error ->
                Log.w(TAG, message, error)
            }
        }
        private val backlogItemActions by lazy {
            BacklogItemActions(
                repositories =
                    BacklogItemRepositories(
                        goalRepository = goalRepository,
                        contextRepository = contextRepository,
                        noteDocumentRepository = noteDocumentRepository,
                        musicNoteRepository = musicNoteRepository,
                        checklistRepository = checklistRepository,
                        noteRepository = noteRepository,
                        listItemRepository = listItemRepository,
                        dayManagementRepository = dayManagementRepository,
                        activityRepository = activityRepository,
                        contextTimeTrackingRepository = contextTimeTrackingRepository,
                    ),
            )
        }
        private val listChooserActions by lazy {
            ListChooserActions(
                listItemRepository = listItemRepository,
                contextRepository = contextRepository,
                backlogClipboardUseCase = backlogClipboardUseCase,
            )
        }
        private val listChooserFlowActions by lazy {
            ListChooserFlowActions(contextRepository = contextRepository, directionRepository = directionRepository)
        }
        private val listChooserOrchestrationActions = ListChooserOrchestrationActions()
        private val listChooserPendingStateActions = ListChooserPendingStateActions()
        private val listChooserResultActions by lazy {
            ListChooserResultActions(
                orchestrationActions = listChooserOrchestrationActions,
                flowActions = listChooserFlowActions,
                listChooserActions = listChooserActions,
            )
        }
        private val listChooserResultCoordinatorActions by lazy {
            ListChooserResultCoordinatorActions(
                listChooserResultActions = listChooserResultActions,
                pendingStateActions = listChooserPendingStateActions,
            )
        }
        private val directionActions by lazy { DirectionActions(directionRepository = directionRepository) }
        private val directionChooserActions = DirectionChooserActions()
        private val creationActions by lazy {
            CreationActions(
                noteDocumentRepository = noteDocumentRepository,
                musicNoteRepository = musicNoteRepository,
            )
        }
        private val creationResultActions = CreationResultActions()
        private val contextSettingsActions by lazy { ContextSettingsActions(contextRepository = contextRepository) }
        private val currentContextActions by lazy {
            CurrentContextActions(
                stateManager = stateManager,
                activityManager = activityManager,
                contextSettingsActions = contextSettingsActions,
            )
        }
        private val contextViewActions by lazy {
            ContextViewActions(
                contextSessionStore = contextSessionStore,
                stateManager = stateManager,
            )
        }
        private val contextDataApplyActions by lazy {
            ContextDataApplyActions(
                stateManager = stateManager,
                contextSessionStore = contextSessionStore,
                recentItemsRepository = recentItemsRepository,
                scope = viewModelScope,
            )
        }
        private val recentItemActions by lazy { RecentItemActions(settingsRepository = settingsRepository) }
        private val inputSuggestionActions = InputSuggestionActions()
        private val uiStateActions by lazy { UiStateActions(stateManager = stateManager) }
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
        val allContextsForPicker: StateFlow<List<Context>> = _allProjects
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
        val contextAttachments: StateFlow<List<AttachmentWithContext>> =
            contextIdFlow
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        contextRepository.getAttachmentsForContextStream(contextId)
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )
        val keyProblemsData: StateFlow<ContextKeyProblemsRepository.KeyProblemsData> =
            contextIdFlow
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf(ContextKeyProblemsRepository.KeyProblemsData())
                    } else {
                        contextKeyProblemsRepository.observe(contextId)
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = ContextKeyProblemsRepository.KeyProblemsData(),
                )
        private val focusedContextIds: StateFlow<Set<String>> =
            focusContextRepository
                .observeActiveFocusContextIds()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptySet(),
                )
        val isCurrentContextFocused: StateFlow<Boolean> =
            combine(contextIdFlow, focusedContextIds) { contextId, focusedIds ->
                contextId.isNotBlank() && focusedIds.contains(contextId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )
        val pickerAttachmentOptions: StateFlow<List<AttachmentOption>> =
            contextRepository.getAttachmentLibraryItemsFlow()
                .map { results ->
                    results
                        .map { it.toAttachmentOption() }
                        .sortedBy { it.name.lowercase() }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )
        private var batchSaveJob: Job? = null
        private val contextScreenDataMapper = ContextScreenDataMapper()
        private val contextScreenDataObserver =
            ContextScreenDataObserver(
                dependencies =
                    ContextScreenDataObserverDependencies(
                        contextRepository = contextRepository,
                        listItemRepository = listItemRepository,
                        contextStructureRepository = contextStructureRepository,
                        contextLogRepository = contextLogRepository,
                        checklistRepository = checklistRepository,
                        noteDocumentRepository = noteDocumentRepository,
                        musicNoteRepository = musicNoteRepository,
                        directionRepository = directionRepository,
                        reminderRepository = reminderRepository,
                        recentItemsRepository = recentItemsRepository,
                        noteRepository = noteRepository,
                        goalRepository = goalRepository,
                    ),
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
            combine(
                uiState.map { it.inputValue },
                tagManager.allTags,
                contextHandler.contextMarkerNamesFlow,
            ) { inputValue, allTags, contextMarkerNames ->
                inputSuggestionActions.buildSuggestions(
                    currentText = inputValue.text,
                    cursorPosition = inputValue.selection.start,
                    contextMarkerNames = contextMarkerNames,
                    tags = allTags,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        private val contextPickerActions by lazy {
            ContextPickerActions(
                repositories =
                    ContextPickerRepositories(
                        contextRepository = contextRepository,
                        contextKeyProblemsRepository = contextKeyProblemsRepository,
                        focusContextRepository = focusContextRepository,
                        noteDocumentRepository = noteDocumentRepository,
                        musicNoteRepository = musicNoteRepository,
                        checklistRepository = checklistRepository,
                    ),
                listChooserFlowActions = listChooserFlowActions,
                loggerTag = TAG,
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
            observeDirectionFrontAutoLinkSyncOnOpen()
            tagManager.loadTags()
            activityManager.observeCurrentActivity()
        }

        private fun observeContextIdChanges() {
            viewModelScope.launch {
                contextIdFlow
                    .drop(1)
                    .collect {
                        backlogDndCoordinator.reset()
                        stateManager.updateState { it.copy(isContextSwitching = true) }
                        _listContent.value = emptyList()
                        _attachmentItems.value = emptyList()
                    }
            }
        }

        private fun observeDirectionFrontAutoLinkSyncOnOpen() {
            viewModelScope.launch(ioDispatcher) {
                contextIdFlow
                    .filter { it.isNotBlank() }
                    .distinctUntilChanged()
                    .collectLatest { contextId ->
                        runCatching {
                            contextRepository.ensureDirectionFrontLinksForExistingChildren(contextId)
                        }.onFailure { error ->
                            Log.w(TAG, "Failed to sync child context links into direction front for context=$contextId", error)
                        }
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
                            is ContextData.Loaded ->
                                contextDataApplyActions.applyLoaded(
                                    data = data,
                                    setListContent = { observed ->
                                        _listContent.value = backlogDndCoordinator.applyObserved(observed, _listContent.value)
                                    },
                                    setAttachmentItems = { _attachmentItems.value = it },
                                )
                            is ContextData.Empty ->
                                contextDataApplyActions.applyEmpty(
                                    clearListContent = {
                                        backlogDndCoordinator.reset()
                                        _listContent.value = emptyList()
                                    },
                                    clearAttachmentItems = { _attachmentItems.value = emptyList() },
                                )
                        }
                    }
            }
        }

        override fun onBackPressed(): Boolean {
            val backResult =
                topNavigationActions.resolveBack(
                    originContextId = originContextId,
                    currentContextId = contextIdFlow.value,
                )
            if (backResult.shouldClearOriginContext) {
                savedStateHandle.remove<String>("originContextId")
            }
            viewModelScope.launch { uiEventActions.emitAll(backResult.events) }
            return true
        }

        fun onForwardPressed() = onForwardPressed(contextIdFlow.value)

        override fun onForwardPressed(id: String) {
            viewModelScope.launch {
                withNavigationManager { manager ->
                    manager.navigateToProject(id, "Context")
                }
            }
        }

        override fun onHomeClick() {
            viewModelScope.launch { uiEventActions.tryEmit(topNavigationActions.homeEvent()) }
        }

        fun deleteCurrentProject() = deleteCurrentProject(contextIdFlow.value)

        override fun deleteCurrentProject(id: String) {
            viewModelScope.launch {
                contextSettingsActions.deleteCurrentProject(id)
                uiEventActions.emit(UiEvent.NavigateBack)
            }
        }

        override fun showSnackbar(
            message: String,
            actionLabel: String?,
        ) {
            viewModelScope.launch { uiEventActions.emit(UiEvent.ShowSnackbar(message, actionLabel)) }
        }

        override fun showSnackbar(message: String) = showSnackbar(message, null)

        override fun scrollToListEnd() {
            viewModelScope.launch { uiEventActions.tryEmit(UiEvent.ScrollToLatestInboxRecord) }
        }

        override fun updateInputState(inputValue: TextFieldValue) = stateManager.setInputValue(inputValue)

        fun onProjectViewChange(mode: ContextViewMode) {
            val resolved = contextViewActions.applyViewChange(mode)
            viewModelScope.launch(ioDispatcher) {
                contextSettingsActions.persistContextViewMode(contextIdFlow.value, resolved)
            }
        }

        fun onToggleAttachmentsExpanded() =
            viewModelScope.launch(ioDispatcher) { currentContextActions.toggleAttachmentsExpanded(uiState.value.context) }

        fun onDashboardTabSelected(tab: ContextManagementTab) = uiControlActions.selectDashboardTab(tab)

        fun onToggleSearchMode() = uiControlActions.toggleSearchMode()

        fun onSearchQueryChanged(query: String) = uiControlActions.updateSearchQuery(query)

        fun onDismissDisplayPropertiesDialog() = uiControlActions.dismissDisplayPropertiesDialog()

        fun onShowDisplayPropertiesDialog() = uiControlActions.showDisplayPropertiesDialog()

        fun stopOngoingActivity() = activityManager.stopActivity()

        fun setReminderForOngoingActivity() = currentContextActions.setReminderForOngoingActivity(lastOngoingActivity.value)

        fun onStartTrackingCurrentProject() = currentContextActions.startTrackingCurrentProject(project.value?.id)

        fun hasCapability(capabilityId: CapabilityId) = uiControlActions.hasCapability(capabilityId)

        fun getAvailableCapabilityViewActions(
            currentView: ContextViewMode,
            enabledCapabilities: Set<CapabilityId>,
        ): List<CapabilityViewActionDescriptor> =
            capabilityViewActionRegistry
                .forView(currentView, enabledCapabilities + capabilityForView(currentView))
                .map { it.descriptor }

        fun onCapabilityViewActionClick(actionId: String) {
            when (actionId) {
                CapabilityViewActionIds.BACKLOG_IMPORT_MARKDOWN -> onShowImportBacklogFromMarkdownDialog()
                CapabilityViewActionIds.BACKLOG_EXPORT_MARKDOWN -> onExportBacklogToMarkdown()
                CapabilityViewActionIds.DIRECTION_COPY_LINKED_BACKLOGS_AS_LINKS -> copyDirectionLinkedBacklogsAsLinks()
                else -> showSnackbar("Невідома дія: $actionId", null)
            }
        }

        private fun copyDirectionLinkedBacklogsAsLinks() {
            viewModelScope.launch(ioDispatcher) {
                val currentContextId = contextIdFlow.value
                if (currentContextId.isBlank()) return@launch

                val linkedContextIds =
                    uiState.value.directionItems
                        .mapNotNull { item -> item.linkedContextId?.takeIf { it.isNotBlank() } }
                        .distinct()
                        .filter { it != currentContextId }

                if (linkedContextIds.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showSnackbar("У напрямку немає пов'язаних контекстів", null)
                    }
                    return@launch
                }

                var createdLinks = 0
                var skippedDuplicates = 0
                val alreadyLinkedGoalIds = listItemRepository.getGoalIdsForContext(currentContextId).toMutableSet()

                linkedContextIds.forEach { linkedContextId ->
                    val sourceGoalIds = listItemRepository.getGoalIdsForContext(linkedContextId).distinct()
                    val goalIdsToLink =
                        sourceGoalIds.filterNot { goalId ->
                            goalId in alreadyLinkedGoalIds || listItemRepository.doesLinkExist(goalId, currentContextId)
                        }
                    skippedDuplicates += sourceGoalIds.size - goalIdsToLink.size
                    if (goalIdsToLink.isEmpty()) return@forEach

                    goalRepository.createGoalLinks(
                        goalIds = goalIdsToLink,
                        targetContextId = currentContextId,
                        sourceContextId = linkedContextId,
                    )
                    createdLinks += goalIdsToLink.size
                    alreadyLinkedGoalIds.addAll(goalIdsToLink)
                }

                withContext(Dispatchers.Main) {
                    showSnackbar(
                        "Додано посилань: $createdLinks, дублікати пропущено: $skippedDuplicates",
                        null,
                    )
                    if (createdLinks > 0) {
                        forceRefresh()
                    }
                }
            }
        }

        private fun capabilityForView(viewMode: ContextViewMode): CapabilityId =
            when (viewMode) {
                ContextViewMode.ADVANCED, ContextViewMode.ARTIFACT -> CapabilityId("advanced")
                else -> CapabilityId(viewMode.name.lowercase())
            }

        fun onExportBacklogToMarkdown() = markdownActions.onExportBacklogToMarkdown(_listContent.value)

        fun onImportBacklogFromMarkdown(markdownText: String) =
            markdownActions.onImportBacklogFromMarkdownConfirm(markdownText, contextIdFlow.value)

        fun onShowImportBacklogFromMarkdownDialog() = markdownActions.onShowImportBacklogFromMarkdownDialog()

        fun onDismissImportBacklogFromMarkdownDialog() = markdownActions.onDismissImportBacklogFromMarkdownDialog()

        override fun copyToClipboard(
            text: String,
            label: String,
        ) = clipboardActions.copy(text, label)

        override fun forceRefresh() {
            uiControlActions.forceRefresh()
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
            uiStateActions.updateInputState(
                InputStateUpdate(
                    inputValue = inputValue,
                    inputMode = inputMode,
                    localSearchQuery = localSearchQuery,
                    newlyAddedItemId = newlyAddedItemId,
                    detectedReminderSuggestion = detectedReminderSuggestion,
                    detectedReminderCalendar = detectedReminderCalendar,
                    clearDetectedReminder = clearDetectedReminder,
                ),
            )
        }

        override fun updateDialogState(
            showAddWebLinkDialog: Boolean?,
            showAddObsidianLinkDialog: Boolean?,
        ) {
            uiStateActions.updateDialogState(
                showAddWebLinkDialog = showAddWebLinkDialog,
                showAddObsidianLinkDialog = showAddObsidianLinkDialog,
            )
        }

        override fun showRecentListsSheet(show: Boolean) = stateManager.updateState { it.copy(showRecentProjectsSheet = show) }

        override fun addQuickRecord(text: String) = inboxHandler.addQuickRecord(text)

        override fun addProjectComment(text: String) = logHandler.addProjectComment(text, contextIdFlow.value)

        override fun addMilestone(text: String) = logHandler.addMilestone(text, contextIdFlow.value)

        override fun createObsidianNote(noteName: String) = noteDocumentHandler.createObsidianNote(noteName)

        override fun openUri(uri: String) {
            viewModelScope.launch { uiEventActions.tryEmit(UiEvent.OpenUri(uri)) }
        }

        override fun requestNavigation(route: String) {
            viewModelScope.launch {
                val links =
                    (listContent.value + attachmentItems.value)
                        .filterIsInstance<BacklogItemContent.LinkItem>()
                        .map { it.link.linkData }
                val outcome = navigationActions.resolveRoute(route, links)
                dispatchNavigationEffects(navigationEventActions.fromRouteOutcome(outcome))
            }
        }
        override fun openGoalInlineEditor(goal: Goal) {
            stateManager.setGoalToEditInline(goal)
        }

        fun onLinkItemClick(link: RelatedLink) {
            Log.d(TAG, "onLinkItemClick: Clicked link with type=${link.type}, target=${link.target}")
            viewModelScope.launch {
                val result = navigationActions.resolveLinkItemClick(link)
                dispatchNavigationEffects(navigationEventActions.fromLinkClickResult(result))
            }
        }

        private suspend fun dispatchNavigationEffects(effects: List<NavigationEventActions.Effect>) {
            navigationEffectDispatcherActions.dispatch(
                effects = effects,
                navigateToProject = { contextId, contextName ->
                    withNavigationManager { manager ->
                        manager.navigateToProject(contextId, contextName)
                    }
                },
                emitUiEvent = { event ->
                    uiEventActions.tryEmit(event)
                },
                resolveLinkClick = { link ->
                    navigationActions.resolveLinkItemClick(link)
                },
                logUnknownRoute = { route ->
                    Log.w(TAG, "Unknown navigation route: $route")
                },
            )
        }

        private inline fun withNavigationManager(block: (EnhancedNavigationManager) -> Unit) {
            runCatching { block(enhancedNavigationManager) }
                .onFailure { error -> Log.w(TAG, "Navigation manager call failed", error) }
        }

        override fun setPendingAction(
            actionType: GoalActionType,
            itemIds: Set<String>,
            goalIds: Set<String>,
        ) {
            listChooserPendingStateActions.savePendingAction(savedStateHandle, actionType, itemIds, goalIds)
            val navigation =
                listChooserActions.buildPendingActionNavigation(
                    actionType = actionType,
                    currentContextId = contextIdFlow.value,
                )
            viewModelScope.launch {
                uiEventActions.tryEmit(UiEvent.Navigate(navigation.target))
            }
        }

        fun onShowRecentProjectsSheet() = uiStateActions.showRecentProjectsSheet()

        fun onDismissRecentProjectsSheet() = uiStateActions.dismissRecentProjectsSheet()

        fun onShowShareDialog() = uiStateActions.showShareDialog()

        fun onDismissShareDialog() = uiStateActions.dismissShareDialog()

        fun onShowAddWebLinkDialog() = uiStateActions.showAddWebLinkDialog()

        fun onDismissAddWebLinkDialog() = uiStateActions.dismissAddWebLinkDialog()

        fun onShowAddObsidianLinkDialog() = uiStateActions.showAddObsidianLinkDialog()

        fun onDismissAddObsidianLinkDialog() = uiStateActions.dismissAddObsidianLinkDialog()

        fun onHighlightItem(itemId: String?) = uiStateActions.highlightItem(itemId)

        fun onHighlightGoal(goalId: String?) = uiStateActions.highlightGoal(goalId)

        fun onHighlightInboxRecord(recordId: String?) = uiStateActions.highlightInboxRecord(recordId)

        fun onItemSwiped(itemId: String?) = uiStateActions.setSwipedItem(itemId)

        fun onResetSwipeState() = uiStateActions.resetSwipeState()

        fun onToggleCheckboxes() = uiStateActions.toggleCheckboxes()

        override fun onCleared() {
            super.onCleared()
            batchSaveJob?.cancel()
        }

        override fun isSelectionModeActive(): Boolean = stateManager.uiState.value.isSelectionModeActive

        override fun toggleSelection(itemId: String) = stateManager.toggleItemSelection(itemId)

        override fun requestAttachmentShare(item: BacklogItemContent) {
            listChooserPendingState = listChooserPendingState.copy(pendingAttachmentShare = item)
            viewModelScope.launch {
                uiEventActions.tryEmit(
                    UiEvent.Navigate(
                        listChooserActions.buildAttachmentShareNavigation(contextIdFlow.value),
                    ),
                )
            }
        }

        override fun updateSelectionState(selectedIds: Set<String>) = stateManager.updateState { it.copy(selectedItemIds = selectedIds) }

        fun onSaveArtifact(content: String) = onSaveArtifact(contextIdFlow.value, content)

        fun onSaveArtifact(
            projectId: String,
            content: String,
        ) = artifactHandler.onSaveArtifact(projectId, content)

        fun onAutoSaveArtifact(content: String) = artifactHandler.onAutoSaveArtifact(content)

        fun onDismissArtifactEditor() = artifactHandler.onDismissArtifactEditor()

        fun onDismissNoteDocumentEditor() = noteDocumentHandler.onDismissNoteDocumentEditor()
        fun onDismissGoalInlineEditor() = stateManager.setGoalToEditInline(null)
        fun onSaveGoalInlineEditor(text: String) {
            val goal = stateManager.uiState.value.goalToEditInline ?: return
            val trimmed = text.trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch {
                goalRepository.updateGoal(goal.copy(text = trimmed))
                stateManager.setGoalToEditInline(null)
            }
        }
        fun openGoalProperties(item: BacklogItemContent) {
            val goal = (item as? BacklogItemContent.GoalItem)?.goal ?: return
            requestNavigation("goal_settings_screen/${goal.id}")
        }
        fun onToggleProjectManagement(isEnabled: Boolean) =
            viewModelScope.launch { currentContextActions.toggleProjectManagement(contextIdFlow.value, isEnabled) }

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

        fun onCopyToClipboardRequest() = markdownActions.onCopyBacklogToClipboardRequest(listContent.value)

        fun onTransferBacklogToServerRequest() {
            viewModelScope.launch {
                when (val result = backlogActions.transferBacklogToServer(project.value?.name, listContent.value)) {
                    is BacklogActions.TransferResult.Message -> showSnackbar(result.text, null)
                }
            }
        }

        fun onSetReminderForProject() = viewModelScope.launch { reminderActions.onSetReminderForProject(project.value) }

        fun onImportFromMarkdownRequest() = markdownActions.onImportFromMarkdownRequest()

        fun onImportFromMarkdownDismiss() = markdownActions.onImportFromMarkdownDismiss()

        fun onSaveNoteDocument(
            content: String,
            newContextId: String?,
        ) = noteDocumentHandler.onSaveNoteDocument(
            newContextId ?: contextIdFlow.value,
            contextIdFlow.value,
            content,
        )

        fun onExportProjectStateRequest() =
            contextMarkdownExporter.exportProjectStateToMarkdown(
                project = project.value,
                backlog = listContent.value,
                logs = uiState.value.logs,
                listener = this,
            )

        fun addCurrentProjectToDayPlan() =
            viewModelScope.launch {
                showSnackbar(backlogItemActions.addCurrentProjectToDayPlan(contextIdFlow.value), null)
            }

        fun onCloseSearch() = uiStateActions.closeSearch()

        fun onAddMilestone(text: String) = addMilestone(text)

        fun onShowCreateNoteDocumentDialog() =
            viewModelScope.launch {
                val result = creationActions.createNoteDocument(contextIdFlow.value)
                dispatchCreationOutcome(creationResultActions.fromNoteDocumentResult(result))
            }

        fun onCreateChecklist() =
            viewModelScope.launch {
                val result = creationActions.createChecklist(contextIdFlow.value)
                dispatchCreationOutcome(creationResultActions.fromChecklistResult(result))
            }

        fun onPickerContextSelected(targetContextId: String) =
            viewModelScope.launch {
                contextPickerActions.onPickerContextSelected(
                    currentContextId = contextIdFlow.value,
                    targetContextId = targetContextId,
                    showSnackbar = { message -> showSnackbar(message, null) },
                )
            }

        fun onBacklogContextLinkSelected(targetContextId: String) =
            viewModelScope.launch {
                contextPickerActions.onBacklogContextLinkSelected(
                    currentContextId = contextIdFlow.value,
                    targetContextId = targetContextId,
                    showSnackbar = { message -> showSnackbar(message, null) },
                    forceRefresh = ::forceRefresh,
                )
            }

        fun onDirectionContextLinkSelected(targetContextId: String) =
            viewModelScope.launch {
                contextPickerActions.onDirectionContextLinkSelected(
                    currentContextId = contextIdFlow.value,
                    targetContextId = targetContextId,
                    directionItems = uiState.value.directionItems,
                    showSnackbar = { message -> showSnackbar(message, null) },
                )
            }

        fun onKeyProblemsDescriptionChanged(description: String) =
            viewModelScope.launch(ioDispatcher) {
                contextPickerActions.onKeyProblemsDescriptionChanged(
                    currentContextId = contextIdFlow.value,
                    description = description,
                )
            }

        fun addKeyProblemsFocusContext(targetContextId: String) =
            viewModelScope.launch(ioDispatcher) {
                contextPickerActions.addKeyProblemsFocusContext(
                    currentContextId = contextIdFlow.value,
                    targetContextId = targetContextId,
                )
            }

        fun removeKeyProblemsFocusContext(targetContextId: String) =
            viewModelScope.launch(ioDispatcher) {
                contextPickerActions.removeKeyProblemsFocusContext(
                    currentContextId = contextIdFlow.value,
                    targetContextId = targetContextId,
                )
            }

        fun toggleCurrentContextFocus() =
            viewModelScope.launch(ioDispatcher) {
                contextPickerActions.toggleCurrentContextFocus(
                    contextId = contextIdFlow.value,
                    showSnackbar = { message -> showSnackbar(message, null) },
                )
            }

        fun onPickerAttachmentSelected(attachmentId: String) =
            viewModelScope.launch {
                contextPickerActions.onPickerAttachmentSelected(
                    currentContextId = contextIdFlow.value,
                    attachmentId = attachmentId,
                )
            }

        fun openScriptEditorForCurrentContext() {
            val contextId = contextIdFlow.value.takeIf { it.isNotBlank() } ?: return
            viewModelScope.launch {
                uiEventActions.tryEmit(UiEvent.Navigate(NavTarget.ScriptEditor(contextId = contextId)))
            }
        }

        fun openScriptAttachment(scriptId: String) {
            if (scriptId.isBlank()) return
            viewModelScope.launch {
                uiEventActions.tryEmit(UiEvent.Navigate(NavTarget.ScriptEditor(scriptId = scriptId)))
            }
        }

        fun deleteAttachmentEverywhereById(attachmentId: String) =
            viewModelScope.launch {
                if (attachmentId.isBlank()) return@launch
                contextRepository.deleteAttachmentEverywhere(attachmentId)
                showSnackbar("Вкладення видалено", null)
                forceRefresh()
            }

        fun unlinkAttachmentFromCurrentContextById(attachmentId: String) =
            viewModelScope.launch {
                if (attachmentId.isBlank()) return@launch
                val currentContextId = contextIdFlow.value
                if (currentContextId.isBlank()) return@launch
                contextRepository.unlinkAttachmentFromContext(currentContextId, attachmentId)
                showSnackbar("Зв'язок видалено з цього списку", null)
                forceRefresh()
            }

        suspend fun createRootContextForPicker(name: String): String? {
            return contextPickerActions.createRootContextForPicker(name)
        }

        suspend fun createAttachmentForPicker(request: NewDocumentDraft): String? {
            return contextPickerActions.createAttachmentForPicker(
                currentContextId = contextIdFlow.value,
                request = request,
            )
        }

        private suspend fun dispatchCreationOutcome(outcome: CreationResultActions.Outcome) {
            when (outcome) {
                is CreationResultActions.Outcome.Navigate ->
                    uiEventActions.tryEmit(UiEvent.Navigate(outcome.target))
                is CreationResultActions.Outcome.ShowMessage ->
                    showSnackbar(outcome.message, null)
            }
        }

        fun onSuggestionClick(suggestion: String) {
            val currentText = uiState.value.inputValue.text
            val cursorPosition = uiState.value.inputValue.selection.start
            val applyResult =
                inputSuggestionActions.applySuggestion(
                    currentText = currentText,
                    cursorPosition = cursorPosition,
                    suggestion = suggestion,
                ) ?: return
            stateManager.updateState {
                it.copy(
                    inputValue =
                        TextFieldValue(
                            text = applyResult.text,
                            selection = TextRange(applyResult.cursorPosition),
                        ),
                )
            }
        }

        fun onMove(
            from: Int,
            to: Int,
        ) {
            if (from == to) return
            _listContent.value = backlogDndCoordinator.move(_listContent.value, from, to)
        }

        fun onBacklogDragStopped() {
            viewModelScope.launch(ioDispatcher) {
                backlogDndCoordinator.onDragStopped(_listContent.value)
            }
        }

        override fun addDirectionItem(text: String) {
            viewModelScope.launch(ioDispatcher) {
                directionActions
                    .addDirectionItemWithLinkedContext(
                        parentContextId = contextIdFlow.value,
                        text = text,
                    )?.let { errorMessage ->
                        withContext(Dispatchers.Main) {
                            showSnackbar(errorMessage, null)
                        }
                    }
            }
        }

        fun onAddDirectionWithLinkedContextRequest() {
            if (!hasCapability(CapabilityId("direction"))) {
                showSnackbar("Можливість direction недоступна.", null)
                return
            }
            val request =
                directionChooserActions.createAddDirectionRequest(
                    disabledIds = contextIdFlow.value.ifBlank { null },
                )
            listChooserPendingState =
                listChooserPendingStateActions.saveDirectionAddRequest(
                    savedStateHandle = savedStateHandle,
                    request = request,
                    currentState = listChooserPendingState,
                )
            viewModelScope.launch {
                uiEventActions.tryEmit(UiEvent.Navigate(request.navigationTarget))
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

        fun copyDirectionItem(item: DirectionItemEntity) {
            backlogClipboardUseCase.copyDirectionItems(
                sourceContextId = contextIdFlow.value,
                itemIds = listOf(item.id),
            )
            showSnackbar("Скопійовано елемент напрямку. Перейди в цільовий список і натисни Вставити", null)
        }

        fun cutDirectionItem(item: DirectionItemEntity) {
            backlogClipboardUseCase.cutDirectionItems(
                sourceContextId = contextIdFlow.value,
                itemIds = listOf(item.id),
            )
            showSnackbar("Вирізано елемент напрямку. Перейди в цільовий список і натисни Вставити", null)
        }

        fun deleteDirectionItem(itemId: String) =
            viewModelScope.launch(ioDispatcher) {
                directionActions.deleteDirectionItem(itemId)
            }

        fun onLinkDirectionItemRequest(itemId: String) {
            val request =
                directionChooserActions.createLinkDirectionRequest(
                    itemId = itemId,
                    disabledIds = contextIdFlow.value.ifBlank { null },
                )
            listChooserPendingState =
                listChooserPendingStateActions.saveDirectionLinkRequest(
                    savedStateHandle = savedStateHandle,
                    request = request,
                    currentState = listChooserPendingState,
                )
            viewModelScope.launch {
                uiEventActions.tryEmit(UiEvent.Navigate(request.navigationTarget))
            }
        }

        fun onUnlinkDirectionItem(itemId: String) {
            updateDirectionItemLink(itemId, null)
        }

        private fun updateDirectionItemLink(
            itemId: String,
            linkedContextId: String?,
        ) = viewModelScope.launch(ioDispatcher) {
            directionActions.updateDirectionItemLink(uiState.value.directionItems, itemId, linkedContextId)
        }

        fun openLinkedContext(contextId: String) {
            val currentId = contextIdFlow.value
            when (val result = directionActions.resolveOpenLinkedContext(contextId, currentId)) {
                is DirectionActions.OpenLinkedContextResult.Error -> {
                    showSnackbar(result.message, null)
                    return
                }
                is DirectionActions.OpenLinkedContextResult.InProgress -> return
                is DirectionActions.OpenLinkedContextResult.Navigate -> Unit
            }
            viewModelScope.launch {
                pendingLinkedContextReplace = true
                uiEventActions.tryEmit(
                    UiEvent.Navigate(
                        NavTarget.ContextDetail(
                            contextId = contextId,
                            originContextId = currentId,
                        ),
                    ),
                )
                directionActions.releaseLinkedNavigationLock()
            }
        }

        fun consumeLinkedContextReplace(): Boolean {
            if (!pendingLinkedContextReplace) return false
            pendingLinkedContextReplace = false
            return true
        }

        fun clearPendingDirectionLink() {
            val cleared = directionChooserActions.clearPendingDirection()
            listChooserPendingState =
                listChooserPendingState.copy(
                    pendingDirectionLinkItemId = cleared.pendingDirectionLinkItemId,
                    pendingAddDirectionFromContextChooser = cleared.pendingAddDirectionFromContextChooser,
                )
            savedStateHandle.remove<String>(ListChooserPendingStateActions.KEY_PENDING_DIRECTION_LINK_ITEM_ID)
            savedStateHandle.remove<Boolean>(ListChooserPendingStateActions.KEY_PENDING_DIRECTION_LINK)
            savedStateHandle.remove<Boolean>(ListChooserPendingStateActions.KEY_PENDING_ADD_DIRECTION_FROM_CHOOSER)
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
        ) = viewModelScope.launch {
            backlogItemActions.updateSubprojectCompleted(subproject, completed)
            forceRefresh()
        }

        fun onDeleteEverywhere(item: BacklogItemContent) =
            viewModelScope.launch {
                val message = backlogItemActions.deleteEverywhere(item)
                showSnackbar(message, null)
                forceRefresh()
            }

        fun onMoveToTop(item: BacklogItemContent) =
            viewModelScope.launch {
                _listContent.value = backlogActions.moveToTop(_listContent.value, item)
            }

        fun addItemToDailyPlan(item: BacklogItemContent) =
            viewModelScope.launch {
                showSnackbar(backlogItemActions.addItemToDailyPlan(item), null)
            }

        fun onStartTrackingRequest(item: BacklogItemContent) =
            viewModelScope.launch {
                showSnackbar(backlogItemActions.startTracking(item), null)
            }

        fun onProjectStatusUpdate(
            newStatus: String,
            statusText: String?,
        ) = viewModelScope.launch {
            backlogItemActions.updateProjectStatus(contextIdFlow.value, newStatus, statusText)
        }

        fun onRecalculateTime() =
            viewModelScope.launch {
                val metrics = backlogItemActions.recalculateTime(contextIdFlow.value)
                stateManager.updateState { it.copy(contextTimeMetrics = metrics) }
            }

        fun onListChooserResult(targetContextId: String) {
            viewModelScope.launch(ioDispatcher) {
                val result =
                    listChooserResultCoordinatorActions.process(
                        ListChooserResultCoordinatorActions.ProcessInput(
                            targetContextId = targetContextId,
                            currentContextId = contextIdFlow.value,
                            savedStateHandle = savedStateHandle,
                            pendingState = listChooserPendingState,
                            hasInboxPromotionRecord = inboxHandler.recordForPromotion.value != null,
                            clearDirectionPending = { directionChooserActions.clearPendingDirection() },
                            clearSelection = { selectionHandler.clearSelection() },
                        ),
                    )
                withContext(Dispatchers.Main) {
                    when (val command = result.command) {
                        is ListChooserResultCoordinatorActions.Command.DirectionLink ->
                            updateDirectionItemLink(command.itemId, command.linkedContextId)
                        is ListChooserResultCoordinatorActions.Command.ShowMessage ->
                            showSnackbar(command.message, null)
                        is ListChooserResultCoordinatorActions.Command.AttachmentShare -> {
                            command.newlyAddedItemId?.let { newItemId ->
                                stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                            }
                            if (command.shouldRefreshCurrentContext) forceRefresh()
                            showSnackbar(command.message, null)
                        }
                        is ListChooserResultCoordinatorActions.Command.InboxPromotion ->
                            inboxHandler.onListSelectedForInboxPromotion(command.targetContextId)
                        is ListChooserResultCoordinatorActions.Command.PendingAction -> {
                            command.newlyAddedItemId?.let { newItemId ->
                                stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                            }
                            command.userMessage?.let { showSnackbar(it, null) }
                            forceRefresh()
                        }
                        ListChooserResultCoordinatorActions.Command.None -> Unit
                    }
                    listChooserPendingState = result.pendingState
                }
            }
        }

        fun onScrolledToNewItem() = uiStateActions.markNewItemConsumed()

        fun onHighlightShown() = uiStateActions.clearHighlightState()

        fun onInboxHighlightShown() {
            Log.d(TAG, "Clearing inbox highlight state.")
            uiStateActions.clearInboxHighlightState()
        }

        fun onLimitLastActivityRequested() {}

        fun resetSwipeStatesExcept(itemId: String) {}

        fun onSetReminderForItem(item: BacklogItemContent) =
            viewModelScope.launch {
                reminderActions.onSetReminderForItem(item)
            }

        fun onOpenRemindersDialog(itemContent: BacklogItemContent) = reminderActions.onOpenRemindersDialog(itemContent)

        fun onDismissRemindersDialog() = reminderActions.onDismissRemindersDialog()

        fun onRecentItemClick(item: RecentItem) {
            viewModelScope.launch {
                val result = recentItemActions.resolve(item)
                dispatchNavigationEffects(navigationEventActions.fromRecentItemResult(result))
            }
        }

        fun onPinRecentItem(item: RecentItem) {}

        fun onClearReminder() = viewModelScope.launch { reminderActions.onClearReminder() }

        fun onSetReminder(timestamp: Long) = viewModelScope.launch { reminderActions.onSetReminder(timestamp) }

        fun onRemoveReminder(reminderId: String) = viewModelScope.launch { reminderActions.onRemoveReminder(reminderId) }

        fun getBacklogAsMarkdown(): String = backlogActions.getBacklogAsMarkdown(listContent.value)

        fun onSwipeStateReset(itemId: String) = uiStateActions.bumpSwipeResetTrigger(itemId)

        fun onExportInboxToMarkdown() = markdownActions.onExportInboxToMarkdown(inboxHandler.inboxRecords.value)

        fun onImportFromMarkdownConfirm(markdownText: String) =
            markdownActions.onImportFromMarkdownConfirm(markdownText, contextIdFlow.value)

        fun copyInboxRecordText(text: String) = markdownActions.copyInboxRecordText(text)
    }

private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption {
    val relatedLink =
        linkDisplayName?.let { json ->
            runCatching { Gson().fromJson(json, RelatedLink::class.java) }.getOrNull()
        }
    val linkLabel =
        relatedLink?.displayName?.takeIf { it.isNotBlank() }
            ?: relatedLink?.target?.takeIf { it.isNotBlank() }
    val label =
        noteName?.takeIf { it.isNotBlank() }
            ?: musicNoteName?.takeIf { it.isNotBlank() }
            ?: checklistName?.takeIf { it.isNotBlank() }
            ?: scriptName?.takeIf { it.isNotBlank() }
            ?: linkLabel
            ?: contextName
            ?: "Attachment ${id.takeLast(4)}"

    return AttachmentOption(
        id = id,
        name = label,
        linkType = relatedLink?.type,
        attachmentType = attachmentType,
        entityId = entityId,
        target = relatedLink?.target,
    )
}
