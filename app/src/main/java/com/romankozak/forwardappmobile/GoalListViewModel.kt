package com.romankozak.forwardappmobile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ... (sealed classes без змін) ...
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
}


class GoalListViewModel(
    application: Application,
    private val goalListDao: GoalListDao,
    private val goalDao: GoalDao,
    private val settingsRepo: SettingsRepository
) : AndroidViewModel(application) {

    private val TAG = "WIFI_DEBUG"
    // ... (решта полів без змін) ...
    private val _allListsFlat = goalListDao.getAllLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listHierarchy: StateFlow<ListHierarchy> = _allListsFlat.map { flatList ->
        val topLevel = flatList.filter { it.parentId == null }
        val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
        ListHierarchy(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchy())

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

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

    private val syncRepo = SyncRepository(goalListDao, goalDao)
    private val wifiSyncServer = WifiSyncServer(syncRepo, getApplication())

    init {
        viewModelScope.launch {
            _desktopAddress.value = settingsRepo.desktopAddressFlow.first()
        }
    }

    fun onShowWifiServerDialog() {
        Log.d(TAG, "Step 1: onShowWifiServerDialog called.")
        _wifiServerAddress.value = null
        _showWifiServerDialog.value = true
        Log.d(TAG, "Step 2: Calling startWifiServer().")
        startWifiServer()
    }

    // ЗМІНА: Повністю новий, надійний патерн запуску
    private fun startWifiServer() {
        viewModelScope.launch {
            Log.d(TAG, "Step 3: Launching coroutine, switching to IO dispatcher.")
            // Переключаємось на фоновий потік
            val result = withContext(Dispatchers.IO) {
                // Викликаємо звичайну, не-suspend функцію
                Log.d(TAG, "Step 4: On IO thread, calling wifiSyncServer.start().")
                wifiSyncServer.start()
            }
            // Повертаємось на головний потік. Результат вже є.
            Log.d(TAG, "Step 5: Back on Main thread. Result isSuccess: ${result.isSuccess}")

            result.onSuccess { address ->
                Log.d(TAG, "Step 6 (SUCCESS): Address: $address. Updating UI.")
                _wifiServerAddress.value = address
            }
                .onFailure { exception ->
                    Log.e(TAG, "Step 6 (FAILURE): Updating UI.", exception)
                    _wifiServerAddress.value = "Помилка: ${exception.message}"
                }
        }
    }

    // ... (решта коду без змін) ...
    private fun stopWifiServer() {
        viewModelScope.launch(Dispatchers.IO) {
            wifiSyncServer.stop()
            withContext(Dispatchers.Main) {
                _wifiServerAddress.value = null
            }
        }
    }

    // ... (решта методів ViewModel без змін)
    fun onToggleExpanded(list: GoalList) {
        viewModelScope.launch {
            goalListDao.update(list.copy(isExpanded = !list.isExpanded))
        }
    }

    fun onAddList(name: String, parentId: String?) {
        viewModelScope.launch {
            val newList = GoalList(
                id = UUID.randomUUID().toString(),
                name = name,
                parentId = parentId,
                description = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            goalListDao.insert(newList)
            dismissDialog()
        }
    }

    fun onDeleteListConfirmed(list: GoalList) {
        viewModelScope.launch {
            val listsToDelete = findChildrenRecursive(list.id, listHierarchy.value.childMap)
            listsToDelete.forEach {
                goalDao.deleteInstancesForLists(listOf(it.id))
                goalListDao.delete(it)
            }
            goalDao.deleteInstancesForLists(listOf(list.id))
            goalListDao.delete(list)
            dismissDialog()
        }
    }

    private fun findChildrenRecursive(listId: String, childMap: Map<String, List<GoalList>>): List<GoalList> {
        val children = childMap[listId] ?: emptyList()
        return children + children.flatMap { findChildrenRecursive(it.id, childMap) }
    }

    fun onMoveListConfirmed(listToMove: GoalList, newParentId: String?) {
        viewModelScope.launch {
            goalListDao.update(listToMove.copy(parentId = newParentId))
            dismissDialog()
        }
    }

    fun onAddNewListRequest() {
        _dialogState.value = DialogState.AddList(null)
    }

    fun onAddSublistRequest(parentList: GoalList) {
        _dialogState.value = DialogState.AddList(parentList.id)
    }

    fun onMoveListRequest(list: GoalList) {
        _dialogState.value = DialogState.MoveList(list)
    }

    fun onMenuRequested(list: GoalList) {
        _dialogState.value = DialogState.ContextMenu(list)
    }

    fun onDeleteRequest(list: GoalList) {
        _dialogState.value = DialogState.ConfirmDelete(list)
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
    }

    fun onListClicked(listId: String) {
        viewModelScope.launch {
            _uiEventChannel.send(GoalListUiEvent.NavigateToDetails(listId))
        }
    }

    fun onDesktopAddressChange(newAddress: String) {
        _desktopAddress.value = newAddress
        viewModelScope.launch {
            settingsRepo.saveDesktopAddress(newAddress)
        }
    }

    fun onDismissWifiServerDialog() {
        _showWifiServerDialog.value = false
        stopWifiServer()
    }

    fun onShowWifiImportDialog() {
        _showWifiImportDialog.value = true
    }

    fun onDismissWifiImportDialog() {
        _showWifiImportDialog.value = false
    }

    fun onRenameRequest(list: GoalList) {
        _dialogState.value = DialogState.RenameList(list)
    }

    fun onRenameListConfirmed(listToRename: GoalList, newName: String) {
        if (newName.isBlank()) {
            dismissDialog()
            return
        }
        viewModelScope.launch {
            goalListDao.update(listToRename.copy(name = newName, updatedAt = System.currentTimeMillis()))
            dismissDialog()
        }
    }

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    fun onShowSearchDialog() {
        _showSearchDialog.value = true
    }

    fun onDismissSearchDialog() {
        _showSearchDialog.value = false
    }

    fun onPerformGlobalSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                _uiEventChannel.send(GoalListUiEvent.NavigateToGlobalSearch(query))
                onDismissSearchDialog()
            }
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
}


class GoalListViewModelFactory(
    private val application: Application,
    private val goalListDao: GoalListDao,
    private val goalDao: GoalDao,
    private val settingsRepo: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoalListViewModel(application, goalListDao, goalDao, settingsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}