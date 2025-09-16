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
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.utils.HierarchyFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull

// --- DATA КЛАСИ, ВИНЕСЕНІ ЗА МЕЖІ VIEWMODEL ДЛЯ ЧИСТОТИ ---

sealed class GoalListUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : GoalListUiEvent()
    data class NavigateToDetails(val listId: String) : GoalListUiEvent()
    data class NavigateToGlobalSearch(val query: String) : GoalListUiEvent()
    object NavigateToSettings : GoalListUiEvent()
    data class ShowToast(val message: String) : GoalListUiEvent()
    data class ScrollToIndex(val index: Int) : GoalListUiEvent()
    object FocusSearchField : GoalListUiEvent()
    data class NavigateToEditListScreen(val listId: String) : GoalListUiEvent()
    data class Navigate(val route: String) : GoalListUiEvent()
    data class NavigateToDayPlan(val date: Long) : GoalListUiEvent()
}

data class SearchResult(
    val list: GoalList,
    val path: List<BreadcrumbItem>
)

sealed class PlanningMode {
    object All : PlanningMode()
    object Daily : PlanningMode()
    object Medium : PlanningMode()
    object Long : PlanningMode()
}

data class AppStatistics(val listCount: Int = 0, val goalCount: Int = 0)

sealed class DialogState {
    object Hidden : DialogState()
    data class AddList(val parentId: String?) : DialogState()
    data class ContextMenu(val list: GoalList) : DialogState()
    data class ConfirmDelete(val list: GoalList) : DialogState()
    data class EditList(val list: GoalList) : DialogState()
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
    val flatList: List<GoalList>,
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
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    companion object {
        private const val LIST_BEING_MOVED_ID_KEY = "listBeingMovedId"
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_SEARCH_HISTORY = 10
    }

    // --- ВІДНОВЛЕНІ ЗМІННІ ---
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _uiEventChannel = Channel<GoalListUiEvent>()
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

