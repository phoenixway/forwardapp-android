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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.romankozak.forwardappmobile.ui.components.*
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.AddLinkDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val draggableItems by viewModel.draggableItems.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()

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

    var showTransportMenu by remember { mutableStateOf(false) }
    var selectedGoalForTransport by remember { mutableStateOf<ListItemContent?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val dragDropState = rememberSimpleDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex -> viewModel.moveItem(fromIndex, toIndex) }
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    // ФІНАЛЬНЕ РІШЕННЯ: Оновлення даних при кожному поверненні на екран (ON_RESUME).
    // Цей метод на 100% надійний у вашому випадку.
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

    // Доданий блок для прокрутки догори при розгортанні додатків
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

    LaunchedEffect(uiState.goalToHighlight, uiState.itemToHighlight, draggableItems, attachmentItems, list?.isAttachmentsExpanded) {
        val goalId = uiState.goalToHighlight
        val itemId = uiState.itemToHighlight

        if ((goalId == null && itemId == null) || (draggableItems.isEmpty() && attachmentItems.isEmpty())) {
            return@LaunchedEffect
        }

        val headerItemsCount = 1
        val expandedAttachmentsCount = if (list?.isAttachmentsExpanded == true) attachmentItems.size else 0

        val indexToScroll = when {
            goalId != null -> draggableItems
                .indexOfFirst { it is ListItemContent.GoalItem && it.goal.id == goalId }
                .takeIf { it != -1 }
                ?.let { it + headerItemsCount + expandedAttachmentsCount }

            itemId != null -> {
                val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == itemId }
                if (attachmentIndex != -1 && list?.isAttachmentsExpanded == true) {
                    headerItemsCount + attachmentIndex
                } else {
                    draggableItems
                        .indexOfFirst { it.item.id == itemId }
                        .takeIf { it != -1 }
                        ?.let { it + headerItemsCount + expandedAttachmentsCount }
                }
            }
            else -> null
        }

        if (indexToScroll != null) {
            listState.animateScrollToItem(indexToScroll)
            delay(2500L)
        }
        viewModel.onHighlightShown()
    }

    LaunchedEffect(uiState.newlyAddedItemId, list?.isAttachmentsExpanded) {
        val newItemId = uiState.newlyAddedItemId
        if (newItemId != null) {
            val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == newItemId }
            if (attachmentIndex != -1 && list?.isAttachmentsExpanded == true) {
                listState.animateScrollToItem(1)
            } else {
                val draggableIndex = draggableItems.indexOfFirst { it.item.id == newItemId }
                if (draggableIndex != -1) {
                    val headerSize = 1 + if (list?.isAttachmentsExpanded == true) attachmentItems.size else 0
                    listState.animateScrollToItem(headerSize + draggableIndex)
                }
            }
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = !isSelectionModeActive) {
        viewModel.flushPendingMoves() // Зберігаємо перед виходом
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushPendingMoves()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
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
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: stringResource(R.string.loading)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_description))
                        }
                    },
                    actions = {
                        list?.let {
                            IconButton(onClick = { viewModel.toggleAttachmentsVisibility() }) {
                                Icon(
                                    imageVector = Icons.Default.Attachment,
                                    contentDescription = "Додатки",
                                    tint = if (it.isAttachmentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.onRevealInExplorer(list?.id ?: "") }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.reveal_in_backlogs),
                            )
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Властивості") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Властивості") },
                                onClick = {
                                    menuExpanded = false
                                    navController.navigate("edit_list_screen/${list?.id}")
                                }
                            )
                        }
                    },
                )
            }
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item(key = "attachments_section") {
                list?.let {
                    AttachmentsSection(
                        attachments = attachmentItems,
                        isExpanded = it.isAttachmentsExpanded,
                        onAddAttachment = { type ->
                            when (type) {
                                AttachmentType.NOTE -> viewModel.onAddNewNoteRequested()
                                AttachmentType.WEB_LINK -> viewModel.onShowAddWebLinkDialog()
                                AttachmentType.OBSIDIAN_LINK -> viewModel.onShowAddObsidianLinkDialog()
                                AttachmentType.LIST_LINK -> viewModel.onAddListLinkRequest()
                            }
                        },
                        onDeleteItem = { item -> viewModel.deleteItem(item) },
                        onItemClick = { item -> viewModel.onItemClick(item) }
                    )
                }
            }

            itemsIndexed(
                items = draggableItems,
                key = { index, item -> "${item.item.id}_$index" }
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
                    dragDropState = dragDropState.apply { this.itemsProvider = { draggableItems } },
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
                    onGoalTransportRequest = {
                        Log.d("swipeActions", "transport")
                        selectedGoalForTransport = content
                        showTransportMenu = true
                    },
                    onCopyContentRequest = {
                        Log.d("swipeActions", "copy content")
                        viewModel.copyContentRequest(content)
                    }
                ) {
                    when (content) {
                        is ListItemContent.GoalItem -> {
                            GoalItem(
                                goal = content.goal,
                                obsidianVaultName = obsidianVaultName,
                                onToggle = { isChecked -> viewModel.toggleGoalCompletedWithState(content.goal, isChecked) },
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
                            Log.w("BacklogScreen", "Unsupported draggable content type: ${content::class.simpleName}")
                        }
                    }
                }
            }
        }

        RecentListsSheet(
            showSheet = showRecentListsSheet,
            recentLists = recentLists,
            onDismiss = { viewModel.onDismissRecentLists() },
            onListClick = { listId -> viewModel.onRecentListSelected(listId) },
        )

        GoalTransportMenu(
            isVisible = showTransportMenu,
            onDismiss = {
                showTransportMenu = false
                selectedGoalForTransport = null
            },
            onCreateInstanceRequest = {
                selectedGoalForTransport?.let { content ->
                    viewModel.createInstanceRequest(content)
                }
                showTransportMenu = false
                selectedGoalForTransport = null
            },
            onMoveInstanceRequest = {
                selectedGoalForTransport?.let { content ->
                    viewModel.moveInstanceRequest(content)
                }
                showTransportMenu = false
                selectedGoalForTransport = null
            },
            onCopyGoalRequest = {
                selectedGoalForTransport?.let { content ->
                    viewModel.copyGoalRequest(content)
                }
                showTransportMenu = false
                selectedGoalForTransport = null
            },
        )

        when (val state = goalActionState) {
            is GoalActionDialogState.Hidden -> {}
            is GoalActionDialogState.AwaitingActionChoice -> {
                GoalActionChoiceDialog(
                    itemContent = state.itemContent,
                    onDismiss = { viewModel.onDismissGoalActionDialogs() },
                    onActionSelected = { actionType ->
                        viewModel.onGoalActionSelected(actionType, state.itemContent)
                    },
                )
            }
        }

        if (uiState.showAddWebLinkDialog) {
            AddLinkDialog(
                title = stringResource(R.string.add_link_dialog_title),
                namePlaceholder = stringResource(R.string.dialog_placeholder_name_optional),
                targetPlaceholder = stringResource(R.string.url_placeholder),
                onDismiss = { viewModel.onDismissLinkDialogs() },
                onConfirm = { name, url ->
                    viewModel.onAddWebLinkConfirm(url, name.takeIf { it.isNotBlank() })
                },
            )
        }

        if (uiState.showAddObsidianLinkDialog) {
            AddLinkDialog(
                title = stringResource(R.string.add_obsidian_link_dialog_title),
                namePlaceholder = stringResource(R.string.dialog_placeholder_note_name),
                isTargetVisible = false,
                onDismiss = { viewModel.onDismissLinkDialogs() },
                onConfirm = { name, _ ->
                    viewModel.onAddObsidianLinkConfirm(name)
                },
            )
        }
    }
}

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