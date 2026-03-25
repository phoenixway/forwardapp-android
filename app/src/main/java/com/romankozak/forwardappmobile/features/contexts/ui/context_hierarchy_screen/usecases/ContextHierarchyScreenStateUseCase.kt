package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.AppStatistics
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextRoleOption
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultFilter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultSort
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.createHierarchyDescendantOverflowMap
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.flattenHierarchyWithLevels
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class ProjectHierarchyScreenStateUseCase
    @Inject
    constructor(
        private val searchUseCase: SearchUseCase,
        private val planningUseCase: PlanningUseCase,
        private val hierarchyUseCase: HierarchyUseCase,
        private val dialogUseCase: DialogUseCase,
        private val syncUseCase: SyncUseCase,
        private val navigationUseCase: NavigationUseCase,
        private val settingsRepository: SettingsRepository,
        private val structurePresetDao: StructurePresetDao,
        private val contextMarkerHandler: ContextMarkerHandler,
        private val recentItemsRepository: RecentItemsRepository,
    ) {
        data class NavigationSnapshot(
            val canGoBack: Boolean = false,
            val canGoForward: Boolean = false,
            val showNavigationMenu: Boolean = false,
        )

        private val defaultUiState = MutableStateFlow(ProjectHierarchyScreenUiState())
        private val defaultHierarchy = MutableStateFlow(ContextHierarchyData())
        private val defaultSearchResults = MutableStateFlow(emptyList<SearchResult>())

        private var uiStateInternal: StateFlow<ProjectHierarchyScreenUiState> = defaultUiState
        private var projectHierarchyInternal: StateFlow<ContextHierarchyData> = defaultHierarchy
        private var searchResultsInternal: StateFlow<List<SearchResult>> = defaultSearchResults

        private var isInitialized = false

        fun initialize(
            scope: CoroutineScope,
            allProjectsFlat: StateFlow<List<Context>>,
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

            val hierarchyState =
                HierarchyStateBuilder(hierarchyUseCase)
                    .buildHierarchyState(
                        scope = scope,
                        filterStates = planningUseCase.filterStateFlow,
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
                    recentItemsRepository.getRecentItems(),
                    contextMarkerHandler.allContextMarkersFlow,
                ) { recentItems, contextMarkers ->
                    ExpensiveCalculations(
                        recentItems = recentItems,
                        allContextMarkers = contextMarkers,
                    )
                }
                    .stateIn(scope, SharingStarted.Lazily, ExpensiveCalculations())

            val availableContextRolesFlow =
                structurePresetDao
                    .getAll()
                    .map(::buildAvailableContextRoles)
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), buildAvailableContextRoles(emptyList()))

            val baseCoreUiStateFlow =
                combine(
                    searchUseCase.subStateStack,
                    searchUseCase.searchQuery,
                    hierarchyState,
                    searchUseCase.currentBreadcrumbs,
                ) { subStateStack, searchQuery, hierarchy, breadcrumbs ->
                    CoreUiState(
                        subStateStack = subStateStack,
                        searchQuery = searchQuery,
                        projectHierarchy = hierarchy,
                        currentBreadcrumbs = breadcrumbs,
                        searchResultFilter = SearchResultFilter.All,
                        searchResultSort = SearchResultSort.Relevance,
                        flattenedHierarchy =
                            flattenHierarchyWithLevels(
                                hierarchy.topLevelProjects,
                                hierarchy.childMap,
                            ),
                        longDescendantsMap = createHierarchyDescendantOverflowMap(hierarchy),
                    )
                }

            val searchControlsFlow =
                combine(
                    searchUseCase.searchResultFilter,
                    searchUseCase.searchResultSort,
                ) { filter, sort ->
                    filter to sort
                }

            val coreUiStateFlow =
                combine(baseCoreUiStateFlow, searchControlsFlow) { baseCoreState, searchControls ->
                    baseCoreState.copy(
                        searchResultFilter = searchControls.first,
                        searchResultSort = searchControls.second,
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
                    contextMarkerHandler.contextMarkerToEmojiMap,
                    availableContextRolesFlow,
                    FeatureToggles.overrides,
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
                        values[9] as ActivityRecord?
                    val obsidianVaultName = values[10] as String
                    val navSnapshot = values[11] as NavigationSnapshot

                    @Suppress("UNCHECKED_CAST")
                    val contextMarkerToEmojiMap = values[12] as Map<String, String>

                    @Suppress("UNCHECKED_CAST")
                    val availableContextRoles = values[13] as List<ContextRoleOption>

                    @Suppress("UNCHECKED_CAST")
                    val featureToggles = values[14] as Map<FeatureFlag, Boolean>

                    ProjectHierarchyScreenUiState(
                        subStateStack = coreState.subStateStack,
                        searchQuery = coreState.searchQuery,
                        searchHistory = searchHistory,
                        projectHierarchy = coreState.projectHierarchy,
                        flattenedHierarchy = coreState.flattenedHierarchy,
                        longDescendantsMap = coreState.longDescendantsMap,
                        currentBreadcrumbs = coreState.currentBreadcrumbs,
                        planningSettings = planningSettings,
                        dialogState = dialogState.dialogState,
                        showRecentListsSheet = dialogState.showRecentListsSheet,
                        isBottomNavExpanded = dialogState.isBottomNavExpanded,
                        recentItems = expensiveCalcs.recentItems,
                        allContextMarkers = expensiveCalcs.allContextMarkers,
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
                        syncStatus = syncState.syncStatus,
                        showSearchDialog = dialogState.showSearchDialog,
                        searchResults = searchResults,
                        searchResultFilter = coreState.searchResultFilter,
                        searchResultSort = coreState.searchResultSort,
                        recordForReminderDialog = recordForReminder,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        availableContextRoles = availableContextRoles,
                        featureToggles = featureToggles,
                    )
                }
                    .stateIn(scope, SharingStarted.Eagerly, MainScreenUiState())

            projectHierarchyInternal = hierarchyState
            searchResultsInternal = searchResultsFlow

            isInitialized = true
        }

        val uiState: StateFlow<ProjectHierarchyScreenUiState>
            get() = uiStateInternal

        val projectHierarchy: StateFlow<ContextHierarchyData>
            get() = projectHierarchyInternal

        val searchResults: StateFlow<List<SearchResult>>
            get() = searchResultsInternal

        private data class CoreUiState(
            val subStateStack: List<ProjectHierarchyScreenSubState>,
            val searchQuery: TextFieldValue,
            val projectHierarchy: ContextHierarchyData,
            val currentBreadcrumbs: List<BreadcrumbItem>,
            val searchResultFilter: SearchResultFilter,
            val searchResultSort: SearchResultSort,
            val flattenedHierarchy: List<FlatHierarchyItem>,
            val longDescendantsMap: Map<String, Boolean>,
        )

        private data class DialogUiState(
            val dialogState: DialogState,
            val showRecentListsSheet: Boolean,
            val isBottomNavExpanded: Boolean,
            val showSearchDialog: Boolean,
        )

        private data class ExpensiveCalculations(
            val recentItems: List<RecentItem> = emptyList(),
            val allContextMarkers: List<UiContextMarker> = emptyList(),
        )

        private fun buildAvailableContextRoles(presets: List<ContextRoleProfile>): List<ContextRoleOption> {
            val rolesByCode = linkedMapOf<String, ContextRoleOption>()

            ContextRoleRegistry.getReservedBaseRoleDefinitions().forEach { definition ->
                rolesByCode[definition.code] = ContextRoleOption(code = definition.code, label = definition.label)
            }

            presets.forEach { preset ->
                val code = preset.code.trim()
                if (code.isEmpty()) return@forEach
                val label = preset.label.trim().ifBlank { code }
                rolesByCode[code] = ContextRoleOption(code = code, label = label)
            }

            return rolesByCode.values.toList()
        }
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
    private var lastNonEmptyFlatList: List<Context> = emptyList()
    private var lastNonEmptyHierarchy: ContextHierarchyData? = null

    fun buildHierarchyState(
        scope: CoroutineScope,
        filterStates: StateFlow<FilterState>,
    ): StateFlow<ContextHierarchyData> {
        val readyFilterState = prepareReadyFilterState(filterStates)

        return readyFilterState.map { filterState ->
            HierarchyDebugLogger.d {
                "coreHierarchyFlow combine triggered: flat=${filterState.flatList.size}, mode=${filterState.mode}, ready=${filterState.isReady}"
            }
            val hierarchy = hierarchyUseCase.createProjectHierarchy(filterState)
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
            .stateIn(scope, SharingStarted.Eagerly, ContextHierarchyData())
    }

    internal fun prepareReadyFilterState(filterStates: StateFlow<FilterState>) =
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
