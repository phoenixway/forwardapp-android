package com.romankozak.forwardappmobile.ui.screens.backlog

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.screens.backlog.components.attachments.AttachmentType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.InboxHandler
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.InboxMarkdownHandler
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.InputHandler
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.ItemActionHandler
import com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel.SelectionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

// ... (sealed class, enum, data class залишаються без змін, оскільки ми перенесли ProjectViewMode в DatabaseModel.kt) ...
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
    val currentView: ProjectViewMode = ProjectViewMode.BACKLOG,
    val showRecentListsSheet: Boolean = false,
    val showImportFromMarkdownDialog: Boolean = false,
    val showImportBacklogFromMarkdownDialog: Boolean = false
)

interface BacklogMarkdownHandlerResultListener {
    fun copyToClipboard(text: String, label: String)
    fun showSnackbar(message: String, action: String?)
    fun forceRefresh()
}

class BacklogMarkdownHandler @Inject constructor(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listener: BacklogMarkdownHandlerResultListener
) {
    fun exportToMarkdown(content: List<ListItemContent>) {
        if (content.isEmpty()) {
            listener.showSnackbar("Беклог порожній. Нічого експортувати.", null)
            return
        }

        val markdownBuilder = StringBuilder()
        content.forEach { item ->
            val line = when (item) {
                is ListItemContent.GoalItem -> {
                    val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                    "$checkbox ${item.goal.text}"
                }
                is ListItemContent.SublistItem -> {
                    "- [С] ${item.sublist.name}"
                }
                is ListItemContent.LinkItem -> {
                    val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                    "- [Л] [$displayName](${item.link.linkData.target})"
                }
            }
            markdownBuilder.appendLine(line)
        }
        val markdownText = markdownBuilder.toString()
        listener.copyToClipboard(markdownText, "Backlog Export")
        listener.showSnackbar("Беклог скопійовано у буфер обміну.", null)
    }

    fun importFromMarkdown(markdownText: String, listId: String) {
        if (markdownText.isBlank()) {
            listener.showSnackbar("Нічого імпортувати.", null)
            return
        }

        scope.launch(Dispatchers.IO) {
            val lines = markdownText.lines().filter { it.isNotBlank() }
            var importedCount = 0
            for (line in lines) {
                try {
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.startsWith("- [ ]") -> {
                            val goalText = trimmedLine.removePrefix("- [ ]").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToList(goalText, listId, completed = false)
                                importedCount++
                            }
                        }
                        trimmedLine.startsWith("- [x]") -> {
                            val goalText = trimmedLine.removePrefix("- [x]").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToList(goalText, listId, completed = true)
                                importedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BacklogMarkdownHandler", "Failed to import line: $line", e)
                }
            }
            withContext(Dispatchers.Main) {
                listener.showSnackbar("Імпортовано $importedCount елементів.", null)
                listener.forceRefresh()
            }
        }
    }
}


