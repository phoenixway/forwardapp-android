package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.domain.reminders.cancelForActivityRecord
import com.romankozak.forwardappmobile.domain.reminders.scheduleForActivityRecord
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.routes.CHAT_ROUTE
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager

import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.RevealResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import com.romankozak.forwardappmobile.config.FeatureToggles
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.DialogUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.PlanningUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.ProjectActionsUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.ThemingUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.NavigationUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SettingsUseCase
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.HierarchyDebugLogger
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.MainScreenStateUseCase

@HiltViewModel
class MainScreenViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val settingsRepo: SettingsRepository,
  private val searchUseCase: SearchUseCase,
  private val dialogUseCase: DialogUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val contextHandler: ContextHandler,
  private val dayManagementRepository: com.romankozak.forwardappmobile.data.repository.DayManagementRepository,
  private val activityRepository: ActivityRepository,
  private val recentItemsRepository: com.romankozak.forwardappmobile.data.repository.RecentItemsRepository,
  private val noteRepository: com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository,
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
  private val mainScreenStateUseCase: MainScreenStateUseCase,
) : ViewModel() {
  companion object {
    private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
    private const val TAG = "MainScreenVM_DEBUG"
  }

  var enhancedNavigationManager: EnhancedNavigationManager? = null
    set(value) {
      if (field === value) return

      field?.let {
        navigationStateJob?.cancel()
        navigationResultJob?.cancel()
        navigationSnapshot.value = MainScreenStateUseCase.NavigationSnapshot()
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
    MutableStateFlow(MainScreenStateUseCase.NavigationSnapshot())

  val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

  val uiState: StateFlow<MainScreenUiState>
    get() = mainScreenStateUseCase.uiState

  val lastOngoingActivity: StateFlow<com.romankozak.forwardappmobile.data.database.models.ActivityRecord?> =
      activityRepository
          .getLogStream()
          .map { log ->
              log.firstOrNull { it.startTime != null && it.endTime == null }
          }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val themeSettings: kotlinx.coroutines.flow.StateFlow<com.romankozak.forwardappmobile.ui.theme.ThemeSettings> = themingUseCase.themeSettings
      .stateIn(
          scope = viewModelScope,
          started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
          initialValue = com.romankozak.forwardappmobile.ui.theme.ThemeSettings()
      )

  private val _uiEventChannel = Channel<ProjectUiEvent>()
  val uiEventFlow = _uiEventChannel.receiveAsFlow()

  private val allProjectsFlow = projectRepository.getAllProjectsFlow()

  private val _allProjectsFlat =
    allProjectsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  private val _showRecentListsSheet = MutableStateFlow(false)
  private val _isBottomNavExpanded = MutableStateFlow(false)
  private val _showSearchDialog = MutableStateFlow(false)
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
    mainScreenStateUseCase.initialize(
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
      mainScreenStateUseCase.projectHierarchy.collect { hierarchy ->
        HierarchyDebugLogger.d {
          "projectHierarchy flow emit: topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}"
        }
      }
    }
    viewModelScope.launch {
      kotlinx.coroutines.delay(1500)
      val filterState = planningUseCase.filterStateFlow.first()
      val filterSize = filterState.flatList.size
      HierarchyDebugLogger.d { "Delayed check: filterState flat=$filterSize" }
      HierarchyDebugLogger.d {
        "Delayed check: hierarchyState topLevel=${mainScreenStateUseCase.projectHierarchy.value.topLevelProjects.size}"
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
    android.util.Log.d("ProjectRevealDebug", "Attempting to reveal projectId: $projectId")
    planningUseCase.onPlanningModeChange(PlanningMode.All)

    when (val result = searchUseCase.revealProjectInHierarchy(projectId)) {
      is RevealResult.Success -> {
        android.util.Log.d("ProjectRevealDebug", "revealProjectInHierarchy result: Success, shouldFocus=${result.shouldFocus}")
        searchUseCase.pushSubState(MainSubState.ProjectFocused(result.projectId))
        if (result.shouldFocus) {
          android.util.Log.d("ProjectRevealDebug", "Calling navigateToProject for ${result.projectId}")
          searchUseCase.navigateToProject(
            result.projectId,
            uiState.value.projectHierarchy,
          )
        } else {
          android.util.Log.d("ProjectRevealDebug", "Setting projectToRevealAndScroll to ${result.projectId}")
          projectToRevealAndScroll = result.projectId
          if (searchUseCase.isSearchActive()) {
            searchUseCase.popToSubState(MainSubState.Hierarchy)
          }
        }
      }
      is RevealResult.Failure -> {
        android.util.Log.d("ProjectRevealDebug", "revealProjectInHierarchy result: Failure")
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
          mainScreenStateUseCase.projectHierarchy,
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
  fun onEvent(event: MainScreenEvent) {
    when (event) {
      is MainScreenEvent.SearchQueryChanged -> searchUseCase.onSearchQueryChanged(event.query)
      is MainScreenEvent.SearchFromHistory -> searchUseCase.onSearchQueryFromHistory(event.query)
      is MainScreenEvent.GlobalSearchPerform -> searchUseCase.onPerformGlobalSearch(event.query)
      is MainScreenEvent.SearchResultClick -> searchUseCase.onSearchResultClick(event.projectId, uiState.value.projectHierarchy)

      is MainScreenEvent.ProjectClick -> onProjectClicked(event.projectId)
      is MainScreenEvent.ProjectMenuRequest -> dialogUseCase.onMenuRequested(event.project)
      is MainScreenEvent.ToggleProjectExpanded -> onToggleExpanded(event.project)
      is MainScreenEvent.ProjectReorder -> {
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

      is MainScreenEvent.BreadcrumbNavigation -> searchUseCase.navigateToBreadcrumb(event.breadcrumb)
      is MainScreenEvent.ClearBreadcrumbNavigation -> searchUseCase.clearNavigation()

      is MainScreenEvent.PlanningModeChange -> planningUseCase.onPlanningModeChange(event.mode)

      is MainScreenEvent.DismissDialog -> dialogUseCase.dismissDialog()
      is MainScreenEvent.AddNewProjectRequest -> {

        val focusedState = uiState.value.currentSubState as? MainSubState.ProjectFocused
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
      is MainScreenEvent.AddSubprojectRequest ->
        dialogUseCase.onAddSubprojectRequest(event.parentProject)
      is MainScreenEvent.DeleteRequest -> dialogUseCase.onDeleteRequest(event.project)
      is MainScreenEvent.MoveRequest -> {
        viewModelScope.launch {
          val route = projectActionsUseCase.getMoveProjectRoute(event.project, _allProjectsFlat.value)
          savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = event.project.id
          dialogUseCase.dismissDialog()
          _uiEventChannel.send(ProjectUiEvent.Navigate(route))
        }
      }
      is MainScreenEvent.DeleteConfirm -> {
        viewModelScope.launch {
          projectActionsUseCase.onDeleteProjectConfirmed(
            event.project,
            uiState.value.projectHierarchy.childMap,
          )
          dialogUseCase.dismissDialog()
        }
      }
      is MainScreenEvent.MoveConfirm -> {
        viewModelScope.launch {
          projectActionsUseCase.onListChooserResult(
            newParentId = event.newParentId,
            projectBeingMovedId = projectBeingMovedId.value,
            allProjects = _allProjectsFlat.value,
          )
          savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
        }
      }
      is MainScreenEvent.FullImportConfirm -> {
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
      is MainScreenEvent.ShowAboutDialog -> dialogUseCase.onShowAboutDialog()
      is MainScreenEvent.ImportFromFileRequest ->
        dialogUseCase.onImportFromFileRequested(event.uri)

      is MainScreenEvent.HomeClick -> onHomeClicked()
      is MainScreenEvent.BackClick -> handleBackNavigation()
      is MainScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
      is MainScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
      is MainScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()

      is MainScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
      is MainScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
      is MainScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
      is MainScreenEvent.RecentItemSelected -> onRecentItemSelected(event.item)
      is MainScreenEvent.RecentItemPinClick -> toggleRecentItemPin(event.item)
      is MainScreenEvent.DayPlanClick -> onDayPlanClicked()
      is MainScreenEvent.ContextSelected -> onContextSelected(event.name)

      is MainScreenEvent.EditRequest -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(event.project.id))
        }
      }
      is MainScreenEvent.AddToDayPlanRequest -> {
        viewModelScope.launch {
          val today = System.currentTimeMillis()
          val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
          dayManagementRepository.addProjectToDayPlan(dayPlan.id, event.project.id)
          _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект додано до плану дня"))
        }
      }
      is MainScreenEvent.SetReminderRequest -> {
        dialogUseCase.onSetReminderForProject(viewModelScope, event.project)
      }
      is MainScreenEvent.FocusProject -> {
        viewModelScope.launch {
          searchUseCase.navigateToProject(event.project.id, uiState.value.projectHierarchy)
          searchUseCase.pushSubState(MainSubState.ProjectFocused(event.project.id))
          dialogUseCase.dismissDialog()
        }
      }
      is MainScreenEvent.GoToSettings -> {
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) }
      }
      is MainScreenEvent.ShowSearchDialog -> _showSearchDialog.value = true
      is MainScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false

      is MainScreenEvent.ShowWifiServerDialog -> syncUseCase.onShowWifiServerDialog()
      is MainScreenEvent.ShowWifiImportDialog -> syncUseCase.onShowWifiImportDialog()
      is MainScreenEvent.ExportToFile ->
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
      is MainScreenEvent.NavigateToChat -> {
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate(CHAT_ROUTE)) }
      }
      is MainScreenEvent.NavigateToActivityTrackerScreen -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate("activity_tracker_screen"))
        }
      }
      
      is MainScreenEvent.NavigateToAiInsights -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.Navigate("ai_insights_screen"))
        }
      }

      is MainScreenEvent.NavigateToStrategicManagement -> {
        viewModelScope.launch {
          _uiEventChannel.send(ProjectUiEvent.NavigateToStrategicManagement)
        }
      }

      is MainScreenEvent.SaveSettings -> {
        settingsUseCase.saveSettings(viewModelScope, event.settings)
      }
      is MainScreenEvent.SaveAllContexts -> {
        settingsUseCase.saveAllContexts(viewModelScope, event.updatedContexts)
      }
      is MainScreenEvent.DismissWifiServerDialog -> syncUseCase.onDismissWifiServerDialog()
      is MainScreenEvent.DismissWifiImportDialog -> syncUseCase.onDismissWifiImportDialog()
      is MainScreenEvent.DesktopAddressChange ->
        syncUseCase.onDesktopAddressChange(event.address)
      is MainScreenEvent.PerformWifiImport -> syncUseCase.performWifiImport(event.address)
      is MainScreenEvent.AddProjectConfirm -> {
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
      is MainScreenEvent.CloseSearch -> searchUseCase.onCloseSearch()
      is MainScreenEvent.NavigateToProject -> navigationUseCase.onNavigateToProject(viewModelScope, event.projectId)
      is MainScreenEvent.CollapseAll -> navigationUseCase.onCollapseAll(viewModelScope)
      is MainScreenEvent.UpdateLightTheme -> themingUseCase.updateLightTheme(viewModelScope, event.themeName)
      is MainScreenEvent.UpdateDarkTheme -> themingUseCase.updateDarkTheme(viewModelScope, event.themeName)
      is MainScreenEvent.UpdateThemeMode -> themingUseCase.updateThemeMode(viewModelScope, event.themeMode)
      is MainScreenEvent.GoToReminders -> {
        viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate("reminders_screen")) }
      }
      is MainScreenEvent.OpenAttachmentsLibrary -> {
        if (FeatureToggles.attachmentsLibraryEnabled) {
          viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate("attachments_library_screen")) }
        }
      }
      is MainScreenEvent.RevealProjectInHierarchy -> {
        viewModelScope.launch { revealProject(event.projectId) }
      }
      is MainScreenEvent.OpenInboxProject -> {
        viewModelScope.launch {
          val specialProject = _allProjectsFlat.value.find { it.projectType == com.romankozak.forwardappmobile.data.database.models.ProjectType.SYSTEM }
          if (specialProject == null) {
            _uiEventChannel.send(ProjectUiEvent.ShowToast("System projects not found"))
            return@launch
          }

          val inboxProject = _allProjectsFlat.value.find { it.reservedGroup == com.romankozak.forwardappmobile.data.database.models.ReservedGroup.Inbox && it.parentId == specialProject.id }
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

  private fun onRecentItemSelected(item: com.romankozak.forwardappmobile.data.database.models.RecentItem) {
    viewModelScope.launch {
        _showRecentListsSheet.value = false
        when (item.type) {
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.PROJECT -> {
                projectRepository.getProjectById(item.target)?.let { recentItemsRepository.logProjectAccess(it) }
                val project = _allProjectsFlat.value.find { it.id == item.target }
                if (project != null) {
                    searchUseCase.popToSubState(MainSubState.Hierarchy)
                    enhancedNavigationManager?.navigateToProject(item.target, project.name)
                }
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE -> {
                noteRepository.getNoteById(item.target)?.let {
                    recentItemsRepository.logNoteAccess(it)
                }
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Legacy note editing is no longer supported"))
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE_DOCUMENT -> {
                noteDocumentRepository.getDocumentById(item.target)?.let {
                    recentItemsRepository.logNoteDocumentAccess(it)
                }
                _uiEventChannel.send(ProjectUiEvent.Navigate("note_document_screen/${item.target}"))
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.CHECKLIST -> {
                checklistRepository.getChecklistById(item.target)?.let {
                    recentItemsRepository.logChecklistAccess(it)
                }
                _uiEventChannel.send(ProjectUiEvent.Navigate("checklist_screen?checklistId=${item.target}"))
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.OBSIDIAN_LINK -> {
                val link = com.romankozak.forwardappmobile.data.database.models.RelatedLink(
                    target = item.target,
                    displayName = item.displayName,
                    type = com.romankozak.forwardappmobile.data.database.models.LinkType.OBSIDIAN
                )
                recentItemsRepository.logObsidianLinkAccess(link)
                val vaultName = settingsRepo.obsidianVaultNameFlow.first()
                val encodedNoteName = java.net.URLEncoder.encode(item.target, "UTF-8")
                val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
                _uiEventChannel.send(ProjectUiEvent.OpenUri(uri))
            }
        }
    }
  }

  private fun toggleRecentItemPin(item: com.romankozak.forwardappmobile.data.database.models.RecentItem) {
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

  override fun onCleared() {
    navigationStateJob?.cancel()
    navigationResultJob?.cancel()
    navigationUseCase.detach()
    super.onCleared()
  }
}
