package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
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
import com.romankozak.forwardappmobile.ui.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.ui.navigation.ClearCommand
import com.romankozak.forwardappmobile.ui.navigation.ClearResult
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.navigation.createClearExecutionContext
import com.romankozak.forwardappmobile.ui.screens.mainscreen.actions.ProjectActionsHandler
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.RevealResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.sync.WifiSyncManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainScreenViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val settingsRepo: SettingsRepository,
  private val application: Application,
  private val syncRepo: SyncRepository,
  private val contextHandler: ContextHandler,
  private val savedStateHandle: SavedStateHandle,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val dialogStateManager: DialogStateManager,
  private val planningModeManager: PlanningModeManager,
  private val actionsHandler: ProjectActionsHandler,
  private val clearAndNavigateHomeUseCase: ClearAndNavigateHomeUseCase,
) : ViewModel() {
  companion object {
    private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
    private const val CAME_FROM_GLOBAL_SEARCH_KEY = "came_from_global_search"
    private const val TAG = "MainScreenVM_DEBUG"
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

  private val _allProjectsFlat =
    projectRepository
      .getAllProjectsFlow()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  private val searchAndNavigationManager =
    SearchAndNavigationManager(
      projectRepository,
      viewModelScope,
      savedStateHandle,
      _uiEventChannel,
      _allProjectsFlat,
    )
  private val hierarchyManager = ProjectHierarchyManager()
  private val wifiSyncManager =
    WifiSyncManager(syncRepo, settingsRepo, application, viewModelScope, _uiEventChannel)

  private val _isProcessingReveal = MutableStateFlow(false)
  private var projectToRevealAndScroll: String? = null
  private val _showRecentListsSheet = MutableStateFlow(false)
  private val _isBottomNavExpanded = MutableStateFlow(false)
  private val _showSearchDialog = MutableStateFlow(false)
  private val _isReadyForFiltering = MutableStateFlow(false)
  private val projectBeingMovedId =
    savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

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
          savedStateHandle[CAME_FROM_GLOBAL_SEARCH_KEY] = true

          if (_isProcessingReveal.value) return@launch
          _isProcessingReveal.value = true
          try {
            when (val result = searchAndNavigationManager.revealProjectInHierarchy(value)) {
              is RevealResult.Success -> {
                pushSubState(MainSubState.ProjectFocused(value))
                if (result.shouldFocus) {
                  searchAndNavigationManager.navigateToProject(
                    result.projectId,
                    uiState.value.projectHierarchy,
                  )
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
      val popped = currentStack.last()
      _subStateStack.value = currentStack.dropLast(1)
      popped
    } else {
      null
    }
  }

  private fun replaceCurrentSubState(newState: MainSubState) {
    val currentStack = _subStateStack.value
    _subStateStack.value = currentStack.dropLast(1) + newState
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

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  private fun initializeAndCollectStates() {
    viewModelScope.launch(ioDispatcher) {
      contextHandler.initialize()
      settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
        _isBottomNavExpanded.value = savedState
      }
      _isReadyForFiltering.value = true
    }

    val debouncedSearchQueryText =
      searchAndNavigationManager.searchQuery.map { it.text }.debounce(100L).distinctUntilChanged()

    val instantSubStateStack = _subStateStack

    val planningSettingsState =
      combine(
          settingsRepo.showPlanningModesFlow,
          settingsRepo.dailyTagFlow,
          settingsRepo.mediumTagFlow,
          settingsRepo.longTagFlow,
        ) { show, daily, medium, long ->
          PlanningSettingsState(show, daily, medium, long)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, PlanningSettingsState(false, "", "", ""))

    val filterStateFlow =
      combine(
          _allProjectsFlat,
          debouncedSearchQueryText,
          instantSubStateStack.map { stack -> stack.any { it is MainSubState.LocalSearch } },
          planningModeManager.planningMode,
        ) { flatList, query, searchActive, mode ->
          FilterState(
            flatList = flatList,
            query = if (_isReadyForFiltering.value) query else "",
            searchActive = if (_isReadyForFiltering.value) searchActive else false,
            mode = if (_isReadyForFiltering.value) mode else PlanningMode.All,
            settings = planningSettingsState.value,
          )
        }
        .stateIn(
          viewModelScope,
          SharingStarted.Lazily,
          FilterState(
            flatList = emptyList(),
            query = "",
            searchActive = false,
            mode = PlanningMode.All,
            settings = PlanningSettingsState(false, "", "", ""),
          ),
        )

    val coreHierarchyFlow =
      combine(
          filterStateFlow,
          planningModeManager.expandedInDailyMode,
          planningModeManager.expandedInMediumMode,
          planningModeManager.expandedInLongMode,
        ) { filterState, expandedDaily, expandedMedium, expandedLong ->
          hierarchyManager.createProjectHierarchy(
            filterState,
            expandedDaily,
            expandedMedium,
            expandedLong,
          )
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, ListHierarchyData())

    viewModelScope.launch {
      combine(coreHierarchyFlow, filterStateFlow, _isProcessingReveal) {
          hierarchy,
          filterState,
          isProcessingReveal ->
          val projectId = projectToRevealAndScroll
          if (projectId != null && !filterState.searchActive && !isProcessingReveal) {
            projectToRevealAndScroll = null
            projectId
          } else {
            null
          }
        }
        .filterNotNull()
        .collect { projectId ->
          val displayedProjects =
            flattenHierarchy(
              coreHierarchyFlow.value.topLevelProjects,
              coreHierarchyFlow.value.childMap,
            )
          val index = displayedProjects.indexOfFirst { it.id == projectId }
          if (index != -1) {
            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
          }
        }
    }

    val searchResultsFlow =
      filterStateFlow
        .map { filterState ->
          if (filterState.searchActive && filterState.query.isNotBlank()) {
            hierarchyManager.createSearchResults(filterState, coreHierarchyFlow.value)
          } else {
            emptyList()
          }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val expensiveCalculationsFlow =
      combine(
          _allProjectsFlat,
          projectRepository.getRecentProjects(),
          contextHandler.allContextsFlow,
        ) { allProjects, recentProjects, contexts ->
          ExpensiveCalculations(
            areAnyProjectsExpanded = allProjects.any { it.isExpanded },
            recentProjects = recentProjects,
            allContexts = contexts,
          )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, ExpensiveCalculations())

    viewModelScope.launch {
      val navManager = enhancedNavigationManager

      val coreUiStateFlow =
        combine(
          instantSubStateStack,
          searchAndNavigationManager.searchQuery,
          coreHierarchyFlow,
          searchAndNavigationManager.currentBreadcrumbs,
          planningModeManager.planningMode,
        ) { subStateStack, searchQuery, hierarchy, breadcrumbs, planningMode ->
          CoreUiState(
            subStateStack = subStateStack,
            searchQuery = searchQuery,
            projectHierarchy = hierarchy,
            currentBreadcrumbs = breadcrumbs,
            planningMode = planningMode,
          )
        }

      val dialogUiStateFlow =
        combine(
          dialogStateManager.dialogState,
          _showRecentListsSheet,
          _isBottomNavExpanded,
          _showSearchDialog,
        ) { dialogState, showRecentLists, bottomNavExpanded, showSearchDialog ->
          DialogUiState(
            dialogState = dialogState,
            showRecentListsSheet = showRecentLists,
            isBottomNavExpanded = bottomNavExpanded,
            showSearchDialog = showSearchDialog,
          )
        }

      combine(
          coreUiStateFlow,
          dialogUiStateFlow,
          expensiveCalculationsFlow,
          searchResultsFlow,
          searchAndNavigationManager.searchHistory,
          planningSettingsState,
        ) { values ->
          val coreState = values[0] as CoreUiState
          val dialogState = values[1] as DialogUiState
          val expensiveCalcs = values[2] as ExpensiveCalculations

          @Suppress("UNCHECKED_CAST") val searchResults = values[3] as List<SearchResult>

          @Suppress("UNCHECKED_CAST") val searchHistory = values[4] as List<String>
          val planningSettings = values[5] as PlanningSettingsState

          MainScreenUiState(
            subStateStack = coreState.subStateStack,
            searchQuery = coreState.searchQuery,
            searchHistory = searchHistory,
            projectHierarchy = coreState.projectHierarchy,
            currentBreadcrumbs = coreState.currentBreadcrumbs,
            areAnyProjectsExpanded = expensiveCalcs.areAnyProjectsExpanded,
            planningMode = coreState.planningMode,
            planningSettings = planningSettings,
            dialogState = dialogState.dialogState,
            showRecentListsSheet = dialogState.showRecentListsSheet,
            isBottomNavExpanded = dialogState.isBottomNavExpanded,
            recentProjects = expensiveCalcs.recentProjects,
            allContexts = expensiveCalcs.allContexts,
            searchResults = searchResults,
            showSearchDialog = dialogState.showSearchDialog,
            canGoBack = navManager?.canGoBack?.value ?: false,
            canGoForward = navManager?.canGoForward?.value ?: false,
            showNavigationMenu = navManager?.showNavigationMenu?.value ?: false,
            listChooserFinalExpandedIds = emptySet(),
            filteredListHierarchyForDialog = ListHierarchyData(),
            isProcessingReveal = _isProcessingReveal.value,
            isReadyForFiltering = _isReadyForFiltering.value,
            obsidianVaultName = settingsRepo.obsidianVaultNameFlow.firstOrNull() ?: "",
            appStatistics = AppStatistics(),
            showWifiServerDialog = wifiSyncManager.showWifiServerDialog.value,
            wifiServerAddress = wifiSyncManager.wifiServerAddress.value,
            showWifiImportDialog = wifiSyncManager.showWifiImportDialog.value,
            desktopAddress = wifiSyncManager.desktopAddress.value,
          )
        }
        .distinctUntilChanged()
        .collect { newState -> _uiState.value = newState }
    }
  }

  data class CoreUiState(
    val subStateStack: List<MainSubState>,
    val searchQuery: TextFieldValue,
    val projectHierarchy: ListHierarchyData,
    val currentBreadcrumbs: List<BreadcrumbItem>,
    val planningMode: PlanningMode,
  )

  data class DialogUiState(
    val dialogState: DialogState,
    val showRecentListsSheet: Boolean,
    val isBottomNavExpanded: Boolean,
    val showSearchDialog: Boolean,
  )

  data class ExpensiveCalculations(
    val areAnyProjectsExpanded: Boolean = false,
    val recentProjects: List<Project> = emptyList(),
    val allContexts: List<UiContext> = emptyList(),
  )

  private fun isSearchActive(): Boolean {
    return _subStateStack.value.any { it is MainSubState.LocalSearch }
  }

  fun onEvent(event: MainScreenEvent) {
    when (event) {
      is MainScreenEvent.SearchQueryChanged -> {
        searchAndNavigationManager.onSearchQueryChanged(event.query)
        if (!isSearchActive()) {
          pushSubState(MainSubState.LocalSearch(event.query.text))
        } else {
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
            allProjects = _allProjectsFlat.value,
          )
        }
      }

      is MainScreenEvent.BreadcrumbNavigation -> {
        searchAndNavigationManager.navigateToBreadcrumb(event.breadcrumb)
        // This line ensures the UI's focus state is updated to the new project.
        replaceCurrentSubState(MainSubState.ProjectFocused(event.breadcrumb.id))
      }
      is MainScreenEvent.ClearBreadcrumbNavigation -> {
        searchAndNavigationManager.clearNavigation()
        popToSubState(MainSubState.Hierarchy)
      }

      is MainScreenEvent.PlanningModeChange -> onPlanningModeChange(event.mode)

      is MainScreenEvent.DismissDialog -> dialogStateManager.dismissDialog()
      is MainScreenEvent.AddNewProjectRequest -> {

        val focusedState = uiState.value.currentSubState as? MainSubState.ProjectFocused
        if (focusedState != null) {

          val parentProject = _allProjectsFlat.value.find { it.id == focusedState.projectId }
          if (parentProject != null) {

            dialogStateManager.onAddSubprojectRequest(parentProject)
          } else {

            dialogStateManager.onAddNewProjectRequest()
          }
        } else {

          dialogStateManager.onAddNewProjectRequest()
        }
      }
      is MainScreenEvent.AddSubprojectRequest ->
        dialogStateManager.onAddSubprojectRequest(event.parentProject)
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
          actionsHandler.onDeleteProjectConfirmed(
            event.project,
            uiState.value.projectHierarchy.childMap,
          )
          dialogStateManager.dismissDialog()
        }
      }
      is MainScreenEvent.MoveConfirm -> {
        viewModelScope.launch {
          actionsHandler.onListChooserResult(
            newParentId = event.newParentId,
            projectBeingMovedId = projectBeingMovedId.value,
            allProjects = _allProjectsFlat.value,
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
      is MainScreenEvent.ImportFromFileRequest ->
        dialogStateManager.onImportFromFileRequested(event.uri)

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
      is MainScreenEvent.ExportToFile ->
        viewModelScope.launch {
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
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate(CHAT_ROUTE)) }
      }
      is MainScreenEvent.NavigateToActivityTracker -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate("activity_tracker_screen"))
        }
      }

      is MainScreenEvent.NavigateToAiInsights -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate("ai_insights_screen"))
        }
      }

      is MainScreenEvent.SaveSettings -> {
        saveSettings(
          show = event.show,
          daily = event.daily,
          medium = event.medium,
          long = event.long,
          vaultName = event.vaultName,
        )
      }
      is MainScreenEvent.SaveAllContexts -> {
        saveAllContexts(event.updatedContexts)
      }
      is MainScreenEvent.DismissWifiServerDialog -> wifiSyncManager.onDismissWifiServerDialog()
      is MainScreenEvent.DismissWifiImportDialog -> wifiSyncManager.onDismissWifiImportDialog()
      is MainScreenEvent.DesktopAddressChange ->
        wifiSyncManager.onDesktopAddressChange(event.address)
      is MainScreenEvent.PerformWifiImport -> wifiSyncManager.performWifiImport(event.address)
      is MainScreenEvent.AddProjectConfirm -> {
        onAddNewProjectConfirmed(event.name, event.parentId)
        dialogStateManager.dismissDialog()
      }
      is MainScreenEvent.CloseSearch -> onCloseSearch()
      is MainScreenEvent.NavigateToProject -> onNavigateToProject(event.projectId)
      is MainScreenEvent.CollapseAll -> onCollapseAll()
    }
  }

  private fun onCloseSearch() {
    if (_isProcessingReveal.value) return

    viewModelScope.launch {
      _isProcessingReveal.value = true
      try {
        val result =
          clearAndNavigateHomeUseCase.execute(
            command = ClearCommand.CloseSearch,
            context = createClearContext(),
          )

        if (result is ClearResult.Error) {
          _uiEventChannel.send(
            ProjectUiEvent.ShowToast("Помилка закриття пошуку: ${result.message}")
          )
        }
      } finally {
        _isProcessingReveal.value = false
      }
    }
  }

  private fun handleBackNavigation() {
    val currentStack = _subStateStack.value
    when {
      currentStack.lastOrNull() is MainSubState.ProjectFocused -> {
        popSubState()
        searchAndNavigationManager.clearNavigation()
      }
      currentStack.lastOrNull() is MainSubState.LocalSearch -> {
        popSubState()
        searchAndNavigationManager.onSearchQueryChanged(TextFieldValue(""))
      }
      uiState.value.currentBreadcrumbs.isNotEmpty() -> {
        searchAndNavigationManager.clearNavigation()
      }
      uiState.value.areAnyProjectsExpanded -> {
        viewModelScope.launch { actionsHandler.collapseAllProjects(_allProjectsFlat.value) }
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
            searchAndNavigationManager.navigateToProject(
              result.projectId,
              uiState.value.projectHierarchy,
            )
            replaceCurrentSubState(MainSubState.ProjectFocused(result.projectId))
            searchAndNavigationManager.onSearchQueryChanged(TextFieldValue(""))
            projectToRevealAndScroll = result.projectId
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
    if (_isProcessingReveal.value) return

    viewModelScope.launch {
      _isProcessingReveal.value = true
      try {
        val result =
          clearAndNavigateHomeUseCase.execute(
            command = ClearCommand.Home,
            context = createClearContext(),
          )

        if (result is ClearResult.Error) {
          _uiEventChannel.send(ProjectUiEvent.ShowToast("Помилка навігації: ${result.message}"))
        }
      } finally {
        _isProcessingReveal.value = false
      }
    }
  }

  private fun onPerformGlobalSearch(query: String) {
    if (query.isNotBlank()) {
      searchAndNavigationManager.onSearchQueryFromHistory(query)
      if (isSearchActive()) {
        popToSubState(MainSubState.Hierarchy)
        searchAndNavigationManager.onToggleSearch(isActive = false)
      }
      enhancedNavigationManager?.navigateToGlobalSearch(query)
      _showSearchDialog.value = false
    }
  }

  private fun onPlanningModeChange(mode: PlanningMode) {
    if (isSearchActive()) {
      popToSubState(MainSubState.Hierarchy)
      searchAndNavigationManager.onToggleSearch(isActive = false)
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
      withContext(ioDispatcher) { settingsRepo.saveBottomNavExpanded(isExpanded) }
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
    viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) }
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

  private fun onAddNewProjectConfirmed(name: String, parentId: String?) {
    if (name.isBlank()) {
      viewModelScope.launch {
        _uiEventChannel.send(ProjectUiEvent.ShowToast("Назва проекту не може бути порожньою"))
      }
      return
    }

    val newProjectId = UUID.randomUUID().toString()

    viewModelScope.launch(ioDispatcher) {
      actionsHandler.addNewProject(
        id = newProjectId,
        name = name,
        parentId = parentId,
        allProjects = _allProjectsFlat.value,
      )
    }
  }

  private fun createClearContext() =
    createClearExecutionContext(
      currentProjects = _allProjectsFlat.value,
      subStateStack = _subStateStack,
      searchAndNavigationManager = searchAndNavigationManager,
      planningModeManager = planningModeManager,
      enhancedNavigationManager = enhancedNavigationManager,
      uiEventChannel = _uiEventChannel,
    )

  private fun onNavigateToProject(projectId: String) {
    if (_isProcessingReveal.value) return

    viewModelScope.launch {
      _isProcessingReveal.value = true
      try {
        val project = _allProjectsFlat.value.find { it.id == projectId }
        val projectName = project?.name ?: "Unknown Project"

        val result =
          clearAndNavigateHomeUseCase.execute(
            command = ClearCommand.NavigateToProject(projectId, projectName),
            context = createClearContext(),
          )

        if (result is ClearResult.Error) {
          _uiEventChannel.send(
            ProjectUiEvent.ShowToast("Помилка навігації до проєкту: ${result.message}")
          )
        }
      } finally {
        _isProcessingReveal.value = false
      }
    }
  }

  private fun onCollapseAll() {
    if (_isProcessingReveal.value) return

    viewModelScope.launch {
      _isProcessingReveal.value = true
      try {
        val result =
          clearAndNavigateHomeUseCase.execute(
            command = ClearCommand.CollapseAll,
            context = createClearContext(),
          )

        when (result) {
          is ClearResult.Success -> {
            _uiEventChannel.send(ProjectUiEvent.ShowToast("Всі проєкти згорнуто"))
          }
          is ClearResult.Error -> {
            _uiEventChannel.send(ProjectUiEvent.ShowToast("Помилка згортання: ${result.message}"))
          }
        }
      } finally {
        _isProcessingReveal.value = false
      }
    }
  }
}