@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val application: Application,

    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val ollamaService: OllamaService,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle,

    ) : ViewModel(), ItemActionHandler.ResultListener, InputHandler.ResultListener,
    SelectionHandler.ResultListener, InboxHandler.ResultListener, InboxMarkdownHandler.ResultListener, BacklogMarkdownHandlerResultListener {

    companion object {
        const val HANDLE_LINK_CLICK_ROUTE = "handle_link_click"
    }

    private val TAG = "DND_DEBUG"
    private var batchSaveJob: Job? = null
    private val BATCH_DELAY_MS = 500L

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")
    private val _listContent = MutableStateFlow<List<ListItemContent>>(emptyList())
    val listContent: StateFlow<List<ListItemContent>> = _listContent.asStateFlow()

    // --- Handlers ---
    val itemActionHandler = ItemActionHandler(goalRepository, viewModelScope, listIdFlow, this)
    val inputHandler = InputHandler(
        goalRepository,
        viewModelScope,
        listIdFlow,
        this
    )
    val selectionHandler = SelectionHandler(goalRepository, viewModelScope, _listContent, this)
    val inboxHandler =
        InboxHandler(goalRepository, viewModelScope, listIdFlow, this)
    val inboxMarkdownHandler = InboxMarkdownHandler(goalRepository, viewModelScope, this)
    val backlogMarkdownHandler = BacklogMarkdownHandler(goalRepository, viewModelScope, this)


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

    val recentLists: StateFlow<List<GoalList>> = goalRepository.getRecentLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contextMarkerToEmojiMap: StateFlow<Map<String, String>> =
        contextHandler.contextMarkerToEmojiMap

    val goalList: StateFlow<GoalList?> = combine(listIdFlow, _refreshTrigger) { id, _ -> id }
        .flatMapLatest { id ->
            if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(
                null
            )
        }
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
        combine(
            listIdFlow,
            _uiState.map { it.localSearchQuery }.distinctUntilChanged(),
            _refreshTrigger
        ) { id, query, _ -> Pair(id, query) }
            .flatMapLatest { (id, query) ->
                if (id.isEmpty()) flowOf(emptyList())
                else goalRepository.getListContentStream(id).map { content ->
                    if (query.isNotBlank()) {
                        content.filter { itemContent ->
                            val textToSearch = when (itemContent) {
                                is ListItemContent.GoalItem -> itemContent.goal.text
                                is ListItemContent.SublistItem -> itemContent.sublist.name
                                is ListItemContent.LinkItem -> itemContent.link.linkData.displayName
                                    ?: itemContent.link.linkData.target
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
    private val _inboxRecords = MutableStateFlow<List<InboxRecord>>(emptyList())
    val inboxRecords: StateFlow<List<InboxRecord>> = _inboxRecords.asStateFlow()
    private val _recordToEdit = MutableStateFlow<InboxRecord?>(null)
    val recordToEdit: StateFlow<InboxRecord?> = _recordToEdit.asStateFlow()


    init {
        Log.d(TAG, "ViewModel instance created: ${this.hashCode()}")

        viewModelScope.launch {
            goalList.filterNotNull()
                .map { it.defaultViewModeName }
                .distinctUntilChanged()
                .collect { savedModeName ->
                    val viewMode = try {
                        ProjectViewMode.valueOf(savedModeName)
                    } catch (e: Exception) {
                        ProjectViewMode.BACKLOG
                    }
                    Log.d(TAG, "Init: Loaded view mode for list ${listIdFlow.value}: ${viewMode.name}")
                    _uiState.update { it.copy(currentView = viewMode) }
                }
        }

        viewModelScope.launch {
            databaseContentStream.collect { dbContent ->
                _listContent.value = dbContent
            }
        }
        viewModelScope.launch {
            listIdFlow.filter { it.isNotEmpty() }.collect { id -> goalRepository.logListAccess(id) }
        }
        viewModelScope.launch { contextHandler.initialize() }
        viewModelScope.launch {
            listIdFlow.filter { it.isNotEmpty() }.flatMapLatest { id ->
                goalRepository.getInboxRecordsStream(id)
            }.collect { records ->
                _inboxRecords.value = records
            }
        }
    }

    // ... (решта методів ViewModel) ...

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

    override fun setPendingAction(
        actionType: GoalActionType,
        itemIds: Set<String>,
        goalIds: Set<String>
    ) {
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

    override fun updateDialogState(
        showAddWebLinkDialog: Boolean?,
        showAddObsidianLinkDialog: Boolean?
    ) {
        _uiState.update {
            it.copy(
                showAddWebLinkDialog = showAddWebLinkDialog ?: it.showAddWebLinkDialog,
                showAddObsidianLinkDialog = showAddObsidianLinkDialog
                    ?: it.showAddObsidianLinkDialog
            )
        }
    }

    override fun showRecentListsSheet(show: Boolean) {
        _uiState.update { it.copy(showRecentListsSheet = show) }
    }

    override fun updateSelectionState(selectedIds: Set<String>) {
        _uiState.update { it.copy(selectedItemIds = selectedIds) }
    }

    fun onListChooserResult(targetListId: String) {
        val actionType = pendingAction ?: return
        val itemIds = pendingSourceItemIds.toList()
        val goalIds = pendingSourceGoalIds.toList()
        viewModelScope.launch(Dispatchers.IO) {
            when (actionType) {
                GoalActionType.CreateInstance -> goalRepository.createGoalLinks(
                    goalIds,
                    targetListId
                )

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
        selectionHandler.clearSelection()
    }

    private fun navigateToListChooser(title: String) {
        viewModelScope.launch {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val disabledIds = listIdFlow.value
            _uiEventFlow.send(UiEvent.Navigate("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds"))
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
        val draggableItems =
            currentContent.filterNot { it is ListItemContent.LinkItem }.toMutableList()
        if (fromIndex !in draggableItems.indices || toIndex !in draggableItems.indices) return
        if (fromIndex == toIndex) return
        val movedItem = draggableItems.removeAt(fromIndex)
        draggableItems.add(toIndex, movedItem)
        val newFullList = mutableListOf<ListItemContent>()
        val reorderedDraggablesIterator = draggableItems.iterator()
        currentContent.forEach { originalItem ->
            if (originalItem is ListItemContent.LinkItem) newFullList.add(originalItem)
            else if (reorderedDraggablesIterator.hasNext()) newFullList.add(
                reorderedDraggablesIterator.next()
            )
        }
        _listContent.value = newFullList
        viewModelScope.launch { saveListOrder(newFullList) }
    }

    private suspend fun saveListOrder(listToSave: List<ListItemContent>) =
        withContext(Dispatchers.IO) {
            try {
                val updatedItems =
                    listToSave.mapIndexed { index, content -> content.item.copy(order = index.toLong()) }
                goalRepository.updateListItemsOrder(updatedItems)
            } catch (e: Exception) {
                Log.e(TAG, "[saveListOrder] Failed to save list order", e)
            }
        }

    fun onStateRefreshed() {
        _uiState.update { it.copy(needsStateRefresh = false) }
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
            AttachmentType.WEB_LINK -> inputHandler.onShowAddWebLinkDialog()
            AttachmentType.OBSIDIAN_LINK -> inputHandler.onShowAddObsidianLinkDialog()
            AttachmentType.LIST_LINK -> inputHandler.onAddListLinkRequest()
            AttachmentType.SHORTCUT -> inputHandler.onAddListShortcutRequest()
        }
    }

    override fun copyToClipboard(text: String, label: String) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun deleteCurrentList() {
        viewModelScope.launch(Dispatchers.IO) {
            val listId = listIdFlow.value
            if (listId.isNotEmpty()) {
                goalRepository.deleteGoalList(listId)
                withContext(Dispatchers.Main) {
                    requestNavigation("back")
                }
            }
        }
    }

    override fun updateCurrentView(view: ProjectViewMode) {
        Log.d(TAG, "updateCurrentView: Updating _uiState with new view: ${view.name}")
        _uiState.update { it.copy(currentView = view) }
    }

    override fun showEditInboxRecordDialog(record: InboxRecord) {
        _recordToEdit.value = record
    }

    fun onProjectViewChange(newView: ProjectViewMode) {
        inboxHandler.onProjectViewChange(newView)
        viewModelScope.launch {
            Log.d(TAG, "onProjectViewChange: Persisting view mode '${newView.name}' for list ${listIdFlow.value}")
            goalRepository.updateGoalListViewMode(listIdFlow.value, newView)
        }
    }

    fun deleteInboxRecord(recordId: String) {
        inboxHandler.deleteInboxRecord(recordId)
    }

    fun promoteInboxRecordToGoal(record: InboxRecord) {
        inboxHandler.promoteInboxRecordToGoal(record)
    }

    fun onInboxRecordEditRequest(record: InboxRecord) {
        inboxHandler.onInboxRecordEditRequest(record)
    }

    fun onInboxRecordEditDismiss() {
        _recordToEdit.value = null
    }

    fun onInboxRecordEditConfirm(newText: String) {
        recordToEdit.value?.let { record ->
            inboxHandler.updateInboxRecordText(record, newText)
        }
        _recordToEdit.value = null
    }

    override fun addQuickRecord(text: String) {
        inboxHandler.addQuickRecord(text)
        updateInputState(inputValue = TextFieldValue(""))
    }

    fun copyInboxRecordText(text: String) {
        copyToClipboard(text, "Inbox Record")
    }
    fun onImportFromMarkdownRequest() {
        _uiState.update { it.copy(showImportFromMarkdownDialog = true) }
    }

    fun onImportFromMarkdownDismiss() {
        _uiState.update { it.copy(showImportFromMarkdownDialog = false) }
    }

    fun onImportFromMarkdownConfirm(markdownText: String) {
        inboxMarkdownHandler.importFromMarkdown(markdownText, listIdFlow.value)
        onImportFromMarkdownDismiss()
    }

    fun onExportToMarkdownRequest() {
        inboxMarkdownHandler.exportToMarkdown(inboxRecords.value)
    }

    fun onImportBacklogFromMarkdownRequest() {
        _uiState.update { it.copy(showImportBacklogFromMarkdownDialog = true) }
    }

    fun onImportBacklogFromMarkdownDismiss() {
        _uiState.update { it.copy(showImportBacklogFromMarkdownDialog = false) }
    }

    fun onImportBacklogFromMarkdownConfirm(markdownText: String) {
        backlogMarkdownHandler.importFromMarkdown(markdownText, listIdFlow.value)
        onImportBacklogFromMarkdownDismiss()
    }

    fun onExportBacklogToMarkdownRequest() {
        backlogMarkdownHandler.exportToMarkdown(listContent.value)
    }
}