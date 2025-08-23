// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/GoalDetailScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.components.listItemsRenderers.*
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    val haptic = LocalHapticFeedback.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val emojiToHide by viewModel.currentListContextEmojiToHide.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val goalActionState by viewModel.goalActionDialogState.collectAsStateWithLifecycle()

    val listId = remember {
        navController.currentBackStackEntry?.arguments?.getString("listId") ?: ""
    }

    // Блок для отримання результатів від інших екранів (включно з list_chooser_screen)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val navGraphEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Перевірка результату від вибору списку
            val selectedListId = resultViewModel.consumeResult<String>("selectedListId")
            if (selectedListId != null) {
                viewModel.onListChooserResult(selectedListId)
            }

            // Перевірка, чи потрібно оновити екран після редагування
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
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
                is UiEvent.ScrollTo -> listState.animateScrollToItem(event.index)
                is UiEvent.NavigateBackAndReveal -> {
                    navController.getBackStackEntry("goal_lists_screen")
                        .savedStateHandle
                        .set("list_to_reveal", event.listId)
                    navController.popBackStack("goal_lists_screen", inclusive = false)
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(listContent) {
        val newItemId = uiState.newlyAddedItemId
        if (newItemId != null && listContent.firstOrNull()?.item?.id == newItemId) {
            coroutineScope.launch { listState.animateScrollToItem(0) }
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        viewModel.moveItem(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val isAnyItemDragging = reorderableLazyListState.isAnyItemDragging

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedItemIds.size,
                    areAllSelected = listContent.isNotEmpty() && uiState.selectedItemIds.size == listContent.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllItems() },
                    onDelete = { viewModel.deleteSelectedItems() },
                    onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                    onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) },
                )
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: "Loading...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onRevealInExplorer(listId) }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = "Reveal in Backlogs"
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                AnimatedVisibility(
                    visible = !isSelectionModeActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    GoalInputBar(
                        inputValue = uiState.inputValue,
                        inputMode = uiState.inputMode,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        onSubmit = { viewModel.submitInput() },
                        onInputModeSelected = { viewModel.onInputModeSelected(it) },
                        onRecentsClick = { viewModel.onShowRecentLists() }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                itemsIndexed(listContent, key = { _, item -> item.item.id }) { index, content ->
                    ReorderableItem(reorderableLazyListState, key = content.item.id) { isDragging ->
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        val itemModifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(elevation, RoundedCornerShape(12.dp))

                        when (content) {
                            is ListItemContent.GoalItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                val backgroundColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                    label = "goal_background_color"
                                )

                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = backgroundColor,
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) }
                                ) {
                                    GoalItem(
                                        goal = content.goal,
                                        obsidianVaultName = obsidianVaultName,
                                        onToggle = { viewModel.toggleGoalCompletedWithState(content.goal, it) },
                                        onItemClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        onTagClick = { viewModel.onTagClicked(it) },
                                        onRelatedLinkClick = {
                                            handleRelatedLinkClick(it, localContext, navController)
                                        },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        ),
                                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                        emojiToHide = emojiToHide
                                    )
                                }
                            }
                            is ListItemContent.NoteItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                val backgroundColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                    label = "note_background_color"
                                )

                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = backgroundColor,
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) }
                                ) {
                                    NoteItemRow(
                                        noteContent = content,
                                        isSelected = isSelected,
                                        onClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        )
                                    )
                                }
                            }
                            is ListItemContent.SublistItem -> {
                                SublistItemRow(
                                    modifier = itemModifier,
                                    sublistContent = content,
                                    isSelected = content.item.id in uiState.selectedItemIds,
                                    onClick = { viewModel.onItemClick(content) },
                                    onLongClick = { viewModel.onItemLongClick(content.item.id) }
                                )
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
        onListClick = { listId -> viewModel.onRecentListSelected(listId) }
    )

    // Тепер тут залишився тільки діалог вибору ДІЇ, а не списку
    when (val state = goalActionState) {
        is GoalActionDialogState.Hidden -> {}
        is GoalActionDialogState.AwaitingActionChoice -> {
            GoalActionChoiceDialog(
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onActionSelected = { actionType ->
                    viewModel.onGoalActionSelected(actionType, state.itemContent)
                }
            )
        }
    }
}

private fun handleRelatedLinkClick(
    link: RelatedLink,
    context: Context,
    navController: NavController
) {
    when (link.type) {
        LinkType.URL -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }
        LinkType.GOAL_LIST -> {
            navController.navigate("goal_detail_screen/${link.target}") {
                popUpTo(navController.currentDestination!!.id!!) {
                    inclusive = true
                }
            }
        }
        LinkType.NOTE -> { /* TODO */ }
        LinkType.OBSIDIAN -> { /* TODO */ }
    }
}