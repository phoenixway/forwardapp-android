// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/GoalDetailViewModel.kt ---
package com.romankozak.forwardappmobile.ui.screens.goaldetail

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val itemId: String) : UiEvent()
    data class ScrollTo(val index: Int) : UiEvent()
    data class NavigateBackAndReveal(val listId: String) : UiEvent()
}

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val itemContent: ListItemContent) : GoalActionDialogState()
    data class AwaitingListChoice(
        val sourceItemIds: Set<String>,
        val sourceGoalIds: Set<String>,
        val actionType: GoalActionType
    ) : GoalActionDialogState()
}

enum class GoalActionType { CreateInstance, MoveInstance, CopyGoal, MoveToTop }

data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedItemId: String? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedItemId: String? = null
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")

    private val _uiState = MutableStateFlow(UiState(
        goalToHighlight = savedStateHandle.get<String>("goalToHighlight")
    ))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

    val goalList: StateFlow<GoalList?> = listIdFlow.flatMapLatest { id ->
        if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val listContent: StateFlow<List<ListItemContent>> =
        combine(listIdFlow, _uiState.map { it.localSearchQuery }.distinctUntilChanged()) { id, query -> Pair(id, query) }
            .flatMapLatest { (id, query) ->
                if (id.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    goalRepository.getListContentStream(id).map { content ->
                        if (query.isNotBlank()) {
                            content.filter { itemContent ->
                                val textToSearch = when (itemContent) {
                                    is ListItemContent.GoalItem -> itemContent.goal.text
                                    is ListItemContent.NoteItem -> itemContent.note.content
                                    is ListItemContent.SublistItem -> itemContent.sublist.name
                                }
                                textToSearch.contains(query, ignoreCase = true)
                            }
                        } else {
                            content
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSelectionModeActive: StateFlow<Boolean> = _uiState
        .map { it.selectedItemIds.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val obsidianVaultName: StateFlow<String> = settingsRepository.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState: StateFlow<GoalActionDialogState> = _goalActionDialogState.asStateFlow()

    fun onInputTextChanged(newValue: TextFieldValue) {
        _uiState.update { it.copy(inputValue = newValue) }
        if (_uiState.value.inputMode == InputMode.SearchInList) {
            _uiState.update { it.copy(localSearchQuery = newValue.text) }
        }
    }

    fun onInputModeSelected(mode: InputMode) {
        _uiState.update {
            it.copy(
                inputMode = mode,
                localSearchQuery = if(mode == InputMode.SearchInList) it.inputValue.text else ""
            )
        }
    }

    fun submitInput() {
        val textToSubmit = _uiState.value.inputValue.text
        if (textToSubmit.isBlank()) return

        viewModelScope.launch {
            val newItemId = when (_uiState.value.inputMode) {
                InputMode.AddGoal -> goalRepository.addGoalToList(textToSubmit, listIdFlow.value)
                InputMode.AddNote -> goalRepository.addNoteToList(textToSubmit, listIdFlow.value)
                InputMode.SearchInList -> {
                    // Логіка для пошуку, якщо це необхідно.
                    // Наразі нічого не повертаємо.
                    null
                }
                InputMode.SearchGlobal -> {
                    // Логіка для глобального пошуку.
                    // Наразі нічого не повертаємо.
                    null
                }
            }
            _uiState.update {
                it.copy(
                    inputValue = TextFieldValue(""),
                    newlyAddedItemId = newItemId ?: it.newlyAddedItemId
                )
            }
        }
    }

    fun onScrolledToNewItem() {
        _uiState.update { it.copy(newlyAddedItemId = null) }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val currentItems = listContent.value.toMutableList()
        if (fromIndex !in currentItems.indices || toIndex !in currentItems.indices) return

        val itemToMove = currentItems.removeAt(fromIndex)
        currentItems.add(toIndex, itemToMove)

        viewModelScope.launch(Dispatchers.IO) {
            val updatedItems = currentItems.mapIndexed { index, content ->
                content.item.copy(order = index.toLong())
            }
            goalRepository.updateListItemsOrder(updatedItems)
        }
    }

    fun deleteItem(item: ListItemContent) {
        viewModelScope.launch {
            goalRepository.deleteListItems(listOf(item.item.id))
            _uiEventFlow.send(UiEvent.ShowSnackbar("Element deleted", "Undo"))
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(completed = !goal.completed, updatedAt = System.currentTimeMillis()))
        }
    }

    fun onItemClick(item: ListItemContent) {
        if (isSelectionModeActive.value) {
            toggleSelection(item.item.id)
        } else {
            when (item) {
                is ListItemContent.GoalItem -> onEditGoal(item.goal)
                is ListItemContent.NoteItem -> { /* TODO: Navigate to Note Editor */ }
                is ListItemContent.SublistItem -> onNavigateToList(item.sublist.id)
            }
        }
    }

    fun onItemLongClick(itemId: String) {
        _uiState.update {
            it.copy(selectedItemIds = it.selectedItemIds + itemId)
        }
    }

    private fun toggleSelection(itemId: String) {
        _uiState.update {
            val currentSelection = it.selectedItemIds.toMutableSet()
            if (itemId in currentSelection) currentSelection.remove(itemId)
            else currentSelection.add(itemId)
            it.copy(selectedItemIds = currentSelection)
        }
    }

    fun selectAllItems() {
        _uiState.update {
            it.copy(selectedItemIds = listContent.value.map { content -> content.item.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val idsToDelete = _uiState.value.selectedItemIds
            if (idsToDelete.isEmpty()) return@launch
            goalRepository.deleteListItems(idsToDelete.toList())
            clearSelection()
            _uiEventFlow.send(UiEvent.ShowSnackbar("Deleted ${idsToDelete.size} items", "Undo"))
        }
    }

    fun toggleCompletionForSelectedGoals() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedItemIds
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = listContent.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }

            if (goalsToUpdate.isNotEmpty()) {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = !it.completed) }
                goalRepository.updateGoals(updatedGoals)
            }
            clearSelection()
        }
    }

    private fun onEditGoal(goal: Goal) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_edit_screen/${listIdFlow.value}/${goal.id}"))
        }
    }

    private fun onNavigateToList(listId: String) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_detail_screen/$listId"))
        }
    }

    fun onTagClicked(tag: String) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("global_search_screen/%23$tag"))
        }
    }

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()

    val recentLists: StateFlow<List<GoalList>> = goalRepository.getRecentLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onShowRecentLists() {
        _showRecentListsSheet.value = true
    }

    fun onDismissRecentLists() {
        _showRecentListsSheet.value = false
    }

    fun onRevealInExplorer(currentListId: String) {
        if (currentListId.isEmpty()) return
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.NavigateBackAndReveal(currentListId))
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.ShowSnackbar("Undo not implemented yet."))
        }
    }

    fun onSwipeStart(itemId: String) {
        if (_uiState.value.swipedItemId != itemId) {
            _uiState.update { it.copy(swipedItemId = itemId) }
        }
    }

    fun onGoalActionInitiated(item: ListItemContent) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(item)
    }

    fun onGoalActionSelected(actionType: GoalActionType, item: ListItemContent) {
        val goalItem = item as? ListItemContent.GoalItem ?: return
        _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
            sourceItemIds = setOf(item.item.id),
            sourceGoalIds = setOf(goalItem.goal.id),
            actionType = actionType
        )
    }

    fun onBulkActionRequest(actionType: GoalActionType) {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isNotEmpty()) {
            val sourceGoalIds = listContent.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { it.item.entityId }
                .toSet()

            _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
                sourceItemIds = selectedIds,
                sourceGoalIds = sourceGoalIds,
                actionType = actionType
            )
        }
    }
}