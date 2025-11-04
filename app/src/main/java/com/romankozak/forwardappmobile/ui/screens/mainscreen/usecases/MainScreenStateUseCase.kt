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
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val expansionState =
      combine(
          planningUseCase.planningModeManager.expandedInDailyMode,
          planningUseCase.planningModeManager.expandedInMediumMode,
          planningUseCase.planningModeManager.expandedInLongMode,
        ) { expandedDaily, expandedMedium, expandedLong ->
          ExpansionState(
            expandedDaily = expandedDaily,
            expandedMedium = expandedMedium,
            expandedLong = expandedLong,
          )
        }
        .stateIn(
          scope,
          SharingStarted.Eagerly,
          ExpansionState(emptySet(), emptySet(), emptySet()),
        )

    val hierarchyState =
      HierarchyStateBuilder(hierarchyUseCase)
        .buildHierarchyState(
          scope = scope,
          filterStates = planningUseCase.filterStateFlow,
          expansionStates = expansionState,
        )

    scope.launch {
      hierarchyState.collect { hierarchy ->
        HierarchyDebugLogger.d {
          "coreHierarchyFlow emit -> topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}"
        }
      }
    }
    scope.launch {
      planningUseCase.filterStateFlow.collect { state ->
        HierarchyDebugLogger.d {
          "filterState observed in MainScreenStateUseCase flat=${state.flatList.size} mode=${state.mode} ready=${state.isReady}"
        }
      }
    }

    val searchResultsFlow =
      combine(planningUseCase.filterStateFlow, hierarchyState) { filterState, hierarchy ->
        if (!filterState.isReady) {
          emptyList()
        } else {
          hierarchyUseCase.createSearchResults(filterState, hierarchy)
        }
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
          hierarchyState,
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
          contextHandler.contextMarkerToEmojiMap,
        ) { values ->
          val coreState = values[0] as CoreUiState
          val dialogState = values[1] as DialogUiState
          val expensiveCalcs = values[2] as ExpensiveCalculations
          @Suppress("UNCHECKED_CAST") val searchResults = values[3] as List<SearchResult>
          @Suppress("UNCHECKED_CAST") val searchHistory = values[4] as List<String>
          val planningSettings = values[5] as PlanningSettingsState
          val syncState = values[6] as SyncUseCase.SyncUiState
          val isProcessingRevealValue = values[7] as Boolean
          val isReadyForFiltering = values[8] as Boolean
          val recordForReminder =
            values[9] as com.romankozak.forwardappmobile.data.database.models.ActivityRecord?
          val obsidianVaultName = values[10] as String
          val navSnapshot = values[11] as NavigationSnapshot
          @Suppress("UNCHECKED_CAST")
          val contextMarkerToEmojiMap = values[12] as Map<String, String>

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
            contextMarkerToEmojiMap = contextMarkerToEmojiMap,
          )
        }
        .stateIn(scope, SharingStarted.Eagerly, MainScreenUiState())

    projectHierarchyInternal = hierarchyState
    searchResultsInternal = searchResultsFlow

    isInitialized = true
  }

  val uiState: StateFlow<MainScreenUiState>
    get() = uiStateInternal

  val projectHierarchy: StateFlow<ListHierarchyData>
    get() = projectHierarchyInternal

  val searchResults: StateFlow<List<SearchResult>>
    get() = searchResultsInternal

  internal data class ExpansionState(
    val expandedDaily: Set<String>,
    val expandedMedium: Set<String>,
    val expandedLong: Set<String>,
  )

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

/**
 * Координує побудову ієрархії для головного екрану, кешуючи останній валідний snapshot.
 *
 * Зберігає:
 * - [lastNonEmptyFlatList] — використовується, коли `PlanningUseCase` переходить у ready-стан із порожнім flatList;
 * - [lastNonEmptyHierarchy] — дозволяє повертати останню згенеровану ієрархію, поки стан ще не готовий.
 *
 * Логи `HierarchyDebug` залишено без змін, аби не втратити діагностику, якою користується команда.
 */
internal class HierarchyStateBuilder(
  private val hierarchyUseCase: HierarchyUseCase,
) {
  private var lastNonEmptyFlatList: List<Project> = emptyList()
  private var lastNonEmptyHierarchy: ListHierarchyData? = null

  fun buildHierarchyState(
    scope: CoroutineScope,
    filterStates: StateFlow<FilterState>,
    expansionStates: StateFlow<MainScreenStateUseCase.ExpansionState>,
  ): StateFlow<ListHierarchyData> {
    val readyFilterState = prepareReadyFilterState(filterStates)

    return combine(readyFilterState, expansionStates) { filterState, expansion ->
        HierarchyDebugLogger.d {
          "coreHierarchyFlow combine triggered: flat=${filterState.flatList.size}, mode=${filterState.mode}, ready=${filterState.isReady}"
        }
        val hierarchy =
          hierarchyUseCase.createProjectHierarchy(
            filterState,
            expansion.expandedDaily,
            expansion.expandedMedium,
            expansion.expandedLong,
          )
        if (
          hierarchy.topLevelProjects.isEmpty() &&
            hierarchy.childMap.isEmpty()
        ) {
          val fallback = lastNonEmptyHierarchy ?: hierarchy
          HierarchyDebugLogger.d {
            "coreHierarchyFlow produced empty hierarchy, fallback topLevel=${fallback.topLevelProjects.size}"
          }
          fallback
        } else {
          lastNonEmptyHierarchy = hierarchy
          HierarchyDebugLogger.d {
            "coreHierarchyFlow updated hierarchy topLevel=${hierarchy.topLevelProjects.size} childParents=${hierarchy.childMap.size}"
          }
          hierarchy
        }
      }
      .stateIn(scope, SharingStarted.Eagerly, ListHierarchyData())
  }

  internal fun prepareReadyFilterState(
    filterStates: StateFlow<FilterState>,
  ) =
    filterStates
      .onEach { state ->
        HierarchyDebugLogger.d {
          "readyFilterState input flat=${state.flatList.size} ready=${state.isReady}"
        }
      }
      .filter { state ->
        HierarchyDebugLogger.d {
          "readyFilterState filter evaluating flat=${state.flatList.size} ready=${state.isReady}"
        }
        val ready = state.isReady
        if (!ready) {
          HierarchyDebugLogger.d {
            "coreHierarchyFlow filter not ready -> returning cached hierarchy topLevel=${lastNonEmptyHierarchy?.topLevelProjects?.size ?: 0}"
          }
        }
        ready
      }
      .map { state ->
        HierarchyDebugLogger.d {
          "readyFilterState accepted flat=${state.flatList.size} ready=${state.isReady}"
        }
        val normalizedFlatList =
          when {
            state.flatList.isNotEmpty() -> {
              lastNonEmptyFlatList = state.flatList
              state.flatList
            }
            lastNonEmptyFlatList.isNotEmpty() &&
              !state.searchActive &&
              state.mode == PlanningMode.All -> {
              HierarchyDebugLogger.d {
                "coreHierarchyFlow using cached flat list size=${lastNonEmptyFlatList.size}"
              }
              lastNonEmptyFlatList
            }
            else -> state.flatList
          }
        HierarchyDebugLogger.d {
          "readyFilterState normalized flat=${normalizedFlatList.size}"
        }
        if (normalizedFlatList === state.flatList) {
          state
        } else {
          state.copy(flatList = normalizedFlatList)
        }
      }
}
