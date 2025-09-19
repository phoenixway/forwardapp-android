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
        private const val ACTIVE_SEARCH_QUERY_TEXT_KEY = "active_search_query_text"
        private const val IS_SEARCH_ACTIVE_KEY = "is_search_active"
        private const val TAG = "SNM_DEBUG"
    }

    private val _searchQuery = MutableStateFlow(
        TextFieldValue(savedStateHandle.get<String>(ACTIVE_SEARCH_QUERY_TEXT_KEY) ?: "")
    )
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(savedStateHandle.get<Boolean>(IS_SEARCH_ACTIVE_KEY) ?: false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _highlightedProjectId = MutableStateFlow<String?>(null)
    val highlightedProjectId: StateFlow<String?> = _highlightedProjectId.asStateFlow()

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
        Log.d(TAG, "onToggleSearch(isActive=$isActive) CALLED")

        savedStateHandle[IS_SEARCH_ACTIVE_KEY] = isActive
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
            savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = ""
            _searchQuery.value = TextFieldValue("")
        }
    }

    fun onSearchQueryChanged(query: TextFieldValue) {
        savedStateHandle[ACTIVE_SEARCH_QUERY_TEXT_KEY] = query.text
        _searchQuery.value = query
    }

    fun onSearchQueryFromHistory(query: String) {
        Log.d(TAG, "onSearchQueryFromHistory: '$query'")
        onSearchQueryChanged(TextFieldValue(query))
        onToggleSearch(true)
    }

    // --- ПОЧАТОК ВИПРАВЛЕННЯ: Коректна робота з історією як зі списком ---
    private fun addSearchQueryToHistory(query: String) {
        // Надійно отримуємо поточну історію як список рядків
        val currentHistory = savedStateHandle.get<List<String>>(SEARCH_HISTORY_KEY) ?: emptyList()
        val mutableHistory = currentHistory.toMutableList()

        // Видаляємо дублікати
        val existingIndex = mutableHistory.indexOfFirst { it.equals(query, ignoreCase = true) }
        if (existingIndex != -1) {
            mutableHistory.removeAt(existingIndex)
        }

        // Додаємо новий запит на початок
        mutableHistory.add(0, query)

        // Обмежуємо розмір і зберігаємо назад як список
        val newHistory = mutableHistory.take(MAX_SEARCH_HISTORY)
        savedStateHandle[SEARCH_HISTORY_KEY] = newHistory
    }
    // --- КІНЕЦЬ ВИПРАВЛЕННЯ ---

    fun onSearchResultClick(projectId: String, projectHierarchy: StateFlow<ListHierarchyData>, planningMode: MutableStateFlow<PlanningMode>) {
        Log.d(TAG, "onSearchResultClick (Reveal) called for $projectId.")
        viewModelScope.launch {
            // Ця функція призначена для "Показати в ієрархії", тому вимкнення пошуку тут є правильним.
            onToggleSearch(false)
            processRevealRequest(projectId, planningMode)
        }
    }

    fun processRevealRequest(projectId: String, planningMode: MutableStateFlow<PlanningMode>) {
        viewModelScope.launch {
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
            val childMap = finalProjectState.filter { it.parentId != null }.groupBy { it.parentId!! }
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
            _focusedProjectId.value = null
            _currentBreadcrumbs.value = emptyList()
        }
    }

    fun onSearchRevealRequest(projectId: String, planningMode: MutableStateFlow<PlanningMode>, projectHierarchy: StateFlow<ListHierarchyData>) {
        viewModelScope.launch {
            onToggleSearch(isActive = false)
            planningMode.value = PlanningMode.All
            navigateToProject(projectId, projectHierarchy)
        }
    }

    fun clearNavigationCompletely() {
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _focusedProjectId.value = null
                _currentBreadcrumbs.value = emptyList()
                _collapsedAncestorsOnFocus.value = emptySet()
            }
        }
    }
}