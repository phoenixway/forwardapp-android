package com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyManager
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
        private const val TAG = "SearchNavManager"
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
        Log.d(TAG, "onToggleSearch: isActive=$isActive")
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
        Log.d(TAG, "onSearchQueryChanged: '${query.text}'")
        _searchQuery.value = query
    }

    fun onSearchQueryFromHistory(query: String) {
        Log.d(TAG, "onSearchQueryFromHistory: '$query'")
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
        Log.d(TAG, "Updated search history: $newHistory")
    }

    fun onSearchResultClick(projectId: String, projectHierarchy: StateFlow<ListHierarchyData>, planningMode: MutableStateFlow<PlanningMode>) {
        Log.d(TAG, "onSearchResultClick: projectId=$projectId")
        viewModelScope.launch {
            // --- ПОЧАТОК ВИПРАВЛЕННЯ ---
            // Рядок onToggleSearch(isActive = false) ВИДАЛЕНО.
            // Тепер режим пошуку не вимикається при переході на екран проекту.
            // --- КІНЕЦЬ ВИПРАВЛЕННЯ ---

            val allProjects = allProjectsFlat.first()
            val fullHierarchy = ListHierarchyData(
                allProjects = allProjects,
                topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            )

            // Визначаємо шлях і рівень вкладеності
            val path = buildPathToProject(projectId, fullHierarchy)
            val level = if (path.isEmpty()) 0 else path.last().level
            val settings = hierarchySettings.value

            // Перевіряємо, чи має проект довгих нащадків
            // Примітка: для цього потрібна мапа longDescendantsMap, яку треба передати або обчислити тут
            val project = allProjects.firstOrNull { it.id == projectId }
            val longDescendantsMap = ProjectHierarchyManager().createLongDescendantsMap(allProjects)
            val hasLongDescendants = project?.let { longDescendantsMap[it.id] } ?: false

            // Вирішуємо, який режим використовувати: звичайний або фокусний
            if (level >= settings.useBreadcrumbsAfter || hasLongDescendants) {
                // Переходимо в фокус-режим
                navigateToProject(projectId, projectHierarchy)
            } else {
                // Залишаємося в звичайному режимі, просто розгортаємо ієрархію
                processRevealRequest(projectId, planningMode)
            }
        }
    }

    fun processRevealRequest(projectId: String, planningMode: MutableStateFlow<PlanningMode>) {
        Log.d(TAG, "processRevealRequest: projectId=$projectId")
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
                Log.d(TAG, "Expanding projects: ${projectsToExpand.map { it.id }}")
                projectRepository.updateProjects(projectsToExpand.map { it.copy(isExpanded = true) })

                allProjectsFlat.first { updatedProjectState ->
                    projectsToExpand.all { projectToExpand ->
                        updatedProjectState.find { it.id == projectToExpand.id }?.isExpanded == true
                    }
                }
                Log.d(TAG, "Projects expanded successfully")
            }

            val finalProjectState = allProjectsFlat.value
            val topLevel = finalProjectState.filter { it.parentId == null }.sortedBy { it.order }
            val childrenOnly = finalProjectState.filter { it.parentId != null }
            val childMap = childrenOnly.groupBy { it.parentId!! }
            val displayedProjects = flattenHierarchy(topLevel, childMap)

            val index = displayedProjects.indexOfFirst { it.id == projectId }

            if (index != -1) {
                Log.d(TAG, "Scrolling to index: $index for project $projectId")
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
        Log.d(TAG, "navigateToProject: projectId=$projectId")
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
            Log.d(TAG, "Focused on project: $projectId, breadcrumbs: ${path.map { it.id }}")
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        Log.d(TAG, "navigateToBreadcrumb: id=${breadcrumbItem.id}, level=${breadcrumbItem.level}")
        val currentPath = _currentBreadcrumbs.value
        val newPath = currentPath.take(breadcrumbItem.level + 1)
        _currentBreadcrumbs.value = newPath
        _focusedProjectId.value = breadcrumbItem.id
    }

    fun clearNavigation(projectHierarchy: StateFlow<ListHierarchyData>) {
        Log.d(TAG, "clearNavigation: STARTED")
        viewModelScope.launch(Dispatchers.Default) {
            val breadcrumbs = _currentBreadcrumbs.value
            val settings = hierarchySettings.value

            Log.d(TAG, "Current breadcrumbs: ${breadcrumbs.map { it.id }}")
            Log.d(TAG, "Settings: useBreadcrumbsAfter=${settings.useBreadcrumbsAfter}")

            if (breadcrumbs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _focusedProjectId.value = null
                    _currentBreadcrumbs.value = emptyList()
                }
                Log.d(TAG, "clearNavigation: breadcrumbs empty, cleared immediately")
                return@launch
            }

            val newBreadcrumbs = breadcrumbs.dropLast(1)

            if (newBreadcrumbs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _focusedProjectId.value = null
                    _currentBreadcrumbs.value = emptyList()
                    _collapsedAncestorsOnFocus.value = emptySet()
                }
                Log.d(TAG, "clearNavigation: returned to root level")
                return@launch
            }

            val targetAncestorBreadcrumb = newBreadcrumbs.last()
            val targetAncestorLevel = newBreadcrumbs.size - 1
            val targetAncestorId = targetAncestorBreadcrumb.id

            Log.d(TAG, "New breadcrumbs after back: ${newBreadcrumbs.map { it.id }}")
            Log.d(TAG, "Target ancestor level: $targetAncestorLevel, id: $targetAncestorId")

            val currentAllProjects = allProjectsFlat.first()
            val currentExpandedIds = currentAllProjects.filter { it.isExpanded }.map { it.id }.toSet()

            Log.d(TAG, "Currently expanded projects: $currentExpandedIds")

            val pathIdsToExpand = newBreadcrumbs.map { it.id }.toSet()

            Log.d(TAG, "Path to expand: $pathIdsToExpand")

            val projectsToExpand = currentAllProjects
                .filter { it.id in pathIdsToExpand && !it.isExpanded }
                .map { it.copy(isExpanded = true) }

            Log.d(TAG, "Projects to expand: ${projectsToExpand.map { it.id }}")

            val collapsedOnFocus = _collapsedAncestorsOnFocus.value
            val projectsToRestore = currentAllProjects
                .filter { it.id in collapsedOnFocus && it.id !in pathIdsToExpand }
                .map { it.copy(isExpanded = true) }

            Log.d(TAG, "Projects to restore (collapsed on focus): ${projectsToRestore.map { it.id }}")

            val allProjectsToUpdate = (projectsToExpand + projectsToRestore).distinctBy { it.id }

            Log.d(TAG, "Total projects to update: ${allProjectsToUpdate.map { it.id }}")

            if (allProjectsToUpdate.isNotEmpty()) {
                Log.d(TAG, "Updating projects in DB...")
                projectRepository.updateProjects(allProjectsToUpdate)

                try {
                    val updatedState = allProjectsFlat.first { updatedProjects ->
                        allProjectsToUpdate.all { projectToUpdate ->
                            updatedProjects.find { it.id == projectToUpdate.id }?.isExpanded == projectToUpdate.isExpanded
                        }
                    }
                    Log.d(TAG, "DB update confirmed in state. Projects now expanded: ${updatedState.filter { it.isExpanded }.map { it.id }}")
                } catch (e: Exception) {
                    Log.e(TAG, "Timeout waiting for DB state update", e)
                }
            } else {
                Log.d(TAG, "No projects to update in DB")
            }

            withContext(Dispatchers.Main) {
                _focusedProjectId.value = targetAncestorId
                _currentBreadcrumbs.value = newBreadcrumbs
                Log.d(TAG, "Navigation state updated immediately to prevent UI flicker")
            }

            delay(200)
            Log.d(TAG, "Delayed 200ms for UI rebuild")

            val finalAllProjects = allProjectsFlat.value
            val topLevel = finalAllProjects.filter { it.parentId == null }.sortedBy { it.order }
            val childrenOnly = finalAllProjects.filter { it.parentId != null }
            val childMap = childrenOnly.groupBy { it.parentId!! }
            val displayedProjects = flattenHierarchy(topLevel, childMap)

            val scrollIndex = displayedProjects.indexOfFirst { it.id == targetAncestorId }

            Log.d(TAG, "Calculated scroll index: $scrollIndex for target ancestor: $targetAncestorId")
            Log.d(TAG, "Displayed projects (first 5): ${displayedProjects.take(5).map { it.id }}")

            if (scrollIndex != -1) {
                Log.d(TAG, "Sending ScrollToIndex event: $scrollIndex")
                uiEventChannel.send(ProjectUiEvent.ScrollToIndex(scrollIndex))
            } else {
                Log.w(TAG, "Target ancestor not found in displayed projects!")
            }

            withContext(Dispatchers.Main) {
                _focusedProjectId.value = targetAncestorId
                _currentBreadcrumbs.value = newBreadcrumbs
                _collapsedAncestorsOnFocus.value = emptySet()
                Log.d(TAG, "Navigation updated. New focused project: $targetAncestorId")
            }

            Log.d(TAG, "clearNavigation: COMPLETED")
        }
    }

    fun onSearchRevealRequest(projectId: String, planningMode: MutableStateFlow<PlanningMode>, projectHierarchy: StateFlow<ListHierarchyData>) {
        Log.d(TAG, "onSearchRevealRequest: projectId=$projectId")
        viewModelScope.launch {
            onToggleSearch(isActive = false)
            planningMode.value = PlanningMode.All

            val allProjects = allProjectsFlat.first()
            val fullHierarchy = ListHierarchyData(
                allProjects = allProjects,
                topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            )

            val path = buildPathToProject(projectId, fullHierarchy)
            val level = if (path.isEmpty()) 0 else path.last().level
            val settings = hierarchySettings.value

            navigateToProject(projectId, projectHierarchy)
        }
    }

    fun clearNavigationCompletely() {
        Log.d(TAG, "clearNavigationCompletely: STARTED")
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _focusedProjectId.value = null
                _currentBreadcrumbs.value = emptyList()
                _collapsedAncestorsOnFocus.value = emptySet()
            }

            Log.d(TAG, "clearNavigationCompletely: Navigation completely cleared")
        }
    }
}