package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.routes.COMMAND_DECK_ROUTE
import com.romankozak.forwardappmobile.core.theme.ThemeSettings
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayFocusesRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyProjectMenuAvailability
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation.RevealResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextActionsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextClipboardCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextClipboardResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.WorkspaceClipboardCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.WorkspaceClipboardResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextDialogActionCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextMigrationCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextSelectionCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.DialogUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyFocusCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyDebugLogger
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyProjectNavigation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.NavigationUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.PlanningUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ProjectHierarchyScreenStateUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.resolveHierarchyProjectNavigation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SettingsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ThemingUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.UtilityDialogRequest
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.buildPresentationPathToProject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class ContextHierarchyScreenViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepo: SettingsRepository,
        private val searchUseCase: SearchUseCase,
        private val dialogUseCase: DialogUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val contextMarkerHandler: ContextMarkerHandler,
        private val dayManagementRepository: DayManagementRepository,
        private val dayFocusesRepository: DayFocusesRepository,
        private val focusContextRepository: FocusContextRepository,
        private val activityRepository: ActivityRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val noteRepository: LegacyNoteRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val checklistRepository: ChecklistRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
        private val planningUseCase: PlanningUseCase,
        private val syncUseCase: SyncUseCase,
        private val contextActionsUseCase: ContextActionsUseCase,
        private val navigationUseCase: NavigationUseCase,
        private val themingUseCase: ThemingUseCase,
        private val settingsUseCase: SettingsUseCase,
        private val projectHierarchyScreenStateUseCase: ProjectHierarchyScreenStateUseCase,
        private val contextClipboardCoordinator: ContextClipboardCoordinator,
        private val workspaceClipboardCoordinator: WorkspaceClipboardCoordinator,
        private val hierarchyFocusCoordinator: HierarchyFocusCoordinator,
        private val contextSelectionCoordinator: ContextSelectionCoordinator,
        private val contextDialogActionCoordinator: ContextDialogActionCoordinator,
        private val contextMigrationCoordinator: ContextMigrationCoordinator,
    ) : ViewModel() {
        companion object {
            private const val PROJECT_TO_REVEAL_KEY = "projectIdToReveal"
            private const val TAG = "ProjectHierarchyScreenVM_DEBUG"
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
                        hierarchyPresentationFlat = _hierarchyPresentationFlat,
                    )
                    observeNavigationManager(value)
                    initializeNavigationResultHandling(value)
                }
            }

        private val navigationSnapshot =
            MutableStateFlow(ProjectHierarchyScreenStateUseCase.NavigationSnapshot())

        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextMarkerHandler.contextMarkerToEmojiMap
        val focusedContextIds: StateFlow<Set<String>> =
            focusContextRepository
                .observeActiveFocusContextIds()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptySet(),
                )

        val uiState: StateFlow<ProjectHierarchyScreenUiState>
            get() = projectHierarchyScreenStateUseCase.uiState

        val lastOngoingActivity: StateFlow<ActivityRecord?> =
            activityRepository
                .getLogStream()
                .map { log ->
                    log.firstOrNull { it.startTime != null && it.endTime == null }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val themeSettings: StateFlow<ThemeSettings> =
            themingUseCase.themeSettings
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ThemeSettings(),
                )

        private val _uiEventChannel = Channel<ProjectUiEvent>()
        val uiEventFlow = _uiEventChannel.receiveAsFlow()

        private val rawContextsFlow = contextRepository.getAllContextsFlow()

        private val _rawContextsFlat =
            rawContextsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        private val hierarchyPresentationFlow =
            projectHierarchyScreenStateUseCase.observeHierarchyPresentationUniverse(rawContextsFlow)

        private val _hierarchyPresentationFlat =
            hierarchyPresentationFlow.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList(),
            )

        private val _showRecentListsSheet = MutableStateFlow(false)
        private val _isBottomNavExpanded = MutableStateFlow(false)
        private val _showSearchDialog = MutableStateFlow(false)
        private val _isSiblingReorderMode = MutableStateFlow(false)

        init {
            searchUseCase.initialize(
                scope = viewModelScope,
                uiEventChannel = _uiEventChannel,
                onProjectAccess = { projectId ->
                    _hierarchyPresentationFlat.value
                        .firstOrNull { it.id == projectId }
                        ?.let { presentation ->
                            recentItemsRepository.logProjectAccess(
                                projectId = presentation.id,
                                displayName = presentation.name,
                            )
                        }
                },
                hierarchyPresentationFlat = _hierarchyPresentationFlat,
                presentationHierarchy = projectHierarchyScreenStateUseCase.presentationHierarchy,
            )
            planningUseCase.initialize(
                scope = viewModelScope,
                allProjectsFlat = _hierarchyPresentationFlat,
            )
            syncUseCase.initialize(
                scope = viewModelScope,
                application = application,
                uiEventChannel = _uiEventChannel,
            )
            projectHierarchyScreenStateUseCase.initialize(
                scope = viewModelScope,
                rawContextsFlat = _rawContextsFlat,
                showRecentListsSheet = _showRecentListsSheet,
                isBottomNavExpanded = _isBottomNavExpanded,
                showSearchDialog = _showSearchDialog,
                navigationSnapshot = navigationSnapshot,
                selectedContextIds = contextSelectionCoordinator.selectedIds,
                clipboardState = workspaceClipboardCoordinator.uiState,
                hasBeaconClipboard = contextClipboardCoordinator.hasBeaconPayload,
                isSiblingReorderMode = _isSiblingReorderMode,
            )
            viewModelScope.launch {
                projectHierarchyScreenStateUseCase.presentationHierarchy.collect { hierarchy ->
                    if (hierarchy.allProjects.isNotEmpty()) {
                        searchUseCase.reconcileFocusedProjectBreadcrumbs(hierarchy)
                    }
                }
            }
            initializeAndCollectStates()
            viewModelScope.launch {
                _hierarchyPresentationFlat.collect { projects ->
                    val projectIds = projects.mapTo(linkedSetOf()) { it.id }
                    contextSelectionCoordinator.retainExistingProjectIds(projectIds)
                }
            }
            viewModelScope.launch {
                _rawContextsFlat.collect { projects ->
                    contextClipboardCoordinator.retainExistingContextIds(
                        projects.mapTo(linkedSetOf()) { it.id },
                    )
                }
            }
            viewModelScope.launch {
                _hierarchyPresentationFlat
                    .map { projects ->
                        projects.associate { presentation ->
                            presentation.id to presentation.name
                        }
                    }.distinctUntilChanged()
                    .collect { projectsById ->
                        withContext(ioDispatcher) {
                            recentItemsRepository.syncProjectRecentItems(projectsById)
                        }
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
                delay(1500)
                val filterState = planningUseCase.filterStateFlow.first()
                val filterSize = filterState.flatList.size
                HierarchyDebugLogger.d { "Delayed check: filterState flat=$filterSize" }
            }
        }

        private var navigationStateJob: Job? = null
        private var navigationResultJob: Job? = null

        private fun onContextSelected(name: String) {
            viewModelScope.launch {
                val tag = contextMarkerHandler.getContextTag(name)
                val projectId =
                    if (tag != null) {
                        withContext(ioDispatcher) { contextRepository.findContextIdsByTag(tag).firstOrNull() }
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

        suspend fun findFirstContextIdForTag(tag: String): String? {
            val normalized = tag.trim().removePrefix("#")
            if (normalized.isBlank()) return null
            return withContext(ioDispatcher) {
                contextRepository.findContextIdsByTag(normalized).firstOrNull()
            }
        }

        fun consumePendingProjectToReveal(): String? = savedStateHandle.remove(PROJECT_TO_REVEAL_KEY)

        private suspend fun revealProject(
            projectId: String,
            forceFocusMode: Boolean = false,
        ) {
            Log.d("ProjectRevealDebug", "Attempting to reveal projectId: $projectId")
            when (val result = searchUseCase.revealProjectInHierarchy(projectId)) {
                is RevealResult.Success -> {
                    Log.d(
                        "ProjectRevealDebug",
                        "revealProjectInHierarchy result: Success, shouldFocus=${result.shouldFocus}",
                    )
                    val hierarchyForReveal =
                        awaitHierarchyForProjectPath(result.projectId)
                    Log.d("ProjectRevealDebug", "Calling revealProject for ${result.projectId}")
                    hierarchyFocusCoordinator.revealProject(
                        projectId = result.projectId,
                        placementId = result.placementId,
                        currentHierarchy = hierarchyForReveal,
                        currentSubState = uiState.value.currentSubState,
                        currentBreadcrumbs = uiState.value.currentBreadcrumbs,
                        orientationHierarchy = uiState.value.orientationHierarchy,
                        canonicalRead = projectHierarchyScreenStateUseCase.canonicalV2Read.value,
                        enterFocus = true,
                        replaceFocusPath = forceFocusMode,
                    )
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
                            projectHierarchyScreenStateUseCase.presentationHierarchy.value,
                        )
                    }
                }
        }

        private fun initializeAndCollectStates() {
            viewModelScope.launch(ioDispatcher) {
                contextMarkerHandler.initialize()
                settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                    _isBottomNavExpanded.value = savedState
                }
            }

            viewModelScope.launch {
                searchUseCase.subStateStack
                    .map { stack -> stack.lastOrNull() }
                    .distinctUntilChanged()
                    .drop(1)
                    .collect {
                        _isSiblingReorderMode.value = false
                    }
            }

        }

        private fun canonicalBreadcrumbsForPlacement(
            placementId: String?,
        ): List<BreadcrumbItem>? =
            placementId
                ?.let(::PlacementId)
                ?.let { id ->
                    projectHierarchyScreenStateUseCase
                        .canonicalV2Read
                        .value
                        ?.breadcrumbsToOccurrence(id)
                }
                ?.map { breadcrumb ->
                    BreadcrumbItem(
                        id = breadcrumb.id,
                        name = breadcrumb.title,
                        level = breadcrumb.level,
                        target =
                            when (breadcrumb.target) {
                                CanonicalV2BreadcrumbTarget.CONTEXT ->
                                    BreadcrumbTarget.Context
                                CanonicalV2BreadcrumbTarget.ORIENTATION_NODE ->
                                    BreadcrumbTarget.OrientationNode
                            },
                        placementId = breadcrumb.placementId?.value,
                    )
                }


        fun onEvent(event: ContextHierarchyScreenEvent) {
            when (event) {
                is ContextHierarchyScreenEvent.SearchQueryChanged -> searchUseCase.onSearchQueryChanged(event.query)
                is ContextHierarchyScreenEvent.SearchFromHistory -> searchUseCase.onSearchQueryFromHistory(event.query)
                is ContextHierarchyScreenEvent.RemoveSearchHistoryEntry -> searchUseCase.removeSearchHistoryEntry(event.query)
                is ContextHierarchyScreenEvent.ClearSearchHistory -> searchUseCase.clearSearchHistory()
                is ContextHierarchyScreenEvent.SearchFilterChanged -> searchUseCase.onSearchFilterChanged(event.filter)
                is ContextHierarchyScreenEvent.SearchSortChanged -> searchUseCase.onSearchSortChanged(event.sort)
                is ContextHierarchyScreenEvent.GlobalSearchPerform -> searchUseCase.onPerformGlobalSearch(event.query)
                is ContextHierarchyScreenEvent.SearchResultClick ->
                    searchUseCase.onSearchResultClick(
                        projectId = event.projectId,
                        placementId = event.placementId,
                        currentHierarchy = projectHierarchyScreenStateUseCase.presentationHierarchy.value,
                        orientationHierarchy = uiState.value.orientationHierarchy,
                        canonicalBreadcrumbs = canonicalBreadcrumbsForPlacement(event.placementId),
                    )

                is ContextHierarchyScreenEvent.ContextClick -> {
                    if (!contextSelectionCoordinator.handleProjectClick(event.projectId)) {
                        onProjectClicked(event.projectId)
                    }
                }
                is ContextHierarchyScreenEvent.OrientationNodeClick -> {
                    hierarchyFocusCoordinator.focusOrientationNode(
                        nodeId = event.nodeId,
                        orientationHierarchy = uiState.value.orientationHierarchy,
                        placementId = event.placementId,
                    )
                }
                is ContextHierarchyScreenEvent.StartContextSelection -> {
                    contextSelectionCoordinator.start(event.projectId)
                }
                is ContextHierarchyScreenEvent.ToggleContextSelection -> {
                    contextSelectionCoordinator.toggle(event.projectId)
                }
                is ContextHierarchyScreenEvent.ClearContextSelection -> {
                    contextSelectionCoordinator.clear()
                }
                is ContextHierarchyScreenEvent.ContextMenuRequest -> {
                    val projectName =
                        _hierarchyPresentationFlat.value
                            .firstOrNull { it.id == event.projectId }
                            ?.name
                            ?: return

                    val projectContextId = ContextId(event.projectId)
                    val isSystemProject = SystemContexts.isSystem(projectContextId)
                    val canMoveProject =
                        SystemContexts.canRenameOrMove(projectContextId)

                    dialogUseCase.onMenuRequested(
                        projectId = event.projectId,
                        projectName = projectName,
                        availability =
                            HierarchyProjectMenuAvailability(
                                copyWorkspace = true,
                                cutWorkspace = canMoveProject,
                                pasteWorkspace =
                                    workspaceClipboardCoordinator.canPasteInto(event.projectId),
                                delete = !isSystemProject,
                            ),
                        occurrence = event.occurrence,
                    )
                }
                is ContextHierarchyScreenEvent.MigrateRequest -> {
                    viewModelScope.launch {
                        if (!contextMigrationCoordinator.start(event.projectId)) {
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Системний контекст не можна мігрувати"))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.MigrationChoiceSelected ->
                    contextMigrationCoordinator.selectChoice(event.choice)
                is ContextHierarchyScreenEvent.MigrationOrientationKindSelected ->
                    contextMigrationCoordinator.selectOrientationKind(event.kind)
                is ContextHierarchyScreenEvent.MigrationExistingAspectSelected ->
                    contextMigrationCoordinator.selectExistingAspect(event.id)
                is ContextHierarchyScreenEvent.MigrationExistingOrientationSelected ->
                    contextMigrationCoordinator.selectExistingOrientation(event.id)
                ContextHierarchyScreenEvent.MigrationConfirmationRequested ->
                    contextMigrationCoordinator.requestConfirmation()
                ContextHierarchyScreenEvent.MigrationExecute -> {
                    viewModelScope.launch {
                        contextMigrationCoordinator.execute().onSuccess {
                            dialogUseCase.dismissDialog()
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст мігровано"))
                        }
                    }
                }
                ContextHierarchyScreenEvent.ToggleSiblingReorderMode -> {
                    _isSiblingReorderMode.update { !it }
                }
                is ContextHierarchyScreenEvent.ReorderContextSiblings -> {
                    viewModelScope.launch {
                        contextActionsUseCase.reorderContextSiblings(
                            parentPlacementId = event.parentPlacementId,
                            orderedPlacementIds = event.orderedPlacementIds,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ReorderOrientationBeaconSiblings -> {
                    viewModelScope.launch {
                        contextActionsUseCase.reorderOrientationBeaconSiblings(
                            parentNodeId = event.parentNodeId,
                            orderedBeaconIds = event.orderedBeaconIds,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ReorderOrientationGroups -> {
                    viewModelScope.launch {
                        contextActionsUseCase.reorderOrientationGroups(event.orderedGroupIds)
                    }
                }

                is ContextHierarchyScreenEvent.BreadcrumbNavigation ->
                    hierarchyFocusCoordinator.navigateToBreadcrumb(event.breadcrumb)
                is ContextHierarchyScreenEvent.ClearBreadcrumbNavigation -> hierarchyFocusCoordinator.clearNavigation()

                is ContextHierarchyScreenEvent.DismissDialog -> dialogUseCase.dismissDialog()
                is ContextHierarchyScreenEvent.AddNewContextRequest -> {
                    contextDialogActionCoordinator.requestAddContext(
                        currentSubState = uiState.value.currentSubState,
                        currentBreadcrumbs = uiState.value.currentBreadcrumbs,
                        orientationHierarchy = uiState.value.orientationHierarchy,
                    )
                }
                is ContextHierarchyScreenEvent.AddNoteDocumentRequest -> createNoteInInbox()
                is ContextHierarchyScreenEvent.AddChecklistRequest -> {
                    createChecklistInInbox()
                }
                is ContextHierarchyScreenEvent.AddNoteDocumentToContextRequest -> {
                    dialogUseCase.dismissDialog()
                    createNoteInContext(event.projectId)
                }
                is ContextHierarchyScreenEvent.AddChecklistToContextRequest -> {
                    dialogUseCase.dismissDialog()
                    createChecklistInContext(event.projectId)
                }
                is ContextHierarchyScreenEvent.ListChooserResult -> {
                    confirmMove(
                        newParentId = event.projectId,
                        destinationPlacementId = event.destinationPlacementId,
                    )
                }
                is ContextHierarchyScreenEvent.AddSubprojectRequest ->
                    contextDialogActionCoordinator.requestAddSubcontext(
                        parentProjectId = event.parentProjectId,
                        parentOccurrence = event.parentOccurrence,
                    )
                is ContextHierarchyScreenEvent.DeleteRequest -> {
                    val projectName =
                        _hierarchyPresentationFlat.value
                            .firstOrNull { it.id == event.projectId }
                            ?.name
                            ?: return

                    contextDialogActionCoordinator.requestDelete(
                        projectId = event.projectId,
                        projectName = projectName,
                    )
                }
                is ContextHierarchyScreenEvent.MoveRequest -> {
                    viewModelScope.launch {
                        contextDialogActionCoordinator.requestMove(
                            projectId = event.projectId,
                            occurrence = event.occurrence,
                            hierarchyRead = projectHierarchyScreenStateUseCase.canonicalV2Read.value,
                        )?.let { _uiEventChannel.send(it) }
                    }
                }
                is ContextHierarchyScreenEvent.DeleteConfirm -> {
                    viewModelScope.launch {
                        contextDialogActionCoordinator.confirmDelete(
                            projectId = event.projectId,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.MoveConfirm -> {
                    confirmMove(event.newParentId)
                }
                is ContextHierarchyScreenEvent.RestoreConfirm -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(
                            contextDialogActionCoordinator.confirmRestoreImport(event.uri),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.MergeConfirm -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(
                            contextDialogActionCoordinator.confirmMergeImport(event.uri),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ShowAboutDialog ->
                    dialogUseCase.onUtilityDialogRequest(UtilityDialogRequest.About)
                is ContextHierarchyScreenEvent.ImportFromFileRequest ->
                    dialogUseCase.onUtilityDialogRequest(UtilityDialogRequest.Import(event.uri))

                is ContextHierarchyScreenEvent.RestoreImportRequest ->
                    dialogUseCase.onRestoreImportRequested(event.uri)

                is ContextHierarchyScreenEvent.SelectiveImportFromFileRequest -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.ImportExport(uri = event.uri.toString()),
                            ),
                        )
                    }
                }

                is ContextHierarchyScreenEvent.HomeClick -> onHomeClicked()
                is ContextHierarchyScreenEvent.BackClick -> handleBackNavigation()
                is ContextHierarchyScreenEvent.ForwardClick -> enhancedNavigationManager?.goForward()
                is ContextHierarchyScreenEvent.HistoryClick -> enhancedNavigationManager?.showNavigationMenu()
                is ContextHierarchyScreenEvent.HideHistory -> enhancedNavigationManager?.hideNavigationMenu()

                is ContextHierarchyScreenEvent.BottomNavExpandedChange -> onBottomNavExpandedChange(event.isExpanded)
                is ContextHierarchyScreenEvent.ShowRecentLists -> _showRecentListsSheet.value = true
                is ContextHierarchyScreenEvent.DismissRecentLists -> _showRecentListsSheet.value = false
                is ContextHierarchyScreenEvent.RecentItemSelected -> onRecentItemSelected(event.item)
                is ContextHierarchyScreenEvent.RecentItemPinClick -> toggleRecentItemPin(event.item)
                is ContextHierarchyScreenEvent.DayPlanClick -> onDayPlanClicked()
                is ContextHierarchyScreenEvent.ContextSelected -> onContextSelected(event.name)
                is ContextHierarchyScreenEvent.CommandDeckClick -> {
                    enhancedNavigationManager?.navigate(
                        route = COMMAND_DECK_ROUTE,
                        builder = {
                            popUpTo(COMMAND_DECK_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        },
                    )
                }

                is ContextHierarchyScreenEvent.EditRequest -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(event.projectId))
                    }
                }
                is ContextHierarchyScreenEvent.OpenContextRequest -> {
                    dialogUseCase.dismissDialog()
                    onProjectClicked(event.projectId)
                }
                is ContextHierarchyScreenEvent.AddToDayPlanRequest -> {
                    viewModelScope.launch {
                        val today = System.currentTimeMillis()
                        val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
                        dayManagementRepository.addProjectToDayPlan(dayPlan.id, event.projectId)
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект додано до плану дня"))
                    }
                }
                is ContextHierarchyScreenEvent.AddToDayFocusRequest -> {
                    val projectName =
                        _hierarchyPresentationFlat.value
                            .firstOrNull { it.id == event.projectId }
                            ?.name
                            ?: return

                    viewModelScope.launch {
                        val today = System.currentTimeMillis()
                        val dayPlan = dayManagementRepository.createOrUpdateDayPlan(today)
                        dayFocusesRepository.addItem(
                            dayPlanId = dayPlan.id,
                            title = projectName,
                            notes = null,
                            relatedLinks =
                                listOf(
                                    RelatedLink(
                                        type = LinkType.CONTEXT,
                                        target = event.projectId,
                                        displayName = projectName,
                                    ),
                                ),
                            type = DayFocusType.FOCUS,
                            order = dayFocusesRepository.nextOrderForDayPlan(dayPlan.id),
                            budgetPercent = null,
                        )
                        _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст додано у фокус дня"))
                    }
                }
                is ContextHierarchyScreenEvent.SetReminderRequest -> {
                    val presentation =
                        _hierarchyPresentationFlat.value
                            .firstOrNull { it.id == event.projectId }
                            ?: return
                    dialogUseCase.onSetReminderForProject(
                        scope = viewModelScope,
                        projectId = presentation.id,
                        projectName = presentation.name,
                        projectCreatedAt = 0L,
                    )
                }
                is ContextHierarchyScreenEvent.FocusHierarchyProject -> {
                    viewModelScope.launch {
                        hierarchyFocusCoordinator.revealProject(
                            projectId = event.projectId,
                            placementId = event.placementId,
                            currentHierarchy = projectHierarchyScreenStateUseCase.presentationHierarchy.value,
                            currentSubState = uiState.value.currentSubState,
                            currentBreadcrumbs = uiState.value.currentBreadcrumbs,
                            orientationHierarchy = uiState.value.orientationHierarchy,
                            canonicalRead = projectHierarchyScreenStateUseCase.canonicalV2Read.value,
                            enterFocus = true,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ToggleUserFocusContext -> {
                    viewModelScope.launch {
                        val focused = focusContextRepository.toggleFocusContext(event.projectId)
                        _uiEventChannel.send(
                            ProjectUiEvent.ShowToast(
                                if (focused) {
                                    "Контекст додано у фокус"
                                } else {
                                    "Контекст прибрано з фокусу"
                                },
                            ),
                        )
                        dialogUseCase.dismissDialog()
                    }
                }
                is ContextHierarchyScreenEvent.CopyWorkspace -> {
                    val result =
                        workspaceClipboardCoordinator.copyWorkspace(event.projectId)
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        emitWorkspaceClipboardResult(result)
                    }
                }
                is ContextHierarchyScreenEvent.CutWorkspace -> {
                    val result =
                        workspaceClipboardCoordinator.cutWorkspace(event.projectId)
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        emitWorkspaceClipboardResult(result)
                    }
                }
                is ContextHierarchyScreenEvent.PasteWorkspace -> {
                    viewModelScope.launch {
                        emitWorkspaceClipboardResult(
                            workspaceClipboardCoordinator.pasteInto(event.projectId),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.CopyContextLink -> {
                    val toast = contextClipboardCoordinator.copyContextAsLink(
                        contextId = event.projectId,
                        occurrence = event.occurrence,
                    )
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast(toast))
                    }
                }
                is ContextHierarchyScreenEvent.CutContextLink -> {
                    val toast =
                        contextClipboardCoordinator.cutContext(
                            contextId = event.projectId,
                            occurrence = event.occurrence,
                        )
                    dialogUseCase.dismissDialog()
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast(toast))
                    }
                }
                is ContextHierarchyScreenEvent.PasteContextLink -> {
                    viewModelScope.launch {
                        emitClipboardResult(
                            contextClipboardCoordinator.pasteIntoContext(
                                targetContextId = event.projectId,
                                destinationOccurrence = event.destinationOccurrence,
                                hierarchyRead = projectHierarchyScreenStateUseCase.canonicalV2Read.value,
                            ),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.PasteContextLinksIntoBeacon -> {
                    viewModelScope.launch {
                        if (workspaceClipboardCoordinator.hasPayload()) {
                            emitWorkspaceClipboardResult(
                                workspaceClipboardCoordinator.pasteIntoBeacon(
                                    beaconId = event.beaconNodeId,
                                ),
                            )
                        } else {
                            emitClipboardResult(
                                contextClipboardCoordinator.pasteIntoBeacon(
                                    beaconNodeId = event.beaconNodeId,
                                    orientationHierarchy = uiState.value.orientationHierarchy,
                                ),
                            )
                        }
                    }
                }
                ContextHierarchyScreenEvent.PasteContextLinksIntoNoBeacon -> {
                    viewModelScope.launch {
                        emitClipboardResult(
                            contextClipboardCoordinator.pasteIntoNoBeacon(),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.CopyBeacon -> {
                    val toast = contextClipboardCoordinator.copyBeacon(event.beaconNodeId)
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast(toast))
                    }
                }
                is ContextHierarchyScreenEvent.CopyBeaconAsLink -> {
                    val toast = contextClipboardCoordinator.copyBeaconAsLink(event.beaconNodeId)
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast(toast))
                    }
                }
                is ContextHierarchyScreenEvent.CutBeacon -> {
                    val toast = contextClipboardCoordinator.cutBeacon(event.beaconNodeId)
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.ShowToast(toast))
                    }
                }
                is ContextHierarchyScreenEvent.PasteBeaconIntoBeacon -> {
                    viewModelScope.launch {
                        if (workspaceClipboardCoordinator.hasPayload()) {
                            emitWorkspaceClipboardResult(
                                workspaceClipboardCoordinator.pasteIntoBeacon(
                                    beaconId = event.beaconNodeId,
                                ),
                            )
                        } else {
                            emitClipboardResult(
                                contextClipboardCoordinator.pasteBeaconIntoBeacon(event.beaconNodeId),
                            )
                        }
                    }
                }
                is ContextHierarchyScreenEvent.PasteBeaconIntoGroup -> {
                    viewModelScope.launch {
                        emitClipboardResult(
                            contextClipboardCoordinator.pasteBeaconIntoGroup(event.groupNodeId),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.AddContextAppearanceHere -> {
                    viewModelScope.launch {
                        emitClipboardResult(
                            contextClipboardCoordinator.addContextAppearance(
                                parentOccurrence = event.parentOccurrence,
                            ),
                        )
                    }
                }
                is ContextHierarchyScreenEvent.CopySelectedContexts -> {
                    val selectedIds = contextSelectionCoordinator.selectedIds.value
                    val result =
                        workspaceClipboardCoordinator.copyWorkspaces(selectedIds)

                    if (result.success) {
                        contextSelectionCoordinator.clear()
                    }

                    viewModelScope.launch {
                        emitWorkspaceClipboardResult(result)
                    }
                }
                is ContextHierarchyScreenEvent.CutSelectedContexts -> {
                    val selectedIds = contextSelectionCoordinator.selectedIds.value
                    val result =
                        workspaceClipboardCoordinator.cutWorkspaces(selectedIds)

                    if (result.success) {
                        contextSelectionCoordinator.clear()
                    }

                    viewModelScope.launch {
                        emitWorkspaceClipboardResult(result)
                    }
                }
                is ContextHierarchyScreenEvent.GoToSettings -> {
                    viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) }
                }
                is ContextHierarchyScreenEvent.ShowSearchDialog -> {
                    searchUseCase.onSearchQueryChanged(TextFieldValue(""))
                    searchUseCase.onToggleSearch(true)
                }
                is ContextHierarchyScreenEvent.DismissSearchDialog -> _showSearchDialog.value = false

                is ContextHierarchyScreenEvent.ShowWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiServerDialog()
                is ContextHierarchyScreenEvent.ShowWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onShowWifiImportDialog()
                is ContextHierarchyScreenEvent.WifiPush -> {
                    if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) {
                        syncUseCase.performWifiPush(event.address)
                    }
                }
                is ContextHierarchyScreenEvent.ExportToFile ->
                    dialogUseCase.onUtilityDialogRequest(UtilityDialogRequest.Export)
                is ContextHierarchyScreenEvent.ExportToFileV2 ->
                    viewModelScope.launch {
                        val result = contextActionsUseCase.exportToFileV2()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Export V2 successful")
                            } else {
                                ProjectUiEvent.ShowToast("Export V2 error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                is ContextHierarchyScreenEvent.ExportAttachments -> {
                    viewModelScope.launch {
                        val result = contextActionsUseCase.exportAttachments()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments export successful")
                            } else {
                                ProjectUiEvent.ShowToast("Attachments export error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.ImportAttachmentsFromFile -> {
                    Timber
                        .tag("SyncRepo_AttachmentsImport")
                        .d("MainScreenViewModel received ImportAttachmentsFromFile event with uri=${event.uri}")
                    viewModelScope.launch {
                        Timber.tag("SyncRepo_AttachmentsImport").d("Starting attachment import coroutine")
                        val result = contextActionsUseCase.importAttachments(event.uri)
                        Timber
                            .tag("SyncRepo_AttachmentsImport")
                            .d("Import completed with result: isSuccess=${result.isSuccess}, message=${result.getOrNull()}")
                        dialogUseCase.dismissDialog()
                        _uiEventChannel.send(
                            if (result.isSuccess) {
                                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Attachments import successful")
                            } else {
                                ProjectUiEvent.ShowToast("Attachments import error: ${result.exceptionOrNull()?.message}")
                            },
                        )
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToChat -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiChat] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Chat))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToActivityTrackerScreen -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Tracker))
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToAiInsights -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiInsights] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AiInsights))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.NavigateToLifeState -> {
                    if (uiState.value.featureToggles[FeatureFlag.AiLifeManagement] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.LifeState))
                        }
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToTacticsScreen -> {
                    viewModelScope.launch {
                        _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.TacticalManagement))
                    }
                }

                is ContextHierarchyScreenEvent.NavigateToStrategicManagement -> {
                    if (uiState.value.featureToggles[FeatureFlag.StrategicManagement] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.NavigateToStrategicManagement)
                        }
                    }
                }

                is ContextHierarchyScreenEvent.SaveSettings -> {
                    settingsUseCase.saveSettings(viewModelScope, event.settings)
                }
                is ContextHierarchyScreenEvent.SaveAllContextMarkers -> {
                    settingsUseCase.saveAllContextMarkers(viewModelScope, event.updatedContextMarkers)
                }
                is ContextHierarchyScreenEvent.DismissWifiServerDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiServerDialog()
                is ContextHierarchyScreenEvent.DismissWifiImportDialog -> if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) syncUseCase.onDismissWifiImportDialog()
                is ContextHierarchyScreenEvent.DesktopAddressChange ->
                    syncUseCase.onDesktopAddressChange(event.address)
                is ContextHierarchyScreenEvent.PerformWifiImport ->
                    if (uiState.value.featureToggles[FeatureFlag.WifiSync] == true) {
                        syncUseCase.performWifiImport(
                            event.address,
                        )
                    }
                is ContextHierarchyScreenEvent.AddContextConfirm -> {
                    viewModelScope.launch {
                        contextDialogActionCoordinator.confirmAddContext(
                            name = event.name,
                            parentId = event.parentId,
                            parentPlacementId = event.parentPlacementId,
                            roleCode = event.roleCode,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.CloseSearch -> searchUseCase.onCloseSearch()
                is ContextHierarchyScreenEvent.NavigateToContext -> onNavigateToProject(event.projectId)
                is ContextHierarchyScreenEvent.UpdateLightTheme -> themingUseCase.updateLightTheme(viewModelScope, event.themeName)
                is ContextHierarchyScreenEvent.UpdateDarkTheme -> themingUseCase.updateDarkTheme(viewModelScope, event.themeName)
                is ContextHierarchyScreenEvent.UpdateThemeMode -> themingUseCase.updateThemeMode(viewModelScope, event.themeMode)
                is ContextHierarchyScreenEvent.GoToReminders -> {
                    viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.Reminders)) }
                }
                is ContextHierarchyScreenEvent.OpenAttachmentsLibrary -> {
                    if (uiState.value.featureToggles[FeatureFlag.AttachmentsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.AttachmentsLibrary))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.AddScriptRequest -> {
                    if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptEditor()))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.OpenScriptsLibrary -> {
                    if (uiState.value.featureToggles[FeatureFlag.ScriptsLibrary] == true) {
                        viewModelScope.launch {
                            _uiEventChannel.send(ProjectUiEvent.Navigate(NavTarget.ScriptsLibrary))
                        }
                    }
                }
                is ContextHierarchyScreenEvent.RevealContextInHierarchy -> {
                    viewModelScope.launch {
                        revealProject(
                            projectId = event.projectId,
                            forceFocusMode = true,
                        )
                    }
                }
                is ContextHierarchyScreenEvent.OpenInboxContext -> {
                    viewModelScope.launch {
                        val inboxProjectId = getInboxProjectId()
                        if (inboxProjectId == null) {
                            _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox project not found"))
                            return@launch
                        }

                        _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(inboxProjectId))
                    }
                }
                else -> {}
            }
        }

        private fun handleBackNavigation() {
            hierarchyFocusCoordinator.handleBackNavigation(
                currentHierarchy = projectHierarchyScreenStateUseCase.presentationHierarchy.value,
                orientationHierarchy = uiState.value.orientationHierarchy,
                canonicalRead = projectHierarchyScreenStateUseCase.canonicalV2Read.value,
                goBack = { enhancedNavigationManager?.goBack() },
            )
        }

        private fun onProjectClicked(projectId: String) {
            viewModelScope.launch {
                val presentation =
                    _hierarchyPresentationFlat.value
                        .firstOrNull { it.id == projectId }
                when (
                    val navigation =
                        resolveHierarchyProjectNavigation(
                            projectId = projectId,
                            presentation = presentation,
                        )
                ) {
                    is HierarchyProjectNavigation.ContextDetail -> {
                        recentItemsRepository.logProjectAccess(
                            projectId = navigation.projectId,
                            displayName = navigation.title,
                        )
                        enhancedNavigationManager?.navigateToProject(
                            navigation.projectId,
                            navigation.title,
                        )
                    }
                    is HierarchyProjectNavigation.HierarchyRead ->
                        enhancedNavigationManager?.navigate(
                            target =
                                NavTarget.ContextHierarchy(
                                    projectIdToReveal = navigation.projectId,
                                ),
                            recordInHistory = true,
                            historyTitle = navigation.title,
                        )
                    null -> Unit
                }
            }
        }

        private fun onHomeClicked() {
            navigationUseCase.onNavigateHome(viewModelScope)
        }

        private fun onBottomNavExpandedChange(isExpanded: Boolean) {
            viewModelScope.launch {
                _isBottomNavExpanded.value = isExpanded
                contextActionsUseCase.onBottomNavExpandedChange(isExpanded)
            }
        }

        private fun onRecentItemSelected(item: RecentItem) {
            viewModelScope.launch {
                _showRecentListsSheet.value = false
                when (item.type) {
                    RecentItemType.PROJECT -> {
                        val presentation =
                            _hierarchyPresentationFlat.value
                                .firstOrNull { it.id == item.target }
                        when (
                            val navigation =
                                resolveHierarchyProjectNavigation(
                                    projectId = item.target,
                                    presentation = presentation,
                                )
                        ) {
                            is HierarchyProjectNavigation.ContextDetail -> {
                                recentItemsRepository.logProjectAccess(
                                    projectId = navigation.projectId,
                                    displayName = navigation.title,
                                )
                                searchUseCase.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                                enhancedNavigationManager?.navigateToProject(
                                    navigation.projectId,
                                    navigation.title,
                                )
                            }

                            is HierarchyProjectNavigation.HierarchyRead -> {
                                searchUseCase.popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                                enhancedNavigationManager?.navigate(
                                    target =
                                        NavTarget.ContextHierarchy(
                                            projectIdToReveal = navigation.projectId,
                                        ),
                                    recordInHistory = true,
                                    historyTitle = navigation.title,
                                )
                            }

                            null -> {
                                recentItemsRepository.removeRecentItem(item.id)
                                _uiEventChannel.send(ProjectUiEvent.ShowToast("Контекст більше не існує"))
                            }
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
                                NavTarget.NoteDocument(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.CHECKLIST -> {
                        checklistRepository.getChecklistById(item.target)?.let {
                            recentItemsRepository.logChecklistAccess(it)
                        }
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.Checklist(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.MUSIC_NOTE -> {
                        musicNoteRepository.getById(item.target)?.let {
                            recentItemsRepository.logMusicNoteAccess(it)
                        }
                        _uiEventChannel.send(
                            ProjectUiEvent.Navigate(
                                NavTarget.MusicNote(id = item.target),
                            ),
                        )
                    }
                    RecentItemType.OBSIDIAN_LINK -> {
                        val link =
                            RelatedLink(
                                target = item.target,
                                displayName = item.displayName,
                                type = LinkType.OBSIDIAN,
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

        private fun confirmMove(
            newParentId: String?,
            destinationPlacementId: String? = null,
        ) {
            viewModelScope.launch {
                contextDialogActionCoordinator.confirmMove(
                    destinationPlacementId = destinationPlacementId,
                )
            }
        }

        private fun createNoteInInbox() {
            viewModelScope.launch {
                val inboxId = getInboxProjectId()
                if (inboxId == null) {
                    _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено"))
                    return@launch
                }
                createNoteInContext(inboxId)
            }
        }

        private fun createChecklistInInbox() {
            viewModelScope.launch {
                val inboxId = getInboxProjectId()
                if (inboxId == null) {
                    _uiEventChannel.send(ProjectUiEvent.ShowToast("Inbox проект не знайдено"))
                    return@launch
                }
                createChecklistInContext(inboxId)
            }
        }

        private fun createNoteInContext(contextId: String) {
            viewModelScope.launch {
                val documentId =
                    noteDocumentRepository.createDocument(
                        name = "Нова нотатка",
                        contextId = contextId,
                        content = "",
                    )
                _uiEventChannel.send(
                    ProjectUiEvent.Navigate(
                        NavTarget.NoteDocument(id = documentId, startEdit = true),
                    ),
                )
            }
        }

        private fun createChecklistInContext(contextId: String) {
            viewModelScope.launch {
                val checklistId = checklistRepository.createChecklist(name = "Новий чекліст", contextId = contextId)
                _uiEventChannel.send(
                    ProjectUiEvent.Navigate(
                        NavTarget.Checklist(id = checklistId),
                    ),
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
            viewModelScope.launch {
                _hierarchyPresentationFlat.value
                    .firstOrNull { it.id == projectId }
                    ?.let { presentation ->
                        recentItemsRepository.logProjectAccess(
                            projectId = presentation.id,
                            displayName = presentation.name,
                        )
                    }
                navigationUseCase.onNavigateToProject(viewModelScope, projectId)
            }
        }

        private suspend fun emitWorkspaceClipboardResult(
            result: WorkspaceClipboardResult,
        ) {
            if (result.dismissDialog) {
                dialogUseCase.dismissDialog()
            }
            _uiEventChannel.send(ProjectUiEvent.ShowToast(result.toast))
        }

        private suspend fun emitClipboardResult(result: ContextClipboardResult) {
            if (result.dismissDialog) {
                dialogUseCase.dismissDialog()
            }
            _uiEventChannel.send(ProjectUiEvent.ShowToast(result.toast))
        }

        private suspend fun awaitHierarchyForProjectPath(
            projectId: String,
        ): com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData {
            return withTimeoutOrNull(1_500) {
                projectHierarchyScreenStateUseCase.presentationHierarchy.first { hierarchy ->
                    buildPresentationPathToProject(projectId, hierarchy).isNotEmpty()
                }
            } ?: projectHierarchyScreenStateUseCase.presentationHierarchy.value
        }

        suspend fun getInboxProjectPresentation(): HierarchyContextPresentationNode? =
            withContext(ioDispatcher) {
                val allProjects = _hierarchyPresentationFlat.first()

                allProjects
                    .firstOrNull { it.id == SystemContexts.INBOX.raw }
                    ?: allProjects
                        .firstOrNull {
                            !SystemContexts.isSystem(ContextId(it.id)) &&
                                it.name.equals("Inbox", ignoreCase = true)
                        }
            }

        suspend fun getInboxProjectId(): String? = getInboxProjectPresentation()?.id

        internal fun resolveProjectNavigation(projectId: String): HierarchyProjectNavigation? {
            val presentation =
                _hierarchyPresentationFlat.value.firstOrNull { it.id == projectId }
            return resolveHierarchyProjectNavigation(
                projectId = projectId,
                presentation = presentation,
            )
        }

        override fun onCleared() {
            navigationStateJob?.cancel()
            navigationResultJob?.cancel()
            navigationUseCase.detach()
            super.onCleared()
        }
    }
