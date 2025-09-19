package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.actions.ProjectActionsHandler
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.sync.WifiSyncManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
        private const val TAG = "MainVM_DEBUG" // Змінено TAG для логування

    }

    // Core managers
    private val hierarchyManager = ProjectHierarchyManager()

    lateinit var enhancedNavigationManager: EnhancedNavigationManager
    val canGoBack: StateFlow<Boolean> get() = enhancedNavigationManager.canGoBack
    val canGoForward: StateFlow<Boolean> get() = enhancedNavigationManager.canGoForward
    val showNavigationMenu: StateFlow<Boolean> get() = enhancedNavigationManager.showNavigationMenu

    // UI Event Channel
    private val _uiEventChannel = Channel<ProjectUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    // Dialog State
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    // Data flows
    private val _allProjectsFlat = projectRepository
        .getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Managers initialization
    private val searchAndNavigationManager = SearchAndNavigationManager(
        projectRepository, viewModelScope, savedStateHandle, _uiEventChannel, _allProjectsFlat
    )

    private val actionsHandler = ProjectActionsHandler(
        projectRepository, syncRepo, settingsRepo, viewModelScope,
        _allProjectsFlat, _uiEventChannel, _dialogState
    )

    private val wifiSyncManager = WifiSyncManager(
        syncRepo, settingsRepo, application, viewModelScope, _uiEventChannel
    )

    // Planning Mode
    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()

    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)

    // UI States
    private val _isBottomNavExpanded = MutableStateFlow(false)
    val isBottomNavExpanded: StateFlow<Boolean> = combine(
        _isBottomNavExpanded,
        settingsRepo.isBottomNavExpandedFlow
    ) { currentState, savedState ->
        // Use saved state if available, otherwise current state
        savedState ?: currentState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isReadyForFiltering = MutableStateFlow(false)

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()

    // List Chooser states
    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")

    private val projectBeingMovedId = savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

    // Debounced search query
    @OptIn(ExperimentalCoroutinesApi::class)
    private val debouncedSearchQueryText = searchAndNavigationManager.searchQuery
        .map { it.text }
        .debounce(350L)
        .distinctUntilChanged()

    // Settings flows
    val obsidianVaultName: StateFlow<String> = settingsRepo.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val planningSettingsState: StateFlow<PlanningSettingsState> = combine(
        settingsRepo.showPlanningModesFlow,
        settingsRepo.dailyTagFlow,
        settingsRepo.mediumTagFlow,
        settingsRepo.longTagFlow,
    ) { show, daily, medium, long ->
        PlanningSettingsState(show, daily, medium, long)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanningSettingsState(),
    )

    // Computed states
    val areAnyProjectsExpanded: StateFlow<Boolean> = _allProjectsFlat
        .map { projects -> projects.any { it.isExpanded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appStatistics: StateFlow<AppStatistics> = combine(
        _allProjectsFlat,
        projectRepository.getAllGoalsCountFlow()
    ) { allProjects, allGoalsCount ->
        AppStatistics(projectCount = allProjects.size, goalCount = allGoalsCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    val recentProjects: StateFlow<List<Project>> = projectRepository
        .getRecentProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter state flow
    private val filterStateFlow: StateFlow<FilterState> = combine(
        _allProjectsFlat,
        debouncedSearchQueryText,
        searchAndNavigationManager.isSearchActive,
        planningMode,
        planningSettingsState,
        _isReadyForFiltering
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val flatList = values[0] as List<Project>
        val query = values[1] as String
        val searchActive = values[2] as Boolean
        val mode = values[3] as PlanningMode
        val settings = values[4] as PlanningSettingsState
        val isReady = values[5] as Boolean

        if (!isReady) {
            FilterState(flatList, "", false, PlanningMode.All, settings)
        } else {
            FilterState(flatList, query, searchActive, mode, settings)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FilterState(emptyList(), "", false, PlanningMode.All, PlanningSettingsState()),
    )

    // Delegate properties to managers
    val searchQuery = searchAndNavigationManager.searchQuery
    val isSearchActive = searchAndNavigationManager.isSearchActive
    val highlightedProjectId = searchAndNavigationManager.highlightedProjectId
    val currentBreadcrumbs = searchAndNavigationManager.currentBreadcrumbs
    val focusedProjectId = searchAndNavigationManager.focusedProjectId
    val hierarchySettings = searchAndNavigationManager.hierarchySettings
    val searchHistory = searchAndNavigationManager.searchHistory


    // Main project hierarchy
    val projectHierarchy: StateFlow<ListHierarchyData> = combine(
        filterStateFlow,
        _expandedInDailyMode,
        _expandedInMediumMode,
        _expandedInLongMode,
        focusedProjectId, // ← ДОДАНО! Тепер оновлюється при зміні фокусу
    ) { values ->
        Log.d("HIERARCHY", "Rebuilding hierarchy, focusedProjectId=${values[4]}")

        @Suppress("UNCHECKED_CAST")
        val filterState = values[0] as FilterState
        val expandedDaily = values[1] as Set<String>?
        val expandedMedium = values[2] as Set<String>?
        val expandedLong = values[3] as Set<String>?
        // values[4] — це focusedProjectId, ми його ігноруємо, але він тригерить оновлення

        hierarchyManager.createProjectHierarchy(
            filterState, expandedDaily, expandedMedium, expandedLong
        )
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            android.util.Log.e("ProjectViewModel_DEBUG", "Exception in projectHierarchy flow", e)
            emit(ListHierarchyData())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    // Long descendants map
    val longDescendantsMap: StateFlow<Map<String, Boolean>> = _allProjectsFlat
        .map { hierarchyManager.createLongDescendantsMap(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // List chooser states
    val listChooserFinalExpandedIds: StateFlow<Set<String>> = combine(
        _listChooserUserExpandedIds,
        _allProjectsFlat,
        projectBeingMovedId
    ) { userExpanded, allProjects, movingId ->
        if (movingId == null) return@combine emptySet<String>()

        val projectLookup = allProjects.associateBy { it.id }
        val ancestorIds = mutableSetOf<String>()
        val visitedAncestors = mutableSetOf<String>()
        findAncestorsRecursive(movingId, projectLookup, ancestorIds, visitedAncestors)

        userExpanded + ancestorIds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> = combine(
        _allProjectsFlat,
        _listChooserFilterText,
        projectBeingMovedId
    ) { allProjects, filterText, movingId ->
        hierarchyManager.createFilteredListHierarchyForDialog(allProjects, filterText, movingId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val allContextsForDialog: StateFlow<List<UiContext>> = contextHandler.allContextsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // WiFi Sync properties
    val desktopAddress = wifiSyncManager.desktopAddress
    val showWifiServerDialog = wifiSyncManager.showWifiServerDialog
    val showWifiImportDialog = wifiSyncManager.showWifiImportDialog
    val wifiServerAddress = wifiSyncManager.wifiServerAddress

    private val _scrollTargetIndex = MutableStateFlow<Int?>(null)
    val scrollTargetIndex: StateFlow<Int?> = _scrollTargetIndex.asStateFlow()

    fun clearScrollTarget() {
        _scrollTargetIndex.value = null
    }


// File: MainScreenViewModel.kt

    init {
        initializeViewModel()
        // Use the asFlow() method to observe SavedStateHandle values via Kotlin Flows.
        savedStateHandle.getStateFlow<String?>("project_to_reveal", null)
            .filterNotNull() // Only proceed when the value is not null
            .onEach { projectId ->
                // Use viewModelScope to launch a coroutine to handle the reveal request
                viewModelScope.launch {
                    Log.d("SearchNavManager", "msvm here")
                    // Check if the planningMode flow is mutable
                    val planningModeMutable = planningMode as? MutableStateFlow<PlanningMode>
                    // Check if the projectHierarchy flow is mutable
                    val projectHierarchyState = projectHierarchy as? StateFlow<ListHierarchyData>

                    if (planningModeMutable != null && projectHierarchyState != null) {
                        searchAndNavigationManager.onSearchRevealRequest(projectId, planningModeMutable, projectHierarchyState)
                    }

                    // Clear the value to prevent re-triggering on configuration changes
                    savedStateHandle["project_to_reveal"] = null
                }
            }.launchIn(viewModelScope)
    }

    private fun initializeViewModel() {
        viewModelScope.launch {
            // Initialize search results
            filterStateFlow.collect { state ->
                if (state.searchActive && state.query.isNotBlank()) {
                    val allProjects = _allProjectsFlat.first()
                    val fullHierarchy = ListHierarchyData(
                        allProjects = allProjects,
                        topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                        childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
                    )

                    val results = hierarchyManager.createSearchResults(state, fullHierarchy)
                    _searchResults.value = results
                } else {
                    _searchResults.value = emptyList()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // FIXED: Properly initialize the bottom nav state
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                withContext(Dispatchers.Main) {
                    _isBottomNavExpanded.value = savedState
                }
            }
            contextHandler.initialize()
        }
    }


    // Search and Navigation Methods
    fun onToggleSearch(isActive: Boolean) = searchAndNavigationManager.onToggleSearch(isActive)
    fun onSearchQueryChanged(query: TextFieldValue) = searchAndNavigationManager.onSearchQueryChanged(query)
    fun onSearchQueryFromHistory(query: String) = searchAndNavigationManager.onSearchQueryFromHistory(query)
    fun onSearchResultClick(projectId: String) {
        Log.d(TAG, "onSearchResultClick (Reveal) TRIGGERED for projectId: $projectId")
        searchAndNavigationManager.onSearchResultClick(projectId, projectHierarchy, _planningMode)
    }
    fun processRevealRequest(projectId: String) = searchAndNavigationManager.processRevealRequest(projectId, _planningMode)
    fun navigateToProject(projectId: String) = searchAndNavigationManager.navigateToProject(projectId, projectHierarchy)
    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) = searchAndNavigationManager.navigateToBreadcrumb(breadcrumbItem)
    fun clearNavigation() = searchAndNavigationManager.clearNavigation(projectHierarchy)

    // Project Actions Methods
    fun addNewProject(id: String, parentId: String?, name: String) = actionsHandler.addNewProject(id, parentId, name)
    fun onDeleteProjectConfirmed(project: Project) = actionsHandler.onDeleteProjectConfirmed(project, projectHierarchy.value.childMap)
    fun onMoveProjectRequest(project: Project) = actionsHandler.onMoveProjectRequest(project, savedStateHandle)
    fun onListChooserResult(newParentId: String?) = actionsHandler.onListChooserResult(newParentId, projectBeingMovedId, savedStateHandle)
    fun onProjectReorder(fromId: String, toId: String, position: DropPosition) = actionsHandler.onProjectReorder(fromId, toId, position, isSearchActive.value)
    fun collapseAllProjects() = actionsHandler.collapseAllProjects()
    fun exportToFile() = actionsHandler.exportToFile()
    fun onFullImportConfirmed(uri: Uri) = actionsHandler.onFullImportConfirmed(uri)
    fun onBottomNavExpandedChange(expanded: Boolean) {
        viewModelScope.launch {
            _isBottomNavExpanded.value = expanded
            // Save to settings repository
            withContext(Dispatchers.IO) {
                settingsRepo.saveBottomNavExpanded(expanded)
            }
        }
    }

    // WiFi Sync Methods
    fun onShowWifiServerDialog() = wifiSyncManager.onShowWifiServerDialog()
    fun onDismissWifiServerDialog() = wifiSyncManager.onDismissWifiServerDialog()
    fun onShowWifiImportDialog() = wifiSyncManager.onShowWifiImportDialog()
    fun onDismissWifiImportDialog() = wifiSyncManager.onDismissWifiImportDialog()
    fun performWifiImport(address: String) = wifiSyncManager.performWifiImport(address)
    fun onDesktopAddressChange(newAddress: String) = wifiSyncManager.onDesktopAddressChange(newAddress)

    // Planning Mode Methods
    fun onPlanningModeChange(mode: PlanningMode) {
        if (searchAndNavigationManager.isSearchActive.value) {
            searchAndNavigationManager.onToggleSearch(false)
        }
        _planningMode.value = mode
    }

    fun onToggleExpanded(project: Project) {
        if (planningMode.value != PlanningMode.All) {
            val currentStateFlow = when (planningMode.value) {
                is PlanningMode.Daily -> _expandedInDailyMode
                is PlanningMode.Medium -> _expandedInMediumMode
                is PlanningMode.Long -> _expandedInLongMode
                else -> return
            }

            val currentExpanded = (currentStateFlow.value ?: emptySet()).toMutableSet()
            if (project.isExpanded) {
                currentExpanded.remove(project.id)
            } else {
                currentExpanded.add(project.id)
            }
            currentStateFlow.value = currentExpanded
        } else {
            actionsHandler.onToggleExpanded(project)
        }
    }

    // Dialog Methods
    fun onAddNewProjectRequest() { _dialogState.value = DialogState.AddProject(null) }
    fun onAddSubprojectRequest(parentProject: Project) { _dialogState.value = DialogState.AddProject(parentProject.id) }
    fun onMenuRequested(project: Project) { _dialogState.value = DialogState.ContextMenu(project) }
    fun onDeleteRequest(project: Project) { _dialogState.value = DialogState.ConfirmDelete(project) }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp }
    fun onImportFromFileRequested(uri: Uri) { _dialogState.value = DialogState.ConfirmFullImport(uri) }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
        _listChooserFilterText.value = ""
        _listChooserUserExpandedIds.value = emptySet()
    }

    // Navigation Methods
    fun onEditRequest(project: Project) {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(project.id))
        }
    }

    fun onShowSettingsScreen() {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToSettings)
        }
    }

/*    fun onProjectClicked(projectId: String) {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(projectId))
        }
    }*/

    // Recent Projects Methods
    fun onShowRecentLists() {
        _showRecentListsSheet.value = true
    }

    fun onDismissRecentLists() {
        _showRecentListsSheet.value = false
    }

    fun onRecentProjectSelected(projectId: String) {
        viewModelScope.launch {
            onDismissRecentLists()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(projectId))
        }
    }

    // Home Navigation Methods
    fun onDayPlanClicked() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDayPlan(today))
        }
    }

    fun onHomeClicked() {
        viewModelScope.launch {
            // Повністю очищаємо навігацію (не використовуємо clearNavigation)
            searchAndNavigationManager.clearNavigationCompletely()

            // Встановлюємо звичайний режим планування
            if (planningMode.value != PlanningMode.All) {
                onPlanningModeChange(PlanningMode.All)
            }

            // Очищаємо розгортання для всіх режимів планування
            _expandedInDailyMode.value = emptySet()
            _expandedInMediumMode.value = emptySet()
            _expandedInLongMode.value = emptySet()

            // Згортаємо всі проекти в базовому режимі
            collapseAllProjects()

            // Прокручуємо до верху
            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    // Search Dialog Methods
    fun onShowSearchDialog() { _showSearchDialog.value = true }
    fun onDismissSearchDialog() { _showSearchDialog.value = false }

//    fun onPerformGlobalSearch(query: String) {
//        if (query.isNotBlank()) {
//            searchAndNavigationManager.onSearchQueryFromHistory(query)
//            viewModelScope.launch {
//                _uiEventChannel.send(ProjectUiEvent.NavigateToGlobalSearch(query))
//                onDismissSearchDialog()
//            }
//        }
//    }

    // List Chooser Methods
    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserUserExpandedIds.value.toMutableSet()
        if (listId in currentIds) currentIds.remove(listId) else currentIds.add(listId)
        _listChooserUserExpandedIds.value = currentIds
    }

    // Context Methods
    fun onContextSelected(contextName: String) {
        viewModelScope.launch {
            val targetTag = contextHandler.getContextTag(contextName)
            if (targetTag.isNullOrBlank()) {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Тег для контексту '$contextName' не знайдено або порожній"))
                return@launch
            }
            val targetProject = _allProjectsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetProject != null) {
                _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(targetProject.id))
            } else {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект з тегом '#$targetTag' не знайдено"))
            }
        }
    }

    // Settings Methods
    fun saveSettings(
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

    fun saveAllContexts(updatedContexts: List<UiContext>) {
        viewModelScope.launch {
            val customContextsToSave = updatedContexts.filter { !it.isReserved }
            settingsRepo.saveCustomContexts(customContextsToSave)
            contextHandler.initialize()
        }
    }

    // Utility Methods
    fun enableFiltering() {
        _isReadyForFiltering.value = true
    }

    fun setScrollTarget(index: Int) {
        Log.d("VM_SCROLL", "Setting scroll target to: $index")
        _scrollTargetIndex.value = index
    }

    fun onProjectClicked(projectId: String) {
        Log.d(TAG, "onProjectClicked (Open) TRIGGERED for projectId: $projectId. Search state should be preserved.")
        viewModelScope.launch {
            val project = _allProjectsFlat.value.find { it.id == projectId }
            if (project != null) {
                enhancedNavigationManager.navigateToProject(projectId, project.name)
            } else {
                Log.w(TAG, "onProjectClicked: Project with id $projectId not found!")
            }
        }
    }


    fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            searchAndNavigationManager.onSearchQueryFromHistory(query)
            // Прямий виклик менеджера
            enhancedNavigationManager.navigateToGlobalSearch(query)
            onDismissSearchDialog()
        }
    }

    // Оновлений BackHandler
    fun onBackPressed(): Boolean {
        return when {
            focusedProjectId.value != null -> {
                clearNavigation()
                true
            }
            isSearchActive.value -> {
                onToggleSearch(false)
                true
            }
            enhancedNavigationManager?.canGoBack?.value == true -> {
                enhancedNavigationManager?.goBack()
                true
            }
            else -> false
        }
    }

    fun onForwardPressed(): Boolean {
        return enhancedNavigationManager?.goForward() ?: false
    }

    fun onShowNavigationHistory() {
        enhancedNavigationManager?.showNavigationMenu()
    }

    fun onHideNavigationHistory() {
        enhancedNavigationManager?.hideNavigationMenu()
    }

    fun navigateToHistoryEntry(index: Int) {
        enhancedNavigationManager?.navigateToHistoryEntry(index)
    }

    fun getNavigationHistory(): List<NavigationEntry> {
        return enhancedNavigationManager?.getNavigationHistory() ?: emptyList()
    }
}