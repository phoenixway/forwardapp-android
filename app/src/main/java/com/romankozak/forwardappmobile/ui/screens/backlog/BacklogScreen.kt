@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.*
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.AddLinkDialog
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.URLEncoder

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Отримуємо готові, відфільтровані списки з ViewModel
    val draggableItems by viewModel.draggableItems.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()

    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val showRecentListsSheet by viewModel.showRecentListsSheet.collectAsStateWithLifecycle()
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val goalActionState by viewModel.goalActionDialogState.collectAsStateWithLifecycle()
    val listId = remember {
        navController.currentBackStackEntry?.arguments?.getString("listId") ?: ""
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val navGraphEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)

    // Пам'ятаємо попередню довжину списку для правильного DnD
    val previousDraggableItemsCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(draggableItems.size) {
        // Оновлюємо лише якщо список дійсно змінився
        if (draggableItems.size != previousDraggableItemsCount.intValue) {
            previousDraggableItemsCount.intValue = draggableItems.size
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val pickedGoalId = resultViewModel.consumeResult<String>("picked_goal_id")
            if (pickedGoalId != null) {
                viewModel.onExistingItemSelected(pickedGoalId)
            }
            val selectedListId = resultViewModel.consumeResult<String>("selectedListId")
            if (selectedListId != null) {
                viewModel.onListChooserResult(selectedListId)
            }
            val needsRefresh = resultViewModel.consumeResult<Boolean>("refresh_needed")
            if (needsRefresh == true) {
                viewModel.forceRefresh()
            }
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
                is UiEvent.ScrollTo -> {
                    listState.animateScrollToItem(event.index)
                }
                is UiEvent.NavigateBackAndReveal -> {
                    navController.getBackStackEntry("goal_lists_screen")
                        .savedStateHandle["list_to_reveal"] = event.listId
                    navController.popBackStack("goal_lists_screen", inclusive = false)
                }
                is UiEvent.HandleLinkClick -> {
                    handleRelatedLinkClick(event.link, obsidianVaultName, localContext, navController)
                }
                is UiEvent.ResetSwipeState -> {
                    viewModel.onSwipeStateReset(event.itemId)
                }
            }
        }
    }

    LaunchedEffect(uiState.goalToHighlight, uiState.itemToHighlight, draggableItems, attachmentItems) {
        val goalId = uiState.goalToHighlight
        val itemId = uiState.itemToHighlight

        if ((goalId == null && itemId == null) ||
            (draggableItems.isEmpty() && attachmentItems.isEmpty())) {
            return@LaunchedEffect
        }

        val attachmentsCount = if (list?.isAttachmentsExpanded == true) attachmentItems.size else 0
        val offsetIndex = 1 + attachmentsCount // 1 для AttachmentsSection

        val index = when {
            goalId != null -> {
                val draggableIndex = draggableItems.indexOfFirst {
                    (it is ListItemContent.GoalItem) && it.goal.id == goalId
                }
                if (draggableIndex >= 0) offsetIndex + draggableIndex else -1
            }
            itemId != null -> {
                val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == itemId }
                if (attachmentIndex >= 0 && list?.isAttachmentsExpanded == true) {
                    1 + attachmentIndex // 1 для AttachmentsSection header
                } else {
                    val draggableIndex = draggableItems.indexOfFirst { it.item.id == itemId }
                    if (draggableIndex >= 0) offsetIndex + draggableIndex else -1
                }
            }
            else -> -1
        }

        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(2500L)
        } else {
            delay(500L)
        }
        viewModel.onHighlightShown()
    }

    LaunchedEffect(draggableItems, attachmentItems, list?.isAttachmentsExpanded) {
        val newItemId = uiState.newlyAddedItemId
        if (newItemId != null) {
            // Перевіряємо чи новий елемент в attachments
            val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == newItemId }
            if (attachmentIndex == 0 && list?.isAttachmentsExpanded == true) {
                // Новий attachment на першому місці
                coroutineScope.launch { listState.animateScrollToItem(1) } // 1 для header
            } else {
                // Перевіряємо чи новий елемент в draggableItems
                val draggableIndex = draggableItems.indexOfFirst { it.item.id == newItemId }
                if (draggableIndex == 0) {
                    val attachmentsOffset = if (list?.isAttachmentsExpanded == true)
                        1 + attachmentItems.size else 1
                    coroutineScope.launch { listState.animateScrollToItem(attachmentsOffset) }
                }
            }
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    // Створюємо reorderable state з додатковими перевірками
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        try {
            // Перевіряємо валідність індексів
            val fromIndex = from.index
            val toIndex = to.index

            Log.d("DragAndDrop", "Moving from $fromIndex to $toIndex, draggableItems.size: ${draggableItems.size}")

            if (fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < draggableItems.size && toIndex < draggableItems.size) {
                viewModel.moveItem(fromIndex, toIndex)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                Log.w("DragAndDrop", "Invalid indices: from=$fromIndex, to=$toIndex, size=${draggableItems.size}")
            }
        } catch (e: Exception) {
            Log.e("DragAndDrop", "Error during drag operation: ${e.message}", e)
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
                        IconButton(onClick = { viewModel.onRevealInExplorer(listId) }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.reveal_in_backlogs),
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
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
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // Attachments section (non-draggable) - завжди фіксована секція
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

                // Draggable items only - стабільні ключі та індекси
                itemsIndexed(
                    items = draggableItems,
                    key = { _, item -> "draggable_${item.item.id}" }
                ) { index, content ->
                    // Додаємо додаткову перевірку для запобігання помилок
                    if (index >= draggableItems.size) {
                        Log.w("DragAndDrop", "Index $index out of bounds for draggableItems.size=${draggableItems.size}")
                        return@itemsIndexed
                    }

                    ReorderableItem(
                        reorderableLazyListState,
                        key = "draggable_${content.item.id}"
                    ) { isDragging ->
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.05f else 1f,
                            label = "scale"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )

                        val itemModifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(elevation, RoundedCornerShape(12.dp))

                        val isHighlighted = (uiState.itemToHighlight == content.item.id) ||
                                (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)

                        val dragHandle = @Composable {
                            Surface(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 4.dp),
                                color = Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.semantics { contentDescription = "Перетягнути" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .longPressDraggableHandle(
                                                    onDragStarted = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                )
                                                .size(24.dp)
                                                .padding(4.dp)
                                                .pointerInput(Unit) {
                                                    detectTapGestures {
                                                        // Пустий обробник для запобігання конфліктів
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        when (content) {
                            is ListItemContent.GoalItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = reorderableLazyListState.isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = Color.Transparent,
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.item.id),
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) },
                                ) {
                                    GoalItem(
                                        goalContent = content,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleGoalCompletedWithState(content.goal, isChecked)
                                        },
                                        onClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        isSelected = isSelected,
                                        isHighlighted = isHighlighted,
                                        endAction = dragHandle,
                                    )
                                }
                            }

                            is ListItemContent.SublistItem -> {
                                SublistItemRow(
                                    modifier = itemModifier,
                                    sublistContent = content,
                                    isSelected = content.item.id in uiState.selectedItemIds,
                                    isHighlighted = isHighlighted,
                                    onClick = { viewModel.onItemClick(content) },
                                    onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                    endAction = dragHandle,
                                )
                            }

                            else -> {
                                // Для інших типів контенту
                                Log.w("DragAndDrop", "Unsupported content type: ${content::class.simpleName}")
                            }
                        }
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
                navController.navigate("goal_detail_screen/${link.target}") {
                    popUpTo(navController.currentDestination!!.id) {
                        inclusive = true
                    }
                }
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