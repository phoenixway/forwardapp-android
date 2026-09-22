package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
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
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyScreenMetadataAssembler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyScreenOperationalBeaconMetadata
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyScreenPresentationAdapter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ReactiveHierarchyReadSource
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyReadAuthorityRouter
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
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.fuzzyMatch
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
        private val canonicalV2ReactiveHierarchyReadSource: CanonicalV2ReactiveHierarchyReadSource,
        private val canonicalV2HierarchyScreenMetadataAssembler: CanonicalV2HierarchyScreenMetadataAssembler,
        private val canonicalV2HierarchyScreenPresentationAdapter: CanonicalV2HierarchyScreenPresentationAdapter,
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
        private val canonicalV2ReadInternal =
            MutableStateFlow<CanonicalV2ProductionHierarchyRead?>(null)

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

            val canonicalV2ReadFlow =
                canonicalV2ReactiveHierarchyReadSource
                    .observe()
                    .onEach { canonicalV2ReadInternal.value = it }

            val presentationHierarchyState =
                HierarchyReadAuthorityRouter().route(
                    currentPreCutover = {
                        HierarchyStateBuilder(hierarchyUseCase)
                            .buildHierarchyState(
                                scope = scope,
                                filterStates = planningUseCase.filterStateFlow,
                            )
                    },
                    v2Authority = {
                        observeHierarchyPresentationUniverse(rawContextsFlat)
                            .map(::buildNonStructuralPresentationUniverse)
                            .stateIn(
                                scope,
                                SharingStarted.Eagerly,
                                HierarchyPresentationData(),
                            )
                    },
                )

            // Raw Context backing for focused and mutation-adjacent paths.
            // Normal hierarchy rendering consumes presentation nodes directly;
            // this only joins stable IDs to existing raw rows.
            val orientationHierarchyFlow: Flow<List<OrientationHierarchyItem>> =
                HierarchyReadAuthorityRouter().route(
                    currentPreCutover = {
                        val retiredOrdinaryContextIdsState =
                            systemWorkspacePresentationContextProjector
                                .observeRetiredOrdinaryContextIds()
                                .stateIn(scope, SharingStarted.Eagerly, emptySet())
                        val hierarchyProjectionFlow =
                            combine(
                                presentationHierarchyState,
                                workspaceDao.observeAll(),
                                retiredOrdinaryContextIdsState,
                            ) { presentationHierarchy, workspaces, retiredOrdinaryContextIds ->
                                HierarchyProjection(
                                    presentationHierarchy = presentationHierarchy,
                                    workspaces = workspaces,
                                    retiredOrdinaryContextIds = retiredOrdinaryContextIds,
                                )
                            }

                        val mainBeaconDetailsFlow =
                            mainBeaconRepository.observeMainBeaconDetails()
                        val mainBeaconGroupsFlow =
                            mainBeaconRepository.observeGroups()
                        val contextParentLinksFlow =
                            contextParentLinkDao.observeActiveLinks()
                        val mainBeaconParentLinksFlow =
                            mainBeaconRepository.observeParentLinks()
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
                            }

                        combine(
                            hierarchyProjectionFlow,
                            orientationHierarchyInputsFlow,
                        ) { hierarchyProjection, orientationHierarchyInputs ->
                            orientationHierarchyBuilder.build(
                                presentationHierarchy = hierarchyProjection.presentationHierarchy,
                                beacons = orientationHierarchyInputs.beacons,
                                groups = orientationHierarchyInputs.groups,
                                parentLinks = orientationHierarchyInputs.parentLinks,
                                beaconParentLinks = orientationHierarchyInputs.beaconParentLinks,
                                workspaces = hierarchyProjection.workspaces,
                            )
                        }
                    },
                    v2Authority = {
                        combine(
                            canonicalV2ReadFlow,
                            observeHierarchyPresentationUniverse(rawContextsFlat),
                            mainBeaconRepository.observeMainBeaconDetails(),
                        ) { read, workspacePresentations, beacons ->
                            val metadata =
                                canonicalV2HierarchyScreenMetadataAssembler.assemble(
                                    read = read,
                                    workspacePresentations = workspacePresentations,
                                    beacons =
                                        beacons.map { beacon ->
                                            CanonicalV2HierarchyScreenOperationalBeaconMetadata(
                                                presentationId = beacon.beacon.id,
                                                readinessStatus = beacon.beacon.readinessStatus,
                                                relatedOwnerIds = beacon.relatedOwnerIds,
                                            )
                                        },
                                )
                            canonicalV2HierarchyScreenPresentationAdapter.adapt(
                                read = read,
                                metadata = metadata,
                            )
                        }
                    },
                )

            scope.launch {
                planningUseCase.filterStateFlow.collect { state ->
                    HierarchyDebugLogger.d {
                        "filterState observed in MainScreenStateUseCase flat=${state.flatList.size} mode=${state.mode} ready=${state.isReady}"
                    }
                }
            }

            val searchResultsFlow =
                HierarchyReadAuthorityRouter().route(
                    currentPreCutover = {
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
                    },
                    v2Authority = {
                        combine(
                            planningUseCase.filterStateFlow,
                            canonicalV2ReadFlow,
                        ) { filterState, read ->
                            if (!filterState.isReady) {
                                emptyList()
                            } else {
                                createCanonicalV2SearchResults(
                                    filterState = filterState,
                                    read = read,
                                )
                            }
                        }
                    },
                ).stateIn(scope, SharingStarted.Lazily, emptyList())

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

            val presentationBackingFlow = presentationHierarchyState

            val baseCoreUiStateFlow =
                combine(
                    searchUseCase.subStateStack,
                    searchUseCase.searchQuery,
                    presentationBackingFlow,
                    orientationHierarchyFlow,
                    searchUseCase.currentBreadcrumbs,
                ) { subStateStack, searchQuery, presentationBacking, orientationHierarchy, breadcrumbs ->
                    val presentationHierarchy = presentationBacking
                    CoreUiState(
                        subStateStack = subStateStack,
                        searchQuery = searchQuery,
                        orientationHierarchy = orientationHierarchy,
                        currentBreadcrumbs = breadcrumbs,
                        searchResultFilter = SearchResultFilter.All,
                        searchResultSort = SearchResultSort.Relevance,
                        presentationHierarchy = presentationHierarchy,
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
                        presentationHierarchy = coreState.presentationHierarchy,
                        orientationHierarchy = coreState.orientationHierarchy,
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

        val canonicalV2Read: StateFlow<CanonicalV2ProductionHierarchyRead?>
            get() = canonicalV2ReadInternal

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
            val orientationHierarchy: List<OrientationHierarchyItem>,
            val currentBreadcrumbs: List<BreadcrumbItem>,
            val searchResultFilter: SearchResultFilter,
            val searchResultSort: SearchResultSort,
            val presentationHierarchy: HierarchyPresentationData,
        )

        private data class HierarchyProjection(
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
/**
 * Exact occurrence-native local search for V2 authority.
 *
 * FilterState still owns search admission/matching policy, but ancestry and
 * result identity come only from the already-adapted V2 occurrence tree.
 * Duplicate Workspace targets therefore remain separate search results.
 */
internal fun createCanonicalV2SearchResults(
    filterState: FilterState,
    read: CanonicalV2ProductionHierarchyRead?,
): List<SearchResult> {
    if (!filterState.searchActive || filterState.query.isBlank()) {
        return emptyList()
    }

    val admittedTargetIds = filterState.flatList.mapTo(hashSetOf()) { it.id }
    val query = filterState.query

    val canonicalRead =
        read ?: return emptyList()

    return canonicalRead.hierarchy.occurrences
        .asSequence()
        .mapNotNull { occurrence ->
            if (occurrence.target.type != HierarchyTargetType.WORKSPACE) {
                return@mapNotNull null
            }
            if (occurrence.target.id !in admittedTargetIds) return@mapNotNull null

            val presentation = canonicalRead.occurrence(occurrence.placementId)
                ?: return@mapNotNull null

            val matches =
                if (query.length > 3) {
                    fuzzyMatch(query, presentation.title)
                } else {
                    presentation.title.contains(query, ignoreCase = true)
                }
            if (!matches) return@mapNotNull null

            val breadcrumbs =
                canonicalRead.breadcrumbsToOccurrence(occurrence.placementId)

            SearchResult(
                projectId = occurrence.target.id,
                projectName = presentation.title,
                parentPath = breadcrumbs.map { it.title },
                placementId = occurrence.placementId.value,
            )
        }
        .sortedBy { it.projectName }
        .toList()
}

/**
 * Compatibility metadata universe for V2-authority screen code that still
 * expects [HierarchyPresentationData].
 *
 * This deliberately carries no structural claims: V2 parentage, ordering and
 * occurrence identity live only in HierarchyPlacement / the V2 read surface.
 */
internal fun buildNonStructuralPresentationUniverse(
    presentations: List<HierarchyContextPresentationNode>,
): HierarchyPresentationData =
    HierarchyPresentationData(
        allProjects = presentations,
        topLevelProjects = emptyList(),
        childMap = emptyMap(),
    )
