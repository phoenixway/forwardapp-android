package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.features.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.navigation.routes.COMMAND_DECK_ROUTE

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation.RevealResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.flattenHierarchy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.database.models.RecentItemType
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.DialogUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.PlanningUseCase
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedProjectKeys
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ProjectActionsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ThemingUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.NavigationUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SettingsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyDebugLogger
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ProjectHierarchyScreenStateUseCase
import com.romankozak.forwardappmobile.features.navigation.NavTarget
import com.romankozak.forwardappmobile.ui.theme.ThemeSettings
import kotlinx.coroutines.delay
import java.net.URLEncoder

@HiltViewModel
class ProjectHierarchyScreenViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val settingsRepo: SettingsRepository,
  private val searchUseCase: SearchUseCase,
  private val dialogUseCase: DialogUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val contextHandler: ContextHandler,
  private val dayManagementRepository: DayManagementRepository,
  private val activityRepository: ActivityRepository,
  private val recentItemsRepository: RecentItemsRepository,
  private val noteRepository: LegacyNoteRepository,
  private val noteDocumentRepository: NoteDocumentRepository,
  private val checklistRepository: ChecklistRepository,

  private val application: Application,
  private val savedStateHandle: SavedStateHandle,
  private val planningUseCase: PlanningUseCase,
  private val syncUseCase: SyncUseCase,
  private val projectActionsUseCase: ProjectActionsUseCase,
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

  val uiState: StateFlow<ProjectHierarchyScreenUiState>
    get() = projectHierarchyScreenStateUseCase.uiState

  val lastOngoingActivity: StateFlow<ActivityRecord?> =
      activityRepository
          .getLogStream()
          .map { log ->
              log.firstOrNull { it.startTime != null && it.endTime == null }
          }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val themeSettings: StateFlow<ThemeSettings> = themingUseCase.themeSettings
      .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = ThemeSettings()
      )

  private val _uiEventChannel = Channel<ProjectUiEvent>()
  val uiEventFlow = _uiEventChannel.receiveAsFlow()

  private val allProjectsFlow = projectRepository.getAllProjectsFlow()

  private val _allProjectsFlat =
    allProjectsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  private val _showRecentListsSheet = MutableStateFlow(false)
  private val _isBottomNavExpanded = MutableStateFlow(false)
  private val _showSearchDialog = MutableStateFlow(false)
  private val pendingChooserAction = MutableStateFlow<PendingChooserAction?>(null)
  private val projectBeingMovedId =
    savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)
  init {
    searchUseCase.initialize(
      scope = viewModelScope,
      uiEventChannel = _uiEventChannel,
      allProjectsFlat = _allProjectsFlat
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
          withContext(ioDispatcher) { projectRepository.findProjectIdsByTag(tag).firstOrNull() }
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
        Log.d("ProjectRevealDebug", "revealProjectInHierarchy result: Success, shouldFocus=${result.shouldFocus}")
        searchUseCase.pushSubState(ProjectHierarchyScreenSubState.ProjectFocused(result.projectId))
        if (result.shouldFocus) {
          Log.d("ProjectRevealDebug", "Calling navigateToProject for ${result.projectId}")
          searchUseCase.navigateToProject(
            result.projectId,
            uiState.value.projectHierarchy,
          )
        } else {
          Log.d("ProjectRevealDebug", "Setting projectToRevealAndScroll to ${result.projectId}")
          projectToRevealAndScroll = result.projectId
          if (searchUseCase.isSearchActive()) {
            searchUseCase.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
          }
        }
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
  fun onEvent(event: ProjectHierarchyScreenEvent) {
    when (event) {
      is ProjectHierarchyScreenEvent.SearchQueryChanged -> searchUseCase.onSearchQueryChanged(event.query)
      is ProjectHierarchyScreenEvent.SearchFromHistory -> searchUseCase.onSearchQueryFromHistory(event.query)
      is ProjectHierarchyScreenEvent.GlobalSearchPerform -> searchUseCase.onPerformGlobalSearch(event.query)
      is ProjectHierarchyScreenEvent.SearchResultClick -> searchUseCase.onSearchResultClick(event.projectId, uiState.value.projectHierarchy)

      is ProjectHierarchyScreenEvent.ProjectClick -> onProjectClicked(event.projectId)
      is ProjectHierarchyScreenEvent.ProjectMenuRequest -> dialogUseCase.onMenuRequested(event.project)
      is ProjectHierarchyScreenEvent.ToggleProjectExpanded -> onToggleExpanded(event.project)
      is ProjectHierarchyScreenEvent.ProjectReorder -> {
        viewModelScope.launch {
          projectActionsUseCase.onProjectReorder(
            fromId = event.fromId,
            toId = event.toId,
            position = event.position,
            isSearchActive = searchUseCase.isSearchActive(),
            allProjects = _allProjectsFlat.value,
          )
        }
      }

      is ProjectHierarchyScreenEvent.BreadcrumbNavigation -> searchUseCase.navigateToBreadcrumb(event.breadcrumb)
      is ProjectHierarchyScreenEvent.ClearBreadcrumbNavigation -> searchUseCase.clearNavigation()

      is ProjectHierarchyScreenEvent.PlanningModeChange -> {
        if (uiState.value.featureToggles[FeatureFlag.PlanningModes] == true) {
          planningUseCase.onPlanningModeChange(event.mode)
        }
      }

      is ProjectHierarchyScreenEvent.DismissDialog -> dialogUseCase.dismissDialog()
      is ProjectHierarchyScreenEvent.AddNewProjectRequest -> {

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
      is ProjectHierarchyScreenEvent.AddNoteDocumentRequest -> createNoteInInbox()
      is ProjectHierarchyScreenEvent.AddChecklistRequest -> {
        createChecklistInInbox()
      }
      is ProjectHierarchyScreenEvent.ListChooserResult -> {
        val targetProjectId = event.projectId?.takeUnless { it.isBlank() || it == "root" }
        when (val action = pendingChooserAction.value) {
          PendingChooserAction.MoveProject -> handleMoveConfirm(event.projectId)
          null -> {
            handleMoveConfirm(event.projectId)
          }
        }
        pendingChooserAction.value = null
      }
      is ProjectHierarchyScreenEvent.AddSubprojectRequest ->
        dialogUseCase.onAddSubprojectRequest(event.parentProject)
      is ProjectHierarchyScreenEvent.DeleteRequest -> dialogUseCase.onDeleteRequest(event.project)
      is ProjectHierarchyScreenEvent.MoveRequest -> {
        viewModelScope.launch {
          val target = projectActionsUseCase.getMoveProjectRoute(event.project, _allProjectsFlat.value)
          savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = event.project.id
          pendingChooserAction.value = PendingChooserAction.MoveProject
          dialogUseCase.dismissDialog()
          _uiEventChannel.send(ProjectUiEvent.Navigate(target))
        }
      }
      is ProjectHierarchyScreenEvent.DeleteConfirm -> {
        viewModelScope.launch {
          projectActionsUseCase.onDeleteProjectConfirmed(
            event.project,
            uiState.value.projectHierarchy.childMap,
          )
          dialogUseCase.dismissDialog()
        }
      }
      is ProjectHierarchyScreenEvent.MoveConfirm -> {
        handleMoveConfirm(event.newParentId)
      }
      is ProjectHierarchyScreenEvent.FullImportConfirm -> {
        viewModelScope.launch {
          val result = projectActionsUseCase.onFullImportConfirmed(event.uri)
          dialogUseCase.dismissDialog()
          _uiEventChannel.send(
            if (result.isSuccess) {
              ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import successful")
            } else {
              ProjectUiEvent.ShowToast("Import error: ${result.exceptionOrNull()?.message}")
            }
          )
        }
      }
      is ProjectHierarchyScreenEvent.ShowAboutDialog -> dialogUseCase.onShowAboutDialog()
      is ProjectHierarchyScreenEvent.ImportFromFileRequest ->
        dialogUseCase.onImportFromFileRequested(event.uri)

      is ProjectHierarchyScreenEvent.SelectiveImportFromFileRequest -> {
        viewModelScope.launch {
          _uiEventChannel.send(
            ProjectUiEvent.Navigate(
              NavTarget.ImportExport(uri = event.uri.toString())
            )
          )
        }
      }

      is ProjectHierarchyScreenEvent.HomeClick -> onHomeClicked()
      is ProjectHierarchyScreenEvent.BackClick -> handleBackNavigation()
      is ProjectHierarchyScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
      is ProjectHierarchyScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
      is ProjectHierarchyScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()

      is ProjectHierarchyScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
      is ProjectHierarchyScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
      is ProjectHierarchyScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
      is ProjectHierarchyScreenEvent.RecentItemSelected -> onRecentItemSelected(event.item)
      is ProjectHierarchyScreenEvent.RecentItemPinClick -> toggleRecentItemPin(event.item)
      is ProjectHierarchyScreenEvent.DayPlanClick -> onDayPlanClicked()
      is ProjectHierarchyScreenEvent.ContextSelected -> onContextSelected(event.name)
      is ProjectHierarchyScreenEvent.CommandDeckClick -> {
        enhancedNavigationManager?.navigate(COMMAND_DECK_ROUTE) {
          popUpTo(COMMAND_DECK_ROUTE) { inclusive = true }
          launchSingleTop = true
        }
      }

      is ProjectHierarchyScreenEvent.EditRequest -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(event.project.id))
        }
      }
      is ProjectHierarchyScreenEvent.AddToDayPlanRequest -> {
        viewModelScope.launch {
          val today = System.currentTimeMillis()
          val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
          dayManagementRepository.addProjectToDayPlan(dayPlan.id, event.project.id)
          _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект додано до плану дня"))
        }
      }
      is ProjectHierarchyScreenEvent.SetReminderRequest -> {
        dialogUseCase.onSetReminderForProject(viewModelScope, event.project)
      }
      is ProjectHierarchyScreenEvent.FocusProject -> {
        viewModelScope.launch {
          searchUseCase.navigateToProject(event.project.id, uiState.value.projectHierarchy)
          searchUseCase.pushSubState(ProjectHierarchyScreenSubState.ProjectFocused(event.project.id))
          dialogUseCase.dismissDialog()
        }
      }
      is ProjectHierarchyScreenEvent.GoToSettings -> {
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) }
      }
      is ProjectHierarchyScreenEvent.ShowSearchDialog -> {
        searchUseCase.onSearchQueryChanged(TextFieldValue(""))
        searchUseCase.onToggleSearch(true)
      }
      is ProjectHierarchyScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false

      is ProjectHierarchyScreenEvent.ShowWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiServerDialog()
      is ProjectHierarchyScreenEvent.ShowWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiImportDialog()
      is ProjectHierarchyScreenEvent.WifiPush -> {
        if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) {
          syncUseCase.performWifiPush(event.address)
        }
      }
      is ProjectHierarchyScreenEvent.ExportToFile ->
        viewModelScope.launch {
          val result = projectActionsUseCase.exportToFile()
          _uiEventChannel.send(
            if (result.isSuccess) {
              ProjectUiEvent.ShowToast(result.getOrNull() ?: "Export successful")
            } else {
              ProjectUiEvent.ShowToast("Export error: ${result.exceptionOrNull()?.message}")
            }
          )
        }
      is ProjectHierarchyScreenEvent.ExportAttachments -> {
          viewModelScope.launch {
              val result = projectActionsUseCase.exportAttachments()
              _uiEventChannel.send(
                  if (result.isSuccess) {
                      ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments export successful")
                  } else {
                      ProjectUiEvent.ShowToast("Attachments export error: ${result.exceptionOrNull()?.message}")
                  }
              )
          }
      }
      is ProjectHierarchyScreenEvent.ImportAttachmentsFromFile -> {
          Log.d("SyncRepo_AttachmentsImport", "MainScreenViewModel received ImportAttachmentsFromFile event with uri=${event.uri}")
          viewModelScope.launch {
              Log.d("SyncRepo_AttachmentsImport", "Starting attachment import coroutine")
              val result = projectActionsUseCase.importAttachments(event.uri)
              Log.d("SyncRepo_AttachmentsImport", "Import completed with result: isSuccess=${result.isSuccess}, message=${result.getOrNull()}")
              dialogUseCase.dismissDialog()
              _uiEventChannel.send(
                  if (result.isSuccess) {
                      ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments import successful")
                  } else {
                      ProjectUiEvent.ShowToast("Attachments import error: ${result.exceptionOrNull()?.message}")
                  }
              )
          }
      }
      is ProjectHierarchyScreenEvent.NavigateToChat -> {
        if (uiState.value.featureToggles[FeatureFlag.AiChat] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Chat))
          }
        }
      }
      is ProjectHierarchyScreenEvent.NavigateToActivityTrackerScreen -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Tracker))
        }
      }
      
      is ProjectHierarchyScreenEvent.NavigateToAiInsights -> {
        if (uiState.value.featureToggles[FeatureFlag.AiInsights] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AiInsights))
          }
        }
      }
      is ProjectHierarchyScreenEvent.NavigateToLifeState -> {
        if (uiState.value.featureToggles[FeatureFlag.AiLifeManagement] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.LifeState))
          }
        }
      }

      is ProjectHierarchyScreenEvent.NavigateToTacticsScreen -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.TacticalManagement))
        }
      }

      is ProjectHierarchyScreenEvent.NavigateToStrategicManagement -> {
        if (uiState.value.featureToggles[FeatureFlag.StrategicManagement] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToStrategicManagement)
          }
        }
      }

      is ProjectHierarchyScreenEvent.SaveSettings -> {
        settingsUseCase.saveSettings(viewModelScope, event.settings)
      }
      is ProjectHierarchyScreenEvent.SaveAllContexts -> {
        settingsUseCase.saveAllContexts(viewModelScope, event.updatedContexts)
      }
      is ProjectHierarchyScreenEvent.DismissWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiServerDialog()
      is ProjectHierarchyScreenEvent.DismissWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiImportDialog()
      is ProjectHierarchyScreenEvent.DesktopAddressChange ->
        syncUseCase.onDesktopAddressChange(event.address)
      is ProjectHierarchyScreenEvent.PerformWifiImport -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.performWifiImport(event.address)
      is ProjectHierarchyScreenEvent.AddProjectConfirm -> {
        viewModelScope.launch {
          projectActionsUseCase.addNewProject(
            id = UUID.randomUUID().toString(),
            name = event.name,
            parentId = event.parentId,
            allProjects = _allProjectsFlat.value,
          )
        }
        dialogUseCase.dismissDialog()
      }
      is ProjectHierarchyScreenEvent.CloseSearch -> searchUseCase.onCloseSearch()
      is ProjectHierarchyScreenEvent.NavigateToProject -> navigationUseCase.onNavigateToProject(viewModelScope, event.projectId)
      is ProjectHierarchyScreenEvent.CollapseAll -> navigationUseCase.onCollapseAll(viewModelScope)
      is ProjectHierarchyScreenEvent.UpdateLightTheme -> themingUseCase.updateLightTheme(viewModelScope, event.themeName)
      is ProjectHierarchyScreenEvent.UpdateDarkTheme -> themingUseCase.updateDarkTheme(viewModelScope, event.themeName)
      is ProjectHierarchyScreenEvent.UpdateThemeMode -> themingUseCase.updateThemeMode(viewModelScope, event.themeMode)
      is ProjectHierarchyScreenEvent.GoToReminders -> {
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Reminders)) }
      }
      is ProjectHierarchyScreenEvent.OpenAttachmentsLibrary -> {
        if (uiState.value.featureToggles[FeatureFlag.AttachmentsLibrary] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AttachmentsLibrary))
          }
        }
      }
      is ProjectHierarchyScreenEvent.AddScriptRequest -> {
        if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptEditor()))
          }
        }
      }
      is ProjectHierarchyScreenEvent.OpenScriptsLibrary -> {
        if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
          viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptsLibrary))
          }
        }
      }
      is ProjectHierarchyScreenEvent.RevealProjectInHierarchy -> {
        viewModelScope.launch { revealProject(event.projectId) }
      }
      is ProjectHierarchyScreenEvent.OpenInboxProject -> {
        viewModelScope.launch {
          val inboxProject =
              _allProjectsFlat.value.firstOrNull { it.systemKey == ReservedProjectKeys.INBOX }
                  ?: _allProjectsFlat.value.firstOrNull {
                      it.name.equals("Inbox", ignoreCase = true) && it.systemKey != ReservedProjectKeys.TODAY
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
        collapseAllProjects = { viewModelScope.launch { projectActionsUseCase.collapseAllProjects(_allProjectsFlat.value) } },
        goBack = { enhancedNavigationManager?.goBack() }
    )
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
    navigationUseCase.onNavigateHome(viewModelScope)
  }



  private fun onToggleExpanded(project: Project) {
    viewModelScope.launch {
      if (uiState.value.planningMode == PlanningMode.All) {
        projectActionsUseCase.onToggleExpanded(project)
      } else {
        planningUseCase.toggleExpandedInPlanningMode(project)
      }
    }
  }

  private fun onBottomNavExpandedChange(isExpanded: Boolean) {
    viewModelScope.launch {
      _isBottomNavExpanded.value = isExpanded
      projectActionsUseCase.onBottomNavExpandedChange(isExpanded)
    }
  }

  private fun onRecentItemSelected(item: RecentItem) {
    viewModelScope.launch {
        _showRecentListsSheet.value = false
        when (item.type) {
            RecentItemType.PROJECT -> {
                projectRepository.getProjectById(item.target)?.let { recentItemsRepository.logProjectAccess(it) }
                val project = _allProjectsFlat.value.find { it.id == item.target }
                if (project != null) {
                    searchUseCase.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                    enhancedNavigationManager?.navigateToProject(item.target, project.name)
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
                    NavTarget.NoteDocument(id = item.target)
                  )
                )
            }
            RecentItemType.CHECKLIST -> {
                checklistRepository.getChecklistById(item.target)?.let {
                    recentItemsRepository.logChecklistAccess(it)
                }
                _uiEventChannel.send(
                  ProjectUiEvent.Navigate(
                    NavTarget.Checklist(id = item.target)
                  )
                )
            }
            RecentItemType.OBSIDIAN_LINK -> {
                val link = RelatedLink(
                    target = item.target,
                    displayName = item.displayName,
                    type = LinkType.OBSIDIAN
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
      projectActionsUseCase.onListChooserResult(
        newParentId = newParentId,
        projectBeingMovedId = projectBeingMovedId.value,
        allProjects = _allProjectsFlat.value,
      )
      savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
    }
  }

  private fun createNoteInInbox() {
    val inboxProjectId =
      _allProjectsFlat.value.firstOrNull { it.systemKey == ReservedProjectKeys.INBOX }?.id
    if (inboxProjectId == null) {
      viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено")) }
      return
    }
    viewModelScope.launch {
      val documentId = noteDocumentRepository.createDocument(
        name = "Нова нотатка",
        projectId = inboxProjectId,
        content = "",
      )
      _uiEventChannel.send(
        ProjectUiEvent.Navigate(
          NavTarget.NoteDocument(id = documentId, startEdit = true)
        )
      )
    }
  }

  private fun createChecklistInInbox() {
    val inboxProjectId =
      _allProjectsFlat.value.firstOrNull { it.systemKey == ReservedProjectKeys.INBOX }?.id
    if (inboxProjectId == null) {
      viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено")) }
      return
    }
    viewModelScope.launch {
      val checklistId = checklistRepository.createChecklist(name = "Новий чекліст", projectId = inboxProjectId)
      _uiEventChannel.send(
        ProjectUiEvent.Navigate(
          NavTarget.Checklist(id = checklistId)
        )
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
    navigationUseCase.onNavigateToProject(viewModelScope, projectId)
  }

  suspend fun getInboxProjectId(): String? = withContext(ioDispatcher) {
    val allProjects = _allProjectsFlat.first()
    val inboxProject =
        allProjects.firstOrNull { it.systemKey == ReservedProjectKeys.INBOX }
            ?: allProjects.firstOrNull {
                it.name.equals("Inbox", ignoreCase = true) && it.systemKey != ReservedProjectKeys.TODAY
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
