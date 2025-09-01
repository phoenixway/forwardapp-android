package com.romankozak.forwardappmobile.ui.screens.backlog

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.screens.backlog.components.attachments.AttachmentType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.InputHandler
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.ItemActionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

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
    val needsStateRefresh: Boolean = false,
    val showRecentListsSheet: Boolean = false
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val ollamaService: OllamaService,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ItemActionHandler.ResultListener, InputHandler.ResultListener {

    companion object {
        const val HANDLE_LINK_CLICK_ROUTE = "handle_link_click"
    }

    private val TAG = "DND_DEBUG"
    private var batchSaveJob: Job? = null
    private val BATCH_DELAY_MS = 500L

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")

    // --- Handlers ---
    val itemActionHandler = ItemActionHandler(goalRepository, viewModelScope, listIdFlow, this)
    val inputHandler = InputHandler(goalRepository, settingsRepository, ollamaService, viewModelScope, listIdFlow, this)

    // --- State Flows ---
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

    val recentLists: StateFlow<List<GoalList>> = goalRepository.getRecentLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextHandler.contextMarkerToEmojiMap

    val goalList: StateFlow<GoalList?> = combine(listIdFlow, _refreshTrigger) { id, _ -> id }
        .flatMapLatest { id -> if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tagToContextNameMap: StateFlow<Map<String, String>> = contextHandler.tagToContextNameMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentListContextMarker: StateFlow<String?> = combine(
        goalList,
        tagToContextNameMap
    ) { list, tagMap ->
        val listTags = list?.tags ?: emptyList()
        if (listTags.isEmpty() || tagMap.isEmpty()) return@combine null
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
        combine(listIdFlow, _uiState.map { it.localSearchQuery }.distinctUntilChanged(), _refreshTrigger) { id, query, _ -> Pair(id, query) }
            .flatMapLatest { (id, query) ->
                if (id.isEmpty()) flowOf(emptyList())
                else goalRepository.getListContentStream(id).map { content ->
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
                    } else content
                }
            }

    val isSelectionModeActive: StateFlow<Boolean> = _uiState
        .map { it.selectedItemIds.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val obsidianVaultName: StateFlow<String> = settingsRepository.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // --- Pending Action State ---
    private var pendingAction: GoalActionType? = null
    private var pendingSourceItemIds: Set<String> = emptySet()
    private var pendingSourceGoalIds: Set<String> = emptySet()

    init {
        viewModelScope.launch { databaseContentStream.collect { dbContent -> _listContent.value = dbContent } }
        viewModelScope.launch { listIdFlow.filter { it.isNotEmpty() }.collect { id -> goalRepository.logListAccess(id) } }
        viewModelScope.launch { contextHandler.initialize() }
    }

    // --- ItemActionHandler.ResultListener Implementation ---
    override fun requestNavigation(route: String) {
        viewModelScope.launch {
            if (route.startsWith(HANDLE_LINK_CLICK_ROUTE)) {
                val target = route.substringAfter(HANDLE_LINK_CLICK_ROUTE + "/")
                val link = listContent.value
                    .filterIsInstance<ListItemContent.LinkItem>()
                    .map { it.link.linkData }
                    .find { it.target == target }

                if (link != null) {
                    _uiEventFlow.send(UiEvent.HandleLinkClick(link))
                }
            } else {
                _uiEventFlow.send(UiEvent.Navigate(route))
            }
        }
    }

    override fun showSnackbar(message: String, action: String?) {
        viewModelScope.launch { _uiEventFlow.send(UiEvent.ShowSnackbar(message, action)) }
    }

    override fun forceRefresh() {
        viewModelScope.launch { _refreshTrigger.value++ }
    }

    override fun isSelectionModeActive(): Boolean = isSelectionModeActive.value

    override fun toggleSelection(itemId: String) {
        _uiState.update {
            val currentSelection = it.selectedItemIds.toMutableSet()
            if (itemId in currentSelection) currentSelection.remove(itemId)
            else currentSelection.add(itemId)
            it.copy(selectedItemIds = currentSelection)
        }
    }

    override fun setPendingAction(actionType: GoalActionType, itemIds: Set<String>, goalIds: Set<String>) {
        pendingAction = actionType
        pendingSourceItemIds = itemIds
        pendingSourceGoalIds = goalIds
        val title = when (actionType) {
            GoalActionType.CreateInstance -> "Створити посилання у..."
            GoalActionType.MoveInstance -> "Перемістити до..."
            GoalActionType.CopyGoal -> "Копіювати до..."
            GoalActionType.AddLinkToList -> "Додати посилання на список..."
            GoalActionType.ADD_LIST_SHORTCUT -> "Додати ярлик на список..."
        }
        navigateToListChooser(title)
    }

    // --- InputHandler.ResultListener Implementation ---
    override fun updateInputState(
        inputValue: TextFieldValue?,
        inputMode: InputMode?,
        localSearchQuery: String?,
        newlyAddedItemId: String?
    ) {
        _uiState.update {
            it.copy(
                inputValue = inputValue ?: it.inputValue,
                inputMode = inputMode ?: it.inputMode,
                localSearchQuery = localSearchQuery ?: it.localSearchQuery,
                newlyAddedItemId = newlyAddedItemId
            )
        }
    }

    override fun updateDialogState(showAddWebLinkDialog: Boolean?, showAddObsidianLinkDialog: Boolean?) {
        _uiState.update {
            it.copy(
                showAddWebLinkDialog = showAddWebLinkDialog ?: it.showAddWebLinkDialog,
                showAddObsidianLinkDialog = showAddObsidianLinkDialog ?: it.showAddObsidianLinkDialog
            )
        }
    }

    override fun showRecentListsSheet(show: Boolean) {
        _uiState.update { it.copy(showRecentListsSheet = show) }
    }

    // --- Logic Remaining in ViewModel ---

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
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                    }
                }
                GoalActionType.ADD_LIST_SHORTCUT -> {
                    val newItemId = goalRepository.addListLinkToList(targetListId, listIdFlow.value)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(newlyAddedItemId = newItemId) }
                    }
                }
            }
            withContext(Dispatchers.Main) { forceRefresh() }
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

    fun onBulkActionRequest(actionType: GoalActionType) {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isNotEmpty()) {
            val sourceGoalIds = _listContent.value
                .filter { it.item.id in selectedIds && it is ListItemContent.GoalItem }
                .map { it.item.entityId }
                .toSet()
            setPendingAction(actionType, selectedIds, sourceGoalIds)
        }
    }

    fun onHighlightShown() {
        _uiState.update { it.copy(goalToHighlight = null, itemToHighlight = null) }
    }

    fun onScrolledToNewItem() {
        _uiState.update { it.copy(newlyAddedItemId = null) }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val currentContent = _listContent.value
        val draggableItems = currentContent.filterNot { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }.toMutableList()
        if (fromIndex !in draggableItems.indices || toIndex !in draggableItems.indices) return
        if (fromIndex == toIndex) return
        val movedItem = draggableItems.removeAt(fromIndex)
        draggableItems.add(toIndex, movedItem)
        val newFullList = mutableListOf<ListItemContent>()
        val reorderedDraggablesIterator = draggableItems.iterator()
        currentContent.forEach { originalItem ->
            if (originalItem is ListItemContent.NoteItem || originalItem is ListItemContent.LinkItem) newFullList.add(originalItem)
            else if (reorderedDraggablesIterator.hasNext()) newFullList.add(reorderedDraggablesIterator.next())
        }
        _listContent.value = newFullList
        viewModelScope.launch { saveListOrder(newFullList) }
    }

    private suspend fun saveListOrder(listToSave: List<ListItemContent>) = withContext(Dispatchers.IO) {
        try {
            val updatedItems = listToSave.mapIndexed { index, content -> content.item.copy(order = index.toLong()) }
            goalRepository.updateListItemsOrder(updatedItems)
        } catch (e: Exception) {
            Log.e(TAG, "[saveListOrder] Failed to save list order", e)
        }
    }

    fun onStateRefreshed() {
        _uiState.update { it.copy(needsStateRefresh = false) }
    }

    fun selectAllItems() {
        _uiState.update {
            val itemsToSelect = _listContent.value
                .filterNot { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
                .map { it.item.id }
                .toSet()
            it.copy(selectedItemIds = itemsToSelect)
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
            showSnackbar("Видалено елементів: ${idsToDelete.size}", "Скасувати")
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
                val areAllCompleted = goalsToUpdate.all { it.completed }
                val targetState = !areAllCompleted
                val updatedGoals = goalsToUpdate.map { it.copy(completed = targetState, updatedAt = System.currentTimeMillis()) }
                goalRepository.updateGoals(updatedGoals)
            }
            clearSelection()
            forceRefresh()
        }
    }

    fun onTagClicked(tag: String) {
        viewModelScope.launch {
            val encodedTag = URLEncoder.encode(tag, "UTF-8")
            requestNavigation("global_search_screen/$encodedTag")
        }
    }

    fun onRevealInExplorer(currentListId: String) {
        if (currentListId.isEmpty()) return
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.NavigateBackAndReveal(currentListId))
        }
    }

    fun onSwipeStart(itemId: String) {
        if (_uiState.value.swipedItemId != itemId) {
            _uiState.update { it.copy(swipedItemId = itemId) }
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

    fun flushPendingMoves() {
        batchSaveJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        batchSaveJob?.cancel()
    }

    fun toggleAttachmentsVisibility() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = goalList.value ?: return@launch
            val newState = !currentList.isAttachmentsExpanded
            goalRepository.updateGoalList(currentList.copy(isAttachmentsExpanded = newState))
        }
    }

    fun onAddAttachment(type: AttachmentType) {
        when (type) {
            AttachmentType.NOTE -> itemActionHandler.scope.launch { /* Handled by InputHandler now */ } // Consider removing or refactoring
            AttachmentType.WEB_LINK -> inputHandler.onShowAddWebLinkDialog()
            AttachmentType.OBSIDIAN_LINK -> inputHandler.onShowAddObsidianLinkDialog()
            AttachmentType.LIST_LINK -> inputHandler.onAddListLinkRequest()
            AttachmentType.SHORTCUT -> inputHandler.onAddListShortcutRequest()
        }
    }
}