package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.navigation.RevealResult
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.utils.buildPathToProject
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.utils.findAncestorsRecursive
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
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.MainSubState
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState

@ViewModelScoped
class SearchUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val savedStateHandle: SavedStateHandle,
): PlanningSearchAdapter {
    private lateinit var scope: CoroutineScope
    private lateinit var uiEventChannel: Channel<ProjectUiEvent>
    private lateinit var allProjectsFlat: StateFlow<List<Project>>

    fun initialize(
        scope: CoroutineScope,
        uiEventChannel: Channel<ProjectUiEvent>,
        allProjectsFlat: StateFlow<List<Project>>
    ) {
        this.scope = scope
        this.uiEventChannel = uiEventChannel
        this.allProjectsFlat = allProjectsFlat
        initializeSearchState()
    }

    companion object {
        private const val TAG = "SearchUseCase_DEBUG"
        private const val FOCUS_DEPTH_THRESHOLD = 2
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

                val projectLookup = allProjectsFlat.value.associateBy { it.id }
                val ancestorIds = mutableSetOf<String>()
                findAncestorsRecursive(projectId, projectLookup, ancestorIds, mutableSetOf())

                val shouldFocus = ancestorIds.size >= FOCUS_DEPTH_THRESHOLD
                Log.d(TAG, "Глибина проекту: ${ancestorIds.size}. Потрібен фокус-режим: $shouldFocus")

                val projectsToExpand =
                    ancestorIds
                        .mapNotNull { projectLookup[it] }
                        .filter { !it.isExpanded }

                if (projectsToExpand.isNotEmpty()) {
                    projectRepository.updateProjects(projectsToExpand.map { it.copy(isExpanded = true) })
                    allProjectsFlat.first { currentProjects ->
                        projectsToExpand.all { toExpand ->
                            currentProjects.find { it.id == toExpand.id }?.isExpanded == true
                        }
                    }
                    Log.d(TAG, "Всі предки гарантовано розгорнуті.")
                }

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

    fun navigateToProject(
        projectId: String,
        currentHierarchy: ListHierarchyData,
    ) {
        scope.launch {
            projectRepository.getProjectById(projectId)?.let {
                recentItemsRepository.logProjectAccess(it)
            }
            val path = buildPathToProject(projectId, currentHierarchy)
            currentBreadcrumbs.value = path
            focusedProjectId.value = projectId
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        currentBreadcrumbs.update { it.take(breadcrumbItem.level + 1) }
        focusedProjectId.value = breadcrumbItem.id
        replaceCurrentSubState(ProjectHierarchyScreenSubState.ProjectFocused(breadcrumbItem.id))
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

    fun onSearchResultClick(projectId: String, currentHierarchy: ListHierarchyData) {
        scope.launch {
            when (val result = revealProjectInHierarchy(projectId)) {
                is RevealResult.Success -> {
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

    fun handleBackNavigation(areAnyProjectsExpanded: Boolean, collapseAllProjects: () -> Unit, goBack: () -> Unit) {
        val currentStack = _subStateStack.value
        when {
            currentStack.lastOrNull() is ProjectHierarchyScreenSubState.ProjectFocused -> {
                popSubState()
                clearNavigation()
            }
            currentStack.lastOrNull() is ProjectHierarchyScreenSubState.LocalSearch -> {
                onCloseSearch()
            }
            currentBreadcrumbs.value.isNotEmpty() -> {
                clearNavigation()
            }
            areAnyProjectsExpanded -> {
                collapseAllProjects()
            }
            else -> {
                goBack()
            }
        }
    }

    fun handleNavigationResult(key: String, value: String, projectHierarchy: ListHierarchyData, onProjectToReveal: (String) -> Unit) {
        when (key) {
            "project_to_reveal" -> {
                scope.launch {
                    savedStateHandle[CAME_FROM_GLOBAL_SEARCH_KEY] = true

                    when (val result = revealProjectInHierarchy(value)) {
                        is RevealResult.Success -> {
                            pushSubState(ProjectHierarchyScreenSubState.ProjectFocused(value))
                            if (result.shouldFocus) {
                                navigateToProject(
                                    result.projectId,
                                    projectHierarchy,
                                )
                            } else {
                                onProjectToReveal(result.projectId)
                                if (isSearchActive()) {
                                    popToSubState(ProjectHierarchyScreenSubState.Hierarchy)
                                }
                            }
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
