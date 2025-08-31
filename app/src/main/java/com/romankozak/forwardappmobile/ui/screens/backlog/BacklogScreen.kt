@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.InteractiveListItem
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.GoalTransportMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

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
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp, // додає глибину
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)

            ) {
            Column(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 300f
                    )
                ),
                //verticalArrangement = Arrangement.spacedBy(8.dp) // відстань між панелями

            )  {
                // Окрема панель для назви списку - тепер зверху
                ListTitleBar(
                    title = list?.name ?: stringResource(R.string.loading)
                )

                BrowserNavigationBar(
                    canGoBack = navController.previousBackStackEntry != null,
                    onBackClick = { navController.popBackStack() },
                    onForwardClick = { /* TODO: implement forward navigation */ },
                    onHomeClick = { viewModel.onRevealInExplorer(list?.id ?: "") },
                    isAttachmentsExpanded = list?.isAttachmentsExpanded == true,
                    onToggleAttachments = { viewModel.toggleAttachmentsVisibility() },
                    onEditList = {
                        menuExpanded = false
                        navController.navigate("edit_list_screen/${list?.id}")
                    },
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { menuExpanded = it },
                )

                // Панель для режиму вибору
                if (isSelectionModeActive) {
                    MultiSelectTopAppBar(
                        selectedCount = uiState.selectedItemIds.size,
                        areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                        onClearSelection = { viewModel.clearSelection() },
                        onSelectAll = { viewModel.selectAllItems() },
                        onDelete = { viewModel.deleteSelectedItems() },
                        onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                        onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) },
                    )
                }
            }}
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

/**
 * Окрема, тонка панель для відображення назви списку.
 */
@Composable
private fun ListTitleBar(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent // 👈 ЗМІНА ТУТ
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun BrowserNavigationBar(
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onEditList: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent, // 👈 ЗМІНА ТУТ
        tonalElevation = 0.dp // 👈 ЗМІНА ТУТ
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick, enabled = canGoBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            // Forward button (placeholder)
            IconButton(onClick = onForwardClick, enabled = false) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.forward)
                )
            }
            // Home button (Reveal in explorer)
            IconButton(onClick = onHomeClick) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.go_to_home_list)
                )
            }

            // Spacer to push items to the right
            Spacer(modifier = Modifier.weight(1f))

            // Attachments toggle
            val attachmentIconColor by animateColorAsState(
                targetValue = if (isAttachmentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "attachmentIconColor"
            )
            IconButton(onClick = onToggleAttachments) {
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = stringResource(R.string.toggle_attachments),
                    tint = attachmentIconColor
                )
            }

            // More options menu
            Box {
                IconButton(onClick = { onMenuExpandedChange(true) }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_list)) },
                        onClick = {
                            onEditList()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
            }
        }
    }
}


// ... (решта коду без змін)
private fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
    navController: NavController,
) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            }
            LinkType.GOAL_LIST -> {
                navController.navigate("goal_detail_screen/${link.target}")
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_obsidian_vault_not_set), Toast.LENGTH_LONG).show()
                }
            }
            LinkType.NOTE -> { /* TODO */ }
        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_link_open_failed), Toast.LENGTH_LONG).show()
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

@Composable
fun NoteItemRow(
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    SwipeableListItem(
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true,
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = { },
        onDelete = onDelete,
        onMoreActionsRequest = { },
        onGoalTransportRequest = { },
        onCopyContentRequest = { },
        backgroundColor = backgroundColor,
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = noteContent.note.title?.takeIf { it.isNotBlank() } ?: "Без назви",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (noteContent.note.content.isNotBlank()) {
                        Text(
                            text = noteContent.note.content.take(100) + if (noteContent.note.content.length > 100) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun LinkItemRow(
    linkContent: ListItemContent.LinkItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    SwipeableListItem(
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true,
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = { },
        onDelete = onDelete,
        onMoreActionsRequest = { },
        onGoalTransportRequest = { },
        onCopyContentRequest = { },
        backgroundColor = backgroundColor,
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (linkContent.link.linkData.type) {
                            LinkType.URL -> Icons.Default.Link
                            LinkType.OBSIDIAN -> Icons.Default.Description
                            LinkType.GOAL_LIST -> Icons.Default.List
                            LinkType.NOTE -> Icons.Default.Note
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = linkContent.link.linkData.displayName ?: linkContent.link.linkData.target,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (linkContent.link.linkData.displayName != null) {
                            Text(
                                text = linkContent.link.linkData.target,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    )
}