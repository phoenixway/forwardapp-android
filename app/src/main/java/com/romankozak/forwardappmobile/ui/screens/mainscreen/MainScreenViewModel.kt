package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.WifiSyncServer
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

// --- DATA КЛАСИ, ВИНЕСЕНІ ЗА МЕЖІ VIEWMODEL ДЛЯ ЧИСТОТИ ---
private const val TAG = "SendDebug"
sealed class ProjectUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : ProjectUiEvent()
    data class NavigateToDetails(val projectId: String) : ProjectUiEvent()
    data class NavigateToGlobalSearch(val query: String) : ProjectUiEvent()
    object NavigateToSettings : ProjectUiEvent()
    data class ShowToast(val message: String) : ProjectUiEvent()
    data class ScrollToIndex(val index: Int) : ProjectUiEvent()
    object FocusSearchField : ProjectUiEvent()
    data class NavigateToEditProjectScreen(val projectId: String) : ProjectUiEvent()
    data class Navigate(val route: String) : ProjectUiEvent()
    data class NavigateToDayPlan(val date: Long) : ProjectUiEvent()
}

data class ListHierarchyData(
    val allProjects: List<Project> = emptyList(),
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap()
)

data class SearchResult(
    val project: Project,
    val path: List<BreadcrumbItem>
)

sealed class PlanningMode {
    object All : PlanningMode()
    object Daily : PlanningMode()
    object Medium : PlanningMode()
    object Long : PlanningMode()
}

data class AppStatistics(val projectCount: Int = 0, val goalCount: Int = 0)

sealed class DialogState {
    object Hidden : DialogState()
    data class AddProject(val parentId: String?) : DialogState()
    data class ContextMenu(val project: Project) : DialogState()
    data class ConfirmDelete(val project: Project) : DialogState()
    data class EditProject(val project: Project) : DialogState()
    object AboutApp : DialogState()
    data class ConfirmFullImport(val uri: Uri) : DialogState()
}

data class PlanningSettingsState(
    val showModes: Boolean = false,
    val dailyTag: String = "daily",
    val mediumTag: String = "medium",
    val longTag: String = "long",
)

private data class FilterState(
    val flatList: List<Project>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
)

enum class DropPosition { BEFORE, AFTER }

private fun fuzzyMatch(query: String, text: String): Boolean {
    if (query.isBlank()) return true
    if (text.isBlank()) return false
    val lowerQuery = query.lowercase()
    val lowerText = text.lowercase()
    var queryIndex = 0
    var textIndex = 0
    while (queryIndex < lowerQuery.length && textIndex < lowerText.length) {
        if (lowerQuery[queryIndex] == lowerText[textIndex]) {
            queryIndex++
        }
        textIndex++
    }
    return queryIndex == lowerQuery.length
}

data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int
)

