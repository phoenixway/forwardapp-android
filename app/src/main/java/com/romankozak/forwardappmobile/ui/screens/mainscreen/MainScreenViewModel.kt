// File: MainScreenViewModel.kt

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
    // WARNING: Constructor parameter is never used as a property
    private val syncRepo: SyncRepository,
    // WARNING: Constructor parameter is never used as a property
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dialogStateManager: DialogStateManager,
    private val planningModeManager: PlanningModeManager,
    private val actionsHandler: ProjectActionsHandler
) : ViewModel() {

    companion object {
        private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
    }

    // ЗМІНА: Робимо enhancedNavigationManager nullable і додаємо перевірку
    var enhancedNavigationManager: EnhancedNavigationManager? = null
        set(value) {
            field = value
            // Після встановлення менеджера, ініціалізуємо стани
            if (value != null && !isInitialized) {
                isInitialized = true
                initializeAndCollectStates()
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

    // ЗМІНА: Видаляємо виклик initializeAndCollectStates() з init блоку
    init {
        // Ініціалізація буде викликана в setter enhancedNavigationManager
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun initializeAndCollectStates() {
        viewModelScope.launch(ioDispatcher) {
            contextHandler.initialize()
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                _isBottomNavExpanded.value = savedState
            }
            // ЗМІНА: Активуємо фільтрацію після ініціалізації
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
                searchAndNavigationManager.isSearchActive,
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
            // ЗМІНА: Додаємо перевірку на null для enhancedNavigationManager
            val navManager = enhancedNavigationManager
            if (navManager == null) {
                // Якщо менеджер ще не ініціалізований, використовуємо заглушки
                combine(
                    searchAndNavigationManager.isSearchActive, // 0
                    searchAndNavigationManager.searchQuery, // 1
                    searchAndNavigationManager.searchHistory, // 2
                    searchResultsFlow, // 3
                    projectHierarchyFlow, // 4
                    searchAndNavigationManager.focusedProjectId, // 5
                    searchAndNavigationManager.currentBreadcrumbs, // 6
                    areAnyProjectsExpandedFlow, // 7
                    planningModeManager.planningMode, // 8
                    planningSettingsState, // 9
                    dialogStateManager.dialogState, // 10
                    _showRecentListsSheet, // 11
                    _isBottomNavExpanded, // 12
                    projectRepository.getRecentProjects(), // 13
                    contextHandler.allContextsFlow, // 14
                    listChooserFinalExpandedIdsFlow, // 15
                    filteredListHierarchyForDialogFlow, // 16
                    _isProcessingReveal, // 17
                    _isReadyForFiltering, // 18
                    settingsRepo.obsidianVaultNameFlow // 19
                ) { states ->
                    @Suppress("UNCHECKED_CAST")
                    MainScreenUiState(
                        isSearchActive = states[0] as Boolean,
                        searchQuery = states[1] as TextFieldValue,
                        searchHistory = states[2] as List<String>,
                        searchResults = states[3] as List<SearchResult>,
                        projectHierarchy = states[4] as ListHierarchyData,
                        focusedProjectId = states[5] as String?,
                        currentBreadcrumbs = states[6] as List<BreadcrumbItem>,
                        areAnyProjectsExpanded = states[7] as Boolean,
                        planningMode = states[8] as PlanningMode,
                        planningSettings = states[9] as PlanningSettingsState,
                        dialogState = states[10] as DialogState,
                        showRecentListsSheet = states[11] as Boolean,
                        isBottomNavExpanded = states[12] as Boolean,
                        recentProjects = states[13] as List<Project>,
                        allContexts = states[14] as List<UiContext>,
                        listChooserFinalExpandedIds = states[15] as Set<String>,
                        filteredListHierarchyForDialog = states[16] as ListHierarchyData,
                        canGoBack = false, // Заглушка
                        canGoForward = false, // Заглушка
                        showNavigationMenu = false, // Заглушка
                        isProcessingReveal = states[17] as Boolean,
                        isReadyForFiltering = states[18] as Boolean,
                        obsidianVaultName = states[19] as String
                    )
                }
            } else {
                combine(
                    searchAndNavigationManager.isSearchActive, // 0
                    searchAndNavigationManager.searchQuery, // 1
                    searchAndNavigationManager.searchHistory, // 2
                    searchResultsFlow, // 3
                    projectHierarchyFlow, // 4
                    searchAndNavigationManager.focusedProjectId, // 5
                    searchAndNavigationManager.currentBreadcrumbs, // 6
                    areAnyProjectsExpandedFlow, // 7
                    planningModeManager.planningMode, // 8
                    planningSettingsState, // 9
                    dialogStateManager.dialogState, // 10
                    _showRecentListsSheet, // 11
                    _isBottomNavExpanded, // 12
                    projectRepository.getRecentProjects(), // 13
                    contextHandler.allContextsFlow, // 14
                    listChooserFinalExpandedIdsFlow, // 15
                    filteredListHierarchyForDialogFlow, // 16
                    navManager.canGoBack, // 17
                    navManager.canGoForward, // 18
                    navManager.showNavigationMenu, // 19
                    _isProcessingReveal, // 20
                    _isReadyForFiltering, // 21
                    settingsRepo.obsidianVaultNameFlow // 22
                ) { states ->
                    @Suppress("UNCHECKED_CAST")
                    MainScreenUiState(
                        isSearchActive = states[0] as Boolean,
                        searchQuery = states[1] as TextFieldValue,
                        searchHistory = states[2] as List<String>,
                        searchResults = states[3] as List<SearchResult>,
                        projectHierarchy = states[4] as ListHierarchyData,
                        focusedProjectId = states[5] as String?,
                        currentBreadcrumbs = states[6] as List<BreadcrumbItem>,
                        areAnyProjectsExpanded = states[7] as Boolean,
                        planningMode = states[8] as PlanningMode,
                        planningSettings = states[9] as PlanningSettingsState,
                        dialogState = states[10] as DialogState,
                        showRecentListsSheet = states[11] as Boolean,
                        isBottomNavExpanded = states[12] as Boolean,
                        recentProjects = states[13] as List<Project>,
                        allContexts = states[14] as List<UiContext>,
                        listChooserFinalExpandedIds = states[15] as Set<String>,
                        filteredListHierarchyForDialog = states[16] as ListHierarchyData,
                        canGoBack = states[17] as Boolean,
                        canGoForward = states[18] as Boolean,
                        showNavigationMenu = states[19] as Boolean,
                        isProcessingReveal = states[20] as Boolean,
                        isReadyForFiltering = states[21] as Boolean,
                        obsidianVaultName = states[22] as String
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ToggleSearch, is MainScreenEvent.SearchQueryChanged,
            is MainScreenEvent.SearchFromHistory, is MainScreenEvent.GlobalSearchPerform,
            is MainScreenEvent.SearchResultClick, is MainScreenEvent.BreadcrumbNavigation,
            is MainScreenEvent.ClearBreadcrumbNavigation -> handleSearchAndNavigationEvents(event)

            is MainScreenEvent.ProjectClick, is MainScreenEvent.ProjectMenuRequest,
            is MainScreenEvent.ToggleProjectExpanded, is MainScreenEvent.ProjectReorder,
            is MainScreenEvent.EditRequest -> handleHierarchyEvents(event)

            is MainScreenEvent.DismissDialog, is MainScreenEvent.AddNewProjectRequest,
            is MainScreenEvent.AddSubprojectRequest, is MainScreenEvent.DeleteRequest,
            is MainScreenEvent.MoveRequest, is MainScreenEvent.DeleteConfirm,
            is MainScreenEvent.MoveConfirm, is MainScreenEvent.FullImportConfirm,
            is MainScreenEvent.ShowAboutDialog, is MainScreenEvent.ImportFromFileRequest,
            is MainScreenEvent.ShowSearchDialog, is MainScreenEvent.DismissSearchDialog -> handleDialogEvents(event)

            is MainScreenEvent.HomeClick, is MainScreenEvent.BackClick,
            is MainScreenEvent.ForwardClick, is MainScreenEvent.HistoryClick,
            is MainScreenEvent.HideHistory, is MainScreenEvent.BottomNavExpandedChange,
            is MainScreenEvent.ShowRecentLists, is MainScreenEvent.DismissRecentLists,
            is MainScreenEvent.RecentProjectSelected, is MainScreenEvent.DayPlanClick,
            is MainScreenEvent.ContextSelected, is MainScreenEvent.GoToSettings,
            is MainScreenEvent.NavigateToActivityTracker, is MainScreenEvent.NavigateToChat -> handleMainNavigationEvents(event)

            is MainScreenEvent.PlanningModeChange -> onPlanningModeChange(event.mode)
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

    private fun handleSearchAndNavigationEvents(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ToggleSearch -> searchAndNavigationManager.onToggleSearch(event.isActive)
            is MainScreenEvent.SearchQueryChanged -> searchAndNavigationManager.onSearchQueryChanged(event.query)
            is MainScreenEvent.SearchFromHistory -> searchAndNavigationManager.onSearchQueryFromHistory(event.query)
            is MainScreenEvent.GlobalSearchPerform -> onPerformGlobalSearch(event.query)
            is MainScreenEvent.SearchResultClick -> onSearchResultClick(event.projectId)
            is MainScreenEvent.BreadcrumbNavigation -> searchAndNavigationManager.navigateToBreadcrumb(event.breadcrumb)
            is MainScreenEvent.ClearBreadcrumbNavigation -> searchAndNavigationManager.clearNavigation()
            else -> {}
        }
    }

    private fun handleHierarchyEvents(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ProjectClick -> onProjectClicked(event.projectId)
            is MainScreenEvent.ProjectMenuRequest -> dialogStateManager.onMenuRequested(event.project)
            is MainScreenEvent.ToggleProjectExpanded -> onToggleExpanded(event.project)
            is MainScreenEvent.ProjectReorder -> {
                viewModelScope.launch {
                    actionsHandler.onProjectReorder(
                        event.fromId,
                        event.toId,
                        event.position,
                        uiState.value.isSearchActive,
                        _allProjectsFlat.value
                    )
                }
            }
            is MainScreenEvent.EditRequest -> onEditRequest(event.project)
            else -> {}
        }
    }

    private fun handleDialogEvents(event: MainScreenEvent) {
        when (event) {
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
                    actionsHandler.onListChooserResult(event.newParentId, projectBeingMovedId.value, _allProjectsFlat.value)
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
            is MainScreenEvent.ShowSearchDialog -> _showSearchDialog.value = true
            is MainScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false
            else -> {}
        }
    }

    private fun handleMainNavigationEvents(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.HomeClick -> onHomeClicked()
            is MainScreenEvent.BackClick -> enhancedNavigationManager?.goBack()
            is MainScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
            is MainScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
            is MainScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()
            is MainScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
            is MainScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
            is MainScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
            is MainScreenEvent.RecentProjectSelected -> onRecentProjectSelected(event.projectId)
            is MainScreenEvent.DayPlanClick -> onDayPlanClicked()
            is MainScreenEvent.ContextSelected -> onContextSelected(event.name)
            is MainScreenEvent.GoToSettings -> onShowSettingsScreen()
            is MainScreenEvent.NavigateToActivityTracker -> {
                viewModelScope.launch {
                    _uiEventChannel.send(ProjectUiEvent.Navigate("activity_tracker_screen"))
                }
            }
            is MainScreenEvent.NavigateToChat -> {
                viewModelScope.launch {
                    _uiEventChannel.send(ProjectUiEvent.Navigate(CHAT_ROUTE))
                }
            }
            else -> {}
        }
    }

    private fun onSearchResultClick(projectId: String) {
        if (_isProcessingReveal.value) return
        viewModelScope.launch {
            _isProcessingReveal.value = true
            try {
                when (val result = searchAndNavigationManager.revealProjectInHierarchy(projectId)) {
                    is RevealResult.Success -> {
                        if (result.shouldFocus) {
                            searchAndNavigationManager.navigateToProject(result.projectId, uiState.value.projectHierarchy)
                        } else {
                            projectToRevealAndScroll = result.projectId
                        }
                    }
                    is RevealResult.Failure -> {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Не вдалося показати локацію"))
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
                enhancedNavigationManager?.navigateToProject(projectId, project.name)
            }
        }
    }

    private fun onHomeClicked() {
        viewModelScope.launch {
            searchAndNavigationManager.clearAllSearchState()
            planningModeManager.changeMode(PlanningMode.All)
            planningModeManager.resetExpansionStates()
            actionsHandler.collapseAllProjects(_allProjectsFlat.value)
            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    private fun onPlanningModeChange(mode: PlanningMode) {
        if (uiState.value.isSearchActive) {
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
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Тег для контексту '$name' не знайдено"))
                return@launch
            }
            val targetProject = _allProjectsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetProject != null) {
                enhancedNavigationManager?.navigateToProject(targetProject.id, targetProject.name)
            } else {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект з тегом '#$targetTag' не знайдено"))
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

    private fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            searchAndNavigationManager.onSearchQueryFromHistory(query)
            enhancedNavigationManager?.navigateToGlobalSearch(query)
            _showSearchDialog.value = false
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
}