// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/GoalListViewModel.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.screens.mainscreen.delegates.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // --- Core State ---
    private val _uiEventChannel = Channel<GoalListUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _isBottomNavExpanded = MutableStateFlow(false)
    val isBottomNavExpanded: StateFlow<Boolean> = _isBottomNavExpanded.asStateFlow()

    private val _isReadyForFiltering = MutableStateFlow(false)

    // --- Delegates ---
    private val searchDelegate = SearchDelegate(savedStateHandle, _uiEventChannel)
    private val navigationDelegate = NavigationDelegate(goalRepository, _uiEventChannel)
    private val wifiDelegate = WifiDelegate(syncRepo, settingsRepo, application, _uiEventChannel)
    private val listOperationsDelegate = ListOperationsDelegate(goalRepository, savedStateHandle, _uiEventChannel)
    private val planningDelegate = PlanningDelegate(settingsRepo, contextHandler, _uiEventChannel, viewModelScope)

    // --- Delegate Properties ---
    val searchResults = searchDelegate.searchResults
    val searchQuery = searchDelegate.searchQuery
    val isSearchActive = searchDelegate.isSearchActive
    val showSearchDialog = searchDelegate.showSearchDialog
    val searchHistory = searchDelegate.searchHistory

    val currentBreadcrumbs = navigationDelegate.currentBreadcrumbs
    val focusedListId = navigationDelegate.focusedListId
    val hierarchySettings = navigationDelegate.hierarchySettings
    val highlightedListId = navigationDelegate.highlightedListId

    val desktopAddress = wifiDelegate.desktopAddress
    val showWifiServerDialog = wifiDelegate.showWifiServerDialog
    val showWifiImportDialog = wifiDelegate.showWifiImportDialog
    val wifiServerAddress = wifiDelegate.wifiServerAddress

    val planningMode = planningDelegate.planningMode
    val planningSettingsState = planningDelegate.planningSettingsState
    val allContextsForDialog = planningDelegate.allContextsForDialog
    val obsidianVaultName = planningDelegate.obsidianVaultName
    val showRecentListsSheet = planningDelegate.showRecentListsSheet

    // --- Core Data Flows ---
    private val _allListsFlat = goalRepository
        .getAllGoalListsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val debouncedSearchQueryText = searchQuery
        .map { it.text }
        .debounce(350L)
        .distinctUntilChanged()

    val areAnyListsExpanded: StateFlow<Boolean> = _allListsFlat
        .map { lists -> lists.any { it.isExpanded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appStatistics: StateFlow<AppStatistics> = combine(
        _allListsFlat,
        goalRepository.getAllGoalsCountFlow()
    ) { allLists, allGoalsCount ->
        AppStatistics(listCount = allLists.size, goalCount = allGoalsCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    val recentLists: StateFlow<List<GoalList>> = goalRepository
        .getRecentLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filter State Flow ---
    private val filterStateFlow: StateFlow<FilterState> = combine(
        _allListsFlat,
        debouncedSearchQueryText,
        isSearchActive,
        planningMode,
        planningSettingsState,
        _isReadyForFiltering
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val flatList = values[0] as List<GoalList>
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

    // --- List Hierarchy ---
    val listHierarchy: StateFlow<ListHierarchyData> = combine(
        filterStateFlow,
        planningDelegate.getExpandedState(PlanningMode.Daily),
        planningDelegate.getExpandedState(PlanningMode.Medium),
        planningDelegate.getExpandedState(PlanningMode.Long),
    ) { filterState, expandedDaily, expandedMedium, expandedLong ->
        try {
            buildHierarchy(filterState, expandedDaily, expandedMedium, expandedLong)
        } catch (e: Exception) {
            Log.e("GoalListViewModel_DEBUG", "Exception in listHierarchy combine block", e)
            ListHierarchyData()
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            Log.e("GoalListViewModel_DEBUG", "Exception in listHierarchy flow", e)
            emit(ListHierarchyData())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    // --- List Chooser States ---
    val listChooserFinalExpandedIds: StateFlow<Set<String>> = combine(
        listOperationsDelegate.listChooserUserExpandedIds,
        _allListsFlat,
        savedStateHandle.getStateFlow<String?>("listBeingMovedId", null)
    ) { userExpanded, allLists, movingId ->
        if (movingId == null) return@combine emptySet<String>()

        val listLookup = allLists.associateBy { it.id }
        val ancestorIds = mutableSetOf<String>()
        val visitedAncestors = mutableSetOf<String>()
        findAncestorsRecursive(movingId, listLookup, ancestorIds, visitedAncestors)

        userExpanded + ancestorIds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> = combine(
        _allListsFlat,
        listOperationsDelegate.listChooserFilterText,
        savedStateHandle.getStateFlow<String?>("listBeingMovedId", null)
    ) { allLists, filterText, movingId ->
        if (movingId == null) return@combine ListHierarchyData()

        val filteredLists = if (filterText.isBlank()) {
            allLists
        } else {
            allLists.filter {
                it.name.contains(filterText, ignoreCase = true) || fuzzyMatch(filterText, it.name)
            }
        }

        val topLevel = filteredLists.filter { it.parentId == null }.sortedBy { it.order }
        val childMap = filteredLists.filter { it.parentId != null }.groupBy { it.parentId!! }

        ListHierarchyData(
            allLists = filteredLists,
            topLevelLists = topLevel,
            childMap = childMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    init {
        setupSearchResultsFlow()
        setupInitialization()
    }

    // --- Initialization ---
    private fun setupSearchResultsFlow() {
        viewModelScope.launch {
            filterStateFlow.collect { state ->
                if (state.searchActive && state.query.isNotBlank()) {
                    val fullHierarchy = ListHierarchyData(
                        allLists = _allListsFlat.first(),
                        topLevelLists = _allListsFlat.first().filter { it.parentId == null }.sortedBy { it.order },
                        childMap = _allListsFlat.first().filter { it.parentId != null }.groupBy { it.parentId!! }
                    )
                    searchDelegate.updateSearchResults(state.query, state.searchActive, state.flatList, fullHierarchy)
                }
            }
        }
    }

    private fun setupInitialization() {
        viewModelScope.launch {
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                _isBottomNavExpanded.value = savedState
            }
            wifiDelegate.initialize()
            planningDelegate.initialize()
        }
    }

    // --- Public Methods ---
    fun enableFiltering() { _isReadyForFiltering.value = true }

    // Search Methods
    fun onToggleSearch(isActive: Boolean) = viewModelScope.launch {
        searchDelegate.onToggleSearch(isActive)
    }

    fun onSearchQueryChanged(query: androidx.compose.ui.text.input.TextFieldValue) =
        searchDelegate.onSearchQueryChanged(query)

    fun onSearchQueryFromHistory(query: String) {
        searchDelegate.onSearchQueryFromHistory(query)
        viewModelScope.launch { searchDelegate.onToggleSearch(true) }
    }

    fun onSearchResultClick(listId: String) = viewModelScope.launch {
        searchDelegate.onToggleSearch(false)

        val allLists = _allListsFlat.first()
        val fullHierarchy = ListHierarchyData(
            allLists = allLists,
            topLevelLists = allLists.filter { it.parentId == null }.sortedBy { it.order },
            childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
        )

        val path = buildPathToList(listId, fullHierarchy)
        val level = if (path.isEmpty()) 0 else path.last().level

        if (level >= hierarchySettings.value.useBreadcrumbsAfter) {
            navigationDelegate.navigateToList(listId, listHierarchy)
        } else {
            navigationDelegate.processRevealRequest(listId, _allListsFlat, _planningMode, searchDelegate.isSearchActive as MutableStateFlow<Boolean>)
        }
    }

    fun onShowSearchDialog() = searchDelegate.onShowSearchDialog()
    fun onDismissSearchDialog() = searchDelegate.onDismissSearchDialog()
    fun onPerformGlobalSearch(query: String) = viewModelScope.launch {
        searchDelegate.onPerformGlobalSearch(query)
    }

    // Navigation Methods
    fun navigateToList(listId: String) = viewModelScope.launch {
        navigationDelegate.navigateToList(listId, listHierarchy)
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) =
        navigationDelegate.navigateToBreadcrumb(breadcrumbItem)

    fun clearNavigation() = viewModelScope.launch {
        navigationDelegate.clearNavigation(_allListsFlat)
    }

    fun processRevealRequest(listId: String) = viewModelScope.launch {
        navigationDelegate.processRevealRequest(
            listId, _allListsFlat,
            planningDelegate.planningMode as MutableStateFlow<PlanningMode>,
            searchDelegate.isSearchActive as MutableStateFlow<Boolean>
        )
    }

    // Planning Methods
    fun onPlanningModeChange(mode: PlanningMode) = planningDelegate.onPlanningModeChange(
        mode,
        searchDelegate.isSearchActive as MutableStateFlow<Boolean>,
        searchDelegate.searchQuery as MutableStateFlow<androidx.compose.ui.text.input.TextFieldValue>
    )

    fun onContextSelected(contextName: String) = viewModelScope.launch {
        planningDelegate.onContextSelected(contextName, _allListsFlat)
    }

    fun onShowRecentLists() = planningDelegate.onShowRecentLists()
    fun onDismissRecentLists() = planningDelegate.onDismissRecentLists()
    fun onRecentListSelected(listId: String) = viewModelScope.launch {
        planningDelegate.onRecentListSelected(listId)
    }
    fun onDayPlanClicked() = viewModelScope.launch { planningDelegate.onDayPlanClicked() }

    // List Operations Methods
    fun addNewList(id: String, parentId: String?, name: String) = viewModelScope.launch {
        listOperationsDelegate.addNewList(id, parentId, name)
    }

    fun onDeleteListConfirmed(list: GoalList) = viewModelScope.launch {
        listOperationsDelegate.onDeleteListConfirmed(list, listHierarchy.value.childMap)
        dismissDialog()
    }

    fun onMoveListRequest(list: GoalList) = viewModelScope.launch {
        dismissDialog()
        listOperationsDelegate.onMoveListRequest(list, _allListsFlat)
    }

    fun onListChooserResult(newParentId: String?) = viewModelScope.launch {
        listOperationsDelegate.onListChooserResult(newParentId, _allListsFlat)
    }

    fun onListReorder(fromId: String, toId: String, position: DropPosition) = viewModelScope.launch {
        listOperationsDelegate.onListReorder(fromId, toId, position, isSearchActive.value, _allListsFlat)
    }

    fun onListChooserFilterChanged(text: String) = listOperationsDelegate.onListChooserFilterChanged(text)
    fun onListChooserToggleExpanded(listId: String) = listOperationsDelegate.onListChooserToggleExpanded(listId)

    // WiFi and Import/Export Methods
    fun onShowWifiServerDialog() = viewModelScope.launch { wifiDelegate.onShowWifiServerDialog() }
    fun onDismissWifiServerDialog() = viewModelScope.launch { wifiDelegate.onDismissWifiServerDialog() }
    fun onShowWifiImportDialog() = wifiDelegate.onShowWifiImportDialog()
    fun onDismissWifiImportDialog() = wifiDelegate.onDismissWifiImportDialog()
    fun onDesktopAddressChange(newAddress: String) {
        wifiDelegate.onDesktopAddressChange(newAddress)
        viewModelScope.launch { settingsRepo.saveDesktopAddress(newAddress) }
    }
    fun performWifiImport(address: String) = viewModelScope.launch { wifiDelegate.performWifiImport(address) }
    fun exportToFile() = viewModelScope.launch { wifiDelegate.exportToFile() }
    fun onFullImportConfirmed(uri: Uri) = viewModelScope.launch {
        dismissDialog()
        wifiDelegate.onFullImportConfirmed(uri)
    }

    // General UI Methods
    fun onBottomNavExpandedChange(expanded: Boolean) {
        if (_isBottomNavExpanded.value == expanded) return
        _isBottomNavExpanded.value = expanded
        viewModelScope.launch { settingsRepo.saveBottomNavExpanded(expanded) }
    }

    fun collapseAllLists() = viewModelScope.launch {
        val listsToCollapse = _allListsFlat.value
            .filter { it.isExpanded }
            .map { it.copy(isExpanded = false) }
        if (listsToCollapse.isNotEmpty()) {
            goalRepository.updateGoalLists(listsToCollapse)
        }
    }

    fun onToggleExpanded(list: GoalList) {
        if (planningMode.value != PlanningMode.All) {
            val currentExpanded = (planningDelegate.getExpandedState(planningMode.value).value ?: emptySet()).toMutableSet()
            if (list.isExpanded) {
                currentExpanded.remove(list.id)
            } else {
                currentExpanded.add(list.id)
            }
            planningDelegate.updateExpandedState(planningMode.value, currentExpanded)
        } else {
            viewModelScope.launch {
                goalRepository.updateGoalList(list.copy(isExpanded = !list.isExpanded))
            }
        }
    }

    // Dialog Methods
    fun onAddNewListRequest() { _dialogState.value = DialogState.AddList(null) }
    fun onAddSublistRequest(parentList: GoalList) { _dialogState.value = DialogState.AddList(parentList.id) }
    fun onMenuRequested(list: GoalList) { _dialogState.value = DialogState.ContextMenu(list) }
    fun onDeleteRequest(list: GoalList) { _dialogState.value = DialogState.ConfirmDelete(list) }
    fun onEditRequest(list: GoalList) = viewModelScope.launch {
        _uiEventChannel.send(GoalListUiEvent.NavigateToEditListScreen(list.id))
    }
    fun onShowSettingsScreen() = viewModelScope.launch {
        _uiEventChannel.send(GoalListUiEvent.NavigateToSettings)
    }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp }
    fun onListClicked(listId: String) = viewModelScope.launch {
        _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId))
    }
    fun onImportFromFileRequested(uri: Uri) { _dialogState.value = DialogState.ConfirmFullImport(uri) }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
        listOperationsDelegate.resetChooserState()
    }

    // Settings Methods
    fun saveSettings(show: Boolean, daily: String, medium: String, long: String, vaultName: String) =
        viewModelScope.launch { planningDelegate.saveSettings(show, daily, medium, long, vaultName) }

    fun saveAllContexts(updatedContexts: List<com.romankozak.forwardappmobile.ui.dialogs.UiContext>) =
        viewModelScope.launch { planningDelegate.saveAllContexts(updatedContexts) }

    fun hasDescendantsWithLongNames(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        characterLimit: Int = 35
    ): Boolean = planningDelegate.hasDescendantsWithLongNames(listId, childMap, _allListsFlat.value, characterLimit)

    // --- Private Helper Methods ---
    private fun buildHierarchy(
        filterState: FilterState,
        expandedDaily: Set<String>?,
        expandedMedium: Set<String>?,
        expandedLong: Set<String>?
    ): ListHierarchyData {
        val (flatList, _, _, mode, settings) = filterState
        val isPlanningModeActive = mode != PlanningMode.All

        if (!isPlanningModeActive) {
            val topLevel = flatList.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
            return ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
        }

        val listLookup = flatList.associateBy { it.id }

        val targetTag = when (mode) {
            PlanningMode.Daily -> settings.dailyTag
            PlanningMode.Medium -> settings.mediumTag
            PlanningMode.Long -> settings.longTag
            else -> null
        }
        val matchingLists = if (targetTag != null) flatList.filter { it.tags?.contains(targetTag) == true } else emptyList()

        val ancestorIds = mutableSetOf<String>()
        val visitedAncestors = mutableSetOf<String>()
        matchingLists.forEach { list ->
            findAncestorsRecursive(list.id, listLookup, ancestorIds, visitedAncestors)
        }

        val visibleIds = ancestorIds + matchingLists.map { it.id }

        val currentExpandedState = when (mode) {
            is PlanningMode.Daily -> expandedDaily
            is PlanningMode.Medium -> expandedMedium
            is PlanningMode.Long -> expandedLong
            else -> null
        }

        val shouldInitialize = currentExpandedState == null && matchingLists.isNotEmpty()
        val currentExpandedIds = if (shouldInitialize) ancestorIds else (currentExpandedState ?: emptySet())

        if (shouldInitialize) {
            planningDelegate.updateExpandedState(mode, ancestorIds)
        }

        val visibleLists = flatList.filter { it.id in visibleIds }
        val displayLists = visibleLists.map { list -> list.copy(isExpanded = currentExpandedIds.contains(list.id)) }
        val topLevel = displayLists.filter { it.parentId == null || it.parentId !in visibleIds }.sortedBy { it.order }
        val childMap = displayLists.filter { it.parentId != null }.groupBy { it.parentId!! }

        return ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
    }

    private fun findAncestorsRecursive(
        listId: String?,
        listLookup: Map<String, GoalList>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        var currentId = listId
        while (currentId != null && visited.add(currentId)) {
            ids.add(currentId)
            currentId = listLookup[currentId]?.parentId
        }
    }

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

    private fun buildPathToList(targetId: String, hierarchy: ListHierarchyData): List<BreadcrumbItem> {
        val path = mutableListOf<BreadcrumbItem>()
        fun findPath(lists: List<GoalList>, level: Int): Boolean {
            val sortedLists = lists.sortedBy { it.order }
            for (list in sortedLists) {
                path.add(BreadcrumbItem(list.id, list.name, level))
                if (list.id == targetId) return true
                val children = hierarchy.childMap[list.id] ?: emptyList()
                if (findPath(children, level + 1)) return true
                path.removeLastOrNull()
            }
            return false
        }
        findPath(hierarchy.topLevelLists, 0)
        return path.toList()
    }
}