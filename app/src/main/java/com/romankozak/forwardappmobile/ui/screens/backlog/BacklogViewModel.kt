package com.romankozak.forwardappmobile.ui.screens.backlog

import android.util.Log // Переконайтесь, що цей імпорт додано
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

// ... (sealed classes UiEvent, GoalActionDialogState and enums are unchanged)
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val itemId: String) : UiEvent()
    data class ScrollTo(val index: Int) : UiEvent()
    data class NavigateBackAndReveal(val listId: String) : UiEvent()
    data class HandleLinkClick(val link: RelatedLink) : UiEvent()
}

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val itemContent: ListItemContent) : GoalActionDialogState()
}

enum class GoalActionType { CreateInstance, MoveInstance, CopyGoal, AddLinkToList, ADD_LIST_SHORTCUT }

data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedItemId: String? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedItemId: String? = null,
    val showAddWebLinkDialog: Boolean = false,
    val showAddObsidianLinkDialog: Boolean = false,
    val itemToHighlight: String? = null,
    val needsStateRefresh: Boolean = false
)


@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val ollamaService: OllamaService,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var batchedMoves = mutableListOf<Pair<ListItemContent, ListItemContent>>()
    private var batchSaveJob: Job? = null
    private val BATCH_DELAY_MS = 500L

    private val TAG = "DnD_Debug"

    private var optimisticUpdateInProgress = false

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")

    private val _uiState = MutableStateFlow(
        UiState(
            goalToHighlight = savedStateHandle.get<String>("goalId"),
            itemToHighlight = savedStateHandle.get<String>("itemIdToHighlight"),
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _refreshTrigger = MutableStateFlow(0)

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    private val _listContent = MutableStateFlow<List<ListItemContent>>(emptyList())

    val listContent: StateFlow<List<ListItemContent>> = _listContent.asStateFlow()

    val draggableItems: StateFlow<List<ListItemContent>> = _listContent.map { content ->
        content.filterNot { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachmentItems: StateFlow<List<ListItemContent>> = _listContent.map { content ->
        content.filter { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

    val goalList: StateFlow<GoalList?> = listIdFlow.flatMapLatest { id ->
        if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tagToContextNameMap: StateFlow<Map<String, String>> = contextHandler.tagToContextNameMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentListContextMarker: StateFlow<String?> = combine(
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

    private val databaseContentStream: Flow<List<ListItemContent>> =
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
                                    is ListItemContent.LinkItem -> itemContent.link.linkData.displayName ?: itemContent.link.linkData.target
                                }
                                textToSearch.contains(query, ignoreCase = true)
                            }
                        } else {
                            content
                        }
                    }
                }
            }

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
    private var pendingAction: GoalActionType? = null
    private var pendingSourceItemIds: Set<String> = emptySet()
    private var pendingSourceGoalIds: Set<String> = emptySet()

    private val _listVersion = MutableStateFlow(0)
    val listVersion: StateFlow<Int> = _listVersion.asStateFlow()


    init {
        Log.d(TAG, "ViewModel INIT")
        viewModelScope.launch {
            databaseContentStream.collect { dbContent ->
                Log.d(TAG, "DB collector received update. Flag is: $optimisticUpdateInProgress. List size: ${dbContent.size}")
                if (optimisticUpdateInProgress) {
                    Log.d(TAG, "-> SKIPPING DB update due to flag.")
                } else {
                    Log.d(TAG, "-> APPLYING DB update.")
                    _listContent.value = dbContent
                }
            }
        }

        viewModelScope.launch {
            listIdFlow.filter { it.isNotEmpty() }.collect { id ->
                goalRepository.logListAccess(id)
            }
        }
        viewModelScope.launch {
            contextHandler.initialize()
        }
    }

    // ... (інші функції залишаються без змін) ...
    fun onHighlightShown() {
        _uiState.update { it.copy(goalToHighlight = null, itemToHighlight = null) }
    }

    fun toggleAttachmentsVisibility() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = goalList.value ?: return@launch
            val newState = !currentList.isAttachmentsExpanded
            goalRepository.updateGoalList(currentList.copy(isAttachmentsExpanded = newState))
        }
    }

    fun onAddNewNoteRequested() {
        viewModelScope.launch(Dispatchers.IO) {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = "",
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            goalRepository.addNoteToList(newNote, listIdFlow.value)
            _uiEventFlow.send(UiEvent.Navigate("note_edit_screen/${listIdFlow.value}/${newNote.id}"))
        }
    }

    fun onListChooserResult(targetListId: String) {
        val actionType = pendingAction ?: return
        val itemIds = pendingSourceItemIds.toList()
        val goalIds = pendingSourceGoalIds.toList()

        viewModelScope.launch(Dispatchers.IO) {
            when (actionType) {
                GoalActionType.CreateInstance -> goalRepository.createGoalLinks(goalIds, targetListId)
                GoalActionType.MoveInstance -> goalRepository.moveListItems(itemIds, targetListId)
                GoalActionType.CopyGoal -> goalRepository.copyGoalsToList(goalIds, targetListId)
                GoalActionType.AddLinkToList -> {
                    val targetList = goalRepository.getGoalListById(targetListId)
                    val link = RelatedLink(
                        type = LinkType.GOAL_LIST,
                        target = targetListId,
                        displayName = targetList?.name ?: "Список без назви"
                    )
                    val newItemId = goalRepository.addLinkItemToList(listIdFlow.value, link)
                    _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                }
                GoalActionType.ADD_LIST_SHORTCUT -> {
                    val newItemId = goalRepository.addListLinkToList(targetListId, listIdFlow.value)
                    _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                }
            }
        }

        pendingAction = null
        pendingSourceItemIds = emptySet()
        pendingSourceGoalIds = emptySet()
        clearSelection()
    }
    private fun navigateToListChooser(title: String) {
        viewModelScope.launch {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val disabledIds = listIdFlow.value
            _uiEventFlow.send(UiEvent.Navigate("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds"))
        }
    }

    fun onGoalActionSelected(actionType: GoalActionType, item: ListItemContent) {
        val (itemIds, goalIds) = when (item) {
            is ListItemContent.GoalItem -> Pair(setOf(item.item.id), setOf(item.goal.id))
            is ListItemContent.NoteItem -> Pair(setOf(item.item.id), emptySet())
            is ListItemContent.LinkItem -> Pair(setOf(item.item.id), emptySet())
            is ListItemContent.SublistItem -> Pair(setOf(item.item.id), emptySet())
        }

        val isActionApplicable = when (actionType) {
            GoalActionType.MoveInstance -> true
            GoalActionType.CreateInstance, GoalActionType.CopyGoal -> item is ListItemContent.GoalItem
            else -> false
        }
        if (!isActionApplicable) return

        pendingAction = actionType
        pendingSourceItemIds = itemIds
        pendingSourceGoalIds = goalIds

        val title = when (actionType) {
            GoalActionType.CreateInstance -> "Створити посилання у..."
            GoalActionType.MoveInstance -> "Перемістити до..."
            GoalActionType.CopyGoal -> "Копіювати до..."
            else -> "Виберіть список"
        }
        onDismissGoalActionDialogs()
        navigateToListChooser(title)
    }

    fun onBulkActionRequest(actionType: GoalActionType) {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isNotEmpty()) {
            val sourceGoalIds = _listContent.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { it.item.entityId }
                .toSet()

            pendingAction = actionType
            pendingSourceItemIds = selectedIds
            pendingSourceGoalIds = sourceGoalIds

            val title = when (actionType) {
                GoalActionType.CreateInstance -> "Створити посилання у..."
                GoalActionType.MoveInstance -> "Перемістити до..."
                GoalActionType.CopyGoal -> "Копіювати до..."
                else -> "Виберіть список"
            }
            onDismissGoalActionDialogs()
            navigateToListChooser(title)
        }
    }

    fun onAddListLinkRequest() {
        pendingAction = GoalActionType.AddLinkToList
        pendingSourceItemIds = emptySet()
        pendingSourceGoalIds = emptySet()
        navigateToListChooser("Додати посилання на список...")
    }

    fun onAddListShortcutRequest() {
        pendingAction = GoalActionType.ADD_LIST_SHORTCUT
        pendingSourceItemIds = emptySet()
        pendingSourceGoalIds = emptySet()
        navigateToListChooser("Додати ярлик на список...")
    }

    fun onAddWebLinkConfirm(url: String, name: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val displayName = if (name.isNullOrBlank()) {
                try { URL(url).host } catch (_: Exception) { url }
            } else {
                name
            }
            val link = RelatedLink(type = LinkType.URL, target = url, displayName = displayName)
            val newItemId = goalRepository.addLinkItemToList(listIdFlow.value, link)
            _uiState.update { it.copy(newlyAddedItemId = newItemId) }
        }
        onDismissLinkDialogs()
    }

    fun onAddObsidianLinkConfirm(noteName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val link = RelatedLink(type = LinkType.OBSIDIAN, target = noteName, displayName = noteName)
            val newItemId = goalRepository.addLinkItemToList(listIdFlow.value, link)
            _uiState.update { it.copy(newlyAddedItemId = newItemId) }
        }
        onDismissLinkDialogs()
    }

    fun onShowAddWebLinkDialog() {
        _uiState.update { it.copy(showAddWebLinkDialog = true) }
    }

    fun onShowAddObsidianLinkDialog() {
        _uiState.update { it.copy(showAddObsidianLinkDialog = true) }
    }

    fun onDismissLinkDialogs() {
        _uiState.update { it.copy(showAddWebLinkDialog = false, showAddObsidianLinkDialog = false) }
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
        val textToSubmit = uiState.value.inputValue.text.trim()
        if (textToSubmit.isBlank()) return

        val currentListId = listIdFlow.value
        if (currentListId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val inputMode = uiState.value.inputMode

            val newItemId: String? = when (inputMode) {
                InputMode.AddGoal -> {
                    goalRepository.addGoalToList(textToSubmit, currentListId)
                }
                InputMode.AddNote -> {
                    val words = textToSubmit.split(Regex("\\s+"))
                    val isTooLongForTitle = textToSubmit.length > 60 || words.size > 5

                    if (isTooLongForTitle) {
                        val initialNote = Note(
                            id = UUID.randomUUID().toString(),
                            title = "Генерація заголовку...",
                            content = textToSubmit,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val generatedItemId = goalRepository.addNoteToList(initialNote, currentListId)

                        withContext(Dispatchers.Main) {
                            _refreshTrigger.value++
                        }

                        launch {
                            val baseUrl = settingsRepository.ollamaUrlFlow.first()
                            val fastModel = settingsRepository.ollamaFastModelFlow.first()

                            if (baseUrl.isNotBlank() && fastModel.isNotBlank()) {
                                val result = ollamaService.generateTitle(baseUrl, fastModel, textToSubmit)
                                val finalTitle = result.getOrElse {
                                    textToSubmit.split(Regex("\\s+")).take(5).joinToString(" ") + "..."
                                }
                                val updatedNote = initialNote.copy(
                                    title = finalTitle,
                                    updatedAt = System.currentTimeMillis()
                                )
                                goalRepository.updateNote(updatedNote)
                            } else {
                                val fallbackTitle = textToSubmit.split(Regex("\\s+")).take(5).joinToString(" ") + "..."
                                val updatedNote = initialNote.copy(
                                    title = fallbackTitle,
                                    updatedAt = System.currentTimeMillis()
                                )
                                goalRepository.updateNote(updatedNote)
                            }
                        }
                        generatedItemId
                    } else {
                        val newNote = Note(
                            id = UUID.randomUUID().toString(),
                            title = textToSubmit,
                            content = "",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        goalRepository.addNoteToList(newNote, currentListId)
                    }
                }
                InputMode.SearchInList -> null
                InputMode.SearchGlobal -> {
                    _uiEventFlow.send(UiEvent.Navigate("global_search_screen/$textToSubmit"))
                    null
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        inputValue = TextFieldValue(""),
                        newlyAddedItemId = newItemId
                    )
                }
                if (inputMode != InputMode.AddNote || !(uiState.value.inputValue.text.length > 60 || uiState.value.inputValue.text.split(Regex("\\s+")).size > 5)) {
                    _refreshTrigger.value++
                }
            }
        }
    }

    fun onScrolledToNewItem() { _uiState.update { it.copy(newlyAddedItemId = null) } }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
        _uiState.update { it.copy(swipedItemId = null) }
        if (!isSelectionModeActive.value) {
            clearSelection()
        }
    }

    // ...
    fun moveItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _listContent.value.toMutableList()
            println("moveItem: from=$fromIndex, to=$toIndex, listSize=${currentList.size}")

            if (fromIndex !in currentList.indices || fromIndex == toIndex) {
                println("✗ Move rejected: conditions not met")
                return@launch
            }

            // Видаляємо елемент з початкової позиції
            val itemToMove = currentList.removeAt(fromIndex)

            // Обчислюємо правильний індекс для вставки
            val insertIndex = when {
                // Якщо toIndex == currentList.size (після видалення), вставляємо в кінець
                toIndex >= currentList.size -> currentList.size
                // Якщо рухаємося вниз, коректуємо індекс (бо елемент вже видалений)
                toIndex > fromIndex -> toIndex - 1
                // Якщо рухаємося вгору або залишається на місці, індекс не змінюється
                else -> toIndex
            }.coerceIn(0, currentList.size)

            println("Inserting at index: $insertIndex (list size after removal: ${currentList.size})")

            currentList.add(insertIndex, itemToMove)
            _listContent.value = currentList
            _listVersion.value++
            _uiState.update { it.copy(needsStateRefresh = true) }

            // Batch operations
            val targetItem = currentList.getOrNull(insertIndex) ?: itemToMove
            batchedMoves.add(itemToMove to targetItem)
            batchSaveJob?.cancel()
            batchSaveJob = launch {
                delay(BATCH_DELAY_MS)
                saveBatchedMoves()
            }

            println("✓ Move completed: item now at position $insertIndex")
        }
    }


    fun onStateRefreshed() {
        _uiState.update { it.copy(needsStateRefresh = false) }
    }

    fun deleteItem(item: ListItemContent) {
        viewModelScope.launch {
            recentlyDeletedItems = listOf(item)
            goalRepository.deleteListItems(listOf(item.item.id))
            _uiEventFlow.send(UiEvent.ShowSnackbar("Елемент видалено", "Скасувати"))
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
            val itemsToSelect = draggableItems.value
                .map { it.item.id }
                .toSet()
            it.copy(selectedItemIds = itemsToSelect)
        }
    }

    fun clearSelection() { _uiState.update { it.copy(selectedItemIds = emptySet()) } }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val idsToDelete = _uiState.value.selectedItemIds
            if (idsToDelete.isEmpty()) return@launch
            recentlyDeletedItems = _listContent.value.filter { it.item.id in idsToDelete }
            goalRepository.deleteListItems(idsToDelete.toList())
            clearSelection()
            _uiEventFlow.send(UiEvent.ShowSnackbar("Видалено елементів: ${idsToDelete.size}", "Скасувати"))
        }
    }

    fun toggleCompletionForSelectedGoals() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedItemIds
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = _listContent.value
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
        viewModelScope.launch {
            val encodedTag = URLEncoder.encode(tag, "UTF-8")
            _uiEventFlow.send(UiEvent.Navigate("global_search_screen/$encodedTag"))
        }
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

    fun onItemClick(item: ListItemContent) {
        if (isSelectionModeActive.value) {
            toggleSelection(item.item.id)
        } else {
            when (item) {
                is ListItemContent.GoalItem -> onEditGoal(item.goal)
                is ListItemContent.NoteItem -> onEditNote(item.note)
                is ListItemContent.SublistItem -> onNavigateToList(item.sublist.id)
                is ListItemContent.LinkItem -> onLinkItemClick(item.link.linkData)
            }
        }
    }

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

    fun forceRefresh() {
        viewModelScope.launch {
            _refreshTrigger.value++
        }
    }

    fun onExistingItemSelected(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            goalRepository.createGoalLinks(listOf(goalId), listIdFlow.value)
        }
    }

    fun onSwipeStateReset(itemId: String) {
        _uiState.update { currentState ->
            val newTriggers = currentState.resetTriggers.toMutableMap()
            newTriggers[itemId] = (newTriggers[itemId] ?: 0) + 1
            currentState.copy(
                resetTriggers = newTriggers,
                swipedItemId = null
            )
        }
    }

    fun onLinkItemClick(link: RelatedLink) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.HandleLinkClick(link))
        }
    }

    private suspend fun saveBatchedMoves() = withContext(Dispatchers.IO) {
        if (batchedMoves.isEmpty()) return@withContext

        try {
            Log.d("DnD_Batch", "Saving ${batchedMoves.size} batched moves")

            val currentList = _listContent.value

            val updatedItems = currentList.mapIndexed { index, content ->
                content.item.copy(order = index.toLong())
            }

            goalRepository.updateListItemsOrder(updatedItems)

            Log.d("DnD_Batch", "Successfully saved batch of ${batchedMoves.size} moves")

        } catch (e: Exception) {
            Log.e("DnD_Batch", "Error saving batched moves", e)
        } finally {
            batchedMoves.clear()
        }
    }

    fun flushPendingMoves() {
        viewModelScope.launch {
            batchSaveJob?.cancel()
            saveBatchedMoves()
        }
    }

    override fun onCleared() {
        super.onCleared()
        batchSaveJob?.cancel()
        if (batchedMoves.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                saveBatchedMoves()
            }
        }
    }

    fun copyContentRequest(content: ListItemContent) {
        Log.d("swipeActions", "copy content")
        viewModelScope.launch {
            try {
                when (content) {
                    is ListItemContent.GoalItem -> {
                        val text = content.goal.text
                        _uiEventFlow.send(UiEvent.ShowSnackbar("Текст скопійовано: ${text.take(50)}..."))
                    }
                    is ListItemContent.NoteItem -> {
                        val text = "${content.note.title}\n\n${content.note.content}"
                        _uiEventFlow.send(UiEvent.ShowSnackbar("Нотатка скопійована: ${content.note.title}"))
                    }
                    is ListItemContent.LinkItem -> {
                        val text = "${content.link.linkData.displayName ?: content.link.linkData.target}"
                        _uiEventFlow.send(UiEvent.ShowSnackbar("Посилання скопійовано"))
                    }
                    is ListItemContent.SublistItem -> {
                        val text = content.sublist.name
                        _uiEventFlow.send(UiEvent.ShowSnackbar("Назва списку скопійована"))
                    }
                }
            } catch (e: Exception) {
                Log.e("GoalDetailViewModel", "Error copying content", e)
                _uiEventFlow.send(UiEvent.ShowSnackbar("Помилка копіювання"))
            }
        }
    }

    fun moveInstanceRequest(content: ListItemContent) {
        Log.d("swipeActions", "move instance request")
        val itemIds = setOf(content.item.id)
        val goalIds = if (content is ListItemContent.GoalItem) setOf(content.goal.id) else emptySet()

        pendingAction = GoalActionType.MoveInstance
        pendingSourceItemIds = itemIds
        pendingSourceGoalIds = goalIds

        navigateToListChooser("Перемістити до...")
    }

    fun copyGoalRequest(content: ListItemContent) {
        Log.d("swipeActions", "copy goal request")

        if (content !is ListItemContent.GoalItem) {
            viewModelScope.launch {
                _uiEventFlow.send(UiEvent.ShowSnackbar("Копіювання доступне тільки для цілей"))
            }
            return
        }

        val goalIds = setOf(content.goal.id)

        pendingAction = GoalActionType.CopyGoal
        pendingSourceItemIds = emptySet()
        pendingSourceGoalIds = goalIds

        navigateToListChooser("Копіювати до...")
    }

    fun createInstanceRequest(content: ListItemContent) {
        Log.d("swipeActions", "create instance request")

        if (content !is ListItemContent.GoalItem) {
            viewModelScope.launch {
                _uiEventFlow.send(UiEvent.ShowSnackbar("Створення посилання доступне тільки для цілей"))
            }
            return
        }

        val goalIds = setOf(content.goal.id)

        pendingAction = GoalActionType.CreateInstance
        pendingSourceItemIds = emptySet()
        pendingSourceGoalIds = goalIds

        navigateToListChooser("Створити посилання у...")
    }
}