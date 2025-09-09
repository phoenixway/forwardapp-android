// File: GoalDetailScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.components.attachments.AttachmentsSection
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.SublistItemRow
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.InteractiveListItem
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.ui.screens.backlog.components.utils.handleRelatedLinkClick
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private const val TAG = "BACKLOG_UI_DEBUG"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {



    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(60_000L) // Чекаємо одну хвилину
            }
        }.collect {
            currentTime = it
        }
    }


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()

    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val showRecentListsSheet = uiState.showRecentListsSheet
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val goalActionState by viewModel.itemActionHandler.goalActionDialogState.collectAsStateWithLifecycle()
    val showGoalTransportMenu by viewModel.itemActionHandler.showGoalTransportMenu.collectAsStateWithLifecycle()

    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val currentListContextEmojiToHide by viewModel.currentListContextEmojiToHide.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }

    val inboxRecords by viewModel.inboxRecords.collectAsStateWithLifecycle()
    val inboxListState = rememberLazyListState()

    BackHandler(enabled = uiState.inputValue.text.isNotEmpty()) {
        // Викликаємо правильний метод з InputHandler для очищення поля.
        // Це гарантує, що вся пов'язана логіка (наприклад, скасування аналізу) також спрацює.
        viewModel.inputHandler.onInputTextChanged(
            TextFieldValue(""),
            uiState.inputMode
        )
    }

    if (uiState.showAddWebLinkDialog) {
        AddWebLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { url, name ->
                viewModel.inputHandler.onAddWebLinkConfirm(url, name)
            }
        )
    }

    if (uiState.showAddObsidianLinkDialog) {
        AddObsidianLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { noteName ->
                viewModel.inputHandler.onAddObsidianLinkConfirm(noteName)
            }
        )
    }

    val recordToEdit by viewModel.recordToEdit.collectAsStateWithLifecycle()
    recordToEdit?.let { record ->
        EditInboxRecordDialog(
            record = record,
            onDismiss = { viewModel.onInboxRecordEditDismiss() },
            onConfirm = { newText -> viewModel.onInboxRecordEditConfirm(newText) }
        )
    }

    val displayList = remember(listContent, list?.isAttachmentsExpanded) {
        val attachmentItems = listContent.filterIsInstance<ListItemContent.LinkItem>()
        val draggableItems = listContent.filterNot { it is ListItemContent.LinkItem }

        if (list?.isAttachmentsExpanded == true) {
            attachmentItems + draggableItems
        } else {
            draggableItems
        }
    }

    val dragDropState = rememberSimpleDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex ->
            viewModel.moveItem(fromIndex, toIndex)
        }
    )

    val newItemInList = uiState.newlyAddedItemId?.let { id ->
        displayList.find { it.item.id == id }
    }

    LaunchedEffect(newItemInList) {
        if (newItemInList != null) {
            listState.animateScrollToItem(0)
            viewModel.onScrolledToNewItem()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    DisposableEffect(savedStateHandle, lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (savedStateHandle?.contains("list_chooser_result") == true) {
                    val result = savedStateHandle.get<String>("list_chooser_result")
                    if (result != null) {
                        Log.d("AddSublistDebug", "BacklogScreen: Received result from chooser: '$result'")
                        viewModel.onListChooserResult(result)
                    }
                    savedStateHandle.remove<String>("list_chooser_result")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.forceRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.needsStateRefresh) {
        if (uiState.needsStateRefresh) {
            dragDropState.reset()
            viewModel.onStateRefreshed()
        }
    }

    LaunchedEffect(list?.isAttachmentsExpanded) {
        if (list?.isAttachmentsExpanded == true) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.action,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.itemActionHandler.undoDelete()
                        }
                    }
                }
                is UiEvent.NavigateBackAndReveal -> {
                    navController.getBackStackEntry("goal_lists_screen")
                        .savedStateHandle["list_to_reveal"] = event.listId
                    navController.popBackStack("goal_lists_screen", inclusive = false)
                }
                is UiEvent.HandleLinkClick -> {
                    handleRelatedLinkClick(event.link, obsidianVaultName, localContext, navController)
                }
                is UiEvent.ResetSwipeState -> viewModel.onSwipeStateReset(event.itemId)
                is UiEvent.ScrollTo -> listState.animateScrollToItem(event.index)
                is UiEvent.ScrollToLatestInboxRecord -> {
                    coroutineScope.launch {
                        if (inboxRecords.isNotEmpty()) {
                            inboxListState.animateScrollToItem(inboxRecords.lastIndex)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.goalToHighlight, uiState.itemToHighlight, displayList, list?.isAttachmentsExpanded) {
        val goalId = uiState.goalToHighlight
        val itemId = uiState.itemToHighlight

        if ((goalId == null && itemId == null) || displayList.isEmpty()) {
            return@LaunchedEffect
        }

        val indexToScroll = when {
            goalId != null -> displayList.indexOfFirst { it is ListItemContent.GoalItem && it.goal.id == goalId }.takeIf { it != -1 }
            itemId != null -> displayList.indexOfFirst { it.item.id == itemId }.takeIf { it != -1 }
            else -> null
        }

        if (indexToScroll != null) {
            listState.animateScrollToItem(indexToScroll)
            delay(2500L)
        }
        viewModel.onHighlightShown()
    }

    // --- ПОЧАТОК ЗМІНИ: Більш надійна логіка підсвічування ---

    // Ефект №1: Відповідає за перемикання на вкладку INBOX
    LaunchedEffect(uiState.inboxRecordToHighlight, inboxRecords.isNotEmpty()) {
        val recordId = uiState.inboxRecordToHighlight
        val recordsAreLoaded = inboxRecords.isNotEmpty()

        if (recordId != null && recordsAreLoaded && uiState.currentView != ProjectViewMode.INBOX) {
            val recordExists = inboxRecords.any { it.id == recordId }
            if (recordExists) {
                Log.d(TAG, "Highlight requested. Switching to INBOX view.")
                viewModel.onProjectViewChange(ProjectViewMode.INBOX)
            }
        }
    }

    // Ефект №2: Відповідає за прокрутку та підсвічування, коли вкладка INBOX вже активна
    LaunchedEffect(uiState.inboxRecordToHighlight, uiState.currentView, inboxRecords) {
        val recordId = uiState.inboxRecordToHighlight

        if (recordId != null && uiState.currentView == ProjectViewMode.INBOX && inboxRecords.isNotEmpty()) {
            val indexToScroll = inboxRecords.indexOfFirst { it.id == recordId }
            Log.d(TAG, "INBOX view is active. Searching for record. Found index: $indexToScroll")

            if (indexToScroll != -1) {
                Log.d(TAG, "Scrolling to index: $indexToScroll")
                inboxListState.animateScrollToItem(indexToScroll)

                Log.d(TAG, "Waiting for highlight to finish...")
                delay(2500L)
                Log.d(TAG, "Highlight duration passed. Resetting state.")
                viewModel.onInboxHighlightShown()
            } else {
                // Якщо запис не знайдено (малоймовірно, але можливо), просто скидаємо стан
                Log.w(TAG, "Record ID $recordId not found. Clearing highlight state.")
                viewModel.onInboxHighlightShown()
            }
        }
    }
    // --- КІНЕЦЬ ЗМІНИ ---

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.selectionHandler.clearSelection()
    }

    BackHandler(enabled = !isSelectionModeActive) {
        viewModel.flushPendingMoves()
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushPendingMoves()
        }
    }

    LaunchedEffect(uiState.newlyAddedItemId, displayList) {
        val itemId = uiState.newlyAddedItemId
        if (itemId != null) {
            // Шукаємо індекс елемента в поточному стані списку
            val index = displayList.indexOfFirst { it.item.id == itemId }

            // Якщо елемент знайдено (index != -1), прокручуємо до нього
            if (index != -1) {
                listState.animateScrollToItem(index)
                // Повідомляємо ViewModel, що прокрутку виконано і можна скинути ID
                viewModel.onScrolledToNewItem()
            }
        }
    }

    LaunchedEffect(uiState.newlyAddedItemId, displayList) {
        val itemId = uiState.newlyAddedItemId
        Log.d("AutoScrollDebug", "newlyAddedItemId: $itemId, displayList size: ${displayList.size}")
        if (itemId != null) {
            // Спробуємо знайти по item.id (для звичайних цілей)
            var index = displayList.indexOfFirst { it.item.id == itemId }

            // Якщо не знайшли по item.id, спробуємо по goal.id (для цілей з ремайндером)
            if (index == -1) {
                index = displayList.indexOfFirst {
                    it is ListItemContent.GoalItem && it.goal.id == itemId
                }
                Log.d("AutoScrollDebug", "Trying goal.id search, found index: $index")
            }

            Log.d("AutoScrollDebug", "Final index: $index for itemId: $itemId")
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onScrolledToNewItem()
            } else {
                Log.w("AutoScrollDebug", "Item not found in displayList by any ID!")
            }
        }
    }



    val attachmentItems = remember(listContent) {
        listContent.filterIsInstance<ListItemContent.LinkItem>()
    }
    val draggableItems = remember(listContent) {
        listContent.filterNot { it is ListItemContent.LinkItem }
    }

    if (goalActionState is GoalActionDialogState.AwaitingActionChoice) {
        val itemContent = (goalActionState as GoalActionDialogState.AwaitingActionChoice).itemContent
        GoalActionChoiceDialog(
            itemContent = itemContent,
            onDismiss = { viewModel.itemActionHandler.onDismissGoalActionDialogs() },
            onActionSelected = { actionType ->
                viewModel.itemActionHandler.onGoalActionSelected(actionType, itemContent)
            }
        )
    }

    GoalTransportMenu(
        isVisible = showGoalTransportMenu,
        onDismiss = { viewModel.itemActionHandler.onDismissGoalTransportMenu() },
        onCreateInstanceRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.CreateInstance) },
        onMoveInstanceRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.MoveInstance) },
        onCopyGoalRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.CopyGoal) }
    )

    if (showRecentListsSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.inputHandler.onDismissRecentLists() }) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.recent_lists),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn {
                    items(recentLists, key = { it.id }) { list: GoalList ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable { viewModel.inputHandler.onRecentListSelected(list.id) }
                        )
                    }
                }
            }
        }
    }
    if (uiState.showImportFromMarkdownDialog) {
        ImportMarkdownDialog(
            onDismiss = viewModel::onImportFromMarkdownDismiss,
            onConfirm = viewModel::onImportFromMarkdownConfirm
        )
    }

    if (uiState.showImportBacklogFromMarkdownDialog) {
        ImportMarkdownDialog(
            onDismiss = viewModel::onImportBacklogFromMarkdownDismiss,
            onConfirm = viewModel::onImportBacklogFromMarkdownConfirm
        )
    }

    val reminderParseResult = if (uiState.detectedReminderCalendar != null &&
        uiState.detectedReminderSuggestion != null &&
        uiState.inputValue.text.isNotBlank()) {
        ReminderParseResult(
            originalText = uiState.inputValue.text,
            calendar = uiState.detectedReminderCalendar,
            suggestionText = uiState.detectedReminderSuggestion,
            dateTimeEntities = emptyList(),
            otherEntities = emptyList(),
            success = true,
            errorMessage = null
        )
    } else {
        null // Return null when there's no valid reminder
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AdaptiveTopBar(
                isSelectionModeActive = isSelectionModeActive,
                title = list?.name ?: stringResource(R.string.loading),
                selectedCount = uiState.selectedItemIds.size,
                areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                onClearSelection = { viewModel.selectionHandler.clearSelection() },
                onSelectAll = { viewModel.selectionHandler.selectAllItems() },
                onDelete = { viewModel.selectionHandler.deleteSelectedItems(uiState.selectedItemIds) },
                onMoreActions = { actionType -> viewModel.selectionHandler.onBulkActionRequest(actionType, uiState.selectedItemIds) },
                // --- ЗМІНЕНО ---
                // Замінено onToggleComplete на дві нові дії
                onMarkAsComplete = {
                    // Припускаємо, що в SelectionHandler є метод markSelectedAsComplete
                    viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds)
                },
                onMarkAsIncomplete = {
                    // Припускаємо, що в SelectionHandler є метод markSelectedAsIncomplete
                    viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds)
                }
                // --- КІНЕЦЬ ЗМІН ---
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSelectionModeActive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ModernInputPanel(
                    inputValue = uiState.inputValue,
                    inputMode = uiState.inputMode,
                    onValueChange = {
                        viewModel.inputHandler.onInputTextChanged(
                            it,
                            uiState.inputMode
                        )
                    },
                    onSubmit = {
                        viewModel.inputHandler.submitInput(
                            uiState.inputValue,
                            uiState.inputMode,
                            // uiState.detectedReminderCalendar
                        )
                    },
                    onInputModeSelected = {
                        viewModel.inputHandler.onInputModeSelected(
                            it,
                            uiState.inputValue
                        )
                    },
                    onRecentsClick = { viewModel.inputHandler.onShowRecentLists() },
                    onAddListLinkClick = { viewModel.inputHandler.onAddListLinkRequest() },
                    onShowAddWebLinkDialog = { viewModel.inputHandler.onShowAddWebLinkDialog() },
                    onShowAddObsidianLinkDialog = { viewModel.inputHandler.onShowAddObsidianLinkDialog() },
                    onAddListShortcutClick = { viewModel.inputHandler.onAddListShortcutRequest() },
                    canGoBack = navController.previousBackStackEntry != null,
                    onBackClick = {
                        viewModel.flushPendingMoves()
                        navController.popBackStack()
                    },
                    onForwardClick = { /* Not implemented */ },
                    onHomeClick = { viewModel.onRevealInExplorer(list?.id ?: "") },
                    isAttachmentsExpanded = list?.isAttachmentsExpanded == true,
                    onToggleAttachments = { viewModel.toggleAttachmentsVisibility() },
                    onEditList = {
                        menuExpanded = false
                        navController.navigate("edit_list_screen/${list?.id}")
                    },
                    onShareList = { /* Not implemented */ },
                    onDeleteList = { viewModel.deleteCurrentList() },
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { newStatus -> menuExpanded = newStatus },
                    currentView = uiState.currentView,
                    onViewChange = { newView -> viewModel.onProjectViewChange(newView) },
                    onImportFromMarkdown = viewModel::onImportFromMarkdownRequest,
                    onExportToMarkdown = viewModel::onExportToMarkdownRequest,
                    onImportBacklogFromMarkdown = viewModel::onImportBacklogFromMarkdownRequest,
                    onExportBacklogToMarkdown = viewModel::onExportBacklogToMarkdownRequest,
                    reminderParseResult = reminderParseResult,
                    onClearReminder = viewModel::onClearReminder,
                    isNerActive = uiState.nerState is NerState.Ready,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                )
            }
        },
    ) { paddingValues ->
        val calculatedSwipeEnabled = !isSelectionModeActive && !dragDropState.isDragging
        Log.v(TAG, "РЕКОМПОЗИЦІЯ ЕКРАНУ: isSelectionModeActive=$isSelectionModeActive, dragDropState.isDragging=${dragDropState.isDragging}, calculatedSwipeEnabled=$calculatedSwipeEnabled")

        when (uiState.currentView) {

            ProjectViewMode.BACKLOG -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AttachmentsSection(
                        attachments = attachmentItems,
                        isExpanded = list?.isAttachmentsExpanded == true,
                        onAddAttachment = { viewModel.onAddAttachment(it) },
                        onDeleteItem = { viewModel.itemActionHandler.deleteItem(it) },
                        onItemClick = { viewModel.itemActionHandler.onItemClick(it) },
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(
                            items = draggableItems,
                            key = { _, item -> item.item.id }
                        ) { index, content ->
                            val isSelected = content.item.id in uiState.selectedItemIds
                            val isHighlighted = (uiState.itemToHighlight == content.item.id) ||
                                    (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)

                            InteractiveListItem(
                                item = content,
                                index = index,
                                dragDropState = dragDropState,
                                isSelected = isSelected,
                                isHighlighted = isHighlighted,
                                swipeEnabled = !isSelectionModeActive && !dragDropState.isDragging,
                                isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.item.id),
                                resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                onDelete = { viewModel.itemActionHandler.deleteItem(content) },
                                onMoreActionsRequest = { viewModel.itemActionHandler.onGoalActionInitiated(content) },
                                onCreateInstanceRequest = {
                                    viewModel.itemActionHandler.onGoalActionSelected(
                                        GoalActionType.CreateInstance,
                                        content
                                    )
                                },
                                onMoveInstanceRequest = {
                                    viewModel.itemActionHandler.onGoalActionSelected(
                                        GoalActionType.MoveInstance,
                                        content
                                    )
                                },
                                onCopyGoalRequest = {
                                    viewModel.itemActionHandler.onGoalActionSelected(
                                        GoalActionType.CopyGoal,
                                        content
                                    )
                                },
                                modifier = Modifier,
                                onGoalTransportRequest = { viewModel.itemActionHandler.onGoalTransportInitiated(content) },
                                onCopyContentRequest = {
                                    viewModel.itemActionHandler.copyContentRequest(content)
                                }
                            ) { isDragging ->
                                when (content) {
                                    is ListItemContent.GoalItem -> {
                                        GoalItem(
                                            goal = content.goal,
                                            obsidianVaultName = obsidianVaultName,
                                            onToggle = { isChecked ->
                                                viewModel.itemActionHandler.toggleGoalCompletedWithState(
                                                    content.goal,
                                                    isChecked
                                                )
                                            },
                                            onItemClick = { viewModel.itemActionHandler.onItemClick(content) },
                                            onLongClick = { viewModel.toggleSelection(content.item.id) },
                                            onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                            onRelatedLinkClick = { link -> viewModel.onLinkItemClick(link) },
                                            contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                            emojiToHide = currentListContextEmojiToHide,
                                            // --- ВИПРАВЛЕНО: Передаємо поточний час в GoalItem ---
                                            currentTimeMillis = currentTime
                                        )
                                    }
                                    is ListItemContent.SublistItem -> {
                                        SublistItemRow(
                                            sublistContent = content,
                                            isSelected = isSelected,
                                            onClick = { viewModel.itemActionHandler.onItemClick(content) },
                                            onLongClick = { viewModel.toggleSelection(content.item.id) },
                                            onCheckedChange = { isCompleted ->
                                                viewModel.onSublistCompletedChanged(content.sublist, isCompleted)
                                            }
                                        )
                                    }
                                    else -> {
                                        Log.w(
                                            "BacklogScreen",
                                            "Unsupported type in draggableItems list: ${content::class.simpleName}"
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }
            ProjectViewMode.INBOX -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    InboxScreen(
                        records = inboxRecords,
                        onDelete = viewModel::deleteInboxRecord,
                        onPromoteToGoal = viewModel::promoteInboxRecordToGoal,
                        onRecordClick = viewModel::onInboxRecordEditRequest,
                        onCopy = { text -> viewModel.copyInboxRecordText(text) },
                        listState = inboxListState,
                        highlightedRecordId = uiState.inboxRecordToHighlight
                    )
                }
            }
            ProjectViewMode.ADDONS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add-ons (in development)")
                }
            }
        }
    }
}

@Composable
fun rememberSimpleDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): SimpleDragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) {
        SimpleDragDropState(state = lazyListState, scope = scope, onMove = onMove)
    }
}