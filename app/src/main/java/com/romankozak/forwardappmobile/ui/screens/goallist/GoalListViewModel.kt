// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goallist/GoalListViewModel.kt

package com.romankozak.forwardappmobile.ui.screens.goallist

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.WifiSyncServer
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

sealed class GoalListUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : GoalListUiEvent()
    data class NavigateToDetails(val listId: String) : GoalListUiEvent()
    data class NavigateToGlobalSearch(val query: String) : GoalListUiEvent()
    data class ShowToast(val message: String) : GoalListUiEvent()
}

sealed class PlanningMode {
    object All : PlanningMode()
    object Daily : PlanningMode()
    object Medium : PlanningMode()
    object Long : PlanningMode()
}

data class AppStatistics(
    val listCount: Int = 0,
    val goalCount: Int = 0,
)

sealed class DialogState {
    object Hidden : DialogState()
    data class AddList(val parentId: String?) : DialogState()
    data class MoveList(val list: GoalList) : DialogState()
    data class ContextMenu(val list: GoalList) : DialogState()
    data class ConfirmDelete(val list: GoalList) : DialogState()
    data class EditList(val list: GoalList) : DialogState()
    object AppSettings : DialogState()
    object ReservedContextsSettings : DialogState()
    object AboutApp : DialogState()
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

@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application,
    private val syncRepo: SyncRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _planningMode = MutableStateFlow<PlanningMode>(PlanningMode.All)
    val planningMode = _planningMode.asStateFlow()

    private val _expandedInSearchMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInDailyMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInMediumMode = MutableStateFlow<Set<String>?>(null)
    private val _expandedInLongMode = MutableStateFlow<Set<String>?>(null)