data class HierarchyDisplaySettings(
    val maxCollapsibleLevels: Int = 3,
    val useBreadcrumbsAfter: Int = 2,
    val maxIndentation: Dp = 120.dp
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    companion object {
        private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10
    }

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _uiEventChannel = Channel<ProjectUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val _isBottomNavExpanded = MutableStateFlow(false)
    val isBottomNavExpanded: StateFlow<Boolean> = _isBottomNavExpanded.asStateFlow()

    private val _desktopAddress = MutableStateFlow("")
    val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

    private val _showWifiServerDialog = MutableStateFlow(false)
    val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

    private val _showWifiImportDialog = MutableStateFlow(false)
    val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

    private val _wifiServerAddress = MutableStateFlow<String?>(null)
    val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

    private val wifiSyncServer = WifiSyncServer(syncRepo, application)

    private val projectBeingMovedId = savedStateHandle.getStateFlow<String?>(PROJECT_BEING_MOVED_ID_KEY, null)

    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isReadyForFiltering = MutableStateFlow(false)
    private val _highlightedProjectId = MutableStateFlow<String?>(null)
    val highlightedProjectId: StateFlow<String?> = _highlightedProjectId.asStateFlow()
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()
    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()

    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)

    val searchHistory: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())

    val obsidianVaultName: StateFlow<String> =
        settingsRepo.obsidianVaultNameFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val planningSettingsState: StateFlow<PlanningSettingsState> =
        combine(
            settingsRepo.showPlanningModesFlow,
            settingsRepo.dailyTagFlow,
            settingsRepo.mediumTagFlow,
            settingsRepo.longTagFlow,
        ) { show, daily, medium, long ->
            PlanningSettingsState(show, daily, medium, long)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlanningSettingsState(),
        )

    private val _allProjectsFlat =
        projectRepository
            .getAllProjectsFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val debouncedSearchQueryText =
        searchQuery
            .map { it.text }
            .debounce(350L)
            .distinctUntilChanged()

    val areAnyProjectsExpanded: StateFlow<Boolean> =
        _allProjectsFlat
            .map { projects ->
                projects.any { it.isExpanded }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appStatistics: StateFlow<AppStatistics> =
        combine(_allProjectsFlat, projectRepository.getAllGoalsCountFlow()) { allProjects, allGoalsCount ->
            AppStatistics(projectCount = allProjects.size, goalCount = allGoalsCount)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    private val filterStateFlow: StateFlow<FilterState> =
        combine(
            _allProjectsFlat,
            debouncedSearchQueryText,
            isSearchActive,
            planningMode,
            planningSettingsState,
            _isReadyForFiltering
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val flatList = values[0] as List<Project>
            val query = values[1] as String
            val searchActive = values[2] as Boolean
            val mode = values[3] as PlanningMode
            val settings = values[4] as PlanningSettingsState
            val isReady = values[5] as Boolean

            if (!isReady) {
                FilterState(flatList, "", false, PlanningMode.All, settings)
            } else {
                FilterState(flatList, query, searchActive, mode, settings)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FilterState(emptyList(), "", false, PlanningMode.All, PlanningSettingsState()),
        )

    val projectHierarchy: StateFlow<ListHierarchyData> =
        combine(
            filterStateFlow,
            _expandedInDailyMode,
            _expandedInMediumMode,
            _expandedInLongMode,
        ) { filterState: FilterState, expandedDaily: Set<String>?, expandedMedium: Set<String>?, expandedLong: Set<String>? ->
            try {
                val (flatList, _, _, mode, settings) = filterState
                val isPlanningModeActive = mode != PlanningMode.All

                if (!isPlanningModeActive) {
                    val topLevel = flatList.filter { it.parentId == null }.sortedBy { it.order }
                    val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
                    return@combine ListHierarchyData(allProjects = flatList, topLevelProjects = topLevel, childMap = childMap)
                }

                val projectLookup = flatList.associateBy { it.id }

                val targetTag =
                    when (mode) {
                        PlanningMode.Daily -> settings.dailyTag
                        PlanningMode.Medium -> settings.mediumTag
                        PlanningMode.Long -> settings.longTag
                        else -> null
                    }
                val matchingProjects = if (targetTag != null) flatList.filter { it.tags?.contains(targetTag) == true } else emptyList()

                val ancestorIds = mutableSetOf<String>()
                val visitedAncestors = mutableSetOf<String>()
                matchingProjects.forEach { project ->
                    findAncestorsRecursive(project.id, projectLookup, ancestorIds, visitedAncestors)
                }

                val visibleIds = ancestorIds + matchingProjects.map { it.id }

                val currentExpandedState = when (mode) {
                    is PlanningMode.Daily -> expandedDaily
                    is PlanningMode.Medium -> expandedMedium
                    is PlanningMode.Long -> expandedLong
                    else -> null
                }

                val shouldInitialize = currentExpandedState == null && matchingProjects.isNotEmpty()
                val currentExpandedIds = if (shouldInitialize) ancestorIds else (currentExpandedState ?: emptySet())

                if (shouldInitialize) {
                    when (mode) {
                        is PlanningMode.Daily -> _expandedInDailyMode.value = ancestorIds
                        is PlanningMode.Medium -> _expandedInMediumMode.value = ancestorIds
                        is PlanningMode.Long -> _expandedInLongMode.value = ancestorIds
                        else -> Unit
                    }
                }

                val visibleProjects = flatList.filter { it.id in visibleIds }
                val displayProjects = visibleProjects.map { project -> project.copy(isExpanded = currentExpandedIds.contains(project.id)) }
                val topLevel = displayProjects.filter { it.parentId == null || it.parentId !in visibleIds }.sortedBy { it.order }
                val childMap = displayProjects.filter { it.parentId != null }.groupBy { it.parentId!! }

                return@combine ListHierarchyData(allProjects = flatList, topLevelProjects = topLevel, childMap = childMap)
            } catch (e: Exception) {
                Log.e("ProjectViewModel_DEBUG", "Exception in projectHierarchy combine block", e)
                return@combine ListHierarchyData()
            }
        }.flowOn(Dispatchers.Default)
            .catch { e ->
                Log.e("ProjectViewModel_DEBUG", "Exception in projectHierarchy flow", e)
                emit(ListHierarchyData())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val longDescendantsMap: StateFlow<Map<String, Boolean>> = _allProjectsFlat
        .map { allProjects ->
            if (allProjects.isEmpty()) return@map emptyMap<String, Boolean>()
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val resultMap = mutableMapOf<String, Boolean>()
            val memo = mutableMapOf<String, Boolean>()
            val characterLimit = 35

            fun hasLongDescendantsRecursive(projectId: String): Boolean {
                if (memo.containsKey(projectId)) return memo[projectId]!!

                val children = childMap[projectId] ?: emptyList()
                for (child in children) {
                    if (child.name.length > characterLimit) {
                        memo[projectId] = true
                        return true
                    }
                    if (hasLongDescendantsRecursive(child.id)) {
                        memo[projectId] = true
                        return true
                    }
                }
                memo[projectId] = false
                return false
            }

            allProjects.forEach { project ->
                resultMap[project.id] = hasLongDescendantsRecursive(project.id)
            }
            resultMap
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _collapsedAncestorsOnFocus = MutableStateFlow<Set<String>>(emptySet())

    val listChooserFinalExpandedIds: StateFlow<Set<String>> =
        combine(
            _listChooserUserExpandedIds,
            _allProjectsFlat,
            projectBeingMovedId
        ) { userExpanded, allProjects, movingId ->
            if (movingId == null) return@combine emptySet<String>()

            val projectLookup = allProjects.associateBy { it.id }
            val ancestorIds = mutableSetOf<String>()
            val visitedAncestors = mutableSetOf<String>()
            findAncestorsRecursive(movingId, projectLookup, ancestorIds, visitedAncestors)

            userExpanded + ancestorIds
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> =
        combine(
            _allProjectsFlat,
            _listChooserFilterText,
            projectBeingMovedId
        ) { allProjects, filterText, movingId ->
            if (movingId == null) {
                return@combine ListHierarchyData()
            }

            val filteredProjects = if (filterText.isBlank()) {
                allProjects
            } else {
                allProjects.filter {
                    it.name.contains(filterText, ignoreCase = true) ||
                            fuzzyMatch(filterText, it.name)
                }
            }

            val topLevel = filteredProjects.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = filteredProjects.filter { it.parentId != null }.groupBy { it.parentId!! }

            ListHierarchyData(
                allProjects = filteredProjects,
                topLevelProjects = topLevel,
                childMap = childMap
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val allContextsForDialog: StateFlow<List<UiContext>> =
        contextHandler.allContextsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            filterStateFlow.collect { state ->
                if (state.searchActive && state.query.isNotBlank()) {
                    val allProjects = _allProjectsFlat.first()
                    val fullHierarchy = ListHierarchyData(
                        allProjects = allProjects,
                        topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                        childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
                    )

                    val matchingProjects = if (state.query.length > 3) {
                        allProjects.filter { fuzzyMatch(state.query, it.name) }
                    } else {
                        allProjects.filter { it.name.contains(state.query, ignoreCase = true) }
                    }

                    val results = matchingProjects.map { project ->
                        SearchResult(
                            project = project,
                            path = buildPathToProject(project.id, fullHierarchy)
                        )
                    }
                    _searchResults.value = results.sortedBy { it.project.name }
                } else {
                    _searchResults.value = emptyList()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                withContext(Dispatchers.Main) {
                    _isBottomNavExpanded.value = savedState
                }
            }
            _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
            contextHandler.initialize()
        }
    }

    fun onSearchResultClick(projectId: String) {
        viewModelScope.launch {
            onToggleSearch(false)

            val allProjects = _allProjectsFlat.first()
            val fullHierarchy = ListHierarchyData(
                allProjects = allProjects,
                topLevelProjects = allProjects.filter { it.parentId == null }.sortedBy { it.order },
                childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            )

            val path = buildPathToProject(projectId, fullHierarchy)
            val level = if (path.isEmpty()) 0 else path.last().level

            val settings = hierarchySettings.value

            if (level >= settings.useBreadcrumbsAfter) {
                navigateToProject(projectId)
            } else {
                processRevealRequest(projectId)
            }
        }
    }

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()
    val recentProjects: StateFlow<List<Project>> =
        projectRepository
            .getRecentProjects()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onShowRecentLists() {
        _showRecentListsSheet.value = true
    }

    fun onDismissRecentLists() {
        _showRecentListsSheet.value = false
    }

    fun onRecentProjectSelected(projectId: String) {
        viewModelScope.launch {
            onDismissRecentLists()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(projectId))
        }
    }

    fun onDayPlanClicked() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            _uiEventChannel.send(ProjectUiEvent.NavigateToDayPlan(today))
        }
    }

    fun onHomeClicked() {
        viewModelScope.launch {
            if (focusedProjectId.value != null) {
                clearNavigation()
            }
            if (planningMode.value != PlanningMode.All) {
                onPlanningModeChange(PlanningMode.All)
            }
            collapseAllProjects()
            _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    fun processRevealRequest(projectId: String) {
        viewModelScope.launch {
            _isSearchActive.value = false
            _planningMode.value = PlanningMode.All

            val allProjects = _allProjectsFlat.first { it.isNotEmpty() }
            val projectLookup = allProjects.associateBy { it.id }

            if (!projectLookup.containsKey(projectId)) {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Could not find project."))
                return@launch
            }

            val ancestorIds = mutableSetOf<String>()
            findAncestorsRecursive(projectId, projectLookup, ancestorIds, mutableSetOf())

            val projectsToExpand = ancestorIds
                .mapNotNull { projectLookup[it] }
                .filter { !it.isExpanded && it.id != projectId }

            if (projectsToExpand.isNotEmpty()) {
                projectRepository.updateProjects(projectsToExpand.map { it.copy(isExpanded = true) })

                _allProjectsFlat.first { updatedProjectState ->
                    projectsToExpand.all { projectToExpand ->
                        updatedProjectState.find { it.id == projectToExpand.id }?.isExpanded == true
                    }
                }
            }

            val finalProjectState = _allProjectsFlat.value
            val topLevel = finalProjectState.filter { it.parentId == null }.sortedBy { it.order }

            fun flattenHierarchy(currentProjects: List<Project>, projectMap: Map<String, List<Project>>): List<Project> {
                val result = mutableListOf<Project>()
                for (project in currentProjects) {
                    result.add(project)
                    if (project.isExpanded) {
                        val children = projectMap[project.id]?.sortedBy { it.order } ?: emptyList()
                        if (children.isNotEmpty()) {
                            result.addAll(flattenHierarchy(children, projectMap))
                        }
                    }
                }
                return result
            }

            val childrenOnly = finalProjectState.filter { it.parentId != null }
            val childMap = childrenOnly.groupBy { it.parentId!! }
            val displayedProjects = flattenHierarchy(topLevel, childMap)

            val index = displayedProjects.indexOfFirst { it.id == projectId }

            if (index != -1) {
                _uiEventChannel.send(ProjectUiEvent.ScrollToIndex(index))
            }

            _highlightedProjectId.value = projectId
            delay(1500)
            if (_highlightedProjectId.value == projectId) {
                _highlightedProjectId.value = null
            }
        }
    }

    fun onToggleSearch(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (isActive) {
            val currentText = _searchQuery.value.text
            _searchQuery.value = TextFieldValue(currentText, TextRange(0, currentText.length))
            viewModelScope.launch {
                _uiEventChannel.send(ProjectUiEvent.FocusSearchField)
            }
        } else {
            val query = _searchQuery.value.text
            if (query.isNotBlank()) {
                addSearchQueryToHistory(query)
            }
            _searchQuery.value = TextFieldValue("")
        }
    }

    fun onPlanningModeChange(mode: PlanningMode) {
        if (_isSearchActive.value) {
            _isSearchActive.value = false
            _searchQuery.value = TextFieldValue("")
        }
        _planningMode.value = mode
    }

    fun collapseAllProjects() {
        viewModelScope.launch {
            val projectsToCollapse =
                _allProjectsFlat.value
                    .filter { it.isExpanded }
                    .map { it.copy(isExpanded = false) }
            if (projectsToCollapse.isNotEmpty()) {
                projectRepository.updateProjects(projectsToCollapse)
            }
        }
    }

    fun onToggleExpanded(project: Project) {
        if (planningMode.value != PlanningMode.All) {
            val currentStateFlow =
                when (planningMode.value) {
                    is PlanningMode.Daily -> _expandedInDailyMode
                    is PlanningMode.Medium -> _expandedInMediumMode
                    is PlanningMode.Long -> _expandedInLongMode
                    else -> return
                }

            val currentExpanded = (currentStateFlow.value ?: emptySet()).toMutableSet()
            if (project.isExpanded) {
                currentExpanded.remove(project.id)
            } else {
                currentExpanded.add(project.id)
            }
            currentStateFlow.value = currentExpanded

        } else {
            viewModelScope.launch {
                projectRepository.updateProject(project.copy(isExpanded = !project.isExpanded))
            }
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

    fun onShowWifiServerDialog() {
        _wifiServerAddress.value = null
        _showWifiServerDialog.value = true
        startWifiServer()
    }

    private fun startWifiServer() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { wifiSyncServer.start() }
            result.onSuccess { address ->
                _wifiServerAddress.value = address
            }.onFailure { exception ->
                _wifiServerAddress.value = "Error: ${exception.message}"
            }
        }
    }

    private fun stopWifiServer() {
        viewModelScope.launch(Dispatchers.IO) {
            wifiSyncServer.stop()
            withContext(Dispatchers.Main) {
                _wifiServerAddress.value = null
            }
        }
    }

    fun addNewProject(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectRepository.createProjectWithId(id, name, parentId)
            if (parentId != null) {
                val parentProject = _allProjectsFlat.value.find { it.id == parentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    projectRepository.updateProject(parentProject.copy(isExpanded = true))
                }
            }
        }
    }

    fun onDeleteProjectConfirmed(project: Project) {
        viewModelScope.launch {
            val projectsToDelete = findDescendantsForDeletion(project.id, projectHierarchy.value.childMap)
            projectRepository.deleteProjectsAndSubProjects(listOf(project) + projectsToDelete)
            dismissDialog()
        }
    }

    private fun findAncestorsRecursive(
        projectId: String?,
        projectLookup: Map<String, Project>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        var currentId = projectId
        while (currentId != null && visited.add(currentId)) {
            ids.add(currentId)
            currentId = projectLookup[currentId]?.parentId
        }
    }

    private fun findDescendantsForDeletion(
        projectId: String,
        childMap: Map<String, List<Project>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): List<Project> {
        if (!visited.add(projectId)) return emptyList()
        val children = childMap[projectId] ?: emptyList()
        return children + children.flatMap { findDescendantsForDeletion(it.id, childMap, visited) }
    }

    fun performWifiImport(address: String) {
        viewModelScope.launch {
            val result = syncRepo.fetchBackupFromWifi(address)
            result.onSuccess { jsonString ->
                _uiEventChannel.send(ProjectUiEvent.NavigateToSyncScreenWithData(jsonString))
                onDismissWifiImportDialog()
            }.onFailure {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Error: ${it.message}"))
            }
        }
    }

    fun onAddNewProjectRequest() { _dialogState.value = DialogState.AddProject(null) }
    fun onAddSubprojectRequest(parentProject: Project) { _dialogState.value = DialogState.AddProject(parentProject.id) }

    private fun getDescendantIds(projectId: String, childMap: Map<String, List<Project>>): Set<String> {
        val descendants = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(projectId)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            childMap[currentId]?.forEach { child ->
                descendants.add(child.id)
                queue.add(child.id)
            }
        }
        return descendants
    }

    fun onMoveProjectRequest(project: Project) {
        dismissDialog()
        savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = project.id
        viewModelScope.launch {
            val title = "Перемістити '${project.name}'"
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val allProjects = _allProjectsFlat.first()
            val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantIds = getDescendantIds(project.id, childMap).joinToString(",")
            val currentParentId = project.parentId ?: "root"
            val disabledIds = "${project.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
            val route = "project_chooser_screen/$encodedTitle?currentParentId=$currentParentId&disabledIds=$disabledIds"
            _uiEventChannel.send(ProjectUiEvent.Navigate(route))
        }
    }

    fun onMenuRequested(project: Project) { _dialogState.value = DialogState.ContextMenu(project) }
    fun onDeleteRequest(project: Project) { _dialogState.value = DialogState.ConfirmDelete(project) }
    fun onEditRequest(project: Project) {
        viewModelScope.launch {
            _uiEventChannel.send(ProjectUiEvent.NavigateToEditProjectScreen(project.id))
        }
    }

    fun onShowSettingsScreen() { viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToSettings) } }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp }
    fun onProjectClicked(projectId: String) { viewModelScope.launch { _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(projectId)) } }
    fun onDesktopAddressChange(newAddress: String) {
        _desktopAddress.value = newAddress
        viewModelScope.launch { settingsRepo.saveDesktopAddress(newAddress) }
    }
    fun onDismissWifiServerDialog() { _showWifiServerDialog.value = false; stopWifiServer() }
    fun onShowWifiImportDialog() { _showWifiImportDialog.value = true }
    fun onDismissWifiImportDialog() { _showWifiImportDialog.value = false }

    fun onShowSearchDialog() { _showSearchDialog.value = true }
    fun onDismissSearchDialog() { _showSearchDialog.value = false }
    fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            addSearchQueryToHistory(query)
            viewModelScope.launch {
                _uiEventChannel.send(ProjectUiEvent.NavigateToGlobalSearch(query))
                onDismissSearchDialog()
            }
        }
    }

    fun exportToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.exportFullBackupToFile()
            result.onSuccess { message ->
                _uiEventChannel.send(ProjectUiEvent.ShowToast(message))
            }.onFailure { error ->
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Export error: ${error.message}"))
            }
        }
    }

    fun onImportFromFileRequested(uri: Uri) { _dialogState.value = DialogState.ConfirmFullImport(uri) }
    fun onListChooserFilterChanged(text: String) { _listChooserFilterText.value = text }
    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserUserExpandedIds.value.toMutableSet()
        if (listId in currentIds) currentIds.remove(listId) else currentIds.add(listId)
        _listChooserUserExpandedIds.value = currentIds
    }

    fun onListChooserResult(newParentId: String?) {
        viewModelScope.launch {
            val projectToMoveId = projectBeingMovedId.value ?: return@launch
            val projectToMove = _allProjectsFlat.value.find { it.id == projectToMoveId } ?: return@launch
            val finalNewParentId = if (newParentId == "root") null else newParentId
            if (projectToMove.parentId == finalNewParentId) {
                savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
                return@launch
            }
            projectRepository.moveProject(projectToMove, finalNewParentId)
            if (finalNewParentId != null) {
                val parentProject = _allProjectsFlat.value.find { it.id == finalNewParentId }
                if (parentProject != null && !parentProject.isExpanded) {
                    projectRepository.updateProject(parentProject.copy(isExpanded = true))
                }
            }
            savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
        }
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
        _listChooserFilterText.value = ""
        _listChooserUserExpandedIds.value = emptySet()
    }

    fun onProjectReorder(fromId: String, toId: String, position: DropPosition) {
        if (fromId == toId || isSearchActive.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val allProjects = _allProjectsFlat.first()
            val fromProject = allProjects.find { it.id == fromId }
            val toProject = allProjects.find { it.id == toId }
            if (fromProject == null || toProject == null || fromProject.parentId != toProject.parentId) return@launch
            val parentId = fromProject.parentId
            val siblings = allProjects.filter { it.parentId == parentId }.sortedBy { it.order }
            val mutableSiblings = siblings.toMutableList()
            val fromIndex = mutableSiblings.indexOfFirst { it.id == fromId }
            val toIndex = mutableSiblings.indexOfFirst { it.id == toId }
            if (fromIndex == -1 || toIndex == -1) return@launch
            val movedItem = mutableSiblings.removeAt(fromIndex)
            val insertionIndex = when {
                fromIndex < toIndex -> if (position == DropPosition.BEFORE) toIndex - 1 else toIndex
                else -> if (position == DropPosition.BEFORE) toIndex else toIndex + 1
            }
            val finalIndex = insertionIndex.coerceIn(0, mutableSiblings.size)
            mutableSiblings.add(finalIndex, movedItem)
            val projectsToUpdate = mutableSiblings.mapIndexed { index, project ->
                project.copy(order = index.toLong(), updatedAt = System.currentTimeMillis())
            }
            projectRepository.updateProjects(projectsToUpdate)
        }
    }

    fun onFullImportConfirmed(uri: Uri) {
        dismissDialog()
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.importFullBackupFromFile(uri)
            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    _uiEventChannel.send(ProjectUiEvent.ShowToast(message))
                }.onFailure { error ->
                    _uiEventChannel.send(ProjectUiEvent.ShowToast("Import error: ${error.message}"))
                }
            }
        }
    }

    fun enableFiltering() { _isReadyForFiltering.value = true }
    fun onBottomNavExpandedChange(expanded: Boolean) {
        if (_isBottomNavExpanded.value == expanded) return
        _isBottomNavExpanded.value = expanded
        viewModelScope.launch {
            settingsRepo.saveBottomNavExpanded(expanded)
        }
    }

    private val _currentBreadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val currentBreadcrumbs = _currentBreadcrumbs.asStateFlow()
    private val _focusedProjectId = MutableStateFlow<String?>(null)
    val focusedProjectId = _focusedProjectId.asStateFlow()
    private val _hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())
    val hierarchySettings = _hierarchySettings.asStateFlow()

    fun navigateToProject(projectId: String) {
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

    fun clearNavigation() {
        viewModelScope.launch {
            val breadcrumbs = _currentBreadcrumbs.value
            if (breadcrumbs.isNotEmpty()) {
                val topLevelAncestorId = breadcrumbs.first().id
                val ancestorProject = _allProjectsFlat.value.find { it.id == topLevelAncestorId }

                if (ancestorProject != null && !ancestorProject.isExpanded) {
                    projectRepository.updateProject(ancestorProject.copy(isExpanded = true))

                    _allProjectsFlat.first { updatedProjects ->
                        updatedProjects.find { it.id == topLevelAncestorId }?.isExpanded == true
                    }
                }
            }

            val projectsToCollapseIds = _collapsedAncestorsOnFocus.value
            if (projectsToCollapseIds.isNotEmpty()) {
                val projectsToUpdate = _allProjectsFlat.value
                    .filter { it.id in projectsToCollapseIds }
                    .map { it.copy(isExpanded = false) }

                if (projectsToUpdate.isNotEmpty()) {
                    projectRepository.updateProjects(projectsToUpdate)
                }
            }

            _collapsedAncestorsOnFocus.value = emptySet()
            _currentBreadcrumbs.value = emptyList()
            _focusedProjectId.value = null
        }
    }

    private fun buildPathToProject(targetId: String, hierarchy: ListHierarchyData): List<BreadcrumbItem> {
        val path = mutableListOf<BreadcrumbItem>()
        fun findPath(projects: List<Project>, level: Int): Boolean {
            val sortedProjects = projects.sortedBy { it.order }
            for (project in sortedProjects) {
                path.add(BreadcrumbItem(project.id, project.name, level))
                if (project.id == targetId) return true
                val children = hierarchy.childMap[project.id] ?: emptyList()
                if (findPath(children, level + 1)) return true
                path.removeLastOrNull()
            }
            return false
        }
        findPath(hierarchy.topLevelProjects, 0)
        return path.toList()
    }

    fun onContextSelected(contextName: String) {
        viewModelScope.launch {
            val targetTag = contextHandler.getContextTag(contextName)
            if (targetTag.isNullOrBlank()) {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Тег для контексту '$contextName' не знайдено або порожній"))
                return@launch
            }
            val targetProject = _allProjectsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetProject != null) {
                _uiEventChannel.send(ProjectUiEvent.NavigateToDetails(targetProject.id))
            } else {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проект з тегом '#$targetTag' не знайдено"))
            }
        }
    }

    fun saveSettings(
        show: Boolean,
        daily: String,
        medium: String,
        long: String,
        vaultName: String,
    ) {
        viewModelScope.launch {
            settingsRepo.saveShowPlanningModes(show)
            settingsRepo.saveDailyTag(daily.trim())
            settingsRepo.saveMediumTag(medium.trim())
            settingsRepo.saveLongTag(long.trim())
            settingsRepo.saveObsidianVaultName(vaultName.trim())
        }
    }

    fun saveAllContexts(updatedContexts: List<UiContext>) {
        viewModelScope.launch {
            val customContextsToSave = updatedContexts.filter { !it.isReserved }
            settingsRepo.saveCustomContexts(customContextsToSave)
            contextHandler.initialize()
        }
    }
}