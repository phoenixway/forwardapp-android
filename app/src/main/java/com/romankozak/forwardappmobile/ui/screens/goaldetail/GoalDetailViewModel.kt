// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/GoalDetailViewModel.kt ---
package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.util.Log
import android.util.Log.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.ui.utils.HierarchyFilter
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import kotlinx.coroutines.delay

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
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")

    private val _uiState = MutableStateFlow(
        UiState(
            goalToHighlight = savedStateHandle.get<String>("goalToHighlight")
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _refreshTrigger = MutableStateFlow(0)

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

    val goalList: StateFlow<GoalList?> = listIdFlow.flatMapLatest { id ->
        if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val tagToContextNameMap: StateFlow<Map<String, String>> = contextHandler.tagToContextNameMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val currentListContextMarker: StateFlow<String?> = combine(
        goalList,
        tagToContextNameMap
    ) { list, tagMap ->
        val listTags = list?.tags ?: emptyList()
        if (listTags.isEmpty() || tagMap.isEmpty()) {
            return@combine null
        }
        val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in listTags }?.value
        contextName?.let { contextHandler.getContextMarker(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentListContextEmojiToHide: StateFlow<String?> = combine(
        currentListContextMarker,
        contextMarkerToEmojiMap
    ) { marker, emojiMap ->
        marker?.let { emojiMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val rawListContent: Flow<List<ListItemContent>> =
        combine(
            listIdFlow,
            _uiState.map { it.localSearchQuery }.distinctUntilChanged(),
            _refreshTrigger
        ) { id, query, _ -> Pair(id, query) }
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
            }

    private val allListsMap: Flow<Map<String, String>> = goalRepository.getAllGoalListsFlow()
        .map { lists -> lists.associate { it.id to it.name } }
        .flowOn(Dispatchers.Default)

    val listContent: StateFlow<List<ListItemContent>> = combine(
        rawListContent,
        allListsMap
    ) { content, listMap ->
        content.map { listItem ->
            if (listItem is ListItemContent.GoalItem) {
                val enrichedLinks = listItem.goal.relatedLinks?.map { link ->
                    if (link.type == LinkType.GOAL_LIST) {
                        link.copy(displayName = listMap[link.target] ?: link.target)
                    } else {
                        link
                    }
                }
                listItem.copy(
                    goal = listItem.goal.copy(relatedLinks = enrichedLinks)
                )
            } else {
                listItem
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

    private val _showRecentListsSheet = MutableStateFlow(false)
    val showRecentListsSheet: StateFlow<Boolean> = _showRecentListsSheet.asStateFlow()

    val recentLists: StateFlow<List<GoalList>> = goalRepository.getRecentLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var recentlyDeletedItems: List<ListItemContent>? = null

    private val _listChooserFilterText = MutableStateFlow("")
    val listChooserFilterText = _listChooserFilterText.asStateFlow()

    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val listChooserUserExpandedIds = _listChooserUserExpandedIds.asStateFlow()

    private val fullListHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevelLists = allLists.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevelLists, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData(emptyList(), emptyList(), emptyMap()))

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> =
        combine(listChooserFilterText, fullListHierarchy) { filter, originalHierarchy ->
            // --- ПОЧАТОК ЗМІН: Створюємо максимально глибоку копію ---
            val copiedAllLists = originalHierarchy.allLists.map { it.copy() }
            val safeHierarchy = ListHierarchyData(
                allLists = copiedAllLists,
                topLevelLists = copiedAllLists.filter { it.parentId == null }.sortedBy { it.order },
                childMap = copiedAllLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            )
            HierarchyFilter.filter(safeHierarchy, filter)
            // --- КІНЕЦЬ ЗМІН ---
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    fun onListChooserFilterChanged(text: String) {
        Log.d("FilterDebug", "[GoalDetailVM] Filter text changed to: '$text'") // --- ДОДАНО ЛОГУВАННЯ ---
        _listChooserFilterText.value = text
    }


    val listChooserFinalExpandedIds: StateFlow<Set<String>> = combine(
        listChooserFilterText,
        filteredListHierarchyForDialog,
        listChooserUserExpandedIds
    ) { filter, filteredHierarchy, userExpandedIds ->
        if (filter.isBlank()) {
            userExpandedIds
        } else {
            val allVisibleIds = filteredHierarchy.topLevelLists.map { it.id }.toSet() +
                    filteredHierarchy.childMap.keys
            userExpandedIds + allVisibleIds
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            listIdFlow.filter { it.isNotEmpty() }.collect { id ->
                goalRepository.logListAccess(id)
            }
        }
        viewModelScope.launch {
            contextHandler.initialize()
        }
    }

    fun onListChooserToggleExpanded(listId: String) {
        _listChooserUserExpandedIds.value = _listChooserUserExpandedIds.value.toMutableSet().apply {
            if (listId in this) remove(listId) else add(listId)
        }
    }

    fun addNewList(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            goalRepository.createGoalListWithId(id, name, parentId)
        }
    }

    fun confirmGoalAction(targetListId: String) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingListChoice) {
            val itemIds = currentState.sourceItemIds.toList()
            val goalIds = currentState.sourceGoalIds.toList()
            val actionType = currentState.actionType

            viewModelScope.launch(Dispatchers.IO) {
                when (actionType) {
                    GoalActionType.CreateInstance -> goalRepository.createGoalLinks(goalIds, targetListId)
                    // Дія MoveInstance використовує itemIds, тому працюватиме і для нотаток
                    GoalActionType.MoveInstance -> goalRepository.moveListItems(itemIds, targetListId)
                    GoalActionType.CopyGoal -> goalRepository.copyGoalsToList(goalIds, targetListId)
                    else -> {}
                }
                launch(Dispatchers.Main) { onDismissGoalActionDialogs() }
            }
        }
    }

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
                localSearchQuery = if (mode == InputMode.SearchInList) it.inputValue.text else ""
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
                InputMode.SearchInList -> null
                InputMode.SearchGlobal -> {
                    _uiEventFlow.send(UiEvent.Navigate("global_search_screen/$textToSubmit"))
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

    fun onScrolledToNewItem() { _uiState.update { it.copy(newlyAddedItemId = null) } }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
        _uiState.update { it.copy(swipedItemId = null, selectedItemIds = emptySet()) }
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
            recentlyDeletedItems = listOf(item)
            goalRepository.deleteListItems(listOf(item.item.id))
            _uiEventFlow.send(UiEvent.ShowSnackbar("Елемент видалено", "Скасувати"))
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(completed = !goal.completed, updatedAt = System.currentTimeMillis()))
        }
    }

    fun onItemLongClick(itemId: String) {
        _uiState.update { it.copy(selectedItemIds = it.selectedItemIds + itemId) }
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

    fun clearSelection() { _uiState.update { it.copy(selectedItemIds = emptySet()) } }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val idsToDelete = _uiState.value.selectedItemIds
            if (idsToDelete.isEmpty()) return@launch
            recentlyDeletedItems = listContent.value.filter { it.item.id in idsToDelete }
            goalRepository.deleteListItems(idsToDelete.toList())
            clearSelection()
            _uiEventFlow.send(UiEvent.ShowSnackbar("Видалено елементів: ${idsToDelete.size}", "Скасувати"))
        }
    }

    fun toggleCompletionForSelectedGoals() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedItemIds
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = listContent.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { (it as ListItemContent.GoalItem).goal }
                .distinctBy { it.id }

            if (goalsToUpdate.isNotEmpty()) {
                val updatedGoals = goalsToUpdate.map { it.copy(completed = !it.completed) }
                goalRepository.updateGoals(updatedGoals)
            }
            clearSelection()
        }
    }

    fun onTagClicked(tag: String) {
        viewModelScope.launch { _uiEventFlow.send(UiEvent.Navigate("global_search_screen/%23$tag")) }
    }

    fun onShowRecentLists() { _showRecentListsSheet.value = true }
    fun onDismissRecentLists() { _showRecentListsSheet.value = false }
    fun onRecentListSelected(listId: String) {
        onNavigateToList(listId)
        onDismissRecentLists()
    }

    fun onRevealInExplorer(currentListId: String) {
        if (currentListId.isEmpty()) return
        viewModelScope.launch { _uiEventFlow.send(UiEvent.NavigateBackAndReveal(currentListId)) }
    }

    fun undoDelete() {
        viewModelScope.launch {
            // TODO: Implement proper undo logic.
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

    // --- ПОЧАТОК ЗМІН: Оновлено логіку для підтримки нотаток ---
    fun onGoalActionSelected(actionType: GoalActionType, item: ListItemContent) {
        when (item) {
            is ListItemContent.GoalItem -> {
                // Стандартна логіка для цілей
                _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
                    sourceItemIds = setOf(item.item.id),
                    sourceGoalIds = setOf(item.goal.id),
                    actionType = actionType
                )
            }
            is ListItemContent.NoteItem -> {
                // Дія "Перемістити" є універсальною, оскільки працює з ID елемента списку (ListItem).
                // Інші дії (копіювання, створення посилання) є специфічними для цілей.
                if (actionType == GoalActionType.MoveInstance) {
                    _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
                        sourceItemIds = setOf(item.item.id),
                        sourceGoalIds = emptySet(), // Для переміщення ID самої сутності не потрібен
                        actionType = actionType
                    )
                }
                // Наразі інші дії для нотаток не обробляються, але їх можна додати тут у майбутньому.
            }
            is ListItemContent.SublistItem -> {
                // Дії для підсписків не застосовуються
            }
        }
    }
    // --- КІНЕЦЬ ЗМІН ---

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

    fun toggleGoalCompletedWithState(goal: Goal, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                val updatedGoal = goal.copy(completed = isChecked, updatedAt = System.currentTimeMillis())
                goalRepository.updateGoal(updatedGoal)
                delay(100)
                _refreshTrigger.value++
            } catch (e: Exception) { Log.e("GoalDetailViewModel", "Error updating goal", e) }
        }
    }

    fun resetSwipe(itemId: String) {
        viewModelScope.launch {
            _uiState.update {
                val currentTriggers = it.resetTriggers
                val newTriggerValue = currentTriggers.getOrDefault(itemId, 0) + 1
                it.copy(
                    resetTriggers = currentTriggers + (itemId to newTriggerValue),
                    swipedItemId = null
                )
            }
        }
    }

    fun onItemClick(item: ListItemContent) {
        if (isSelectionModeActive.value) {
            toggleSelection(item.item.id)
        } else {
            when (item) {
                is ListItemContent.GoalItem -> onEditGoal(item.goal)
                is ListItemContent.NoteItem -> onEditNote(item.note)
                is ListItemContent.SublistItem -> onNavigateToList(item.sublist.id)
            }
        }
    }

    // File: GoalDetailViewModel.kt

    private fun onEditGoal(goal: Goal) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_edit_screen/${listIdFlow.value}?goalId=${goal.id}"))
        }
    }
    private fun onEditNote(note: Note) {
        viewModelScope.launch { _uiEventFlow.send(UiEvent.Navigate("note_edit_screen/${listIdFlow.value}/${note.id}")) }
    }

    private fun onNavigateToList(listId: String) {
        viewModelScope.launch { _uiEventFlow.send(UiEvent.Navigate("goal_detail_screen/$listId")) }
    }
}