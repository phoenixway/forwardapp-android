package com.romankozak.forwardappmobile.ui.screens.goallist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.sync.SyncRepository
import com.romankozak.forwardappmobile.data.sync.WifiSyncServer
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class GoalListUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : GoalListUiEvent()
    data class NavigateToDetails(val listId: String) : GoalListUiEvent()
    data class NavigateToGlobalSearch(val query: String) : GoalListUiEvent()
    data class ShowToast(val message: String) : GoalListUiEvent()
}

sealed class DialogState {
    object Hidden : DialogState()
    data class AddList(val parentId: String?) : DialogState()
    data class MoveList(val list: GoalList) : DialogState()
    data class ContextMenu(val list: GoalList) : DialogState()
    data class ConfirmDelete(val list: GoalList) : DialogState()
    data class RenameList(val list: GoalList) : DialogState()
    object AppSettings : DialogState()
    object AboutApp : DialogState() // --- ДОДАНО: Новий стан для діалогу "Про додаток"
}

@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepo: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private val _allListsFlat = goalRepository.getAllGoalListsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listHierarchy: StateFlow<ListHierarchyData> = _allListsFlat.map { flatList ->
        val topLevel = flatList.filter { it.parentId == null }
        val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
        ListHierarchyData(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    val obsidianVaultName: StateFlow<String> = settingsRepo.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _showWifiServerDialog = MutableStateFlow(false)
    val showWifiServerDialog: StateFlow<Boolean> = _showWifiServerDialog.asStateFlow()

    private val _showWifiImportDialog = MutableStateFlow(false)
    val showWifiImportDialog: StateFlow<Boolean> = _showWifiImportDialog.asStateFlow()

    private val _wifiServerAddress = MutableStateFlow<String?>(null)
    val wifiServerAddress: StateFlow<String?> = _wifiServerAddress.asStateFlow()

    private val _desktopAddress = MutableStateFlow("")
    val desktopAddress: StateFlow<String> = _desktopAddress.asStateFlow()

    private val _uiEventChannel = Channel<GoalListUiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val syncRepo = SyncRepository(goalRepository)
    private val wifiSyncServer = WifiSyncServer(syncRepo, application)

    init {
        viewModelScope.launch {
            _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
        }
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
                _wifiServerAddress.value = "Помилка: ${exception.message}"
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

    fun onToggleExpanded(list: GoalList) {
        viewModelScope.launch {
            goalRepository.updateGoalList(list.copy(isExpanded = !list.isExpanded))
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
            val listsToDelete = findChildrenRecursive(list.id, listHierarchy.value.childMap)
            goalRepository.deleteListsAndSubLists(listOf(list) + listsToDelete)
            dismissDialog()
        }
    }

    private fun findChildrenRecursive(listId: String, childMap: Map<String, List<GoalList>>): List<GoalList> {
        val children = childMap[listId] ?: emptyList()
        return children + children.flatMap { findChildrenRecursive(it.id, childMap) }
    }

    fun onMoveListConfirmed(listToMove: GoalList, newParentId: String?) {
        viewModelScope.launch {
            goalRepository.updateGoalList(listToMove.copy(parentId = newParentId))
            dismissDialog()
        }
    }

    fun onRenameListConfirmed(listToRename: GoalList, newName: String) {
        if (newName.isBlank()) {
            dismissDialog()
            return
        }
        viewModelScope.launch {
            goalRepository.updateGoalList(listToRename.copy(name = newName, updatedAt = System.currentTimeMillis()))
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
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Помилка: ${it.message}"))
            }
        }
    }

    fun onSaveSettings(vaultName: String) {
        viewModelScope.launch {
            settingsRepo.saveObsidianVaultName(vaultName)
            dismissDialog()
        }
    }

    // --- Навігація та UI події ---
    fun onAddNewListRequest() { _dialogState.value = DialogState.AddList(null) }
    fun onAddSublistRequest(parentList: GoalList) { _dialogState.value = DialogState.AddList(parentList.id) }
    fun onMoveListRequest(list: GoalList) { _dialogState.value = DialogState.MoveList(list) }
    fun onMenuRequested(list: GoalList) { _dialogState.value = DialogState.ContextMenu(list) }
    fun onDeleteRequest(list: GoalList) { _dialogState.value = DialogState.ConfirmDelete(list) }
    fun onRenameRequest(list: GoalList) { _dialogState.value = DialogState.RenameList(list) }
    fun onShowSettingsDialog() { _dialogState.value = DialogState.AppSettings }
    fun onShowAboutDialog() { _dialogState.value = DialogState.AboutApp } // --- ДОДАНО: Функція для показу діалогу
    fun dismissDialog() { _dialogState.value = DialogState.Hidden }
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

        val allLists = _allListsFlat.value
        val fromList = allLists.find { it.id == fromId }
        val toList = allLists.find { it.id == toId }

        // Перевіряємо, що обидва списки існують і є сиблінгами (мають однаковий parentId)
        if (fromList == null || toList == null || fromList.parentId != toList.parentId) {
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.ShowToast("Переміщення можливе лише на одному рівні."))
            }
            return
        }

        // Отримуємо всіх сиблінгів для цієї групи та сортуємо їх за поточним порядком
        val siblings = allLists.filter { it.parentId == fromList.parentId }.sortedBy { it.order }
        val mutableSiblings = siblings.toMutableList()

        // Виконуємо переміщення всередині групи сиблінгів
        val fromIndex = mutableSiblings.indexOf(fromList)
        val toIndex = mutableSiblings.indexOf(toList)
        if (fromIndex != -1 && toIndex != -1) {
            mutableSiblings.add(toIndex, mutableSiblings.removeAt(fromIndex))
        }

        // Оновлюємо порядок тільки для зміненої групи
        val updatedOrderIds = mutableSiblings.map { it.id }

        viewModelScope.launch(Dispatchers.IO) {
            goalRepository.updateListsOrder(updatedOrderIds)
        }
    }
}