    val obsidianVaultName: StateFlow<String> = settingsRepo.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val planningSettingsState: StateFlow<PlanningSettingsState> = combine(
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

    private val _allListsFlat = goalRepository.getAllGoalListsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appStatistics: StateFlow<AppStatistics> =
        combine(_allListsFlat, goalRepository.getAllGoalsCountFlow()) { allLists, allGoalsCount ->
            AppStatistics(listCount = allLists.size, goalCount = allGoalsCount)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStatistics())

    private val filterStateFlow: StateFlow<FilterState> = combine(
        _allListsFlat,
        searchQuery,
        isSearchActive,
        planningMode,
        planningSettingsState
    ) { flatList, query, searchActive, mode, settings ->
        FilterState(flatList, query, searchActive, mode, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterState(emptyList(), "", false, PlanningMode.All, PlanningSettingsState()))

    val listHierarchy: StateFlow<ListHierarchyData> =
        combine(
            filterStateFlow,
            _expandedInSearchMode,
            _expandedInDailyMode,
            _expandedInMediumMode,
            _expandedInLongMode,
        ) { filterState, expandedSearch, expandedDaily, expandedMedium, expandedLong ->
            val (flatList, query, searchActive, mode, settings) = filterState
            val isFilterActive = (searchActive && query.isNotBlank()) || (mode != PlanningMode.All)

            if (!isFilterActive) {
                val topLevel = flatList.filter { it.parentId == null }.sortedBy { it.order }
                val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
                val displayLists = flatList.map { list -> list.copy(isExpanded = list.isExpanded) }
                return@combine ListHierarchyData(allLists = displayLists, topLevelLists = topLevel, childMap = childMap)
            }

            val listLookup = flatList.associateBy { it.id }
            val fullChildMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }

            val matchingLists = if (searchActive && query.isNotBlank()) {
                flatList.filter { it.name.contains(query, ignoreCase = true) }
            } else {
                val targetTag = when (mode) {
                    PlanningMode.Daily -> settings.dailyTag
                    PlanningMode.Medium -> settings.mediumTag
                    PlanningMode.Long -> settings.longTag
                    else -> null
                }
                if (targetTag != null) flatList.filter { it.tags?.contains(targetTag) == true } else emptyList()
            }

            val descendantIds = mutableSetOf<String>()
            val ancestorIds = mutableSetOf<String>()
            val visitedAncestors = mutableSetOf<String>()
            val visitedDescendants = mutableSetOf<String>()

            matchingLists.forEach { list ->
                findAncestorsRecursive(list.id, listLookup, ancestorIds, visitedAncestors)
                findDescendantsRecursive(list.id, fullChildMap, descendantIds, visitedDescendants)
            }

            val visibleIds = mutableSetOf<String>().apply {
                addAll(ancestorIds)
                addAll(descendantIds)
            }

            val currentExpandedState = when {
                searchActive -> expandedSearch
                mode is PlanningMode.Daily -> expandedDaily
                mode is PlanningMode.Medium -> expandedMedium
                mode is PlanningMode.Long -> expandedLong
                else -> null
            }

            val shouldInitialize = currentExpandedState == null && matchingLists.isNotEmpty()
            val currentExpandedIds = if (shouldInitialize) visibleIds else (currentExpandedState ?: emptySet())

            if (shouldInitialize) {
                when {
                    searchActive -> _expandedInSearchMode.value = visibleIds
                    mode is PlanningMode.Daily -> _expandedInDailyMode.value = visibleIds
                    mode is PlanningMode.Medium -> _expandedInMediumMode.value = visibleIds
                    mode is PlanningMode.Long -> _expandedInLongMode.value = visibleIds
                }
            }

            val visibleLists = flatList.filter { it.id in visibleIds }
            val displayLists = visibleLists.map { list ->
                list.copy(isExpanded = currentExpandedIds.contains(list.id))
            }

            val topLevel = displayLists.filter { it.parentId == null || it.parentId !in visibleIds }.sortedBy { it.order }
            val childMap = displayLists.filter { it.parentId != null }.groupBy { it.parentId!! }

            return@combine ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    // --- ДОДАНО: Стан для керування новим діалогом переміщення ---
    private val _listChooserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val listChooserExpandedIds = _listChooserExpandedIds.asStateFlow()

    private val _listChooserFilterText = MutableStateFlow("")
    val listChooserFilterText = _listChooserFilterText.asStateFlow()

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> =
        combine(listChooserFilterText, listHierarchy) { filter, originalHierarchy ->
            if (filter.isBlank() || originalHierarchy.allLists.isEmpty()) {
                originalHierarchy
            } else {
                val lowercasedFilter = filter.lowercase()
                val allListsById = originalHierarchy.allLists.associateBy { it.id }
                val matchingIds = originalHierarchy.allLists
                    .filter { it.name.lowercase().contains(lowercasedFilter) }
                    .map { it.id }
                    .toSet()

                if (matchingIds.isEmpty()) {
                    return@combine ListHierarchyData(originalHierarchy.allLists, emptyList(), emptyMap())
                }

                val visibleIds = matchingIds.toMutableSet()
                val visitedInLoop = mutableSetOf<String>()
                matchingIds.forEach { id ->
                    visitedInLoop.clear()
                    var current = allListsById[id]
                    while (current != null) {
                        if (!visitedInLoop.add(current.id)) break
                        visibleIds.add(current.id)
                        current = current.parentId?.let { allListsById[it] }
                    }
                }

                val finalVisibleLists = originalHierarchy.allLists.filter { it.id in visibleIds }
                val topLevel = finalVisibleLists.filter { it.parentId == null || !visibleIds.contains(it.parentId) }.sortedBy { it.order }
                val childMap = finalVisibleLists.filter { it.parentId != null }.groupBy { it.parentId!! }
                ListHierarchyData(originalHierarchy.allLists, topLevel, childMap)
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(500), ListHierarchyData())
    // --- КІНЕЦЬ НОВОГО БЛОКУ ---

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    // ... (решта коду ViewModel без змін до onMoveListConfirmed) ...

    // ОНОВЛЕНО: обробники для нового діалогу
    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserExpandedIds.value.toMutableSet()
        if (listId in currentIds) currentIds.remove(listId) else currentIds.add(listId)
        _listChooserExpandedIds.value = currentIds
    }

    fun onMoveListConfirmed(newParentId: String?) {
        val state = _dialogState.value
        if (state !is DialogState.MoveList) return

        val listToMove = state.list
        if (listToMove.parentId == newParentId) {
            dismissDialog()
            return
        }
        viewModelScope.launch {
            // goalRepository.moveGoalList не оновлює час, тому робимо це тут
            val updatedList = listToMove.copy(
                parentId = newParentId,
                updatedAt = System.currentTimeMillis() // <-- ДОДАТИ
            )
            goalRepository.moveGoalList(updatedList, newParentId) // Передаємо оновлений список
        }
        dismissDialog()
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
        // Скидаємо стан діалогу вибору при закритті
        _listChooserFilterText.value = ""
        _listChooserExpandedIds.value = emptySet()
    }

    // --- Існуючий код ViewModel ---
    private val _showWifiServerDialog = MutableStateFlow(false)
    val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

    private val _showWifiImportDialog = MutableStateFlow(false)
    val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

    private val _wifiServerAddress = MutableStateFlow<String?>(null)
    val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

    private val _desktopAddress = MutableStateFlow("")
    val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

    val contextTagsState: StateFlow<Map<String, String>> = combine(
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.BUY),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.PM),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.PAPER),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.MENTAL),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.PROVIDENCE),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.MANUAL),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.RESEARCH),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.DEVICE),
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.MIDDLE), // <-- Додано
        settingsRepo.getContextTagFlow(SettingsRepository.ContextKeys.LONG)    // <-- Додано
    ) { tags ->
        mapOf(
            "buy" to tags[0], "pm" to tags[1], "paper" to tags[2], "mental" to tags[3],
            "providence" to tags[4], "manual" to tags[5], "research" to tags[6], "device" to tags[7],
            "middle" to tags[8],
            "long" to tags[9]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ✨ ЗМІНЕНО: Допоміжна мапа для збереження
    private val contextKeyMap = mapOf(
        "buy" to SettingsRepository.ContextKeys.BUY, "pm" to SettingsRepository.ContextKeys.PM,
        "paper" to SettingsRepository.ContextKeys.PAPER, "mental" to SettingsRepository.ContextKeys.MENTAL,
        "providence" to SettingsRepository.ContextKeys.PROVIDENCE, "manual" to SettingsRepository.ContextKeys.MANUAL,
        "research" to SettingsRepository.ContextKeys.RESEARCH, "device" to SettingsRepository.ContextKeys.DEVICE,
        "middle" to SettingsRepository.ContextKeys.MIDDLE,
        "long" to SettingsRepository.ContextKeys.LONG
    )
    private val _uiEventChannel = Channel<GoalListUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val wifiSyncServer = WifiSyncServer(syncRepo, application)

    init {
        viewModelScope.launch {
            _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
        }
    }

    fun onToggleSearch(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (!isActive) {
            _searchQuery.value = ""
        }
    }

    fun onPlanningModeChange(mode: PlanningMode) {
        _planningMode.value = mode
    }

    fun onToggleExpanded(list: GoalList) {
        if (isSearchActive.value || planningMode.value != PlanningMode.All) {
            val currentStateFlow = when {
                isSearchActive.value -> _expandedInSearchMode
                planningMode.value is PlanningMode.Daily -> _expandedInDailyMode
                planningMode.value is PlanningMode.Medium -> _expandedInMediumMode
                planningMode.value is PlanningMode.Long -> _expandedInLongMode
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

    fun onSearchQueryChanged(query: String) {
        _expandedInSearchMode.value = null
        _searchQuery.value = query
    }

    fun onShowWifiServerDialog() {
        _wifiServerAddress.value = null
        _showWifiServerDialog.value = true
        startWifiServer()
    }

    private fun startWifiServer() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                wifiSyncServer.start()
            }
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

    fun onAddList(name: String, parentId: String?) {
        viewModelScope.launch {
            goalRepository.createGoalList(name, parentId)
            dismissDialog()
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
        if (listId == null || !visited.add(listId)) return
        ids.add(listId)
        val parent = listLookup[listId]
        findAncestorsRecursive(parent?.parentId, listLookup, ids, visited)
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

    fun onEditListConfirmed(listToEdit: GoalList, newName: String, newTags: List<String>) {
        if (newName.isBlank()) {
            dismissDialog()
            return
        }
        viewModelScope.launch {
            goalRepository.updateGoalList(
                listToEdit.copy(
                    name = newName,
                    tags = newTags.filter { it.isNotBlank() }.map { it.trim() },
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            dismissDialog()
        }
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
    fun onMoveListRequest(list: GoalList) { _dialogState.value = DialogState.MoveList(list) }
    fun onMenuRequested(list: GoalList) { _dialogState.value = DialogState.ContextMenu(list) }
    fun onDeleteRequest(list: GoalList) { _dialogState.value = DialogState.ConfirmDelete(list) }
    fun onEditRequest(list: GoalList) { _dialogState.value = DialogState.EditList(list) }
    fun onShowSettingsDialog() { _dialogState.value = DialogState.AppSettings }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp }
    fun onListClicked(listId: String) { viewModelScope.launch { _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId)) } }
    fun onDesktopAddressChange(newAddress: String) {
        _desktopAddress.value = newAddress
        viewModelScope.launch { settingsRepo.saveDesktopAddress(newAddress) }
    }
    fun onDismissWifiServerDialog() { _showWifiServerDialog.value = false; stopWifiServer() }
    fun onShowWifiImportDialog() { _showWifiImportDialog.value = true }
    fun onDismissWifiImportDialog() { _showWifiImportDialog.value = false }
    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()
    fun onShowSearchDialog() { _showSearchDialog.value = true }
    fun onDismissSearchDialog() { _showSearchDialog.value = false }
    fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.NavigateToGlobalSearch(query))
                onDismissSearchDialog()
            }
        }
    }
    fun onListMoved(fromId: String, toId: String) {
        if (fromId == toId) return
        if (isSearchActive.value) {
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Moving is not possible in filter mode."))
            }
            return
        }

        val allLists = _allListsFlat.value
        val fromList = allLists.find { it.id == fromId }
        val toList = allLists.find { it.id == toId }

        if (fromList == null || toList == null || fromList.parentId != toList.parentId) {
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Moving is only possible on the same level."))
            }
            return
        }

        val siblings = allLists.filter { it.parentId == fromList.parentId }.sortedBy { it.order }
        val mutableSiblings = siblings.toMutableList()

        val fromIndex = mutableSiblings.indexOf(fromList)
        val toIndex = mutableSiblings.indexOf(toList)
        if (fromIndex != -1 && toIndex != -1) {
            mutableSiblings.add(toIndex, mutableSiblings.removeAt(fromIndex))
        }

/*        val updatedOrderIds = mutableSiblings.map { it.id }

        viewModelScope.launch(Dispatchers.IO) {
            goalRepository.updateListsOrder(updatedOrderIds)
        }*/

        val listsToUpdate = mutableSiblings.mapIndexed { index, list ->
            list.copy(
                order = index.toLong(),
                updatedAt = System.currentTimeMillis() // <-- ДОДАТИ
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Потрібен метод, що приймає список об'єктів
            goalRepository.updateGoalLists(listsToUpdate)
        }
    }

    fun exportToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.exportDatabaseToFile()
            result.onSuccess { message ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast(message))
            }.onFailure { error ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Export error: ${error.message}"))
            }
        }
    }

    fun importFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncRepo.importDatabaseFromFile(uri)
            result.onSuccess { message ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast(message))
            }.onFailure { error ->
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Import error: ${error.message}"))
            }
        }
    }
    fun saveSettings(
        show: Boolean, daily: String, medium: String, long: String, vaultName: String
    ) {
        viewModelScope.launch {
            settingsRepo.saveShowPlanningModes(show)
            settingsRepo.saveDailyTag(daily.trim())
            settingsRepo.saveMediumTag(medium.trim())
            settingsRepo.saveLongTag(long.trim())
            settingsRepo.saveObsidianVaultName(vaultName.trim())
        }
    }

    // ✨ НОВА ФУНКЦІЯ: Для збереження налаштувань контекстів
    fun saveContextSettings(newContextTags: Map<String, String>) {
        viewModelScope.launch {
            newContextTags.forEach { (contextName, tagValue) ->
                contextKeyMap[contextName]?.let { preferenceKey ->
                    settingsRepo.saveContextTag(preferenceKey, tagValue.trim())
                }
            }
        }
    }

    fun onManageContextsRequest() {
        _dialogState.value = DialogState.ReservedContextsSettings
    }
}