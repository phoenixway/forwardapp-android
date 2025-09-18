package com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.buildPathToProject
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.flattenHierarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Управляє пошуком та навігацією по проектах
 */
class SearchAndNavigationManager(
    private val projectRepository: ProjectRepository,
    private val viewModelScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val uiEventChannel: Channel<ProjectUiEvent>,
    private val allProjectsFlat: StateFlow<List<Project>>
) {

    companion object {
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10
    }

    // Search states
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _highlightedProjectId = MutableStateFlow<String?>(null)
    val highlightedProjectId: StateFlow<String?> = _highlightedProjectId.asStateFlow()

    // Navigation states
    private val _currentBreadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val currentBreadcrumbs = _currentBreadcrumbs.asStateFlow()

    private val _focusedProjectId = MutableStateFlow<String?>(null)
    val focusedProjectId = _focusedProjectId.asStateFlow()

    private val _hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())
    val hierarchySettings = _hierarchySettings.asStateFlow()

    private val _collapsedAncestorsOnFocus = MutableStateFlow<Set<String>>(emptySet())

    val searchHistory: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())

    fun onToggleSearch(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (isActive) {
            val currentText = _searchQuery.value.text
            _searchQuery.value = TextFieldValue(currentText, TextRange(0, currentText.length))
            viewModelScope.launch {
                uiEventChannel.send(ProjectUiEvent.FocusSearchField)
            }
        } else {
            val query = _searchQuery.value.text
            if (query.isNotBlank()) {
                addSearchQueryToHistory(query)
            }
            _searchQuery.value = TextFieldValue("")
        }
    }

    fun onSearchQueryChanged(query: TextFieldValue) {
        _searchQuery.value = query
    }

    fun onSearchQueryFromHistory(query: String) {
        _searchQuery.value = TextFieldValue(query)
        onToggleSearch(true)
    }

    private fun addSearchQueryToHistory(query: String) {
        val currentHistory = savedStateHandle[SEARCH_HISTORY_KEY] ?: emptyList<String>()
        val mutableHistory = currentHistory.toMutableList()

        val existingIndex = mutableHistory.indexOfFirst { it.lowercase() == query.lowercase() }
        if (existingIndex != -1) {
            mutableHistory.removeAt(existingIndex)
        }

        mutableHistory.add(0, query)
        val newHistory = mutableHistory.take(MAX_SEARCH_HISTORY)
        savedStateHandle[SEARCH_HISTORY_KEY] = newHistory
    }

    fun onSearchResultClick(projectId: String, projectHierarchy: StateFlow<ListHierarchyData>, planningMode: MutableStateFlow<PlanningMode>) {
        viewModelScope.launch {
            onToggleSearch(isActive = false)

            val allProjects = allProjectsFlat.first()
            val fullHierarchy = ListHierarchyData(
                allProjects = allProjects,
                topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            )

            val path = buildPathToProject(projectId, fullHierarchy)
            val level = if (path.isEmpty()) 0 else path.last().level
            val settings = hierarchySettings.value

            if (level >= settings.useBreadcrumbsAfter) {
                navigateToProject(projectId, projectHierarchy)
            } else {
                processRevealRequest(projectId, planningMode)
            }
        }
    }

    fun processRevealRequest(projectId: String, planningMode: MutableStateFlow<PlanningMode>) {
        viewModelScope.launch {
            _isSearchActive.value = false
            planningMode.value = PlanningMode.All

            val allProjects = allProjectsFlat.first { it.isNotEmpty() }
            val projectLookup = allProjects.associateBy { it.id }

            if (!projectLookup.containsKey(projectId)) {
                uiEventChannel.send(ProjectUiEvent.ShowToast("Could not find project."))
                return@launch
            }

            val ancestorIds = mutableSetOf<String>()
            findAncestorsRecursive(projectId, projectLookup, ancestorIds, mutableSetOf())

            val projectsToExpand = ancestorIds
                .mapNotNull { projectLookup[it] }
                .filter { !it.isExpanded && it.id != projectId }

            if (projectsToExpand.isNotEmpty()) {
                projectRepository.updateProjects(projectsToExpand.map { it.copy(isExpanded = true) })

                allProjectsFlat.first { updatedProjectState ->
                    projectsToExpand.all { projectToExpand ->
                        updatedProjectState.find { it.id == projectToExpand.id }?.isExpanded == true
                    }
                }
            }

            val finalProjectState = allProjectsFlat.value
            val topLevel = finalProjectState.filter { it.parentId == null }.sortedBy { it.order }
            val childrenOnly = finalProjectState.filter { it.parentId != null }
            val childMap = childrenOnly.groupBy { it.parentId!! }
            val displayedProjects = flattenHierarchy(topLevel, childMap)

            val index = displayedProjects.indexOfFirst { it.id == projectId }

            if (index != -1) {
                uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
            }

            _highlightedProjectId.value = projectId
            delay(1500)
            if (_highlightedProjectId.value == projectId) {
                _highlightedProjectId.value = null
            }
        }
    }

    fun navigateToProject(projectId: String, projectHierarchy: StateFlow<ListHierarchyData>) {
        viewModelScope.launch(Dispatchers.Default) {
            val hierarchy = projectHierarchy.value
            val path = buildPathToProject(projectId, hierarchy)

            val collapsedIds = path
                .mapNotNull { breadcrumbItem ->
                    hierarchy.allProjects.find { it.id == breadcrumbItem.id }
                }
                .filter { !it.isExpanded }
                .map { it.id }
                .toSet()

            _collapsedAncestorsOnFocus.value = collapsedIds
            _currentBreadcrumbs.value = path
            _focusedProjectId.value = projectId
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        val currentPath = _currentBreadcrumbs.value
        val newPath = currentPath.take(breadcrumbItem.level + 1)
        _currentBreadcrumbs.value = newPath
        _focusedProjectId.value = breadcrumbItem.id
    }

    fun clearNavigation(projectHierarchy: StateFlow<ListHierarchyData>) {
        viewModelScope.launch(Dispatchers.Default) {
            val breadcrumbs = _currentBreadcrumbs.value
            val settings = hierarchySettings.value
            if (breadcrumbs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _focusedProjectId.value = null
                    _currentBreadcrumbs.value = emptyList()
                }
                return@launch
            }

            // Визначаємо цільового предка
            val targetAncestorLevel = (settings.useBreadcrumbsAfter - 1).coerceAtLeast(0)
            val targetAncestorBreadcrumb = breadcrumbs.getOrNull(targetAncestorLevel) ?: breadcrumbs.first()
            val targetAncestorId = targetAncestorBreadcrumb.id

            // Визначаємо шлях, який потрібно розкрити
            val pathIdsToExpand = breadcrumbs
                .take(targetAncestorLevel + 1)
                .map { it.id }
                .toSet()

            val allProjects = allProjectsFlat.first()
            val projectsToExpand = allProjects
                .filter { it.id in pathIdsToExpand && !it.isExpanded }
                .map { it.copy(isExpanded = true) }

            if (projectsToExpand.isNotEmpty()) {
                projectRepository.updateProjects(projectsToExpand)

                allProjectsFlat.first { updatedProjects ->
                    projectsToExpand.all { projectToUpdate ->
                        updatedProjects.find { it.id == projectToUpdate.id }?.isExpanded == true
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _focusedProjectId.value = null
            }

            delay(100)

            val finalHierarchy = projectHierarchy.value
            val displayedProjects = flattenHierarchy(finalHierarchy.topLevelProjects, finalHierarchy.childMap)
            val scrollIndex = displayedProjects.indexOfFirst { it.id == targetAncestorId }

            if (scrollIndex != -1) {
                uiEventChannel.send(ProjectUiEvent.ScrollToIndex(scrollIndex))
            }

            withContext(Dispatchers.Main) {
                _currentBreadcrumbs.value = emptyList()
                _collapsedAncestorsOnFocus.value = emptySet()
            }
        }
    }}