package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import android.net.Uri
import android.util.Log
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
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.actions.ProjectActionsHandler
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.RevealResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.sync.WifiSyncManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchy
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
        private const val TAG = "MainVM_DEBUG"
    }

    private var projectToRevealAndScroll: String? = null
    private val _isProcessingReveal = MutableStateFlow(false)
    val isProcessingReveal = _isProcessingReveal.asStateFlow()

    private val hierarchyManager = ProjectHierarchyManager()
    lateinit var enhancedNavigationManager: EnhancedNavigationManager

    val canGoBack: StateFlow<Boolean> get() = enhancedNavigationManager.canGoBack
    val canGoForward: StateFlow<Boolean> get() = enhancedNavigationManager.canGoForward
    val showNavigationMenu: StateFlow<Boolean> get() = enhancedNavigationManager.showNavigationMenu

    private val _uiEventChannel = Channel<ProjectUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _allProjectsFlat = projectRepository
        .getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    val desktopAddress = wifiSyncManager.desktopAddress
    val showWifiServerDialog = wifiSyncManager.showWifiServerDialog
    val showWifiImportDialog = wifiSyncManager.showWifiImportDialog
    val wifiServerAddress = wifiSyncManager.wifiServerAddress

    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()
    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)
    private val _isBottomNavExpanded = MutableStateFlow(false)
    val isBottomNavExpanded: StateFlow<Boolean> = combine(
        _isBottomNavExpanded,
        settingsRepo.isBottomNavExpandedFlow
    ) { currentState, savedState ->
        savedState ?: currentState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()
    private val _isReadyForFiltering = MutableStateFlow(false)
    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()
    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")
    private val projectBeingMovedId = savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

    val searchQuery = searchAndNavigationManager.searchQuery
    val isSearchActive = searchAndNavigationManager.isSearchActive
    val isPendingStateRestoration = searchAndNavigationManager.isPendingStateRestoration
    val highlightedProjectId = searchAndNavigationManager.highlightedProjectId
    val currentBreadcrumbs = searchAndNavigationManager.currentBreadcrumbs
    val focusedProjectId = searchAndNavigationManager.focusedProjectId
    val hierarchySettings = searchAndNavigationManager.hierarchySettings
    val searchHistory = searchAndNavigationManager.searchHistory

    @OptIn(ExperimentalCoroutinesApi::class)
    private val debouncedSearchQueryText = searchQuery
        .map { it.text }
        .debounce(350L)
        .distinctUntilChanged()

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

    private val filterStateFlow: StateFlow<FilterState> = combine(
        _allProjectsFlat,
        debouncedSearchQueryText,
        isSearchActive,
        planningMode,
        planningSettingsState,
        _isReadyForFiltering,
        isPendingStateRestoration
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
            FilterState(flatList, "", false, PlanningMode.All, settings)
        } else {
            FilterState(flatList, query, searchActive, mode, settings)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FilterState(emptyList(), "", false, PlanningMode.All, PlanningSettingsState()),
    )

    val projectHierarchy: StateFlow<ListHierarchyData> = combine(
        filterStateFlow,
        _expandedInDailyMode,
        _expandedInMediumMode,
        _expandedInLongMode,
        focusedProjectId,
        _isProcessingReveal
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val filterState = values[0] as FilterState
        val expandedDaily = values[1] as Set<String>?
        val expandedMedium = values[2] as Set<String>?
        val expandedLong = values[3] as Set<String>?
        val isProcessingReveal = values[5] as Boolean

        val hierarchy = hierarchyManager.createProjectHierarchy(
            filterState, expandedDaily, expandedMedium, expandedLong
        )

        val projectId = projectToRevealAndScroll
        if (projectId != null && !filterState.searchActive && !isProcessingReveal) {
            val displayedProjects = flattenHierarchy(hierarchy.topLevelProjects, hierarchy.childMap)
            val index = displayedProjects.indexOfFirst { it.id == projectId }
            if (index != -1) {
                Log.d(TAG, "Знайдено проект для показу на індексі $index, надсилаємо подію прокрутки.")
                viewModelScope.launch {
                    _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
                }
                projectToRevealAndScroll = null // Використовуємо запит на прокрутку
            }
        }

        hierarchy
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            Log.e("ProjectViewModel_DEBUG", "Exception in projectHierarchy flow", e)
            emit(ListHierarchyData())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val searchResults: StateFlow<List<SearchResult>> = combine(
        filterStateFlow,
        projectHierarchy
    ) { filterState, hierarchy ->
        if (filterState.searchActive && filterState.query.isNotBlank()) {
            hierarchyManager.createSearchResults(filterState, hierarchy)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areAnyProjectsExpanded: StateFlow<Boolean> = _allProjectsFlat
        .map { projects -> projects.any { it.isExpanded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recentProjects: StateFlow<List<Project>> = projectRepository
        .getRecentProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val longDescendantsMap: StateFlow<Map<String, Boolean>> = _allProjectsFlat
        .map { hierarchyManager.createLongDescendantsMap(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val appStatistics: StateFlow<AppStatistics> = combine(
        _allProjectsFlat,
        projectRepository.getAllGoalsCountFlow()
    ) { allProjects, allGoalsCount ->
        AppStatistics(projectCount = allProjects.size, goalCount = allGoalsCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    private val _scrollTargetIndex = MutableStateFlow<Int?>(null)
    val scrollTargetIndex: StateFlow<Int?> = _scrollTargetIndex.asStateFlow()

    fun clearScrollTarget() {
        _scrollTargetIndex.value = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                withContext(Dispatchers.Main) {
                    _isBottomNavExpanded.value = savedState
                }
            }
            contextHandler.initialize()
        }
    }

    /**
     * --- ENHANCED: Обробник кліку по результату пошуку, що підтримує два режими ---
     */
    fun onSearchResultClick(projectId: String) {
        Log.d(TAG, "onSearchResultClick (Reveal) для ID: $projectId")

        if (_isProcessingReveal.value) {
            Log.w(TAG, "Операція 'reveal' вже в процесі, ігноруємо.")
            return
        }

        viewModelScope.launch {
            _isProcessingReveal.value = true
            try {
                // Викликаємо оновлену функцію, яка повертає результат з рішенням
                when (val result = searchAndNavigationManager.revealProjectInHierarchy(projectId)) {
                    is RevealResult.Success -> {
                        if (result.shouldFocus) {
                            // --- ВИпадок 1: Проект глибоко, вмикаємо ФОКУС-РЕЖИМ ---
                            Log.d(TAG, "Результат: потрібно увімкнути фокус-режим.")
                            searchAndNavigationManager.navigateToProject(result.projectId, projectHierarchy)
                        } else {
                            // --- ВИпадок 2: Проект неглибоко, показуємо в ЗВИЧАЙНОМУ РЕЖИМІ ---
                            Log.d(TAG, "Результат: показуємо у звичайному режимі, готуємо прокрутку.")
                            projectToRevealAndScroll = result.projectId
                        }
                    }
                    is RevealResult.Failure -> {
                        Log.e(TAG, "Не вдалося показати проект в ієрархії.")
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Не вдалося показати локацію"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Помилка під час операції 'reveal'", e)
            } finally {
                _isProcessingReveal.value = false
            }
        }
    }

    fun onProjectClicked(projectId: String) {
        Log.d(TAG, "onProjectClicked (Open) TRIGGERED for projectId: $projectId. Стан пошуку буде збережено через SavedStateHandle.")
        viewModelScope.launch {
            val project = _allProjectsFlat.value.find { it.id == projectId }
            if (project != null) {
                enhancedNavigationManager.navigateToProject(projectId, project.name)
            }
        }
    }

    /**
     * --- ENHANCED: Кнопка Home тепер викликає повне очищення стану пошуку ---
     */
    fun onHomeClicked() {
        viewModelScope.launch {
            searchAndNavigationManager.clearAllSearchState()
            if (planningMode.value != PlanningMode.All) {
                onPlanningModeChange(PlanningMode.All)
            }
            _expandedInDailyMode.value = emptySet()
            _expandedInMediumMode.value = emptySet()
            _expandedInLongMode.value = emptySet()
            collapseAllProjects()
            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    fun onToggleSearch(isActive: Boolean) = searchAndNavigationManager.onToggleSearch(isActive)
    fun onSearchQueryChanged(query: TextFieldValue) = searchAndNavigationManager.onSearchQueryChanged(query)
    fun onSearchQueryFromHistory(query: String) = searchAndNavigationManager.onSearchQueryFromHistory(query)
    fun navigateToProject(projectId: String) = searchAndNavigationManager.navigateToProject(projectId, projectHierarchy)
    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) = searchAndNavigationManager.navigateToBreadcrumb(breadcrumbItem)
    fun clearNavigation() = searchAndNavigationManager.clearNavigation()

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
            withContext(Dispatchers.IO) {
                settingsRepo.saveBottomNavExpanded(expanded)
            }
        }
    }

    fun onShowWifiServerDialog() = wifiSyncManager.onShowWifiServerDialog()
    fun onDismissWifiServerDialog() = wifiSyncManager.onDismissWifiServerDialog()
    fun onShowWifiImportDialog() = wifiSyncManager.onShowWifiImportDialog()
    fun onDismissWifiImportDialog() = wifiSyncManager.onDismissWifiImportDialog()
    fun performWifiImport(address: String) = wifiSyncManager.performWifiImport(address)
    fun onDesktopAddressChange(newAddress: String) = wifiSyncManager.onDesktopAddressChange(newAddress)

    fun onPlanningModeChange(mode: PlanningMode) {
        if (isSearchActive.value) {
            onToggleSearch(false)
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

    fun onShowRecentLists() {
        _showRecentListsSheet.value = true
    }

    fun onDismissRecentLists() {
        _showRecentListsSheet.value = false
    }

    fun onRecentProjectSelected(projectId: String) {
        viewModelScope.launch {
            onDismissRecentLists()
            val project = _allProjectsFlat.value.find { it.id == projectId }
            if (project != null) {
                enhancedNavigationManager.navigateToProject(projectId, project.name)
            }
        }
    }

    fun onDayPlanClicked() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDayPlan(today))
        }
    }

    fun onShowSearchDialog() { _showSearchDialog.value = true }
    fun onDismissSearchDialog() { _showSearchDialog.value = false }

    fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            searchAndNavigationManager.onSearchQueryFromHistory(query)
            enhancedNavigationManager.navigateToGlobalSearch(query)
            onDismissSearchDialog()
        }
    }

    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserUserExpandedIds.value.toMutableSet()
        if (listId in currentIds) currentIds.remove(listId) else currentIds.add(listId)
        _listChooserUserExpandedIds.value = currentIds
    }

    fun onContextSelected(contextName: String) {
        viewModelScope.launch {
            val targetTag = contextHandler.getContextTag(contextName)
            if (targetTag.isNullOrBlank()) {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Тег для контексту '$contextName' не знайдено або порожній"))
                return@launch
            }
            val targetProject = _allProjectsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetProject != null) {
                enhancedNavigationManager.navigateToProject(targetProject.id, targetProject.name)
            } else {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект з тегом '#$targetTag' не знайдено"))
            }
        }
    }

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

    fun enableFiltering() {
        _isReadyForFiltering.value = true
    }

    fun setScrollTarget(index: Int) {
        _scrollTargetIndex.value = index
    }

    fun onShowNavigationHistory() {
        enhancedNavigationManager.showNavigationMenu()
    }

    fun onHideNavigationHistory() {
        enhancedNavigationManager.hideNavigationMenu()
    }
}