@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.InteractiveListItem
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar.ListTitleBar
import com.romankozak.forwardappmobile.ui.screens.backlog.components.utils.handleRelatedLinkClick
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.GoalTransportMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DND_DEBUG"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()

    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val showRecentListsSheet by viewModel.showRecentListsSheet.collectAsStateWithLifecycle()
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val goalActionState by viewModel.goalActionDialogState.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val currentListContextEmojiToHide by viewModel.currentListContextEmojiToHide.collectAsStateWithLifecycle()
    val showGoalTransportMenu by viewModel.showGoalTransportMenu.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }

    val displayList = remember(listContent, list?.isAttachmentsExpanded) {
        val attachmentItems = listContent.filter { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
        val draggableItems = listContent.filterNot { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }

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
                            viewModel.undoDelete()
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

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
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

    val attachmentItems = remember(listContent) {
        listContent.filter { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
    }
    val draggableItems = remember(listContent) {
        listContent.filterNot { it is ListItemContent.NoteItem || it is ListItemContent.LinkItem }
    }

    if (goalActionState is GoalActionDialogState.AwaitingActionChoice) {
        val itemContent = (goalActionState as GoalActionDialogState.AwaitingActionChoice).itemContent
        GoalActionChoiceDialog(
            itemContent = itemContent,
            onDismiss = { viewModel.onDismissGoalActionDialogs() },
            onActionSelected = { actionType ->
                viewModel.onGoalActionSelected(actionType, itemContent)
            }
        )
    }

    GoalTransportMenu(
        isVisible = showGoalTransportMenu,
        onDismiss = { viewModel.onDismissGoalTransportMenu() },
        onCreateInstanceRequest = { viewModel.onTransportActionSelected(GoalActionType.CreateInstance) },
        onMoveInstanceRequest = { viewModel.onTransportActionSelected(GoalActionType.MoveInstance) },
        onCopyGoalRequest = { viewModel.onTransportActionSelected(GoalActionType.CopyGoal) }
    )

    if (showRecentListsSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.onDismissRecentLists() }) {
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
                    items(recentLists, key = { it.id }) { list ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable { viewModel.onRecentListSelected(list.id) }
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            AdaptiveTopBar(
                // Передаємо всі необхідні параметри
                isSelectionModeActive = isSelectionModeActive,
                title = list?.name ?: stringResource(R.string.loading),

                // Параметри для навігації
                canGoBack = navController.previousBackStackEntry != null,
                onBackClick = { navController.popBackStack() },
                onForwardClick = { /* ... */ },
                onHomeClick = { viewModel.onRevealInExplorer(list?.id ?: "") },
                isAttachmentsExpanded = list?.isAttachmentsExpanded == true,
                onToggleAttachments = { viewModel.toggleAttachmentsVisibility() },
                onEditList = {
                    menuExpanded = false
                    navController.navigate("edit_list_screen/${list?.id}")
                },
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },

                // Параметри для режиму вибору
                selectedCount = uiState.selectedItemIds.size,
                areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAllItems() },
                onDelete = { viewModel.deleteSelectedItems() },
                onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) },
                onShareList = {  },
                onDeleteList = { viewModel.deleteSelectedItems() },
                modifier = Modifier
            )


        },


        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSelectionModeActive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                GoalInputBar(
                    inputValue = uiState.inputValue,
                    inputMode = uiState.inputMode,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    onSubmit = { viewModel.submitInput() },
                    onInputModeSelected = { viewModel.onInputModeSelected(it) },
                    onRecentsClick = { viewModel.onShowRecentLists() },
                    onAddListLinkClick = { viewModel.onAddListLinkRequest() },
                    onShowAddWebLinkDialog = { viewModel.onShowAddWebLinkDialog() },
                    onShowAddObsidianLinkDialog = { viewModel.onShowAddObsidianLinkDialog() },
                    onAddListShortcutClick = { viewModel.onAddListShortcutRequest() },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AttachmentsSection(
                attachments = attachmentItems,
                isExpanded = list?.isAttachmentsExpanded == true,
                onAddAttachment = { viewModel.onAddAttachment(it) },
                onDeleteItem = { viewModel.deleteItem(it) },
                onItemClick = { viewModel.onItemClick(it) },
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
                    val isHighlighted = (uiState.itemToHighlight == content.item.id) ||
                            (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                            content.item.id in uiState.selectedItemIds -> MaterialTheme.colorScheme.primaryContainer
                            (content as? ListItemContent.GoalItem)?.goal?.completed == true -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "bgAnim"
                    )

                    InteractiveListItem(
                        item = content,
                        index = index,
                        dragDropState = dragDropState,
                        swipeEnabled = !isSelectionModeActive && !dragDropState.isDragging,
                        isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.item.id),
                        resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                        backgroundColor = backgroundColor,
                        onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                        onDelete = { viewModel.deleteItem(content) },
                        onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                        onCreateInstanceRequest = {
                            viewModel.onGoalActionSelected(
                                GoalActionType.CreateInstance,
                                content
                            )
                        },
                        onMoveInstanceRequest = {
                            viewModel.onGoalActionSelected(
                                GoalActionType.MoveInstance,
                                content
                            )
                        },
                        onCopyGoalRequest = {
                            viewModel.onGoalActionSelected(
                                GoalActionType.CopyGoal,
                                content
                            )
                        },
                        modifier = Modifier,
                        onGoalTransportRequest = { viewModel.onGoalTransportInitiated(content) },
                        onCopyContentRequest = {
                            viewModel.copyContentRequest(content)
                        }
                    ) { isDragging ->
                        when (content) {
                            is ListItemContent.GoalItem -> {
                                GoalItem(
                                    goal = content.goal,
                                    obsidianVaultName = obsidianVaultName,
                                    onToggle = { isChecked ->
                                        viewModel.toggleGoalCompletedWithState(
                                            content.goal,
                                            isChecked
                                        )
                                    },
                                    onItemClick = { viewModel.onItemClick(content) },
                                    onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                    onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                    onRelatedLinkClick = { link -> viewModel.onLinkItemClick(link) },
                                    contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                    emojiToHide = currentListContextEmojiToHide
                                )
                            }

                            is ListItemContent.SublistItem -> {
                                SublistItemRow(
                                    sublistContent = content,
                                    isSelected = content.item.id in uiState.selectedItemIds,
                                    isHighlighted = isHighlighted,
                                    onClick = { viewModel.onItemClick(content) },
                                    onLongClick = { viewModel.onItemLongClick(content.item.id) }
                                )
                            }

                            else -> {
                                Log.w(
                                    "BacklogScreen",
                                    "Непідтримуваний тип у списку draggableItems: ${content::class.simpleName}"
                                )
                            }
                        }
                    }
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