    private val listBeingMovedId = savedStateHandle.getStateFlow<String?>(LIST_BEING_MOVED_ID_KEY, null)

    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _listChooserFilterText = MutableStateFlow("")

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()
    // --- КІНЕЦЬ ВІДНОВЛЕНИХ ЗМІННИХ ---

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isReadyForFiltering = MutableStateFlow(false)
    private val _highlightedListId = MutableStateFlow<String?>(null)
    val highlightedListId: StateFlow<String?> = _highlightedListId.asStateFlow()
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()
    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()

    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)

    // <-- ДОДАНО ЗМІННУ ДЛЯ ІСТОРІЇ ПОШУКУ
    val searchHistory: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(SEARCH_HISTORY_KEY, emptyList())
    // КІНЕЦЬ ДОДАНОЇ ЗМІННОЇ -->

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

    private val _allListsFlat =
        goalRepository
            .getAllGoalListsFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val debouncedSearchQueryText =
        searchQuery
            .map { it.text }
            .debounce(350L)
            .distinctUntilChanged()

    val areAnyListsExpanded: StateFlow<Boolean> =
        _allListsFlat
            .map { lists ->
                lists.any { it.isExpanded }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appStatistics: StateFlow<AppStatistics> =
        combine(_allListsFlat, goalRepository.getAllGoalsCountFlow()) { allLists, allGoalsCount ->
            AppStatistics(listCount = allLists.size, goalCount = allGoalsCount)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    private val filterStateFlow: StateFlow<FilterState> =
        combine(
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

    val listHierarchy: StateFlow<ListHierarchyData> =
        combine(
            filterStateFlow,
            _expandedInDailyMode,
            _expandedInMediumMode,
            _expandedInLongMode,
        ) { filterState, expandedDaily, expandedMedium, expandedLong ->
            try {
                val (flatList, _, _, mode, settings) = filterState
                val isPlanningModeActive = mode != PlanningMode.All

                if (!isPlanningModeActive) {
                    val topLevel = flatList.filter { it.parentId == null }.sortedBy { it.order }
                    val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
                    return@combine ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
                }

                val listLookup = flatList.associateBy { it.id }

                val targetTag =
                    when (mode) {
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
                    when (mode) {
                        is PlanningMode.Daily -> _expandedInDailyMode.value = ancestorIds
                        is PlanningMode.Medium -> _expandedInMediumMode.value = ancestorIds
                        is PlanningMode.Long -> _expandedInLongMode.value = ancestorIds
                        else -> Unit
                    }
                }

                val visibleLists = flatList.filter { it.id in visibleIds }
                val displayLists = visibleLists.map { list -> list.copy(isExpanded = currentExpandedIds.contains(list.id)) }
                val topLevel = displayLists.filter { it.parentId == null || it.parentId !in visibleIds }.sortedBy { it.order }
                val childMap = displayLists.filter { it.parentId != null }.groupBy { it.parentId!! }

                return@combine ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
            } catch (e: Exception) {
                Log.e("GoalListViewModel_DEBUG", "Exception in listHierarchy combine block", e)
                return@combine ListHierarchyData()
            }
        }.flowOn(Dispatchers.Default)
            .catch { e ->
                Log.e("GoalListViewModel_DEBUG", "Exception in listHierarchy flow", e)
                emit(ListHierarchyData())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    private val _collapsedAncestorsOnFocus = MutableStateFlow<Set<String>>(emptySet())

    val listChooserFinalExpandedIds: StateFlow<Set<String>> =
        combine(
            _listChooserUserExpandedIds,
            _allListsFlat,
            listBeingMovedId
        ) { userExpanded, allLists, movingId ->
            if (movingId == null) return@combine emptySet<String>()

            val listLookup = allLists.associateBy { it.id }
            val ancestorIds = mutableSetOf<String>()
            val visitedAncestors = mutableSetOf<String>()
            findAncestorsRecursive(movingId, listLookup, ancestorIds, visitedAncestors)

            userExpanded + ancestorIds
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> =
        combine(
            _allListsFlat,
            _listChooserFilterText,
            listBeingMovedId
        ) { allLists, filterText, movingId ->
            if (movingId == null) {
                return@combine ListHierarchyData()
            }

            val filteredLists = if (filterText.isBlank()) {
                allLists
            } else {
                allLists.filter {
                    it.name.contains(filterText, ignoreCase = true) ||
                            fuzzyMatch(filterText, it.name)
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

    val allContextsForDialog: StateFlow<List<UiContext>> =
        contextHandler.allContextsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            filterStateFlow.collect { state ->
                if (state.searchActive && state.query.isNotBlank()) {
                    val allLists = _allListsFlat.first()
                    val fullHierarchy = ListHierarchyData(
                        allLists = allLists,
                        topLevelLists = allLists.filter { it.parentId == null }.sortedBy { it.order },
                        childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
                    )

                    val matchingLists = if (state.query.length > 3) {
                        allLists.filter { fuzzyMatch(state.query, it.name) }
                    } else {
                        allLists.filter { it.name.contains(state.query, ignoreCase = true) }
                    }

                    val results = matchingLists.map { list ->
                        SearchResult(
                            list = list,
                            path = buildPathToList(list.id, fullHierarchy)
                        )
                    }
                    _searchResults.value = results.sortedBy { it.list.name }
                } else {
                    _searchResults.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            settingsRepo.isBottomNavExpandedFlow.firstOrNull()?.let { savedState ->
                _isBottomNavExpanded.value = savedState
            }
            withContext(ioDispatcher) {
                _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
                contextHandler.initialize()
            }
        }
    }

    fun onSearchResultClick(listId: String) {
        viewModelScope.launch {
            onToggleSearch(false)

            val allLists = _allListsFlat.first()
            val fullHierarchy = ListHierarchyData(
                allLists = allLists,
                topLevelLists = allLists.filter { it.parentId == null }.sortedBy { it.order },
                childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            )

            val path = buildPathToList(listId, fullHierarchy)
            val level = if (path.isEmpty()) 0 else path.last().level

            val settings = hierarchySettings.value

            if (level >= settings.useBreadcrumbsAfter) {
                navigateToList(listId)
            } else {
                processRevealRequest(listId)
            }
        }
    }

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()
    val recentLists: StateFlow<List<GoalList>> =
        goalRepository
            .getRecentLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onShowRecentLists() {
        _showRecentListsSheet.value = true
    }

    fun onDismissRecentLists() {
        _showRecentListsSheet.value = false
    }

    fun onRecentListSelected(listId: String) {
        viewModelScope.launch {
            onDismissRecentLists()
            _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId))
        }
    }

    fun onDayPlanClicked() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            _uiEventChannel.send(GoalListUiEvent.NavigateToDayPlan(today))
        }
    }

    fun processRevealRequest(listId: String) {
        viewModelScope.launch {
            _isSearchActive.value = false
            _planningMode.value = PlanningMode.All

            val allLists = _allListsFlat.first { it.isNotEmpty() }
            val listLookup = allLists.associateBy { it.id }

            if (!listLookup.containsKey(listId)) {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Could not find list."))
                return@launch
            }

            val ancestorIds = mutableSetOf<String>()
            findAncestorsRecursive(listId, listLookup, ancestorIds, mutableSetOf())

            val listsToExpand = ancestorIds
                .mapNotNull { listLookup[it] }
                .filter { !it.isExpanded && it.id != listId }

            if (listsToExpand.isNotEmpty()) {
                goalRepository.updateGoalLists(listsToExpand.map { it.copy(isExpanded = true) })

                _allListsFlat.first { updatedListState ->
                    listsToExpand.all { listToExpand ->
                        updatedListState.find { it.id == listToExpand.id }?.isExpanded == true
                    }
                }
            }

            val finalListState = _allListsFlat.value
            val topLevel = finalListState.filter { it.parentId == null }.sortedBy { it.order }

            fun flattenHierarchy(currentLists: List<GoalList>, listMap: Map<String, List<GoalList>>): List<GoalList> {
                val result = mutableListOf<GoalList>()
                for (list in currentLists) {
                    result.add(list)
                    if (list.isExpanded) {
                        val children = listMap[list.id]?.sortedBy { it.order } ?: emptyList()
                        if (children.isNotEmpty()) {
                            result.addAll(flattenHierarchy(children, listMap))
                        }
                    }
                }
                return result
            }

            val childrenOnly = finalListState.filter { it.parentId != null }
            val childMap = childrenOnly.groupBy { it.parentId!! }
            val displayedLists = flattenHierarchy(topLevel, childMap)

            val index = displayedLists.indexOfFirst { it.id == listId }

            if (index != -1) {
                _uiEventChannel.send(GoalListUiEvent.ScrollToIndex(index))
            }

            _highlightedListId.value = listId
            delay(1500)
            if (_highlightedListId.value == listId) {
                _highlightedListId.value = null
            }
        }
    }

    fun onToggleSearch(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (isActive) {
            val currentText = _searchQuery.value.text
            _searchQuery.value = TextFieldValue(currentText, TextRange(0, currentText.length))
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.FocusSearchField)
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

    fun collapseAllLists() {
        viewModelScope.launch {
            val listsToCollapse =
                _allListsFlat.value
                    .filter { it.isExpanded }
                    .map { it.copy(isExpanded = false) }
            if (listsToCollapse.isNotEmpty()) {
                goalRepository.updateGoalLists(listsToCollapse)
            }
        }
    }

    fun onToggleExpanded(list: GoalList) {
        if (planningMode.value != PlanningMode.All) {
            val currentStateFlow =
                when (planningMode.value) {
                    is PlanningMode.Daily -> _expandedInDailyMode
                    is PlanningMode.Medium -> _expandedInMediumMode
                    is PlanningMode.Long -> _expandedInLongMode
                    else -> return
                }

            val currentExpanded = (currentStateFlow.value ?: emptySet()).toMutableSet()
            if (list.isExpanded) {
                currentExpanded.remove(list.id)
            } else {
                currentExpanded.add(list.id)
            }
            currentStateFlow.value = currentExpanded

        } else {
            viewModelScope.launch {
                goalRepository.updateGoalList(list.copy(isExpanded = !list.isExpanded))
            }
        }
    }

    fun onSearchQueryChanged(query: TextFieldValue) {
        _searchQuery.value = query
    }

    // <-- НОВІ МЕТОДИ ДЛЯ ІСТОРІЇ ПОШУКУ
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
    // КІНЕЦЬ НОВИХ МЕТОДІВ -->

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

    fun addNewList(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            goalRepository.createGoalListWithId(id, name, parentId)
            if (parentId != null) {
                val parentList = _allListsFlat.value.find { it.id == parentId }
                if (parentList != null && !parentList.isExpanded) {
                    goalRepository.updateGoalList(parentList.copy(isExpanded = true))
                }
            }
        }
    }

    fun onDeleteListConfirmed(list: GoalList) {
        viewModelScope.launch {
            val listsToDelete = findDescendantsForDeletion(list.id, listHierarchy.value.childMap)
            goalRepository.deleteListsAndSubLists(listOf(list) + listsToDelete)
            dismissDialog()
        }
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

    private fun findDescendantsRecursive(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        if (!visited.add(listId)) return
        ids.add(listId)
        childMap[listId]?.forEach { child ->
            findDescendantsRecursive(child.id, childMap, ids, visited)
        }
    }

    private fun findDescendantsForDeletion(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): List<GoalList> {
        if (!visited.add(listId)) return emptyList()
        val children = childMap[listId] ?: emptyList()
        return children + children.flatMap { findDescendantsForDeletion(it.id, childMap, visited) }
    }

    fun performWifiImport(address: String) {
        viewModelScope.launch {
            val result = syncRepo.fetchBackupFromWifi(address)
            result.onSuccess { jsonString ->
                _uiEventChannel.send(GoalListUiEvent.NavigateToSyncScreenWithData(jsonString))
                onDismissWifiImportDialog()
            }.onFailure {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Error: ${it.message}"))
            }
        }
    }

    fun onAddNewListRequest() { _dialogState.value = DialogState.AddList(null) }
    fun onAddSublistRequest(parentList: GoalList) { _dialogState.value = DialogState.AddList(parentList.id) }

    private fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
        val descendants = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(listId)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            childMap[currentId]?.forEach { child ->
                descendants.add(child.id)
                queue.add(child.id)
            }
        }
        return descendants
    }

    fun onMoveListRequest(list: GoalList) {
        dismissDialog()
        savedStateHandle[LIST_BEING_MOVED_ID_KEY] = list.id
        viewModelScope.launch {
            val title = "Перемістити '${list.name}'"
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val allLists = _allListsFlat.first()
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            val descendantIds = getDescendantIds(list.id, childMap).joinToString(",")
            val currentParentId = list.parentId ?: "root"
            val disabledIds = "${list.id}${if (descendantIds.isNotEmpty()) ",$descendantIds" else ""}"
            val route = "list_chooser_screen/$encodedTitle?currentParentId=$currentParentId&disabledIds=$disabledIds"
            _uiEventChannel.send(GoalListUiEvent.Navigate(route))
        }
    }

    fun onMenuRequested(list: GoalList) { _dialogState.value = DialogState.ContextMenu(list) }
    fun onDeleteRequest(list: GoalList) { _dialogState.value = DialogState.ConfirmDelete(list) }
    fun onEditRequest(list: GoalList) {
        viewModelScope.launch {
            _uiEventChannel.send(GoalListUiEvent.NavigateToEditListScreen(list.id))
        }
    }

    fun onShowSettingsScreen() { viewModelScope.launch { _uiEventChannel.send(GoalListUiEvent.NavigateToSettings) } }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp }
    fun onListClicked(listId: String) { viewModelScope.launch { _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId)) } }
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
                _uiEventChannel.send(GoalListUiEvent.NavigateToGlobalSearch(query))
                onDismissSearchDialog()
            }
        }
    }

    fun exportToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.exportFullBackupToFile()
            result.onSuccess { message ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast(message))
            }.onFailure { error ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Export error: ${error.message}"))
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
            val listIdToMove = listBeingMovedId.value ?: return@launch
            val listToMove = _allListsFlat.value.find { it.id == listIdToMove } ?: return@launch
            val finalNewParentId = if (newParentId == "root") null else newParentId
            if (listToMove.parentId == finalNewParentId) {
                savedStateHandle[LIST_BEING_MOVED_ID_KEY] = null
                return@launch
            }
            goalRepository.moveGoalList(listToMove, finalNewParentId)
            if (finalNewParentId != null) {
                val parentList = _allListsFlat.value.find { it.id == finalNewParentId }
                if (parentList != null && !parentList.isExpanded) {
                    goalRepository.updateGoalList(parentList.copy(isExpanded = true))
                }
            }
            savedStateHandle[LIST_BEING_MOVED_ID_KEY] = null
        }
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
        _listChooserFilterText.value = ""
        _listChooserUserExpandedIds.value = emptySet()
    }

    fun onListReorder(fromId: String, toId: String, position: DropPosition) {
        if (fromId == toId || isSearchActive.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val allLists = _allListsFlat.first()
            val fromList = allLists.find { it.id == fromId }
            val toList = allLists.find { it.id == toId }
            if (fromList == null || toList == null || fromList.parentId != toList.parentId) return@launch
            val parentId = fromList.parentId
            val siblings = allLists.filter { it.parentId == parentId }.sortedBy { it.order }
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
            val listsToUpdate = mutableSiblings.mapIndexed { index, list ->
                list.copy(order = index.toLong(), updatedAt = System.currentTimeMillis())
            }
            goalRepository.updateGoalLists(listsToUpdate)
        }
    }

    fun onFullImportConfirmed(uri: Uri) {
        dismissDialog()
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.importFullBackupFromFile(uri)
            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    _uiEventChannel.send(GoalListUiEvent.ShowToast(message))
                }.onFailure { error ->
                    _uiEventChannel.send(GoalListUiEvent.ShowToast("Import error: ${error.message}"))
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
    private val _focusedListId = MutableStateFlow<String?>(null)
    val focusedListId = _focusedListId.asStateFlow()
    private val _hierarchySettings = MutableStateFlow(HierarchyDisplaySettings())
    val hierarchySettings = _hierarchySettings.asStateFlow()

    fun navigateToList(listId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val hierarchy = listHierarchy.value
            val path = buildPathToList(listId, hierarchy)

            val collapsedIds = path
                .mapNotNull { breadcrumbItem ->
                    hierarchy.allLists.find { it.id == breadcrumbItem.id }
                }
                .filter { !it.isExpanded }
                .map { it.id }
                .toSet()

            _collapsedAncestorsOnFocus.value = collapsedIds

            _currentBreadcrumbs.value = path
            _focusedListId.value = listId
        }
    }

    fun navigateToBreadcrumb(breadcrumbItem: BreadcrumbItem) {
        val currentPath = _currentBreadcrumbs.value
        val newPath = currentPath.take(breadcrumbItem.level + 1)
        _currentBreadcrumbs.value = newPath
        _focusedListId.value = breadcrumbItem.id
    }

    fun clearNavigation() {
        viewModelScope.launch {
            val breadcrumbs = _currentBreadcrumbs.value
            if (breadcrumbs.isNotEmpty()) {
                val topLevelAncestorId = breadcrumbs.first().id
                val ancestorList = _allListsFlat.value.find { it.id == topLevelAncestorId }

                if (ancestorList != null && !ancestorList.isExpanded) {
                    goalRepository.updateGoalList(ancestorList.copy(isExpanded = true))

                    _allListsFlat.first { updatedLists ->
                        updatedLists.find { it.id == topLevelAncestorId }?.isExpanded == true
                    }
                }
            }

            val listsToCollapseIds = _collapsedAncestorsOnFocus.value
            if (listsToCollapseIds.isNotEmpty()) {
                val listsToUpdate = _allListsFlat.value
                    .filter { it.id in listsToCollapseIds }
                    .map { it.copy(isExpanded = false) }

                if (listsToUpdate.isNotEmpty()) {
                    goalRepository.updateGoalLists(listsToUpdate)
                }
            }

            _collapsedAncestorsOnFocus.value = emptySet()
            _currentBreadcrumbs.value = emptyList()
            _focusedListId.value = null
        }
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

    fun onContextSelected(contextName: String) {
        viewModelScope.launch {
            val targetTag = contextHandler.getContextTag(contextName)
            if (targetTag.isNullOrBlank()) {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Тег для контексту '$contextName' не знайдено або порожній"))
                return@launch
            }
            val targetList = _allListsFlat.value.find { it.tags?.contains(targetTag) == true }
            if (targetList != null) {
                _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(targetList.id))
            } else {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Список з тегом '#$targetTag' не знайдено"))
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

    fun hasDescendantsWithLongNames(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        characterLimit: Int = 35
    ): Boolean {
        val queue = ArrayDeque<String>()

        childMap[listId]?.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val list = _allListsFlat.value.find { it.id == currentId }

            if (list != null) {
                if (list.name.length > characterLimit) {
                    return true
                }

                childMap[currentId]?.forEach { queue.add(it.id) }
            }
        }

        return false
    }

    fun saveAllContexts(updatedContexts: List<UiContext>) {
        viewModelScope.launch {
            val customContextsToSave = updatedContexts.filter { !it.isReserved }

            settingsRepo.saveCustomContexts(customContextsToSave)

            contextHandler.initialize()
        }
    }
}