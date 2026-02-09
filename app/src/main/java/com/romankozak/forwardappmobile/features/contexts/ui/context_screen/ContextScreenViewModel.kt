
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
import com.romankozak.forwardappmobile.domain.wifirestapi.FileDataRequest
import com.romankozak.forwardappmobile.domain.wifirestapi.RetrofitClient
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
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
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
            combine(contextIdFlow, uiState.map { it.refreshTrigger }.distinctUntilChanged()) { contextId, _ ->
                contextId
            }
                .flatMapLatest { contextId ->
                    if (contextId.isBlank()) {
                        flowOf(ContextData.Empty)
                    } else {
                        combine(
                            contextRepository.getContextByIdFlow(contextId),
                            listItemRepository.getItemsForContextStream(contextId),
                            contextStructureRepository.observeStructureOnly(contextId),
                            contextLogRepository.getContextLogsStream(contextId),
                            checklistRepository.getChecklistsForContext(contextId),
                            noteDocumentRepository.getDocumentsForContext(contextId),
                            directionRepository.getDirectionItemsForContext(contextId),
                            contextRepository.getAllContextsFlow(),
                            contextRepository.getAttachmentsForContextStream(contextId),
                            listItemRepository.getAllEntitiesAsFlow(),
                            reminderRepository.getRemindersForEntityFlow(contextId),
                            recentItemsRepository.getRecentItemsForContextFlow(contextId),
                            noteRepository.getNotesForContext(contextId),
                            goalRepository.getGoalsByContextIdFlow(contextId),
                            contextRepository.getSubprojectsByParentIdFlow(contextId),
                        ) { args: Array<Any?> ->
                            val context = args[0] as? com.romankozak.forwardappmobile.core.data.models.entities.Context
                            val rawItems = (args[1] as? List<*>)?.filterIsInstance<BacklogItem>() ?: emptyList()
                            val checklists = (args[4] as? List<*>)?.filterIsInstance<ChecklistEntity>() ?: emptyList()
                            val noteDocuments = (args[5] as? List<*>)?.filterIsInstance<NoteDocumentEntity>() ?: emptyList()
                            val directionItems = (args[6] as? List<*>)?.filterIsInstance<DirectionItemEntity>() ?: emptyList()
                            val allContexts = (args[7] as? List<*>)?.filterIsInstance<com.romankozak.forwardappmobile.core.data.models.entities.Context>() ?: emptyList()
                            val attachments = (args[8] as? List<*>)?.filterIsInstance<AttachmentWithContext>() ?: emptyList()
                            val linkItems = (args[9] as? List<*>)?.filterIsInstance<LinkItemEntity>() ?: emptyList()
                            val reminders = (args[10] as? List<*>)?.filterIsInstance<Reminder>() ?: emptyList()
                            val goals = (args[13] as? List<*>)?.filterIsInstance<Goal>() ?: emptyList()
                            val subprojects = (args[14] as? List<*>)?.filterIsInstance<com.romankozak.forwardappmobile.core.data.models.entities.Context>() ?: emptyList()

                            // Явна типізація результату when як BacklogItemContent?
                            val linkItemsMap = linkItems.associateBy { it.id }

                            val items: List<BacklogItemContent> = rawItems.mapNotNull { item ->
                                val itemReminders = reminders.filter { it.entityId == item.entityId }

                                val result: BacklogItemContent? = when (item.itemType) {
                                    "GOAL" -> {
                                        goals.find { it.id == item.entityId }?.let { foundGoal ->
                                            BacklogItemContent.GoalItem(
                                                goal = foundGoal,
                                                backlogItem = item,
                                                reminders = itemReminders,
                                            )
                                        }
                                    }
                                    "PROJECT" -> {
                                        subprojects.find { it.id == item.entityId }?.let { foundSubProject ->
                                            BacklogItemContent.SublistItem(
                                                project = foundSubProject,
                                                backlogItem = item,
                                                reminders = itemReminders,
                                            )
                                        }
                                    }
                                    "NOTE_DOCUMENT" -> {
                                        noteDocuments.find { it.id == item.entityId }?.let { foundDoc ->
                                            BacklogItemContent.NoteDocumentItem(
                                                document = foundDoc,
                                                backlogItem = item,
                                            )
                                        }
                                    }
                                    "CHECKLIST" -> {
                                        checklists.find { it.id == item.entityId }?.let { foundChk ->
                                            BacklogItemContent.ChecklistItem(
                                                checklist = foundChk,
                                                backlogItem = item,
                                            )
                                        }
                                    }
                                    BacklogItemTypeValues.LINK_ITEM -> {
                                        linkItemsMap[item.entityId]?.let { linkItem ->
                                            BacklogItemContent.LinkItem(linkItem, item)
                                        }
                                    }
                                    "LINK" -> null
                                    else -> null
                                }

                                result
                            }

                            val noteDocumentsMap = noteDocuments.associateBy { it.id }
                            val checklistsMap = checklists.associateBy { it.id }
                            val attachmentItems =
                                attachments
                                    .sortedWith { a, b ->
                                        val orderA = a.attachmentOrder ?: -a.attachment.createdAt
                                        val orderB = b.attachmentOrder ?: -b.attachment.createdAt
                                        val orderCompare = orderA.compareTo(orderB)
                                        if (orderCompare != 0) orderCompare else a.attachment.id.compareTo(b.attachment.id)
                                    }
                                    .mapNotNull { attachment ->
                                        val backlogItem =
                                            BacklogItem(
                                                id = attachment.attachment.id,
                                                contextId = contextId,
                                                itemType = attachment.attachment.attachmentType,
                                                entityId = attachment.attachment.entityId,
                                                order = attachment.attachmentOrder ?: -attachment.attachment.createdAt,
                                            )
                                        when (attachment.attachment.attachmentType) {
                                            BacklogItemTypeValues.NOTE_DOCUMENT ->
                                                noteDocumentsMap[attachment.attachment.entityId]?.let { doc ->
                                                    BacklogItemContent.NoteDocumentItem(doc, backlogItem)
                                                }
                                            BacklogItemTypeValues.CHECKLIST ->
                                                checklistsMap[attachment.attachment.entityId]?.let { checklist ->
                                                    BacklogItemContent.ChecklistItem(checklist, backlogItem)
                                                }
                                            BacklogItemTypeValues.LINK_ITEM ->
                                                linkItemsMap[attachment.attachment.entityId]?.let { linkItem ->
                                                    BacklogItemContent.LinkItem(linkItem, backlogItem)
                                                }
                                            else -> null
                                        }
                                    }

                            val config = args[2] as? ContextConfiguration
                            val logs = (args[3] as? List<*>)?.filterIsInstance<ContextLog>() ?: emptyList()
                            val recentItems = (args[11] as? List<*>)?.filterIsInstance<RecentItem>() ?: emptyList()
                            val notes = (args[12] as? List<*>)?.filterIsInstance<LegacyNoteEntity>() ?: emptyList()

                            val linkedContextNames =
                                if (directionItems.isEmpty()) {
                                    emptyMap()
                                } else {
                                    val linkedIds = directionItems.mapNotNull { it.linkedContextId }.toSet()
                                    if (linkedIds.isEmpty()) {
                                        emptyMap()
                                    } else {
                                        val nameById = allContexts.associateBy({ it.id }, { it.name })
                                        linkedIds.associateWith { id -> nameById[id] ?: "Context" }
                                    }
                                }

                            ContextData.Loaded(
                                context = context,
                                items = items,
                                attachmentItems = attachmentItems,
                                config = config ?: ContextConfiguration.default(contextId),
                                logs = logs,
                                checklists = checklists,
                                noteDocuments = noteDocuments,
                                directionItems = directionItems,
                                linkedContextNames = linkedContextNames,
                                reminders = reminders,
                                recentItems = recentItems,
                                notes = notes,
                            )
                        }
                    }
                }
                .debounce(80)
                .collect { data ->
                    when (data) {
                        is ContextData.Loaded -> {
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
                        is ContextData.Empty -> {
                            _listContent.value = emptyList()
                            _attachmentItems.value = emptyList()
                            stateManager.clear()
                            stateManager.updateState { it.copy(isContextSwitching = false) }
                        }
                    }
                }
        }
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
                if (route == "back") {
                    _uiEventFlow.tryEmit(UiEvent.NavigateBack)
                    return@launch
                }
                if (route.startsWith("goal_detail_screen/")) {
                    val contextId = route.substringAfter("goal_detail_screen/")

                    val projectName =
                        withContext(ioDispatcher) {
                            contextRepository.getContextById(contextId)?.name ?: "Context"
                        }
                    enhancedNavigationManager.navigateToProject(contextId, projectName)
                    return@launch
                } else if (route.startsWith(HANDLE_LINK_CLICK_ROUTE)) {
                    val rawTarget = route.substringAfter(HANDLE_LINK_CLICK_ROUTE + "/")
                    val target = runCatching { URLDecoder.decode(rawTarget, "UTF-8") }.getOrDefault(rawTarget)
                    val link =
                        (listContent.value + attachmentItems.value)
                            .filterIsInstance<BacklogItemContent.LinkItem>()
                            .map { it.link.linkData }
                            .find { it.target == target || runCatching { URLEncoder.encode(it.target, "UTF-8") }.getOrNull() == rawTarget }
                    if (link != null) {
                        onLinkItemClick(link)
                    } else {
                        val obsidianNoteTarget = extractObsidianNoteTarget(target)
                        val project =
                            withContext(ioDispatcher) { contextRepository.getContextById(target) }
                        when {
                            obsidianNoteTarget != null -> {
                                openObsidianNote(obsidianNoteTarget)
                            }
                            target.startsWith("obsidian://") -> {
                                _uiEventFlow.tryEmit(UiEvent.OpenUri(target))
                            }
                            project != null -> {
                                enhancedNavigationManager.navigateToProject(project.id, project.name)
                            }
                            target.startsWith("http://") || target.startsWith("https://") -> {
                                _uiEventFlow.tryEmit(UiEvent.OpenUri(target))
                            }
                            else -> {
                                Log.w(TAG, "Unknown related link target: $target")
                                _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Unknown link: $target"))
                            }
                        }
                    }
                } else {
                    val target = parseRouteToNavTarget(route)
                    if (target != null) {
                        _uiEventFlow.tryEmit(UiEvent.Navigate(target))
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
                    val paramMap =
                        params.split("&").mapNotNull {
                            val parts = it.split("=", limit = 2)
                            if (parts.size == 2) parts[0] to parts[1] else null
                        }.toMap()
                    NavTarget.NoteDocumentEdit(
                        contextId =
                            paramMap["projectId"]?.takeIf { it.isNotBlank() }
                                ?: paramMap["contextId"]?.takeIf { it.isNotBlank() },
                        documentId = paramMap["documentId"]?.takeIf { it.isNotBlank() },
                    )
                }
                route.startsWith("checklist_screen") -> {
                    val params = route.substringAfter("?", "")
                    val paramMap =
                        params.split("&").mapNotNull {
                            val parts = it.split("=", limit = 2)
                            if (parts.size == 2) parts[0] to parts[1] else null
                        }.toMap()
                    NavTarget.Checklist(
                        id = paramMap["checklistId"]?.takeIf { it.isNotBlank() },
                        contextId =
                            paramMap["projectId"]?.takeIf { it.isNotBlank() }
                                ?: paramMap["contextId"]?.takeIf { it.isNotBlank() },
                    )
                }
                route.startsWith("list_chooser_screen/") -> {
                    val titleEncoded = route.substringAfter("list_chooser_screen/").substringBefore("?")
                    val params = route.substringAfter("?", "")
                    val paramMap =
                        params.split("&").mapNotNull {
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

        private fun extractObsidianNoteTarget(target: String): String? {
            val trimmed = target.trim()
            if (trimmed.startsWith("[[") && trimmed.endsWith("]]") && trimmed.length > 4) {
                return trimmed.substring(2, trimmed.length - 2).trim().takeIf { it.isNotBlank() }
            }
            if (!trimmed.startsWith("obsidian://")) {
                return null
            }
            val encodedFile =
                Regex("""[?&]file=([^&]+)""").find(trimmed)?.groupValues?.get(1)
                    ?: Regex("""[?&]name=([^&]+)""").find(trimmed)?.groupValues?.get(1)
            return encodedFile
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        }

        private suspend fun openObsidianNote(noteTarget: String) {
            val vaultName = settingsRepository.obsidianVaultNameFlow.first()
            if (vaultName.isBlank()) {
                _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Назву Obsidian сховища не встановлено."))
                return
            }
            val encodedVault = URLEncoder.encode(vaultName, "UTF-8")
            val encodedNoteName = URLEncoder.encode(noteTarget, "UTF-8")
            val uri = "obsidian://open?vault=$encodedVault&file=$encodedNoteName"
            _uiEventFlow.tryEmit(UiEvent.OpenUri(uri))
        }

        fun onLinkItemClick(link: RelatedLink) {
            Log.d(TAG, "onLinkItemClick: Clicked link with type=${link.type}, target=${link.target}")
            viewModelScope.launch {
                when (link.type) {
                    LinkType.CONTEXT -> {
                        val projectName = link.displayName ?: "Context"
                        enhancedNavigationManager.navigateToProject(link.target, projectName)
                    }
                    LinkType.OBSIDIAN -> {
                        recentItemsRepository.logObsidianLinkAccess(link)
                        val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                        if (vaultName.isNotBlank()) {
                            val encodedVault = URLEncoder.encode(vaultName, "UTF-8")
                            val encodedNoteName = URLEncoder.encode(link.target, "UTF-8")
                            val uri = "obsidian://open?vault=$encodedVault&file=$encodedNoteName"
                            _uiEventFlow.tryEmit(UiEvent.OpenUri(uri))
                        } else {
                            _uiEventFlow.tryEmit(UiEvent.ShowSnackbar("Obsidian vault name is not configured."))
                        }
                    }
                    else -> {
                        _uiEventFlow.tryEmit(UiEvent.HandleLinkClick(link))
                    }
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
            backlogMarkdownHandler.exportToMarkdown(listContent.value)

            showSnackbar("Беклог скопійовано")

            onDismissShareDialog()
        }

// ... (rest of the code)

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

        fun onExportBacklogToMarkdownRequest() = onExportBacklogToMarkdown()

        fun onSetReminderForProject() {
            viewModelScope.launch {
                project.value?.let { proj ->
                    val reminders = reminderRepository.getRemindersForEntityFlow(proj.id).firstOrNull()
                    val record =
                        ActivityRecord(
                            id = proj.id,
                            text = proj.name,
                            reminderTime = reminders?.firstOrNull()?.reminderTime,
                            createdAt = proj.createdAt,
                            contextId = proj.id,
                            goalId = null,
                        )
                    stateManager.updateState { it.copy(recordForReminderDialog = record) }
                }
            }
        }

        fun onImportFromMarkdownRequest() {
            stateManager.updateState { it.copy(showImportFromMarkdownDialog = true) }
        }

        fun onImportFromMarkdownDismiss() {
            stateManager.updateState { it.copy(showImportFromMarkdownDialog = false) }
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
            val currentcontextId = contextIdFlow.value
            if (currentcontextId.isBlank()) {
                showSnackbar("Неможливо додати, проект не визначено", null)
                return
            }

            viewModelScope.launch {
                val today = System.currentTimeMillis()
                val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
                dayManagementRepository.addProjectToDayPlan(dayPlan.id, currentcontextId)
                showSnackbar("Проект додано до плану на сьогодні", null)
            }
        }

        fun onCloseSearch() {
            stateManager.updateState { it.copy(localSearchQuery = "") }
        }

        fun onAddMilestone(text: String) = addMilestone(text)

        fun onShowCreateNoteDocumentDialog() {
            val contextId = contextIdFlow.value
            if (contextId.isNotBlank()) {
                viewModelScope.launch {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = "Нова нотатка",
                            contextId = contextId,
                            content = "",
                        )
                    _uiEventFlow.tryEmit(
                        UiEvent.Navigate(
                            NavTarget.NoteDocument(id = documentId, startEdit = true),
                        ),
                    )
                }
            } else {
                showSnackbar("Не вдалося визначити проект для створення документа", null)
            }
        }

        fun onCreateChecklist() {
            val contextId = contextIdFlow.value
            if (contextId.isNotBlank()) {
                viewModelScope.launch {
                    _uiEventFlow.tryEmit(
                        UiEvent.Navigate(
                            NavTarget.Checklist(contextId = contextId),
                        ),
                    )
                }
            } else {
                showSnackbar("Не вдалося визначити проект для створення чекліста", null)
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
                val currentList = _listContent.value.toMutableList()
                if (from !in currentList.indices || to !in currentList.indices) return@launch
                val movedItem = currentList.removeAt(from)
                currentList.add(to, movedItem)
                _listContent.value = currentList
                val reorderedBacklogItems =
                    currentList.mapIndexed { index, content ->
                        content.backlogItem.copy(order = index.toLong())
                    }
                listItemRepository.updateListItemsOrder(reorderedBacklogItems)
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
            val trimmed = text.trim()
            if (trimmed.isBlank()) {
                showSnackbar("Напрямок не може бути порожнім.", null)
                return
            }
            viewModelScope.launch(ioDispatcher) {
                directionRepository.updateDirectionItem(item.copy(text = trimmed))
            }
        }

        fun deleteDirectionItem(itemId: String) {
            viewModelScope.launch(ioDispatcher) {
                directionRepository.deleteDirectionItem(itemId)
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
            val item = uiState.value.directionItems.firstOrNull { it.id == itemId } ?: return
            viewModelScope.launch(ioDispatcher) {
                directionRepository.updateDirectionItem(item.copy(linkedContextId = linkedContextId))
            }
        }

        fun openLinkedContext(contextId: String) {
            val currentId = contextIdFlow.value
            if (contextId.isBlank() || contextId == currentId) {
                showSnackbar("Це поточний контекст.", null)
                return
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
            val current = uiState.value.directionItems
            if (from !in current.indices || to !in current.indices || from == to) return

            val mutable = current.toMutableList()
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)

            val reordered =
                mutable.mapIndexed { index, item ->
                    val newOrder = index + 1
                    if (item.itemOrder != newOrder) item.copy(itemOrder = newOrder) else item
                }

            stateManager.updateState { it.copy(directionItems = reordered) }
            viewModelScope.launch(ioDispatcher) {
                directionRepository.updateAll(reordered)
            }
        }

        fun onSubprojectCompletedChanged(
            subproject: Context,
            completed: Boolean,
        ) {
            viewModelScope.launch {
                contextRepository.updateContext(subproject.copy(isCompleted = completed))
                forceRefresh()
            }
        }

        fun onDeleteEverywhere(item: BacklogItemContent) {
            viewModelScope.launch {
                if (item is BacklogItemContent.GoalItem) {
                    goalRepository.deleteGoal(item.goal.id)
                }
                // TODO: Handle other item types
            }
        }

        fun onMoveToTop(item: BacklogItemContent) {
            viewModelScope.launch {
                val currentList = _listContent.value.toMutableList()
                val from = currentList.indexOf(item)
                if (from != -1) {
                    val movedItem = currentList.removeAt(from)
                    currentList.add(0, movedItem)
                    _listContent.value = currentList
                    // TODO: save order to repository
                }
            }
        }

        fun addItemToDailyPlan(item: BacklogItemContent) {
            viewModelScope.launch {
                val day = dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
                when (item) {
                    is BacklogItemContent.GoalItem -> dayManagementRepository.addGoalToDayPlan(day.id, item.goal.id)
                    is BacklogItemContent.SublistItem -> dayManagementRepository.addProjectToDayPlan(day.id, item.project.id)
                    else -> {
                        // TODO: show snackbar
                    }
                }
            }
        }

        fun onStartTrackingRequest(item: BacklogItemContent) {
            viewModelScope.launch {
                when (item) {
                    is BacklogItemContent.GoalItem -> activityRepository.startGoalActivity(item.goal.id)
                    is BacklogItemContent.SublistItem -> activityRepository.startContextActivity(item.project.id)
                    else -> {
                        // TODO: show snackbar
                    }
                }
            }
        }

        fun onProjectStatusUpdate(
            newStatus: String,
            statusText: String?,
        ) {
            viewModelScope.launch {
                contextRepository.updateContextStatus(contextIdFlow.value, newStatus, statusText)
            }
        }

        fun onRecalculateTime() {
            viewModelScope.launch {
                val metrics = contextTimeTrackingRepository.calculateContextTimeMetrics(contextIdFlow.value)
                stateManager.updateState { it.copy(contextTimeMetrics = metrics) }
                contextRepository.recalculateAndLogContextTime(contextIdFlow.value)
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
                addDirectionLinkedToContext(targetcontextId)
                return
            }

            pendingAttachmentShare?.let { attachment ->
                pendingAttachmentShare = null
                shareAttachmentToProject(attachment, targetcontextId)
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
                when (actionType) {
                    GoalActionType.CreateInstance -> goalRepository.createGoalLinks(goalIds, targetcontextId)

                    GoalActionType.MoveInstance -> listItemRepository.moveListItemsToContext(itemIds, targetcontextId)
                    GoalActionType.CopyGoal -> goalRepository.copyGoalsToContext(goalIds, targetcontextId)
                    GoalActionType.AddLinkToList -> {
                        val targetProject = contextRepository.getContextById(targetcontextId)
                        val link =
                            RelatedLink(
                                type = LinkType.CONTEXT,
                                target = targetcontextId,
                                displayName = targetProject?.name ?: "Untitled context",
                            )
                        val newItemId = contextRepository.addLinkItemToContextFromLink(contextIdFlow.value, link)
                        withContext(Dispatchers.Main) {
                            stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                        }
                    }

                    GoalActionType.ADD_LIST_SHORTCUT -> {
                        if (goalIds.isNotEmpty()) {
                            val subprojectToLinkId = goalIds.first()
                            val newItemId =
                                listItemRepository.addContextLinkToContext(subprojectToLinkId, targetcontextId)
                            withContext(Dispatchers.Main) {
                                stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
                            }
                        } else {
                            val newItemId =
                                listItemRepository.addContextLinkToContext(targetcontextId, contextIdFlow.value)
                            withContext(Dispatchers.Main) {
                                stateManager.updateState { it.copy(newlyAddedItemId = newItemId) }
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

        private fun addDirectionLinkedToContext(targetcontextId: String) {
            val linkedContextId = targetcontextId.takeIf { it.isNotBlank() && it != "root" }
            if (linkedContextId == null) {
                showSnackbar("Оберіть контекст для напрямку.", null)
                return
            }
            viewModelScope.launch(ioDispatcher) {
                val linkedContextName =
                    contextRepository.getContextById(linkedContextId)?.name?.takeIf { it.isNotBlank() }
                        ?: "Context"
                directionRepository.addDirectionItem(
                    contextId = contextIdFlow.value,
                    text = linkedContextName,
                    linkedContextId = linkedContextId,
                )
            }
        }

        private fun shareAttachmentToProject(
            attachment: BacklogItemContent,
            targetcontextId: String,
        ) {
            viewModelScope.launch(ioDispatcher) { // Use ioDispatcher for repository operations
                val isAttachmentSupported =
                    attachment is BacklogItemContent.LinkItem ||
                        attachment is BacklogItemContent.NoteDocumentItem ||
                        attachment is BacklogItemContent.ChecklistItem
                if (!isAttachmentSupported) {
                    withContext(Dispatchers.Main) {
                        showSnackbar("This attachment type does not support copying", null)
                    }
                    return@launch
                }

                val itemType = attachment.backlogItem.itemType
                val entityId = attachment.backlogItem.entityId

                if (itemType == null || entityId == null) {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Cannot share corrupt attachment", null)
                    }
                    return@launch
                }

                val attachmentId =
                    try {
                        contextRepository.ensureAttachmentLinkedToContext(
                            attachmentType = itemType,
                            entityId = entityId,
                            targetContextId = targetcontextId,
                            ownerContextId = attachment.backlogItem.contextId.takeIf { it.isNotBlank() } ?: contextIdFlow.value,
                        )
                        attachment.backlogItem.entityId
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to link attachment", e)
                        null
                    }

                withContext(Dispatchers.Main) {
                    if (targetcontextId == contextIdFlow.value && attachmentId != null) {
                        stateManager.updateState { it.copy(newlyAddedItemId = attachmentId) }
                        forceRefresh()
                    }
                    showSnackbar("Attachment added to selected context", null)
                }
            }
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
                when (item) {
                    is BacklogItemContent.GoalItem -> {
                        val entityId = item.goal.id
                        val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                        val record =
                            ActivityRecord(
                                id = entityId,
                                text = item.goal.text,
                                reminderTime = reminders.firstOrNull()?.reminderTime,
                                createdAt = item.goal.createdAt,
                                contextId = item.backlogItem.contextId,
                                goalId = item.goal.id,
                            )
                        stateManager.updateState { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
                    }
                    is BacklogItemContent.SublistItem -> {
                        val entityId = item.project.id
                        val reminders = reminderRepository.getRemindersForEntityFlow(entityId).firstOrNull().orEmpty()
                        val record =
                            ActivityRecord(
                                id = entityId,
                                text = item.project.name,
                                reminderTime = reminders.firstOrNull()?.reminderTime,
                                createdAt = item.project.createdAt,
                                contextId = item.project.id,
                                goalId = null,
                            )
                        stateManager.updateState { it.copy(recordForReminderDialog = record, remindersForDialog = reminders) }
                    }
                    else -> return@launch
                }
            }
        }

        fun onOpenRemindersDialog(itemContent: BacklogItemContent) {
            viewModelScope.launch {
                stateManager.updateState { it.copy(showRemindersDialog = true, itemForRemindersDialog = itemContent) }
            }
        }

        fun onDismissRemindersDialog() {
            stateManager.updateState {
                it.copy(
                    recordForReminderDialog = null,
                    remindersForDialog = emptyList(),
                    showRemindersDialog = false,
                    itemForRemindersDialog = null,
                )
            }
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
                val record = uiState.value.recordForReminderDialog ?: return@launch

                val entityId = record.goalId ?: record.contextId ?: record.id
                reminderRepository.clearRemindersForEntity(entityId)

                onDismissRemindersDialog()
                showSnackbar("Нагадування скасовано", null)
                forceRefresh()
            }

        fun onSetReminder(timestamp: Long) =
            viewModelScope.launch {
                val record = uiState.value.recordForReminderDialog ?: return@launch

                val entityType =
                    when {
                        record.goalId != null -> "GOAL"
                        record.contextId != null -> "PROJECT"
                        else -> "TASK" // Assuming ActivityRecord can also be a task
                    }
                val entityId = record.goalId ?: record.contextId ?: record.id

                reminderRepository.createReminder(entityId, entityType, timestamp)

                showSnackbar(
                    "Нагадування додано на ${
                        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(
                            Date(timestamp),
                        )
                    }",
                    null,
                )
                forceRefresh()
            }

            fun onRemoveReminder(reminderId: String) =
                viewModelScope.launch {
                    val record = uiState.value.recordForReminderDialog ?: return@launch
        
                    reminderRepository.clearRemindersForEntity(record.id)
        
                    val refreshed = reminderRepository.getRemindersForEntityFlow(record.id).firstOrNull().orEmpty()
                    val updatedRecord = record.copy(reminderTime = refreshed.firstOrNull()?.reminderTime)
        
                    stateManager.updateState { it.copy(remindersForDialog = refreshed, recordForReminderDialog = updatedRecord) }
        
                    showSnackbar("Нагадування видалено", null)
                    forceRefresh()
                }
    fun getBacklogAsMarkdown(): String {
        val markdownBuilder = StringBuilder()
        listContent.value.forEach { item ->
            val line =
                when (item) {
                    is BacklogItemContent.GoalItem -> {
                        val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                        "$checkbox ${item.goal.text}"
                    }
                    is BacklogItemContent.SublistItem -> "- [С] ${item.project.name}"
                    is BacklogItemContent.LinkItem -> {
                        val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                        "- [Л] [$displayName](${item.link.linkData.target})"
                    }
                    is BacklogItemContent.NoteItem -> "- [Н] ${item.note.title}"
                    is BacklogItemContent.NoteDocumentItem -> "- [К] ${item.document.name}"
                    is BacklogItemContent.ChecklistItem -> "- [Ч] ${item.checklist.name}"
                }
            markdownBuilder.appendLine(line)
        }
        return markdownBuilder.toString()
    }

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
        inboxMarkdownHandler.exportToMarkdown(inboxHandler.inboxRecords.value)
    }

    // File: ContextScreenViewModel.kt

    fun onImportBacklogFromMarkdownRequest() {
        // Звертаємося до МЕНЕДЖЕРА, а не до самого uiState
        stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = true) }
    }

    fun onImportBacklogFromMarkdownDismiss() {
        stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = false) }
    }

    fun onImportBacklogFromMarkdownConfirm(markdownText: String) {
        backlogMarkdownHandler.importFromMarkdown(markdownText, contextIdFlow.value)
        onImportBacklogFromMarkdownDismiss()
    }

    fun onReminderDialogDismiss() {
        // Використовуємо централізований менеджер для оновлення стану
        stateManager.updateState { currentState ->
            currentState.copy(
                recordForReminderDialog = null,
                remindersForDialog = emptyList(),
                showRemindersDialog = false,
                itemForRemindersDialog = null
            )
        }
    }

    fun onImportFromMarkdownConfirm(markdownText: String) {
        inboxMarkdownHandler.importFromMarkdown(markdownText, contextIdFlow.value)
        onImportFromMarkdownDismiss()
    }

    fun copyInboxRecordText(text: String) {
        copyToClipboard(text, "Inbox Record")
    }

    }
