// File: MainScreenViewModel.kt - CORRECTED

package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.routes.CHAT_ROUTE
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.actions.ProjectActionsHandler
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.RevealResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.sync.WifiSyncManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dialogStateManager: DialogStateManager,
    private val planningModeManager: PlanningModeManager,
    private val actionsHandler: ProjectActionsHandler
) : ViewModel() {

    companion object {
        private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
        // Add a key to communicate the "came from global search" state
        private const val CAME_FROM_GLOBAL_SEARCH_KEY = "came_from_global_search"
    }

    var enhancedNavigationManager: EnhancedNavigationManager? = null
        set(value) {
            field = value
            if (value != null && !isInitialized) {
                isInitialized = true
                initializeAndCollectStates()
                initializeNavigationResultHandling()
            }
        }

    private var isInitialized = false

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _uiEventChannel = Channel<ProjectUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val _allProjectsFlat = projectRepository.getAllProjectsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val searchAndNavigationManager = SearchAndNavigationManager(projectRepository, viewModelScope, savedStateHandle, _uiEventChannel, _allProjectsFlat)
    private val hierarchyManager = ProjectHierarchyManager()
    private val wifiSyncManager = WifiSyncManager(syncRepo, settingsRepo, application, viewModelScope, _uiEventChannel)

    private val _isProcessingReveal = MutableStateFlow(false)
    private var projectToRevealAndScroll: String? = null
    private val _showRecentListsSheet = MutableStateFlow(false)
    private val _isBottomNavExpanded = MutableStateFlow(false)
    private val _showSearchDialog = MutableStateFlow(false)
    private val _isReadyForFiltering = MutableStateFlow(false)
    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")
    private val projectBeingMovedId = savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

    private val _subStateStack = MutableStateFlow<List<MainSubState>>(listOf(MainSubState.Hierarchy))

    private fun initializeNavigationResultHandling() {
        enhancedNavigationManager?.let { navManager ->
            viewModelScope.launch {
                navManager.navigationResults.collect { result ->
                    handleNavigationResult(result.key, result.value)
                }
            }
        }
    }

    private fun handleNavigationResult(key: String, value: String) {
        when (key) {
            "project_to_reveal" -> {
                viewModelScope.launch {
                    // **FIXED**: Instead of calling a non-existent method,
                    // set a flag in the SavedStateHandle. The SearchAndNavigationManager
                    // can observe this value to know it's coming from a global search.
                    savedStateHandle[CAME_FROM_GLOBAL_SEARCH_KEY] = true

                    if (_isProcessingReveal.value) return@launch
                    _isProcessingReveal.value = true
                    try {
                        when (val result = searchAndNavigationManager.revealProjectInHierarchy(value)) {
                            is RevealResult.Success -> {
                                pushSubState(MainSubState.ProjectFocused(value))
                                if (result.shouldFocus) {
                                    searchAndNavigationManager.navigateToProject(result.projectId, uiState.value.projectHierarchy)
                                } else {
                                    projectToRevealAndScroll = result.projectId
                                    if (isSearchActive()) {
                                        popToSubState(MainSubState.Hierarchy)
                                    }
                                }
                            }
                            is RevealResult.Failure -> {
                                _uiEventChannel.send(ProjectUiEvent.ShowToast("Не удалось показать локацию"))
                            }
                        }
                    } finally {
                        _isProcessingReveal.value = false
                    }
                }
            }
        }
    }

    private fun pushSubState(subState: MainSubState) {
        val currentStack = _subStateStack.value
        if (currentStack.lastOrNull() != subState) {
            _subStateStack.value = currentStack + subState
        }
    }

    private fun popSubState(): MainSubState? {
        val currentStack = _subStateStack.value
        return if (currentStack.size > 1) {
            _subStateStack.value = currentStack.dropLast(1)
            currentStack.last()
        } else null
    }

    private fun popToSubState(targetState: MainSubState) {
        val currentStack = _subStateStack.value
        val targetIndex = currentStack.indexOfLast { it == targetState }
        if (targetIndex >= 0) {
            _subStateStack.value = currentStack.take(targetIndex + 1)
        } else {
            _subStateStack.value = listOf(targetState)
        }
    }

    private fun replaceCurrentSubState(newState: MainSubState) {
        val currentStack = _subStateStack.value
        _subStateStack.value = currentStack.dropLast(1) + newState
    }

    private fun isSearchActive(): Boolean {
        return _subStateStack.value.any { it is MainSubState.LocalSearch }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun initializeAndCollectStates() {
        viewModelScope.launch(ioDispatcher) {
            contextHandler.initialize()
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                _isBottomNavExpanded.value = savedState
            }
            _isReadyForFiltering.value = true
        }

        val debouncedSearchQueryText = searchAndNavigationManager.searchQuery
            .map { it.text }
            .debounce(350L)
            .distinctUntilChanged()

        val planningSettingsState = combine(
            settingsRepo.showPlanningModesFlow,
            settingsRepo.dailyTagFlow,
            settingsRepo.mediumTagFlow,
            settingsRepo.longTagFlow,
        ) { show, daily, medium, long ->
            PlanningSettingsState(show, daily, medium, long)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanningSettingsState())

        val filterStateFlow: StateFlow<FilterState> = combine(
            listOf(
                _allProjectsFlat,
                debouncedSearchQueryText,
                _subStateStack.map { stack -> stack.any { it is MainSubState.LocalSearch } },
                planningModeManager.planningMode,
                planningSettingsState,
                _isReadyForFiltering,
                searchAndNavigationManager.isPendingStateRestoration
            )
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val flatList = values[0] as List<Project>
            val query = values[1] as String
            val searchActive = values[2] as Boolean
            val mode = values[3] as PlanningMode
            val settings = values[4] as PlanningSettingsState
            val isReady = values[5] as Boolean
            val isPendingRestoration = values[6] as Boolean

            if (!isReady || isPendingRestoration) {
                FilterState(
                    flatList = flatList,
                    query = "",
                    searchActive = false,
                    mode = PlanningMode.All,
                    settings = settings
                )
            } else {
                FilterState(
                    flatList = flatList,
                    query = query,
                    searchActive = searchActive,
                    mode = mode,
                    settings = settings
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FilterState(
                flatList = emptyList(),
                query = "",
                searchActive = false,
                mode = PlanningMode.All,
                settings = PlanningSettingsState()
            )
        )

        val projectHierarchyFlow = combine(
            filterStateFlow,
            planningModeManager.expandedInDailyMode,
            planningModeManager.expandedInMediumMode,
            planningModeManager.expandedInLongMode,
            searchAndNavigationManager.focusedProjectId,
            _isProcessingReveal
        ) { values ->
            val filterState = values[0] as FilterState
            val expandedDaily = values[1] as Set<String>
            val expandedMedium = values[2] as Set<String>
            val expandedLong = values[3] as Set<String>
            val isProcessingReveal = values[5] as Boolean
            val hierarchy = hierarchyManager.createProjectHierarchy(filterState, expandedDaily, expandedMedium, expandedLong)
            val projectId = projectToRevealAndScroll
            if (projectId != null && !filterState.searchActive && !isProcessingReveal) {
                val displayedProjects = flattenHierarchy(hierarchy.topLevelProjects, hierarchy.childMap)
                val index = displayedProjects.indexOfFirst { it.id == projectId }
                if (index != -1) {
                    _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
                    projectToRevealAndScroll = null
                }
            }
            hierarchy
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

        val searchResultsFlow = combine(filterStateFlow, projectHierarchyFlow) { filterState, hierarchy ->
            if (filterState.searchActive && filterState.query.isNotBlank()) {
                hierarchyManager.createSearchResults(filterState, hierarchy)
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val areAnyProjectsExpandedFlow = _allProjectsFlat.map { projects -> projects.any { it.isExpanded } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val listChooserFinalExpandedIdsFlow = combine(
            _listChooserUserExpandedIds,
            _allProjectsFlat,
            projectBeingMovedId
        ) { userExpanded, allProjects, movingId ->
            if (movingId == null) return@combine emptySet()
            val projectLookup = allProjects.associateBy { it.id }
            val ancestorIds = mutableSetOf<String>()
            findAncestorsRecursive(movingId, projectLookup, ancestorIds, mutableSetOf())
            userExpanded + ancestorIds
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

        val filteredListHierarchyForDialogFlow = combine(
            _allProjectsFlat,
            _listChooserFilterText,
            projectBeingMovedId
        ) { allProjects, filterText, movingId ->
            hierarchyManager.createFilteredListHierarchyForDialog(allProjects, filterText, movingId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

        viewModelScope.launch {
            val navManager = enhancedNavigationManager
            if (navManager == null) {
                // Заглушки для состояний навигации
                combine(
                    _subStateStack, // 0
                    searchAndNavigationManager.searchQuery, // 1
                    searchAndNavigationManager.searchHistory, // 2
                    projectHierarchyFlow, // 3
                    searchAndNavigationManager.currentBreadcrumbs, // 4
                    areAnyProjectsExpandedFlow, // 5
                    planningModeManager.planningMode, // 6
                    planningSettingsState, // 7
                    dialogStateManager.dialogState, // 8
                    _showRecentListsSheet, // 9
                    _isBottomNavExpanded, // 10
                    projectRepository.getRecentProjects(), // 11
                    contextHandler.allContextsFlow, // 12
                    listChooserFinalExpandedIdsFlow, // 13
                    filteredListHierarchyForDialogFlow, // 14
                    _isProcessingReveal, // 15
                    _isReadyForFiltering, // 16
                    settingsRepo.obsidianVaultNameFlow, // 17
                    AppStatistics().let { flowOf(it) }, // 18 - як flow
                    wifiSyncManager.showWifiServerDialog, // 19
                    wifiSyncManager.wifiServerAddress, // 20
                    wifiSyncManager.showWifiImportDialog, // 21
                    wifiSyncManager.desktopAddress, // 22
                    _showSearchDialog // 23
                ) { states ->
                    @Suppress("UNCHECKED_CAST")
                    MainScreenUiState(
                        subStateStack = states[0] as List<MainSubState>,
                        searchQuery = states[1] as TextFieldValue,
                        searchHistory = states[2] as List<String>,
                        projectHierarchy = states[3] as ListHierarchyData,
                        currentBreadcrumbs = states[4] as List<BreadcrumbItem>,
                        areAnyProjectsExpanded = states[5] as Boolean,
                        planningMode = states[6] as PlanningMode,
                        planningSettings = states[7] as PlanningSettingsState,
                        dialogState = states[8] as DialogState,
                        showRecentListsSheet = states[9] as Boolean,
                        isBottomNavExpanded = states[10] as Boolean,
                        recentProjects = states[11] as List<Project>,
                        allContexts = states[12] as List<UiContext>,
                        listChooserFinalExpandedIds = states[13] as Set<String>,
                        filteredListHierarchyForDialog = states[14] as ListHierarchyData,
                        canGoBack = false, // Заглушка
                        canGoForward = false, // Заглушка
                        showNavigationMenu = false, // Заглушка
                        isProcessingReveal = states[15] as Boolean,
                        isReadyForFiltering = states[16] as Boolean,
                        obsidianVaultName = states[17] as String,
                        appStatistics = states[18] as AppStatistics,
                        showWifiServerDialog = states[19] as Boolean,
                        wifiServerAddress = states[20] as String?,
                        showWifiImportDialog = states[21] as Boolean,
                        desktopAddress = states[22] as String,
                        showSearchDialog = states[23] as Boolean
                    )
                }
            } else {
                combine(
                    _subStateStack, // 0
                    searchAndNavigationManager.searchQuery, // 1
                    searchAndNavigationManager.searchHistory, // 2
                    projectHierarchyFlow, // 3
                    searchAndNavigationManager.currentBreadcrumbs, // 4
                    areAnyProjectsExpandedFlow, // 5
                    planningModeManager.planningMode, // 6
                    planningSettingsState, // 7
                    dialogStateManager.dialogState, // 8
                    _showRecentListsSheet, // 9
                    _isBottomNavExpanded, // 10
                    projectRepository.getRecentProjects(), // 11
                    contextHandler.allContextsFlow, // 12
                    listChooserFinalExpandedIdsFlow, // 13
                    filteredListHierarchyForDialogFlow, // 14
                    navManager.canGoBack, // 15
                    navManager.canGoForward, // 16
                    navManager.showNavigationMenu, // 17
                    _isProcessingReveal, // 18
                    _isReadyForFiltering, // 19
                    settingsRepo.obsidianVaultNameFlow, // 20
                    AppStatistics().let { flowOf(it) }, // 21 - як flow
                    wifiSyncManager.showWifiServerDialog, // 22
                    wifiSyncManager.wifiServerAddress, // 23
                    wifiSyncManager.showWifiImportDialog, // 24
                    wifiSyncManager.desktopAddress, // 25
                    _showSearchDialog, // 26

                    // VVVVVVVVVV  ОСЬ ТУТ ПОТРІБНО ДОДАТИ РЯДОК VVVVVVVVVV
                    searchResultsFlow // 27
                ) { states ->
                    @Suppress("UNCHECKED_CAST")
                    MainScreenUiState(
                        subStateStack = states[0] as List<MainSubState>,
                        searchQuery = states[1] as TextFieldValue,
                        searchHistory = states[2] as List<String>,
                        projectHierarchy = states[3] as ListHierarchyData,
                        currentBreadcrumbs = states[4] as List<BreadcrumbItem>,
                        areAnyProjectsExpanded = states[5] as Boolean,
                        planningMode = states[6] as PlanningMode,
                        planningSettings = states[7] as PlanningSettingsState,
                        dialogState = states[8] as DialogState,
                        showRecentListsSheet = states[9] as Boolean,
                        isBottomNavExpanded = states[10] as Boolean,
                        recentProjects = states[11] as List<Project>,
                        allContexts = states[12] as List<UiContext>,
                        listChooserFinalExpandedIds = states[13] as Set<String>,
                        filteredListHierarchyForDialog = states[14] as ListHierarchyData,
                        canGoBack = states[15] as Boolean,
                        canGoForward = states[16] as Boolean,
                        showNavigationMenu = states[17] as Boolean,
                        isProcessingReveal = states[18] as Boolean,
                        isReadyForFiltering = states[19] as Boolean,
                        obsidianVaultName = states[20] as String,
                        appStatistics = states[21] as AppStatistics,
                        showWifiServerDialog = states[22] as Boolean,
                        wifiServerAddress = states[23] as String?,
                        showWifiImportDialog = states[24] as Boolean,
                        desktopAddress = states[25] as String,
                        showSearchDialog = states[26] as Boolean,
                        // Тепер цей рядок є правильним, оскільки є 28 джерел (індекси 0-27)
                        searchResults = states[27] as List<SearchResult>

                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.SearchQueryChanged -> {
                searchAndNavigationManager.onSearchQueryChanged(event.query)
                // Якщо ми ще не в режимі пошуку, перейти в нього.
                if (!isSearchActive()) {
                    pushSubState(MainSubState.LocalSearch(event.query.text))
                } else {
                    // Якщо ми вже в режимі пошуку, оновити стан з новим запитом.
                    replaceCurrentSubState(MainSubState.LocalSearch(event.query.text))
                }

            }
            is MainScreenEvent.SearchFromHistory -> {
                pushSubState(MainSubState.LocalSearch(event.query))
                searchAndNavigationManager.onSearchQueryFromHistory(event.query)
            }
            is MainScreenEvent.GlobalSearchPerform -> onPerformGlobalSearch(event.query)
            is MainScreenEvent.SearchResultClick -> onSearchResultClick(event.projectId)

            is MainScreenEvent.ProjectClick -> onProjectClicked(event.projectId)
            is MainScreenEvent.ProjectMenuRequest -> dialogStateManager.onMenuRequested(event.project)
            is MainScreenEvent.ToggleProjectExpanded -> onToggleExpanded(event.project)
            is MainScreenEvent.ProjectReorder -> {
                viewModelScope.launch {
                    actionsHandler.onProjectReorder(
                        fromId = event.fromId,
                        toId = event.toId,
                        position = event.position,
                        isSearchActive = isSearchActive(),
                        allProjects = _allProjectsFlat.value
                    )
                }
            }

            is MainScreenEvent.BreadcrumbNavigation -> searchAndNavigationManager.navigateToBreadcrumb(event.breadcrumb)
            is MainScreenEvent.ClearBreadcrumbNavigation -> {
                searchAndNavigationManager.clearNavigation()
                popToSubState(MainSubState.Hierarchy)
            }

            is MainScreenEvent.PlanningModeChange -> onPlanningModeChange(event.mode)

            is MainScreenEvent.DismissDialog -> dialogStateManager.dismissDialog()
            is MainScreenEvent.AddNewProjectRequest -> dialogStateManager.onAddNewProjectRequest()
            is MainScreenEvent.AddSubprojectRequest -> dialogStateManager.onAddSubprojectRequest(event.parentProject)
            is MainScreenEvent.DeleteRequest -> dialogStateManager.onDeleteRequest(event.project)
            is MainScreenEvent.MoveRequest -> {
                viewModelScope.launch {
                    val route = actionsHandler.getMoveProjectRoute(event.project, _allProjectsFlat.value)
                    savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = event.project.id
                    dialogStateManager.dismissDialog()
                    _uiEventChannel.send(ProjectUiEvent.Navigate(route))
                }
            }
            is MainScreenEvent.DeleteConfirm -> {
                viewModelScope.launch {
                    actionsHandler.onDeleteProjectConfirmed(event.project, uiState.value.projectHierarchy.childMap)
                    dialogStateManager.dismissDialog()
                }
            }
            is MainScreenEvent.MoveConfirm -> {
                viewModelScope.launch {
                    actionsHandler.onListChooserResult(
                        newParentId = event.newParentId,
                        projectBeingMovedId = projectBeingMovedId.value,
                        allProjects = _allProjectsFlat.value
                    )
                    savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
                }
            }
            is MainScreenEvent.FullImportConfirm -> {
                viewModelScope.launch {
                    val result = actionsHandler.onFullImportConfirmed(event.uri)
                    dialogStateManager.dismissDialog()
                    _uiEventChannel.send(
                        if (result.isSuccess) {
                            ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import successful")
                        } else {
                            ProjectUiEvent.ShowToast("Import error: ${result.exceptionOrNull()?.message}")
                        }
                    )
                }
            }
            is MainScreenEvent.ShowAboutDialog -> dialogStateManager.onShowAboutDialog()
            is MainScreenEvent.ImportFromFileRequest -> dialogStateManager.onImportFromFileRequested(event.uri)

            is MainScreenEvent.HomeClick -> onHomeClicked()
            is MainScreenEvent.BackClick -> handleBackNavigation()
            is MainScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
            is MainScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
            is MainScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()

            is MainScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
            is MainScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
            is MainScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
            is MainScreenEvent.RecentProjectSelected -> onRecentProjectSelected(event.projectId)
            is MainScreenEvent.DayPlanClick -> onDayPlanClicked()
            is MainScreenEvent.ContextSelected -> onContextSelected(event.name)

            is MainScreenEvent.EditRequest -> onEditRequest(event.project)
            is MainScreenEvent.GoToSettings -> onShowSettingsScreen()
            is MainScreenEvent.ShowSearchDialog -> _showSearchDialog.value = true
            is MainScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false

            is MainScreenEvent.ShowWifiServerDialog -> wifiSyncManager.onShowWifiServerDialog()
            is MainScreenEvent.ShowWifiImportDialog -> wifiSyncManager.onShowWifiImportDialog()
            is MainScreenEvent.ExportToFile -> viewModelScope.launch {
                val result = actionsHandler.exportToFile()
                _uiEventChannel.send(
                    if (result.isSuccess) {
                        ProjectUiEvent.ShowToast(result.getOrNull() ?: "Export successful")
                    } else {
                        ProjectUiEvent.ShowToast("Export error: ${result.exceptionOrNull()?.message}")
                    }
                )
            }
            is MainScreenEvent.NavigateToChat -> {
                viewModelScope.launch {
                    _uiEventChannel.send(ProjectUiEvent.Navigate(CHAT_ROUTE))
                }
            }
            is MainScreenEvent.NavigateToActivityTracker -> {
                viewModelScope.launch {
                    _uiEventChannel.send(ProjectUiEvent.Navigate("activity_tracker_screen"))
                }
            }

            is MainScreenEvent.SaveSettings -> {
                saveSettings(
                    show = event.show,
                    daily = event.daily,
                    medium = event.medium,
                    long = event.long,
                    vaultName = event.vaultName
                )
            }
            is MainScreenEvent.SaveAllContexts -> {
                saveAllContexts(event.updatedContexts)
            }
            is MainScreenEvent.DismissWifiServerDialog -> wifiSyncManager.onDismissWifiServerDialog()
            is MainScreenEvent.DismissWifiImportDialog -> wifiSyncManager.onDismissWifiImportDialog()
            is MainScreenEvent.DesktopAddressChange -> wifiSyncManager.onDesktopAddressChange(event.address)
            is MainScreenEvent.PerformWifiImport -> wifiSyncManager.performWifiImport(event.address)
        }
    }
    private fun handleBackNavigation() {
        val currentStack = _subStateStack.value
        when {
            currentStack.size > 1 -> {
                popSubState()
                if (currentStack.last() is MainSubState.LocalSearch) {
                    searchAndNavigationManager.onToggleSearch(false)
                }
            }
            uiState.value.currentBreadcrumbs.isNotEmpty() -> {
                searchAndNavigationManager.clearNavigation()
            }
            uiState.value.areAnyProjectsExpanded -> {
                viewModelScope.launch {
                    actionsHandler.collapseAllProjects(_allProjectsFlat.value)
                }
            }
            else -> {
                enhancedNavigationManager?.goBack()
            }
        }
    }

    private fun onSearchResultClick(projectId: String) {
        if (_isProcessingReveal.value) return
        viewModelScope.launch {
            _isProcessingReveal.value = true
            try {
                when (val result = searchAndNavigationManager.revealProjectInHierarchy(projectId)) {
                    is RevealResult.Success -> {
                        replaceCurrentSubState(MainSubState.ProjectFocused(projectId))
                        searchAndNavigationManager.onToggleSearch(false)

                        if (result.shouldFocus) {
                            searchAndNavigationManager.navigateToProject(result.projectId, uiState.value.projectHierarchy)
                        } else {
                            projectToRevealAndScroll = result.projectId
                        }
                    }
                    is RevealResult.Failure -> {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Не удалось показать локацию"))
                    }
                }
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }

    private fun onProjectClicked(projectId: String) {
        viewModelScope.launch {
            val project = _allProjectsFlat.value.find { it.id == projectId }
            if (project != null) {
                popToSubState(MainSubState.Hierarchy)
                enhancedNavigationManager?.navigateToProject(projectId, project.name)
            }
        }
    }

    private fun onHomeClicked() {
        viewModelScope.launch {
            searchAndNavigationManager.clearAllSearchState()
            _subStateStack.value = listOf(MainSubState.Hierarchy)

            planningModeManager.changeMode(PlanningMode.All)
            planningModeManager.resetExpansionStates()

            actionsHandler.collapseAllProjects(_allProjectsFlat.value)

            enhancedNavigationManager?.navigateHome()

            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    private fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            searchAndNavigationManager.onSearchQueryFromHistory(query)
            if (isSearchActive()) {
                popToSubState(MainSubState.Hierarchy)
                searchAndNavigationManager.onToggleSearch(false)
            }
            enhancedNavigationManager?.navigateToGlobalSearch(query)
            _showSearchDialog.value = false
        }
    }

    private fun onPlanningModeChange(mode: PlanningMode) {
        if (isSearchActive()) {
            popToSubState(MainSubState.Hierarchy)
            searchAndNavigationManager.onToggleSearch(false)
        }
        planningModeManager.changeMode(mode)
    }

    private fun onToggleExpanded(project: Project) {
        viewModelScope.launch {
            if (uiState.value.planningMode == PlanningMode.All) {
                actionsHandler.onToggleExpanded(project)
            } else {
                planningModeManager.toggleExpandedInPlanningMode(project)
            }
        }
    }

    private fun onBottomNavExpandedChange(isExpanded: Boolean) {
        viewModelScope.launch {
            _isBottomNavExpanded.value = isExpanded
            withContext(ioDispatcher) {
                settingsRepo.saveBottomNavExpanded(isExpanded)
            }
        }
    }

    private fun onRecentProjectSelected(projectId: String) {
        viewModelScope.launch {
            _showRecentListsSheet.value = false
            val project = _allProjectsFlat.value.find { it.id == projectId }
            if (project != null) {
                popToSubState(MainSubState.Hierarchy)
                enhancedNavigationManager?.navigateToProject(projectId, project.name)
            }
        }
    }

    private fun onDayPlanClicked() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDayPlan(today))
        }
    }

    private fun onContextSelected(name: String) {
        viewModelScope.launch {
            val targetTag = contextHandler.getContextTag(name)
            if (targetTag.isNullOrBlank()) {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Тег для контекста '$name' не найден"))
                return@launch
            }
            val targetProject = _allProjectsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetProject != null) {
                popToSubState(MainSubState.Hierarchy)
                enhancedNavigationManager?.navigateToProject(targetProject.id, targetProject.name)
            } else {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект с тегом '#$targetTag' не найден"))
            }
        }
    }

    private fun onEditRequest(project: Project) {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(project.id))
        }
    }

    private fun onShowSettingsScreen() {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToSettings)
        }
    }

    private fun saveSettings(
        show: Boolean,
        daily: String,
        medium: String,
        long: String,
        vaultName: String,
    ) {
        viewModelScope.launch {
            settingsRepo.saveShowPlanningModes(show)
            settingsRepo.saveDailyTag(daily.trim())
            settingsRepo.saveMediumTag(medium.trim())
            settingsRepo.saveLongTag(long.trim())
            settingsRepo.saveObsidianVaultName(vaultName.trim())
        }
    }

    private fun saveAllContexts(updatedContexts: List<UiContext>) {
        viewModelScope.launch {
            val customContextsToSave = updatedContexts.filter { !it.isReserved }
            settingsRepo.saveCustomContexts(customContextsToSave)
            contextHandler.initialize()
        }
    }

    fun collapseAllProjects() {
        viewModelScope.launch {
            actionsHandler.collapseAllProjects(_allProjectsFlat.value)
        }
    }
}