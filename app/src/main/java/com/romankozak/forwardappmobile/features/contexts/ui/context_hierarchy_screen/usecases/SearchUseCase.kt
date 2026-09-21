package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultFilter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultSort
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.navigation.RevealResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.buildPresentationPathToProject
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.shouldUseHierarchyFocusModeForBreadcrumbNames
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class SearchUseCase
    @Inject
    constructor(
        private val recentItemsRepository: RecentItemsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : PlanningSearchAdapter {
        private lateinit var scope: CoroutineScope
        private lateinit var uiEventChannel: Channel<ProjectUiEvent>
        private lateinit var onProjectAccess: suspend (String) -> Unit
        private lateinit var hierarchyPresentationFlat: StateFlow<List<HierarchyContextPresentationNode>>

        fun initialize(
            scope: CoroutineScope,
            uiEventChannel: Channel<ProjectUiEvent>,
            onProjectAccess: suspend (String) -> Unit,
            hierarchyPresentationFlat: StateFlow<List<HierarchyContextPresentationNode>>,
        ) {
            this.scope = scope
            this.uiEventChannel = uiEventChannel
            this.onProjectAccess = onProjectAccess
            this.hierarchyPresentationFlat = hierarchyPresentationFlat
            initializeSearchState()
        }

        companion object {
            private const val TAG = "SearchUseCase_DEBUG"
            private const val SEARCH_HISTORY_KEY = "search_history"
            private const val MAX_SEARCH_HISTORY = 10
            private const val ACTIVE_SEARCH_QUERY_TEXT_KEY = "active_search_query_text"
            private const val IS_SEARCH_ACTIVE_KEY = "is_search_active"
            private const val CAME_FROM_GLOBAL_SEARCH_KEY = "came_from_global_search"
        }

        private val _isPendingStateRestoration = MutableStateFlow(false)
        val isPendingStateRestoration = _isPendingStateRestoration.asStateFlow()

        private val _searchQuery = MutableStateFlow(TextFieldValue(""))
        override val searchQuery = _searchQuery.asStateFlow()

        private val _isSearchActive = MutableStateFlow(false)
        val isSearchActive = _isSearchActive.asStateFlow()

        private val _searchResultFilter = MutableStateFlow(SearchResultFilter.All)
        val searchResultFilter = _searchResultFilter.asStateFlow()

        private val _searchResultSort = MutableStateFlow(SearchResultSort.Relevance)
        val searchResultSort = _searchResultSort.asStateFlow()

        val highlightedProjectId = MutableStateFlow<String?>(null)
        val currentBreadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
        val focusedProjectId = MutableStateFlow<String?>(null)
        val hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())

        val searchHistory: StateFlow<List<String>> =
            savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())

        private val _subStateStack = MutableStateFlow<List<MainSubState>>(listOf(ProjectHierarchyScreenSubState.Hierarchy))
        override val subStateStack = _subStateStack.asStateFlow()

        private fun initializeSearchState() {
            scope.launch {
                val savedIsActive = savedStateHandle.get<Boolean>(IS_SEARCH_ACTIVE_KEY) ?: false
                if (savedIsActive) {
                    _isPendingStateRestoration.value = true
                    val savedQueryText = savedStateHandle.get<String>(ACTIVE_SEARCH_QUERY_TEXT_KEY) ?: ""
                    _searchQuery.value = TextFieldValue(savedQueryText, TextRange(savedQueryText.length))
                    _isSearchActive.value = true
                    delay(50)
                    _isPendingStateRestoration.value = false
                }
            }
        }

        override fun onToggleSearch(isActive: Boolean) {
            savedStateHandle[IS_SEARCH_ACTIVE_KEY] = isActive
            _isSearchActive.value = isActive
            if (isActive) {
                val currentText = _searchQuery.value.text
                _searchQuery.value = TextFieldValue(currentText, TextRange(currentText.length))
                scope.launch { uiEventChannel.send(ProjectUiEvent.FocusSearchField) }
            } else {
                if (_searchQuery.value.text.isNotBlank()) {
                    addSearchQueryToHistory(_searchQuery.value.text)
                }
                _searchQuery.value = TextFieldValue("")
            }
        }

        fun onSearchQueryChanged(query: TextFieldValue) {
            savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = query.text
            _searchQuery.value = query
            if (!isSearchActive()) {
                pushSubState(ProjectHierarchyScreenSubState.LocalSearch(query.text))
            } else {
                replaceCurrentSubState(ProjectHierarchyScreenSubState.LocalSearch(query.text))
            }
        }

        fun onSearchQueryFromHistory(query: String) {
            pushSubState(ProjectHierarchyScreenSubState.LocalSearch(query))
            onSearchQueryChanged(TextFieldValue(query, TextRange(query.length)))
            onToggleSearch(true)
        }

        fun removeSearchHistoryEntry(query: String) {
            if (query.isBlank()) return
            val currentHistory = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            savedStateHandle[SEARCH_HISTORY_KEY] =
                currentHistory.filterNot { it.equals(query, ignoreCase = true) }
        }

        fun clearSearchHistory() {
            savedStateHandle[SEARCH_HISTORY_KEY] = emptyList<String>()
        }

        fun onSearchFilterChanged(filter: SearchResultFilter) {
            _searchResultFilter.value = filter
        }

        fun onSearchSortChanged(sort: SearchResultSort) {
            _searchResultSort.value = sort
        }

        fun clearAllSearchState() {
            savedStateHandle[IS_SEARCH_ACTIVE_KEY] = false
            savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = ""
            _searchQuery.value = TextFieldValue("")
            _isSearchActive.value = false
        }

        suspend fun revealProjectInHierarchy(projectId: String): RevealResult {
            Log.d(TAG, "Початок 'revealProjectInHierarchy' для ID: $projectId")
            return withContext(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main.immediate) {
                        if (_isSearchActive.value) {
                            _isSearchActive.value = false
                            _searchQuery.value = TextFieldValue("")
                        }
                    }

                    val presentationLookup =
                        hierarchyPresentationFlat.value.associateBy { it.id }
                    if (projectId !in presentationLookup) {
                        Log.w(TAG, "Reveal rejected: $projectId is absent from the presentation universe")
                        return@withContext RevealResult.Failure
                    }
                    val breadcrumbNames =
                        buildBreadcrumbNames(
                            projectId,
                            presentationLookup,
                        )
                    val shouldFocus =
                        shouldUseHierarchyFocusModeForBreadcrumbNames(
                            breadcrumbNames = breadcrumbNames,
                            hasFocusedProject = false,
                        )
                    Log.d(
                        TAG,
                        "Reveal heuristic: segments=${breadcrumbNames.size}, shouldFocus=$shouldFocus",
                    )

                    withContext(Dispatchers.Main.immediate) {
                        clearAllSearchState()
                    }

                    RevealResult.Success(projectId, shouldFocus)
                } catch (e: Exception) {
                    Log.e(TAG, "Помилка в revealProjectInHierarchy", e)
                    RevealResult.Failure
                }
            }
        }

        private fun buildBreadcrumbNames(
            projectId: String,
            projectLookup: Map<String, HierarchyContextPresentationNode>,
        ): List<String> {
            val names = mutableListOf<String>()
            val visited = mutableSetOf<String>()
            var currentId: String? = projectId

            while (currentId != null && visited.add(currentId)) {
                val project = projectLookup[currentId] ?: break
                names.add(project.name)
                currentId = project.parentId
            }

            return names.asReversed()
        }

        fun navigateToProject(
            projectId: String,
            currentHierarchy: HierarchyPresentationData,
            breadcrumbPrefix: List<BreadcrumbItem> = emptyList(),
        ) {
            scope.launch {
                onProjectAccess(projectId)

                val path =
                    buildPresentationPathToProject(projectId, currentHierarchy)
                        .mapIndexed { index, project ->
                            BreadcrumbItem(
                                id = project.id,
                                name = project.name,
                                level = index,
                            )
                        }
                currentBreadcrumbs.value =
                    if (breadcrumbPrefix.isEmpty()) {
                        path
                    } else {
                        breadcrumbPrefix + path.map { it.copy(level = it.level + breadcrumbPrefix.size) }
                    }
                focusedProjectId.value = projectId
            }
        }

        fun reconcileFocusedProjectBreadcrumbs(
            currentHierarchy: HierarchyPresentationData,
        ) {
            val focusedProject =
                _subStateStack.value.lastOrNull() as? ProjectHierarchyScreenSubState.ProjectFocused
                    ?: return

            val canonicalPath =
                buildPresentationPathToProject(
                    targetId = focusedProject.projectId,
                    hierarchy = currentHierarchy,
                )
            if (canonicalPath.isEmpty()) return

            val orientationPrefix =
                currentBreadcrumbs.value
                    .takeWhile { breadcrumb ->
                        breadcrumb.target == BreadcrumbTarget.OrientationNode
                    }

            val projectBreadcrumbs =
                canonicalPath.mapIndexed { index, project ->
                    BreadcrumbItem(
                        id = project.id,
                        name = project.name,
                        level = orientationPrefix.size + index,
                    )
                }

            val reconciled = orientationPrefix + projectBreadcrumbs
            if (currentBreadcrumbs.value != reconciled) {
                currentBreadcrumbs.value = reconciled
            }
        }

        fun navigateToProjectWithBreadcrumbs(
            projectId: String,
            breadcrumbs: List<BreadcrumbItem>,
        ) {
            scope.launch {
                onProjectAccess(projectId)
                currentBreadcrumbs.value =
                    breadcrumbs.mapIndexed { index, breadcrumb ->
                        breadcrumb.copy(level = index)
                    }
                focusedProjectId.value = projectId
            }
        }

        fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
            currentBreadcrumbs.update { it.take(breadcrumbItem.level + 1) }
            when (breadcrumbItem.target) {
                BreadcrumbTarget.OrientationNode -> {
                    focusedProjectId.value = null
                    navigateToExistingOrReplace(
                        ProjectHierarchyScreenSubState.OrientationFocused(
                            nodeId = breadcrumbItem.id,
                            placementId = breadcrumbItem.placementId,
                        ),
                    )
                }
                BreadcrumbTarget.Context -> {
                    focusedProjectId.value = breadcrumbItem.id
                    navigateToExistingOrReplace(
                        ProjectHierarchyScreenSubState.ProjectFocused(
                            projectId = breadcrumbItem.id,
                            placementId = breadcrumbItem.placementId,
                        ),
                    )
                }
            }
        }

        fun clearNavigation() {
            focusedProjectId.value = null
            currentBreadcrumbs.value = emptyList()
            popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
        }

        private fun addSearchQueryToHistory(query: String) {
            if (query.isBlank()) return

            val currentHistory = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
            val mutableHistory = currentHistory.toMutableList()

            val existingIndex = mutableHistory.indexOfFirst { it.equals(query, ignoreCase = true) }
            if (existingIndex != -1) {
                mutableHistory.removeAt(existingIndex)
            }

            mutableHistory.add(0, query)
            val newHistory = mutableHistory.take(MAX_SEARCH_HISTORY)
            savedStateHandle[SEARCH_HISTORY_KEY] = newHistory
        }

        fun onCloseSearch() {
            onToggleSearch(false)
            popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
            clearNavigation()
            scope.launch { uiEventChannel.send(ProjectUiEvent.HideKeyboard) }
        }

        fun onSearchResultClick(
            projectId: String,
            placementId: String? = null,
            currentHierarchy: HierarchyPresentationData,
            orientationHierarchy: List<OrientationHierarchyItem> = emptyList(),
        ) {
            scope.launch {
                if (placementId != null) {
                    val breadcrumbs =
                        buildOrientationBreadcrumbsToContext(
                            items = orientationHierarchy,
                            contextId = projectId,
                            placementId = placementId,
                        )
                    if (breadcrumbs.isEmpty()) {
                        uiEventChannel.send(ProjectUiEvent.ShowToast("Не вдалося показати локацію"))
                        return@launch
                    }

                    clearAllSearchState()
                    navigateToProjectWithBreadcrumbs(
                        projectId = projectId,
                        breadcrumbs = breadcrumbs,
                    )
                    enterProjectFocusPath(
                        projectId = projectId,
                        breadcrumbs = breadcrumbs,
                        placementId = placementId,
                    )
                    return@launch
                }

                when (val result = revealProjectInHierarchy(projectId)) {
                    is RevealResult.Success -> {
                        enterProjectFocus(
                            projectId = result.projectId,
                            placementId = result.placementId,
                        )
                        navigateToProject(
                            result.projectId,
                            currentHierarchy,
                        )
                        onSearchQueryChanged(TextFieldValue(""))
                    }
                    is RevealResult.Failure -> {
                        uiEventChannel.send(ProjectUiEvent.ShowToast("Не вдалося показати локацію"))
                    }
                }
            }
        }

        fun onPerformGlobalSearch(query: String) {
            if (query.isNotBlank()) {
                onSearchQueryFromHistory(query)
                if (isSearchActive.value) {
                    onToggleSearch(isActive = false)
                }
                scope.launch { uiEventChannel.send(ProjectUiEvent.NavigateToGlobalSearch(query)) }
            }
        }

        fun pushSubState(subState: MainSubState) {
            val currentStack = _subStateStack.value
            if (currentStack.lastOrNull() != subState) {
                _subStateStack.value = currentStack + subState
            }
        }

        /**
         * Replaces focus navigation with the already-resolved operational
         * breadcrumb path. Used for an explicit external reveal such as
         * "Show in hierarchy", where Back should walk to the displayed
         * operational parent rather than fall straight to hierarchy root.
         */
        fun enterProjectFocusPath(
            projectId: String,
            breadcrumbs: List<BreadcrumbItem>,
            placementId: String? = null,
        ) {
            val breadcrumbStates =
                breadcrumbs.mapNotNull { breadcrumb ->
                    when (breadcrumb.target) {
                        BreadcrumbTarget.OrientationNode ->
                            ProjectHierarchyScreenSubState.OrientationFocused(
                                nodeId = breadcrumb.id,
                                placementId = breadcrumb.placementId,
                            )
                        BreadcrumbTarget.Context ->
                            ProjectHierarchyScreenSubState.ProjectFocused(
                                projectId = breadcrumb.id,
                                placementId = breadcrumb.placementId,
                            )
                    }
                }

            val targetState =
                ProjectHierarchyScreenSubState.ProjectFocused(
                    projectId = projectId,
                    placementId = placementId,
                )
            val normalizedStates =
                buildList {
                    breadcrumbStates.forEach { state ->
                        if (lastOrNull() != state) add(state)
                    }
                    if (lastOrNull() != targetState) add(targetState)
                }

            _subStateStack.value =
                listOf(ProjectHierarchyScreenSubState.Hierarchy) + normalizedStates
            focusedProjectId.value = projectId
        }

        fun enterProjectFocus(
            projectId: String,
            placementId: String? = null,
        ) {
            val targetState =
                ProjectHierarchyScreenSubState.ProjectFocused(
                    projectId = projectId,
                    placementId = placementId,
                )
            when (val currentState = _subStateStack.value.lastOrNull()) {
                is ProjectHierarchyScreenSubState.ProjectFocused -> {
                    if (
                        currentState.projectId != projectId ||
                        currentState.placementId != placementId
                    ) {
                        pushSubState(targetState)
                    }
                }
                else -> pushSubState(targetState)
            }
        }

        fun popSubState(): MainSubState? {
            val currentStack = _subStateStack.value
            return if (currentStack.size > 1) {
                val popped = currentStack.last()
                _subStateStack.value = currentStack.dropLast(1)
                popped
            } else {
                null
            }
        }

        fun replaceCurrentSubState(newState: MainSubState) {
            val currentStack = _subStateStack.value
            _subStateStack.value = currentStack.dropLast(1) + newState
        }

        private fun navigateToExistingOrReplace(targetState: MainSubState) {
            val currentStack = _subStateStack.value
            val targetIndex = currentStack.indexOfLast { it == targetState }
            if (targetIndex >= 0) {
                _subStateStack.value = currentStack.take(targetIndex + 1)
            } else {
                replaceCurrentSubState(targetState)
            }
        }

        override fun popToSubState(targetState: MainSubState) {
            val currentStack = _subStateStack.value
            val targetIndex = currentStack.indexOfLast { it == targetState }
            if (targetIndex >= 0) {
                _subStateStack.value = currentStack.take(targetIndex + 1)
            } else {
                _subStateStack.value = listOf(targetState)
            }
        }

        override fun isSearchActive(): Boolean {
            return _subStateStack.value.any { it is ProjectHierarchyScreenSubState.LocalSearch }
        }

        fun handleBackNavigation(
            currentHierarchy: HierarchyPresentationData,
            orientationHierarchy: List<OrientationHierarchyItem>,
            goBack: () -> Unit,
        ) {
            val currentStack = _subStateStack.value
            val beaconBreadcrumbPrefix =
                currentBreadcrumbs.value
                    .takeWhile { it.target == BreadcrumbTarget.OrientationNode }
            when {
                currentStack.lastOrNull() is ProjectHierarchyScreenSubState.ProjectFocused -> {
                    popSubState()
                    when (val previousFocusedState = _subStateStack.value.lastOrNull()) {
                        is ProjectHierarchyScreenSubState.ProjectFocused -> {
                            val placementId = previousFocusedState.placementId
                            if (placementId != null) {
                                val exactBreadcrumbs =
                                    buildOrientationBreadcrumbsToContext(
                                        items = orientationHierarchy,
                                        contextId = previousFocusedState.projectId,
                                        placementId = placementId,
                                    )
                                if (exactBreadcrumbs.isEmpty()) {
                                    // Exact occurrence history must never degrade into
                                    // target-only V1 presentation navigation.
                                    clearNavigation()
                                } else {
                                    navigateToProjectWithBreadcrumbs(
                                        projectId = previousFocusedState.projectId,
                                        breadcrumbs = exactBreadcrumbs,
                                    )
                                }
                            } else {
                                navigateToProject(
                                    projectId = previousFocusedState.projectId,
                                    currentHierarchy = currentHierarchy,
                                    breadcrumbPrefix = beaconBreadcrumbPrefix,
                                )
                            }
                        }
                        is ProjectHierarchyScreenSubState.OrientationFocused -> {
                            focusedProjectId.value = null
                            currentBreadcrumbs.value =
                                currentBreadcrumbs.value
                                    .takeWhile { it.target == BreadcrumbTarget.OrientationNode }
                        }
                        else -> clearNavigation()
                    }
                }
                currentStack.lastOrNull() is ProjectHierarchyScreenSubState.OrientationFocused -> {
                    popSubState()
                    focusedProjectId.value = null
                    currentBreadcrumbs.value = emptyList()
                }
                currentStack.lastOrNull() is ProjectHierarchyScreenSubState.LocalSearch -> {
                    onCloseSearch()
                }
                currentBreadcrumbs.value.isNotEmpty() -> {
                    clearNavigation()
                }
                else -> {
                    goBack()
                }
            }
        }

        fun handleNavigationResult(
            key: String,
            value: String,
            projectHierarchy: HierarchyPresentationData,
        ) {
            when (key) {
                "project_to_reveal" -> {
                    scope.launch {
                        savedStateHandle[CAME_FROM_GLOBAL_SEARCH_KEY] = true

                        when (val result = revealProjectInHierarchy(value)) {
                            is RevealResult.Success -> {
                                enterProjectFocus(result.projectId)
                                navigateToProject(
                                    result.projectId,
                                    projectHierarchy,
                                )
                            }
                            is RevealResult.Failure -> {
                                uiEventChannel.send(ProjectUiEvent.ShowToast("Не удалось показать локацию"))
                            }
                        }
                    }
                }
            }
        }
    }

typealias ProjectHierarchyScreenSearchUseCase = SearchUseCase
