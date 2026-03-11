package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.routes.COMMAND_DECK_ROUTE
import com.romankozak.forwardappmobile.core.theme.ThemeSettings
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation.RevealResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextActionsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.DialogUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyDebugLogger
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.NavigationUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.PlanningUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ProjectHierarchyScreenStateUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SettingsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ThemingUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.flattenHierarchy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContextHierarchyScreenViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepo: SettingsRepository,
        private val searchUseCase: SearchUseCase,
        private val dialogUseCase: DialogUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val contextHandler: ContextHandler,
        private val dayManagementRepository: DayManagementRepository,
        private val focusContextRepository: FocusContextRepository,
        private val activityRepository: ActivityRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val noteRepository: LegacyNoteRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
        private val planningUseCase: PlanningUseCase,
        private val syncUseCase: SyncUseCase,
        private val contextActionsUseCase: ContextActionsUseCase,
        private val navigationUseCase: NavigationUseCase,
        private val themingUseCase: ThemingUseCase,
        private val settingsUseCase: SettingsUseCase,
        private val projectHierarchyScreenStateUseCase: ProjectHierarchyScreenStateUseCase,
    ) : ViewModel() {
        companion object {
            private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
            private const val TAG = "ProjectHierarchyScreenVM_DEBUG"
        }

        private sealed class PendingChooserAction {
            data object MoveProject : PendingChooserAction()
        }

        private enum class ContextClipboardOperation {
            COPY,
            CUT,
        }

        private data class ContextClipboardPayload(
            val contextId: String,
            val operation: ContextClipboardOperation,
        )

        var enhancedNavigationManager: EnhancedNavigationManager? = null
            set(value) {
                if (field === value) return

                field?.let {
                    navigationStateJob?.cancel()
                    navigationResultJob?.cancel()
                    navigationSnapshot.value = ProjectHierarchyScreenStateUseCase.NavigationSnapshot()
                    navigationUseCase.detach()
                }

                field = value
                if (value != null) {
                    navigationUseCase.attach(
                        enhancedNavigationManager = value,
                        uiEventChannel = _uiEventChannel,
                        allProjectsFlat = _allProjectsFlat,
                    )
                    observeNavigationManager(value)
                    initializeNavigationResultHandling(value)
                }
            }

        private val navigationSnapshot =
            MutableStateFlow(ProjectHierarchyScreenStateUseCase.NavigationSnapshot())

        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap
        val focusedContextIds: StateFlow<Set<String>> =
            focusContextRepository
                .observeActiveFocusContextIds()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptySet(),
                )

        val uiState: StateFlow<ProjectHierarchyScreenUiState>
            get() = projectHierarchyScreenStateUseCase.uiState

        val lastOngoingActivity: StateFlow<ActivityRecord?> =
            activityRepository
                .getLogStream()
                .map { log ->
                    log.firstOrNull { it.startTime != null && it.endTime == null }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val themeSettings: StateFlow<ThemeSettings> =
            themingUseCase.themeSettings
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ThemeSettings(),
                )

        private val _uiEventChannel = Channel<ProjectUiEvent>()
        val uiEventFlow = _uiEventChannel.receiveAsFlow()

        private val allProjectsFlow = contextRepository.getAllContextsFlow()

        private val _allProjectsFlat =
            allProjectsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private val _showRecentListsSheet = MutableStateFlow(false)
        private val _isBottomNavExpanded = MutableStateFlow(false)
        private val _showSearchDialog = MutableStateFlow(false)
        private val contextClipboard = MutableStateFlow<ContextClipboardPayload?>(null)
        private val pendingChooserAction = MutableStateFlow<PendingChooserAction?>(null)
        private val projectBeingMovedId =
            savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

        init {
            searchUseCase.initialize(
                scope = viewModelScope,
                uiEventChannel = _uiEventChannel,
                allProjectsFlat = _allProjectsFlat,
            )
            planningUseCase.initialize(
                scope = viewModelScope,
                allProjectsFlat = _allProjectsFlat,
            )
            syncUseCase.initialize(
                scope = viewModelScope,
                application = application,
                uiEventChannel = _uiEventChannel,
            )
            projectHierarchyScreenStateUseCase.initialize(
                scope = viewModelScope,
                allProjectsFlat = _allProjectsFlat,
                showRecentListsSheet = _showRecentListsSheet,
                isBottomNavExpanded = _isBottomNavExpanded,
                showSearchDialog = _showSearchDialog,
                navigationSnapshot = navigationSnapshot,
            )
            initializeAndCollectStates()
            viewModelScope.launch {
                _allProjectsFlat.collect { projects ->
                    HierarchyDebugLogger.d { "_allProjectsFlat size=${projects.size}" }
                    withContext(ioDispatcher) {
                        recentItemsRepository.syncProjectRecentItemsWithContexts(projects)
                    }
                }
            }
            viewModelScope.launch {
                planningUseCase.filterStateFlow.collect { state ->
                    HierarchyDebugLogger.d {
                        "filterStateFlow flat=${state.flatList.size} ready=${state.isReady}"
                    }
                }
            }
            viewModelScope.launch {
                projectHierarchyScreenStateUseCase.projectHierarchy.collect { hierarchy ->
                    HierarchyDebugLogger.d {
                        "projectHierarchy flow emit: topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}"
                    }
                }
            }
            viewModelScope.launch {
                delay(1500)
                val filterState = planningUseCase.filterStateFlow.first()
                val filterSize = filterState.flatList.size
                HierarchyDebugLogger.d { "Delayed check: filterState flat=$filterSize" }
                HierarchyDebugLogger.d {
                    "Delayed check: hierarchyState topLevel=${projectHierarchyScreenStateUseCase.projectHierarchy.value.topLevelProjects.size}"
                }
            }
        }

        private var projectToRevealAndScroll: String? = null
        private var navigationStateJob: Job? = null
        private var navigationResultJob: Job? = null

        private fun onContextSelected(name: String) {
            viewModelScope.launch {
                val tag = contextHandler.getContextTag(name)
                val projectId =
                    if (tag != null) {
                        withContext(ioDispatcher) { contextRepository.findContextIdsByTag(tag).firstOrNull() }
                    } else {
                        null
                    }

                if (projectId != null) {
                    onNavigateToProject(projectId)
                } else {
                    val query = tag?.let { if (it.startsWith("#")) it else "#$it" } ?: name
                    searchUseCase.onSearchQueryChanged(TextFieldValue(query))
                    searchUseCase.onToggleSearch(true)
                }
            }
        }

        private suspend fun revealProject(projectId: String) {
            Log.d("ProjectRevealDebug", "Attempting to reveal projectId: $projectId")
            planningUseCase.onPlanningModeChange(PlanningMode.All)

            when (val result = searchUseCase.revealProjectInHierarchy(projectId)) {
                is RevealResult.Success -> {
                    Log.d(
                        "ProjectRevealDebug",
                        "revealProjectInHierarchy result: Success, shouldFocus=${result.shouldFocus}",
                    )
                    searchUseCase.pushSubState(ProjectHierarchyScreenSubState.ProjectFocused(result.projectId))
                    Log.d("ProjectRevealDebug", "Calling navigateToProject for ${result.projectId}")
                    searchUseCase.navigateToProject(
                        result.projectId,
                        uiState.value.projectHierarchy,
                    )
                }
                is RevealResult.Failure -> {
                    Log.d("ProjectRevealDebug", "revealProjectInHierarchy result: Failure")
                    _uiEventChannel.send(ProjectUiEvent.ShowToast("Не удалось показать локацию"))
                }
            }
        }

        private fun observeNavigationManager(navManager: EnhancedNavigationManager) {
            navigationSnapshot.update {
                it.copy(
                    canGoBack = navManager.canGoBack.value,
                    canGoForward = navManager.canGoForward.value,
                    showNavigationMenu = navManager.showNavigationMenu.value,
                )
            }
            navigationStateJob?.cancel()
            navigationStateJob =
                viewModelScope.launch {
                    launch {
                        navManager.canGoBack.collect { value ->
                            navigationSnapshot.update { snapshot -> snapshot.copy(canGoBack = value) }
                        }
                    }
                    launch {
                        navManager.canGoForward.collect { value ->
                            navigationSnapshot.update { snapshot -> snapshot.copy(canGoForward = value) }
                        }
                    }
                    launch {
                        navManager.showNavigationMenu.collect { value ->
                            navigationSnapshot.update { snapshot -> snapshot.copy(showNavigationMenu = value) }
                        }
                    }
                }
        }

        private fun initializeNavigationResultHandling(navManager: EnhancedNavigationManager) {
            navigationResultJob?.cancel()
            navigationResultJob =
                viewModelScope.launch {
                    navManager.navigationResults.collect { result ->
                        searchUseCase.handleNavigationResult(
                            result.key,
                            result.value,
                            uiState.value.projectHierarchy,
                        ) {
                            projectToRevealAndScroll = it
                        }
                    }
                }
        }

        private fun initializeAndCollectStates() {
            viewModelScope.launch(ioDispatcher) {
                contextHandler.initialize()
                settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                    _isBottomNavExpanded.value = savedState
                }
            }

            viewModelScope.launch {
                combine(
                    projectHierarchyScreenStateUseCase.projectHierarchy,
                    planningUseCase.filterStateFlow,
                    navigationUseCase.isProcessingReveal,
                ) { hierarchy, filterState, processingReveal ->
                    val projectId = projectToRevealAndScroll
                    if (projectId != null && !filterState.searchActive && !processingReveal) {
                        projectToRevealAndScroll = null
                        hierarchy to projectId
                    } else {
                        null
                    }
                }
                    .filterNotNull()
                    .collect { (hierarchy, projectId) ->
                        val displayedProjects =
                            flattenHierarchy(
                                hierarchy.topLevelProjects,
                                hierarchy.childMap,
                            )
                        val index = displayedProjects.indexOfFirst { it.id == projectId }
                        if (index != -1) {
                            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
                        }
                    }
            }
        }

        fun onEvent(event: ContextHierarchyScreenEvent) {
            when (event) {
                is ContextHierarchyScreenEvent.SearchQueryChanged -> searchUseCase.onSearchQueryChanged(event.query)
                is ContextHierarchyScreenEvent.SearchFromHistory -> searchUseCase.onSearchQueryFromHistory(event.query)
                is ContextHierarchyScreenEvent.RemoveSearchHistoryEntry -> searchUseCase.removeSearchHistoryEntry(event.query)
                is ContextHierarchyScreenEvent.ClearSearchHistory -> searchUseCase.clearSearchHistory()
                is ContextHierarchyScreenEvent.SearchFilterChanged -> searchUseCase.onSearchFilterChanged(event.filter)
                is ContextHierarchyScreenEvent.SearchSortChanged -> searchUseCase.onSearchSortChanged(event.sort)
                is ContextHierarchyScreenEvent.GlobalSearchPerform -> searchUseCase.onPerformGlobalSearch(event.query)
                is ContextHierarchyScreenEvent.SearchResultClick ->
                    searchUseCase.onSearchResultClick(
                        event.projectId,
                        uiState.value.projectHierarchy,
                    )

                is ContextHierarchyScreenEvent.ContextClick -> onProjectClicked(event.projectId)
                is ContextHierarchyScreenEvent.ContextMenuRequest -> {
                    val canPaste = canPasteContextInto(event.project.id)
                    dialogUseCase.onMenuRequested(event.project, canPaste)
                }
                is ContextHierarchyScreenEvent.ToggleContextExpanded -> onToggleExpanded(event.project)
                is ContextHierarchyScreenEvent.ContextReorder -> {
                    viewModelScope.launch {
                        contextActionsUseCase.onProjectReorder(
                            fromId = event.fromId,
                            toId = event.toId,
                            position = event.position,
                            isSearchActive = searchUseCase.isSearchActive(),
                            allProjects = _allProjectsFlat.value,
                        )
                    }
                }

                is ContextHierarchyScreenEvent.BreadcrumbNavigation -> searchUseCase.navigateToBreadcrumb(event.breadcrumb)
                is ContextHierarchyScreenEvent.ClearBreadcrumbNavigation -> searchUseCase.clearNavigation()

                is ContextHierarchyScreenEvent.PlanningModeChange -> {
                    if (uiState.value.featureToggles[FeatureFlag.PlanningModes] == true) {
                        planningUseCase.onPlanningModeChange(event.mode)
                    }
                }

                is ContextHierarchyScreenEvent.DismissDialog -> dialogUseCase.dismissDialog()
                is ContextHierarchyScreenEvent.AddNewContextRequest -> {
                    val focusedState = uiState.value.currentSubState as? ProjectHierarchyScreenSubState.ProjectFocused
                    if (focusedState != null) {
                        val parentProject = _allProjectsFlat.value.find { it.id == focusedState.projectId }
                        if (parentProject != null) {
                            dialogUseCase.onAddSubprojectRequest(parentProject)
                        } else {
                            dialogUseCase.onAddNewProjectRequest()
                        }
                    } else {
                        dialogUseCase.onAddNewProjectRequest()
                    }
                }
                is ContextHierarchyScreenEvent.AddNoteDocumentRequest -> createNoteInInbox()
                is ContextHierarchyScreenEvent.AddChecklistRequest -> {
                    createChecklistInInbox()
                }
                is ContextHierarchyScreenEvent.AddNoteDocumentToContextRequest -> {
                    dialogUseCase.dismissDialog()
                    createNoteInContext(event.project.id)
                }
                is ContextHierarchyScreenEvent.AddChecklistToContextRequest -> {
                    dialogUseCase.dismissDialog()
                    createChecklistInContext(event.project.id)
                }
                is ContextHierarchyScreenEvent.ListChooserResult -> {
                    val targetProjectId = event.projectId?.takeUnless { it.isBlank() || it == "root" }
                    when (val action = pendingChooserAction.value) {
                        PendingChooserAction.MoveProject -> handleMoveConfirm(event.projectId)
                        null -> {
                            handleMoveConfirm(event.projectId)
                        }
                    }
                    pendingChooserAction.value = null
                }
                is ContextHierarchyScreenEvent.AddSubprojectRequest ->
                    dialogUseCase.onAddSubprojectRequest(event.parentProject)
                is ContextHierarchyScreenEvent.DeleteRequest -> dialogUseCase.onDeleteRequest(event.project)
                is ContextHierarchyScreenEvent.MoveRequest -> {
                    viewModelScope.launch {
                        val target = contextActionsUseCase.getMoveProjectRoute(event.project, _allProjectsFlat.value)
                        savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = event.project.id
                        pendingChooserAction.value = PendingChooserAction.MoveProject
                        dialogUseCase.dismissDialog()
                        _uiEventChannel.send(ProjectUiEvent.Navigate(target))
                    }
                }
                is ContextHierarchyScreenEvent.DeleteConfirm -> {
                    viewModelScope.launch {
                        contextActionsUseCase.onDeleteProjectConfirmed(
                            event.project,
                            uiState.value.projectHierarchy.childMap,
                        )
                        dialogUseCase.dismissDialog()
                    }
                }
                is ContextHierarchyScreenEvent.MoveConfirm -> {
                    handleMoveConfirm(event.newParentId)
                }
                is ContextHierarchyScreenEvent.FullImportConfirm -> {
                    viewModelScope.launch {
                        val result = contextActionsUseCase.onFullImportConfirmed(event.uri)
                        dialogUseCase.dismissDialog()
                        Timber.tag("IMPORT_DEBUG").e("Import error: ${result.exceptionOrNull()?.message}")
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import successful")
                            } else {
                                ProjectUiEvent.ShowToast("Import error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.FullImportConfirmV2 -> {
                    viewModelScope.launch {
                        val result = contextActionsUseCase.onFullImportConfirmedV2(event.uri)
                        dialogUseCase.dismissDialog()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import V2 successful")
                            } else {
                                ProjectUiEvent.ShowToast("Import V2 error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ShowAboutDialog -> dialogUseCase.onShowAboutDialog()
                is ContextHierarchyScreenEvent.ImportFromFileRequest ->
                    dialogUseCase.onImportFromFileRequested(event.uri)

                is ContextHierarchyScreenEvent.SelectiveImportFromFileRequest -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.ImportExport(uri = event.uri.toString()),
                            ),
                        )
                    }
                }

                is ContextHierarchyScreenEvent.HomeClick -> onHomeClicked()
                is ContextHierarchyScreenEvent.BackClick -> handleBackNavigation()
                is ContextHierarchyScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
                is ContextHierarchyScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
                is ContextHierarchyScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()

                is ContextHierarchyScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
                is ContextHierarchyScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
                is ContextHierarchyScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
                is ContextHierarchyScreenEvent.RecentItemSelected -> onRecentItemSelected(event.item)
                is ContextHierarchyScreenEvent.RecentItemPinClick -> toggleRecentItemPin(event.item)
                is ContextHierarchyScreenEvent.DayPlanClick -> onDayPlanClicked()
                is ContextHierarchyScreenEvent.ContextSelected -> onContextSelected(event.name)
                is ContextHierarchyScreenEvent.CommandDeckClick -> {
                    enhancedNavigationManager?.navigate(
                        route = COMMAND_DECK_ROUTE,
                        builder = {
                            popUpTo(COMMAND_DECK_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        },
                    )
                }

                is ContextHierarchyScreenEvent.EditRequest -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(event.project.id))
                    }
                }
                is ContextHierarchyScreenEvent.AddToDayPlanRequest -> {
                    viewModelScope.launch {
                        val today = System.currentTimeMillis()
                        val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
                        dayManagementRepository.addProjectToDayPlan(dayPlan.id, event.project.id)
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект додано до плану дня"))
                    }
                }
                is ContextHierarchyScreenEvent.SetReminderRequest -> {
                    dialogUseCase.onSetReminderForProject(viewModelScope, event.project)
                }
                is ContextHierarchyScreenEvent.FocusContext -> {
                    viewModelScope.launch {
                        searchUseCase.navigateToProject(event.project.id, uiState.value.projectHierarchy)
                        searchUseCase.pushSubState(ProjectHierarchyScreenSubState.ProjectFocused(event.project.id))
                        dialogUseCase.dismissDialog()
                    }
                }
                is ContextHierarchyScreenEvent.ToggleUserFocusContext -> {
                    viewModelScope.launch {
                        val focused = focusContextRepository.toggleFocusContext(event.project.id)
                        _uiEventChannel.send(
                            ProjectUiEvent.ShowToast(
                                if (focused) {
                                    "Контекст додано у фокус"
                                } else {
                                    "Контекст прибрано з фокусу"
                                },
                            ),
                        )
                        dialogUseCase.dismissDialog()
                    }
                }
                is ContextHierarchyScreenEvent.CopyContextLink -> {
                    contextClipboard.value =
                        ContextClipboardPayload(
                            contextId = event.project.id,
                            operation = ContextClipboardOperation.COPY,
                        )
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст скопійовано"))
                    }
                }
                is ContextHierarchyScreenEvent.CutContextLink -> {
                    contextClipboard.value =
                        ContextClipboardPayload(
                            contextId = event.project.id,
                            operation = ContextClipboardOperation.CUT,
                        )
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст вирізано"))
                    }
                }
                is ContextHierarchyScreenEvent.PasteContextLink -> {
                    viewModelScope.launch {
                        val payload = contextClipboard.value
                        if (payload == null) {
                            dialogUseCase.dismissDialog()
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Буфер порожній"))
                            return@launch
                        }

                        val allProjects = _allProjectsFlat.value
                        val source = allProjects.firstOrNull { it.id == payload.contextId }
                        if (source == null) {
                            contextClipboard.value = null
                            dialogUseCase.dismissDialog()
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст у буфері більше не існує"))
                            return@launch
                        }

                        if (!canPasteContextInto(event.project.id, allProjects, payload)) {
                            dialogUseCase.dismissDialog()
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Неможливо вставити в цей контекст"))
                            return@launch
                        }

                        when (payload.operation) {
                            ContextClipboardOperation.CUT -> {
                                withContext(ioDispatcher) {
                                    val allowSystemMoves = settingsRepo.allowSystemProjectMovesFlow.first()
                                    contextRepository.moveContext(
                                        contextToMove = source,
                                        newParentId = event.project.id,
                                        allowSystemMoves = allowSystemMoves,
                                    )
                                }
                                contextClipboard.value = null
                                dialogUseCase.dismissDialog()
                                _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст переміщено"))
                            }

                            ContextClipboardOperation.COPY -> {
                                val copiedName =
                                    generateCopiedContextName(
                                        baseName = source.name,
                                        targetParentId = event.project.id,
                                        allProjects = allProjects,
                                    )
                                withContext(ioDispatcher) {
                                    contextRepository.createContextWithId(
                                        id = UUID.randomUUID().toString(),
                                        name = copiedName,
                                        parentId = event.project.id,
                                        roleCode = source.roleCode,
                                    )
                                    allProjects.firstOrNull { it.id == event.project.id }
                                        ?.takeIf { !it.isExpanded }
                                        ?.let { parent ->
                                            contextRepository.updateContext(parent.copy(isExpanded = true))
                                        }
                                }
                                dialogUseCase.dismissDialog()
                                _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст скопійовано в обраний контекст"))
                            }
                        }
                    }
                }
                is ContextHierarchyScreenEvent.GoToSettings -> {
                    viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) }
                }
                is ContextHierarchyScreenEvent.ShowSearchDialog -> {
                    searchUseCase.onSearchQueryChanged(TextFieldValue(""))
                    searchUseCase.onToggleSearch(true)
                }
                is ContextHierarchyScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false

                is ContextHierarchyScreenEvent.ShowWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiServerDialog()
                is ContextHierarchyScreenEvent.ShowWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiImportDialog()
                is ContextHierarchyScreenEvent.WifiPush -> {
                    if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) {
                        syncUseCase.performWifiPush(event.address)
                    }
                }
                is ContextHierarchyScreenEvent.ExportToFile ->
                    dialogUseCase.onExportToFileRequested()
                is ContextHierarchyScreenEvent.ExportToFileV2 ->
                    viewModelScope.launch {
                        val result = contextActionsUseCase.exportToFileV2()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Export V2 successful")
                            } else {
                                ProjectUiEvent.ShowToast("Export V2 error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                is ContextHierarchyScreenEvent.ExportAttachments -> {
                    viewModelScope.launch {
                        val result = contextActionsUseCase.exportAttachments()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments export successful")
                            } else {
                                ProjectUiEvent.ShowToast("Attachments export error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ImportAttachmentsFromFile -> {
                    Timber
                        .tag("SyncRepo_AttachmentsImport")
                        .d("MainScreenViewModel received ImportAttachmentsFromFile event with uri=${event.uri}")
                    viewModelScope.launch {
                        Timber.tag("SyncRepo_AttachmentsImport").d("Starting attachment import coroutine")
                        val result = contextActionsUseCase.importAttachments(event.uri)
                        Timber
                            .tag("SyncRepo_AttachmentsImport")
                            .d("Import completed with result: isSuccess=${result.isSuccess}, message=${result.getOrNull()}")
                        dialogUseCase.dismissDialog()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments import successful")
                            } else {
                                ProjectUiEvent.ShowToast("Attachments import error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToChat -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiChat] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Chat))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToActivityTrackerScreen -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Tracker))
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToAiInsights -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiInsights] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AiInsights))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToLifeState -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiLifeManagement] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.LifeState))
                        }
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToTacticsScreen -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.TacticalManagement))
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToStrategicManagement -> {
                    if (uiState.value.featureToggles[FeatureFlag.StrategicManagement] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.NavigateToStrategicManagement)
                        }
                    }
                }

                is ContextHierarchyScreenEvent.SaveSettings -> {
                    settingsUseCase.saveSettings(viewModelScope, event.settings)
                }
                is ContextHierarchyScreenEvent.SaveAllContexts -> {
                    settingsUseCase.saveAllContexts(viewModelScope, event.updatedContexts)
                }
                is ContextHierarchyScreenEvent.DismissWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiServerDialog()
                is ContextHierarchyScreenEvent.DismissWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiImportDialog()
                is ContextHierarchyScreenEvent.DesktopAddressChange ->
                    syncUseCase.onDesktopAddressChange(event.address)
                is ContextHierarchyScreenEvent.PerformWifiImport ->
                    if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) {
                        syncUseCase.performWifiImport(
                            event.address,
                        )
                    }
                is ContextHierarchyScreenEvent.AddContextConfirm -> {
                    viewModelScope.launch {
                        contextActionsUseCase.addNewProject(
                            id = UUID.randomUUID().toString(),
                            name = event.name,
                            parentId = event.parentId,
                            roleCode = event.roleCode,
                            allProjects = _allProjectsFlat.value,
                        )
                    }
                    dialogUseCase.dismissDialog()
                }
                is ContextHierarchyScreenEvent.CloseSearch -> searchUseCase.onCloseSearch()
                is ContextHierarchyScreenEvent.NavigateToContext -> onNavigateToProject(event.projectId)
                is ContextHierarchyScreenEvent.CollapseAll -> navigationUseCase.onCollapseAll(viewModelScope)
                is ContextHierarchyScreenEvent.UpdateLightTheme -> themingUseCase.updateLightTheme(viewModelScope, event.themeName)
                is ContextHierarchyScreenEvent.UpdateDarkTheme -> themingUseCase.updateDarkTheme(viewModelScope, event.themeName)
                is ContextHierarchyScreenEvent.UpdateThemeMode -> themingUseCase.updateThemeMode(viewModelScope, event.themeMode)
                is ContextHierarchyScreenEvent.GoToReminders -> {
                    viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Reminders)) }
                }
                is ContextHierarchyScreenEvent.OpenAttachmentsLibrary -> {
                    if (uiState.value.featureToggles[FeatureFlag.AttachmentsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AttachmentsLibrary))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.AddScriptRequest -> {
                    if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptEditor()))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.OpenScriptsLibrary -> {
                    if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptsLibrary))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.RevealContextInHierarchy -> {
                    viewModelScope.launch {
                        revealProject(projectId = event.projectId)
                    }
                }
                is ContextHierarchyScreenEvent.OpenInboxContext -> {
                    viewModelScope.launch {
                        val inboxProject =
                            _allProjectsFlat.value.firstOrNull { it.id == SystemContexts.INBOX.raw }
                                ?: _allProjectsFlat.value.firstOrNull {
                                    it.name.equals("Inbox", ignoreCase = true) && it.id != SystemContexts.TODAY.raw
                                }
                        if (inboxProject == null) {
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox project not found"))
                            return@launch
                        }

                        _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(inboxProject.id))
                    }
                }
                else -> {}
            }
        }

        private fun handleBackNavigation() {
            searchUseCase.handleBackNavigation(
                areAnyProjectsExpanded = uiState.value.areAnyProjectsExpanded,
                collapseAllProjects = { viewModelScope.launch { contextActionsUseCase.collapseAllProjects(_allProjectsFlat.value) } },
                goBack = { enhancedNavigationManager?.goBack() },
            )
        }

        private fun onProjectClicked(projectId: String) {
            viewModelScope.launch {
                val project = _allProjectsFlat.value.find { it.id == projectId }
                if (project != null) {
                    recentItemsRepository.logProjectAccess(project)
                    enhancedNavigationManager?.navigateToProject(projectId, project.name)
                }
            }
        }

        private fun onHomeClicked() {
            navigationUseCase.onNavigateHome(viewModelScope)
        }

        private fun onToggleExpanded(project: Context) {
            viewModelScope.launch {
                if (uiState.value.planningMode == PlanningMode.All) {
                    contextActionsUseCase.onToggleExpanded(project)
                } else {
                    planningUseCase.toggleExpandedInPlanningMode(project)
                }
            }
        }

        private fun onBottomNavExpandedChange(isExpanded: Boolean) {
            viewModelScope.launch {
                _isBottomNavExpanded.value = isExpanded
                contextActionsUseCase.onBottomNavExpandedChange(isExpanded)
            }
        }

        private fun onRecentItemSelected(item: RecentItem) {
            viewModelScope.launch {
                _showRecentListsSheet.value = false
                when (item.type) {
                    RecentItemType.PROJECT -> {
                        contextRepository.getContextById(item.target)?.let { recentItemsRepository.logProjectAccess(it) }
                        val project = _allProjectsFlat.value.find { it.id == item.target }
                        if (project != null) {
                            searchUseCase.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                            enhancedNavigationManager?.navigateToProject(item.target, project.name)
                        } else {
                            recentItemsRepository.removeRecentItem(item.id)
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст більше не існує"))
                        }
                    }
                    RecentItemType.NOTE -> {
                        noteRepository.getNoteById(item.target)?.let {
                            recentItemsRepository.logNoteAccess(it)
                        }
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Legacy note editing is no longer supported"))
                    }
                    RecentItemType.NOTE_DOCUMENT -> {
                        noteDocumentRepository.getDocumentById(item.target)?.let {
                            recentItemsRepository.logNoteDocumentAccess(it)
                        }
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.NoteDocument(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.CHECKLIST -> {
                        checklistRepository.getChecklistById(item.target)?.let {
                            recentItemsRepository.logChecklistAccess(it)
                        }
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.Checklist(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.MUSIC_NOTE -> {
                        musicNoteRepository.getById(item.target)?.let {
                            recentItemsRepository.logMusicNoteAccess(it)
                        }
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.MusicNote(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.OBSIDIAN_LINK -> {
                        val link =
                            RelatedLink(
                                target = item.target,
                                displayName = item.displayName,
                                type = LinkType.OBSIDIAN,
                            )
                        recentItemsRepository.logObsidianLinkAccess(link)
                        val vaultName = settingsRepo.obsidianVaultNameFlow.first()
                        val encodedNoteName = URLEncoder.encode(item.target, "UTF-8")
                        val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
                        _uiEventChannel.send(ProjectUiEvent.OpenUri(uri))
                    }
                }
            }
        }

        private fun handleMoveConfirm(newParentId: String?) {
            viewModelScope.launch {
                contextActionsUseCase.onListChooserResult(
                    newParentId = newParentId,
                    projectBeingMovedId = projectBeingMovedId.value,
                    allProjects = _allProjectsFlat.value,
                )
                savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
            }
        }

        private fun createNoteInInbox() {
            val inboxProjectId =
                _allProjectsFlat.value.firstOrNull { it.id == SystemContexts.INBOX.raw }?.id
            if (inboxProjectId == null) {
                viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено")) }
                return
            }
            createNoteInContext(inboxProjectId)
        }

        private fun createChecklistInInbox() {
            val inboxProjectId =
                _allProjectsFlat.value.firstOrNull { it.id == SystemContexts.INBOX.raw }?.id
            if (inboxProjectId == null) {
                viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено")) }
                return
            }
            createChecklistInContext(inboxProjectId)
        }

        private fun createNoteInContext(contextId: String) {
            viewModelScope.launch {
                val documentId =
                    noteDocumentRepository.createDocument(
                        name = "Нова нотатка",
                        contextId = contextId,
                        content = "",
                    )
                _uiEventChannel.send(
                    ProjectUiEvent.Navigate(
                        NavTarget.NoteDocument(id = documentId, startEdit = true),
                    ),
                )
            }
        }

        private fun createChecklistInContext(contextId: String) {
            viewModelScope.launch {
                val checklistId = checklistRepository.createChecklist(name = "Новий чекліст", contextId = contextId)
                _uiEventChannel.send(
                    ProjectUiEvent.Navigate(
                        NavTarget.Checklist(id = checklistId),
                    ),
                )
            }
        }

        private fun toggleRecentItemPin(item: RecentItem) {
            viewModelScope.launch {
                val updatedItem = item.copy(isPinned = !item.isPinned)
                recentItemsRepository.updateRecentItem(updatedItem)
            }
        }

        private fun onDayPlanClicked() {
            viewModelScope.launch {
                val today = System.currentTimeMillis()
                _uiEventChannel.send(ProjectUiEvent.NavigateToDayPlan(today, "PLAN"))
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
                dialogUseCase.setReminderForOngoingActivity(viewModelScope, lastOngoingActivity)
            }
        }

        fun onReminderDialogDismiss() {
            dialogUseCase.onReminderDialogDismiss()
        }

        fun onSetReminder(timestamp: Long) {
            dialogUseCase.onSetReminder(viewModelScope, timestamp)
        }

        fun onClearReminder() {
            dialogUseCase.onClearReminder(viewModelScope)
        }

        private fun onNavigateToProject(projectId: String) {
            viewModelScope.launch {
                contextRepository.getContextById(projectId)?.let { recentItemsRepository.logProjectAccess(it) }
                navigationUseCase.onNavigateToProject(viewModelScope, projectId)
            }
        }

        private fun canPasteContextInto(targetContextId: String): Boolean {
            val payload = contextClipboard.value ?: return false
            return canPasteContextInto(
                targetContextId = targetContextId,
                allProjects = _allProjectsFlat.value,
                payload = payload,
            )
        }

        private fun canPasteContextInto(
            targetContextId: String,
            allProjects: List<Context>,
            payload: ContextClipboardPayload,
        ): Boolean {
            if (targetContextId.isBlank()) return false
            val source = allProjects.firstOrNull { it.id == payload.contextId } ?: return false
            val target = allProjects.firstOrNull { it.id == targetContextId } ?: return false

            return when (payload.operation) {
                ContextClipboardOperation.COPY -> source.id != target.id
                ContextClipboardOperation.CUT ->
                    source.id != target.id &&
                        !isDescendantOrSelf(
                            candidateDescendantId = target.id,
                            ancestorId = source.id,
                            allProjects = allProjects,
                        )
            }
        }

        private fun isDescendantOrSelf(
            candidateDescendantId: String,
            ancestorId: String,
            allProjects: List<Context>,
        ): Boolean {
            if (candidateDescendantId == ancestorId) return true
            val parentById = allProjects.associate { it.id to it.parentId }
            var currentParent = parentById[candidateDescendantId]
            while (!currentParent.isNullOrBlank()) {
                if (currentParent == ancestorId) return true
                currentParent = parentById[currentParent]
            }
            return false
        }

        private fun generateCopiedContextName(
            baseName: String,
            targetParentId: String,
            allProjects: List<Context>,
        ): String {
            val siblingNames = allProjects.filter { it.parentId == targetParentId }.map { it.name }.toSet()
            val firstCandidate = "$baseName (копія)"
            if (firstCandidate !in siblingNames) return firstCandidate
            var index = 2
            while (true) {
                val candidate = "$baseName (копія $index)"
                if (candidate !in siblingNames) return candidate
                index += 1
            }
        }

        suspend fun getInboxProjectId(): String? =
            withContext(ioDispatcher) {
                val allProjects = _allProjectsFlat.first()
                val inboxProject =
                    allProjects.firstOrNull { it.id == SystemContexts.INBOX.raw }
                        ?: allProjects.firstOrNull {
                            it.name.equals("Inbox", ignoreCase = true) && it.id != SystemContexts.TODAY.raw
                        }
                inboxProject?.id
            }

        override fun onCleared() {
            navigationStateJob?.cancel()
            navigationResultJob?.cancel()
            navigationUseCase.detach()
            super.onCleared()
        }
    }
