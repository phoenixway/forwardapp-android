package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.RawContextHierarchyBacking
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextParentLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.AppStatistics
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextRoleOption
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultFilter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultSort
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.createHierarchyDescendantOverflowMap
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
        private val mainBeaconRepository: MainBeaconRepository,
        private val contextParentLinkDao: ContextParentLinkDao,
        private val workspaceDao: WorkspaceDao,
        private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
        private val orientationHierarchyBuilder: OrientationHierarchyBuilder,
    ) {
        data class NavigationSnapshot(
            val canGoBack: Boolean = false,
            val canGoForward: Boolean = false,
            val showNavigationMenu: Boolean = false,
        )

        private val defaultUiState = MutableStateFlow(ProjectHierarchyScreenUiState())
        private val defaultPresentationHierarchy = MutableStateFlow(HierarchyPresentationData())
        private val defaultSearchResults = MutableStateFlow(emptyList<SearchResult>())

        private var uiStateInternal: StateFlow<ProjectHierarchyScreenUiState> = defaultUiState
        private var presentationHierarchyInternal: StateFlow<HierarchyPresentationData> = defaultPresentationHierarchy
        private var searchResultsInternal: StateFlow<List<SearchResult>> = defaultSearchResults

        private var isInitialized = false

        /**
         * Canonical read universe for hierarchy presentation. Exact reserved
         * System Workspaces may exist here without any Context shell.
         */
        internal fun observeHierarchyPresentationUniverse(
            contexts: Flow<List<Context>>,
        ) =
            systemWorkspacePresentationContextProjector
                .observePresentationUniverse(contexts)
                .map { presentations ->
                    presentations.map { presentation ->
                        presentation.toHierarchyPresentationNode()
                    }
                }

        fun initialize(
            scope: CoroutineScope,
            rawContextsFlat: StateFlow<List<Context>>,
            showRecentListsSheet: StateFlow<Boolean>,
            isBottomNavExpanded: StateFlow<Boolean>,
            showSearchDialog: StateFlow<Boolean>,
            navigationSnapshot: StateFlow<NavigationSnapshot>,
            selectedContextIds: StateFlow<Set<String>>,
            clipboardState: StateFlow<Pair<Set<String>, ContextClipboardOperationUi?>>,
            hasBeaconClipboard: StateFlow<Boolean>,
            isSiblingReorderMode: StateFlow<Boolean>,
        ) {
            if (isInitialized) return

            val obsidianVaultNameFlow =
                settingsRepository
                    .obsidianVaultNameFlow
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), "")

            val presentationHierarchyState =
                HierarchyStateBuilder(hierarchyUseCase)
                    .buildHierarchyState(
                        scope = scope,
                        filterStates = planningUseCase.filterStateFlow,
                    )

            val retiredOrdinaryContextIdsState =
                systemWorkspacePresentationContextProjector
                    .observeRetiredOrdinaryContextIds()
                    .stateIn(scope, SharingStarted.Eagerly, emptySet())

            // Raw Context backing for focused and mutation-adjacent paths.
            // Normal hierarchy rendering consumes presentation nodes directly;
            // this only joins stable IDs to existing raw rows.
            val rawContextBackingState =
                combine(
                    presentationHierarchyState,
                    rawContextsFlat,
                ) { presentationHierarchy, contexts ->
                    buildRawContextHierarchyBacking(
                        presentationHierarchy = presentationHierarchy,
                        contexts = contexts,
                    )
                }.stateIn(
                    scope,
                    SharingStarted.Eagerly,
                    RawContextHierarchyBacking(),
                )

            val hierarchyProjectionFlow =
                combine(
                    rawContextBackingState,
                    presentationHierarchyState,
                    workspaceDao.observeAll(),
                    retiredOrdinaryContextIdsState,
                ) { rawContextBacking, presentationHierarchy, workspaces, retiredOrdinaryContextIds ->
                    HierarchyProjection(
                        rawContextBacking = rawContextBacking,
                        presentationHierarchy = presentationHierarchy,
                        workspaces = workspaces,
                        retiredOrdinaryContextIds = retiredOrdinaryContextIds,
                    )
                }

            val mainBeaconDetailsFlow =
                mainBeaconRepository
                    .observeMainBeaconDetails()
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
            val mainBeaconGroupsFlow =
                mainBeaconRepository
                    .observeGroups()
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
            val contextParentLinksFlow =
                contextParentLinkDao
                    .observeActiveLinks()
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
            val mainBeaconParentLinksFlow =
                mainBeaconRepository
                    .observeParentLinks()
                    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
            val orientationHierarchyInputsFlow =
                combine(
                    mainBeaconDetailsFlow,
                    mainBeaconGroupsFlow,
                    contextParentLinksFlow,
                    mainBeaconParentLinksFlow,
                ) { beacons, groups, parentLinks, beaconParentLinks ->
                    OrientationHierarchyInputs(
                        beacons = beacons.map { it.toOrientationBeaconInput() },
                        groups = groups,
                        parentLinks = parentLinks,
                        beaconParentLinks = beaconParentLinks,
                    )
                }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), OrientationHierarchyInputs())

            scope.launch {
                combine(rawContextBackingState, presentationHierarchyState) { rawBacking, presentation ->
                    rawBacking to presentation
                }.collect { (rawBacking, presentation) ->
                    HierarchyDebugLogger.d {
                        "coreHierarchyFlow emit -> presentation=${presentation.allProjects.size}, admittedRaw=${rawBacking.rawContexts.size}"
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
                combine(
                    planningUseCase.filterStateFlow,
                    presentationHierarchyState,
                ) { filterState, hierarchy ->
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
                    hierarchyProjectionFlow,
                    orientationHierarchyInputsFlow,
                    searchUseCase.currentBreadcrumbs,
                ) { subStateStack, searchQuery, hierarchyProjection, orientationHierarchyInputs, breadcrumbs ->
                    val rawContextBacking = hierarchyProjection.rawContextBacking
                    val presentationHierarchy = hierarchyProjection.presentationHierarchy
                    val workspaces = hierarchyProjection.workspaces
                    CoreUiState(
                        subStateStack = subStateStack,
                        searchQuery = searchQuery,
                        rawContextBacking = rawContextBacking,
                        orientationHierarchy =
                            orientationHierarchyBuilder.build(
                                presentationHierarchy = presentationHierarchy,
                                beacons = orientationHierarchyInputs.beacons,
                                groups = orientationHierarchyInputs.groups,
                                parentLinks = orientationHierarchyInputs.parentLinks,
                                beaconParentLinks = orientationHierarchyInputs.beaconParentLinks,
                                workspaces = workspaces,
                            ),
                        currentBreadcrumbs = breadcrumbs,
                        searchResultFilter = SearchResultFilter.All,
                        searchResultSort = SearchResultSort.Relevance,
                        presentationHierarchy = presentationHierarchy,
                        longDescendantsMap = createHierarchyDescendantOverflowMap(presentationHierarchy),
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
                combine(
                    baseCoreUiStateFlow,
                    searchControlsFlow,
                ) { baseCoreState, searchControls ->
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
                    selectedContextIds,
                    clipboardState,
                    hasBeaconClipboard,
                    isSiblingReorderMode,
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
                    @Suppress("UNCHECKED_CAST")
                    val selectedIds = values[15] as Set<String>
                    @Suppress("UNCHECKED_CAST")
                    val clipboard = values[16] as Pair<Set<String>, ContextClipboardOperationUi?>
                    val hasBeaconPayload = values[17] as Boolean
                    val siblingReorderMode = values[18] as Boolean

                    ProjectHierarchyScreenUiState(
                        subStateStack = coreState.subStateStack,
                        searchQuery = coreState.searchQuery,
                        searchHistory = searchHistory,
                        rawContextsById = coreState.rawContextBacking.rawContextsById,
                        rawChildMap = coreState.rawContextBacking.rawChildMap,
                        presentationHierarchy = coreState.presentationHierarchy,
                        orientationHierarchy = coreState.orientationHierarchy,
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
                        selectedContextIds = selectedIds,
                        clipboardContextIds = clipboard.first,
                        clipboardOperation = clipboard.second,
                        hasBeaconClipboard = hasBeaconPayload,
                        isSiblingReorderMode = siblingReorderMode,
                    )
                }
                    .stateIn(scope, SharingStarted.Eagerly, MainScreenUiState())

            presentationHierarchyInternal = presentationHierarchyState
            searchResultsInternal = searchResultsFlow
            isInitialized = true
        }

        val uiState: StateFlow<ProjectHierarchyScreenUiState>
            get() = uiStateInternal

        val presentationHierarchy: StateFlow<HierarchyPresentationData>
            get() = presentationHierarchyInternal

        val searchResults: StateFlow<List<SearchResult>>
            get() = searchResultsInternal

        private data class OrientationHierarchyInputs(
            val beacons: List<OrientationBeaconInput> = emptyList(),
            val groups: List<MainBeaconGroup> = emptyList(),
            val parentLinks: List<ContextParentLink> = emptyList(),
            val beaconParentLinks: List<MainBeaconParentLink> = emptyList(),
        )

        private fun MainBeaconWithRelations.toOrientationBeaconInput(): OrientationBeaconInput =
            OrientationBeaconInput(
                id = beacon.id,
                title = beacon.title,
                order = beacon.order,
                readinessStatus = beacon.readinessStatus,
                parentBeaconId = beacon.parentBeaconId,
                relatedOwnerIds = relatedOwnerIds,
                groupIds = groupIds,
                groupOrders = groupOrders,
            )

        private data class CoreUiState(
            val subStateStack: List<ProjectHierarchyScreenSubState>,
            val searchQuery: TextFieldValue,
            val rawContextBacking: RawContextHierarchyBacking,
            val orientationHierarchy: List<OrientationHierarchyItem>,
            val currentBreadcrumbs: List<BreadcrumbItem>,
            val searchResultFilter: SearchResultFilter,
            val searchResultSort: SearchResultSort,
            val presentationHierarchy: HierarchyPresentationData,
            val longDescendantsMap: Map<String, Boolean>,
        )

        private data class HierarchyProjection(
            val rawContextBacking: RawContextHierarchyBacking,
            val presentationHierarchy: HierarchyPresentationData,
            val workspaces: List<WorkspaceEntity>,
            val retiredOrdinaryContextIds: Set<String>,
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
                if (definition.code == ContextRoleRegistry.ROLE_MAIN_BEACON) return@forEach
                rolesByCode[definition.code] = ContextRoleOption(code = definition.code, label = definition.label)
            }

            presets.forEach { preset ->
                val code = preset.code.trim()
                if (code.isEmpty()) return@forEach
                if (code == ContextRoleRegistry.ROLE_MAIN_BEACON) return@forEach
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

/**
 * Raw Context backing bridge for focused and mutation-adjacent paths.
 *
 * This deliberately does not project presentation metadata onto Context. Each
 * presentation ID resolves only to the original Context object already in
 * [contexts]. Missing raw objects fail closed and are omitted.
 */
internal fun buildRawContextHierarchyBacking(
    presentationHierarchy: HierarchyPresentationData,
    contexts: List<Context>,
): RawContextHierarchyBacking {
    val repositoryContextsById = contexts.associateBy { it.id }

    val rawContexts =
        presentationHierarchy.allProjects.mapNotNull { node ->
            repositoryContextsById[node.id]
        }
    val rawContextsById = rawContexts.associateBy { it.id }

    val rawChildMap =
        buildMap<String, List<Context>> {
            presentationHierarchy.childMap.forEach { (parentId, children) ->
                if (rawContextsById[parentId] == null) return@forEach
                val resolvedChildren = children.mapNotNull { child -> rawContextsById[child.id] }
                if (resolvedChildren.isNotEmpty()) {
                    put(parentId, resolvedChildren)
                }
            }
        }

    return RawContextHierarchyBacking(
        rawContexts = rawContexts,
        rawContextsById = rawContextsById,
        rawChildMap = rawChildMap,
    )
}

internal class HierarchyStateBuilder(
    private val hierarchyUseCase: HierarchyUseCase,
) {
    private var lastNonEmptyFlatList: List<HierarchyContextPresentationNode> = emptyList()
    private var lastNonEmptyHierarchy: HierarchyPresentationData? = null

    fun buildHierarchyState(
        scope: CoroutineScope,
        filterStates: StateFlow<FilterState>,
    ): StateFlow<HierarchyPresentationData> {
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
            .stateIn(scope, SharingStarted.Eagerly, HierarchyPresentationData())
    }

    internal fun prepareReadyFilterState(filterStates: StateFlow<FilterState>): Flow<FilterState> =
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
