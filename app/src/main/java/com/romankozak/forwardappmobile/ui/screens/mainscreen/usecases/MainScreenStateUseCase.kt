package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.AppStatistics
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.SearchResult
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@ViewModelScoped
class MainScreenStateUseCase
@Inject
constructor(
  private val searchUseCase: SearchUseCase,
  private val planningUseCase: PlanningUseCase,
  private val hierarchyUseCase: HierarchyUseCase,
  private val dialogUseCase: DialogUseCase,
  private val syncUseCase: SyncUseCase,
  private val navigationUseCase: NavigationUseCase,
  private val settingsRepository: SettingsRepository,
  private val contextHandler: ContextHandler,
  private val recentItemsRepository: RecentItemsRepository,
) {

  data class NavigationSnapshot(
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showNavigationMenu: Boolean = false,
  )

  private val defaultUiState = MutableStateFlow(MainScreenUiState())
  private val defaultHierarchy = MutableStateFlow(ListHierarchyData())
  private val defaultSearchResults = MutableStateFlow(emptyList<SearchResult>())

  private var uiStateInternal: StateFlow<MainScreenUiState> = defaultUiState
  private var projectHierarchyInternal: StateFlow<ListHierarchyData> = defaultHierarchy
  private var searchResultsInternal: StateFlow<List<SearchResult>> = defaultSearchResults

  private var isInitialized = false

  fun initialize(
    scope: CoroutineScope,
    allProjectsFlat: StateFlow<List<Project>>,
    showRecentListsSheet: StateFlow<Boolean>,
    isBottomNavExpanded: StateFlow<Boolean>,
    showSearchDialog: StateFlow<Boolean>,
    navigationSnapshot: StateFlow<NavigationSnapshot>,
  ) {
    if (isInitialized) return

    val obsidianVaultNameFlow =
      settingsRepository
        .obsidianVaultNameFlow
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), "")

    val coreHierarchyFlow =
      combine(
          planningUseCase.filterStateFlow,
          planningUseCase.planningModeManager.expandedInDailyMode,
          planningUseCase.planningModeManager.expandedInMediumMode,
          planningUseCase.planningModeManager.expandedInLongMode,
        ) { filterState, expandedDaily, expandedMedium, expandedLong ->
          hierarchyUseCase.createProjectHierarchy(
            filterState,
            expandedDaily,
            expandedMedium,
            expandedLong,
          )
        }
        .stateIn(scope, SharingStarted.Lazily, ListHierarchyData())

    val searchResultsFlow =
      combine(planningUseCase.filterStateFlow, coreHierarchyFlow) { filterState, hierarchy ->
        hierarchyUseCase.createSearchResults(filterState, hierarchy)
      }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val expensiveCalculationsFlow =
      combine(
          allProjectsFlat,
          recentItemsRepository.getRecentItems(),
          contextHandler.allContextsFlow,
        ) { allProjects, recentItems, contexts ->
          ExpensiveCalculations(
            areAnyProjectsExpanded = allProjects.any { it.isExpanded },
            recentItems = recentItems,
            allContexts = contexts,
          )
        }
        .stateIn(scope, SharingStarted.Lazily, ExpensiveCalculations())

    val coreUiStateFlow =
      combine(
          searchUseCase.subStateStack,
          searchUseCase.searchQuery,
          coreHierarchyFlow,
          searchUseCase.currentBreadcrumbs,
          planningUseCase.planningMode,
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
          dialogUseCase.dialogState,
          showRecentListsSheet,
          isBottomNavExpanded,
          showSearchDialog,
        ) { dialogState, showRecentLists, bottomNavExpanded, displaySearchDialog ->
          DialogUiState(
            dialogState = dialogState,
            showRecentListsSheet = showRecentLists,
            isBottomNavExpanded = bottomNavExpanded,
            showSearchDialog = displaySearchDialog,
          )
        }

    uiStateInternal =
      combine(
          coreUiStateFlow,
          dialogUiStateFlow,
          expensiveCalculationsFlow,
          searchResultsFlow,
          searchUseCase.searchHistory,
          planningUseCase.planningSettingsState,
          syncUseCase.syncUiState,
          navigationUseCase.isProcessingReveal,
          planningUseCase.isReadyForFiltering,
          dialogUseCase.recordForReminderDialog,
          obsidianVaultNameFlow,
          navigationSnapshot,
        ) { values ->
          val coreState = values[0] as CoreUiState
          val dialogState = values[1] as DialogUiState
          val expensiveCalcs = values[2] as ExpensiveCalculations
          @Suppress("UNCHECKED_CAST")
          val searchResults = values[3] as List<SearchResult>
          @Suppress("UNCHECKED_CAST")
          val searchHistory = values[4] as List<String>
          val planningSettings = values[5] as PlanningSettingsState
          val syncState = values[6] as SyncUseCase.SyncUiState
          val isProcessingRevealValue = values[7] as Boolean
          val isReadyForFiltering = values[8] as Boolean
          val recordForReminder =
            values[9] as com.romankozak.forwardappmobile.data.database.models.ActivityRecord?
          val obsidianVaultName = values[10] as String
          val navSnapshot = values[11] as NavigationSnapshot

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
            recentItems = expensiveCalcs.recentItems,
            allContexts = expensiveCalcs.allContexts,
            canGoBack = navSnapshot.canGoBack,
            canGoForward = navSnapshot.canGoForward,
            showNavigationMenu = navSnapshot.showNavigationMenu,
            isProcessingReveal = isProcessingRevealValue,
            isReadyForFiltering = isReadyForFiltering,
            obsidianVaultName = obsidianVaultName,
            appStatistics = AppStatistics(),
            showWifiServerDialog = syncState.showWifiServerDialog,
            wifiServerAddress = syncState.wifiServerAddress,
            showWifiImportDialog = syncState.showWifiImportDialog,
            desktopAddress = syncState.desktopAddress,
            showSearchDialog = dialogState.showSearchDialog,
            searchResults = searchResults,
            recordForReminderDialog = recordForReminder,
          )
        }
        .stateIn(scope, SharingStarted.Eagerly, MainScreenUiState())

    projectHierarchyInternal = coreHierarchyFlow
    searchResultsInternal = searchResultsFlow

    isInitialized = true
  }

  val uiState: StateFlow<MainScreenUiState>
    get() = uiStateInternal

  val projectHierarchy: StateFlow<ListHierarchyData>
    get() = projectHierarchyInternal

  val searchResults: StateFlow<List<SearchResult>>
    get() = searchResultsInternal

  private data class CoreUiState(
    val subStateStack: List<MainSubState>,
    val searchQuery: TextFieldValue,
    val projectHierarchy: ListHierarchyData,
    val currentBreadcrumbs: List<BreadcrumbItem>,
    val planningMode: PlanningMode,
  )

  private data class DialogUiState(
    val dialogState: DialogState,
    val showRecentListsSheet: Boolean,
    val isBottomNavExpanded: Boolean,
    val showSearchDialog: Boolean,
  )

  private data class ExpensiveCalculations(
    val areAnyProjectsExpanded: Boolean = false,
    val recentItems: List<RecentItem> = emptyList(),
    val allContexts: List<UiContext> = emptyList(),
  )
}
