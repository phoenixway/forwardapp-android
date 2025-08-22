// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/GoalDetailScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
// --- ПОЧАТОК ЗМІН ---
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet // Додаємо необхідний імпорт
// --- КІНЕЦЬ ЗМІН ---
import com.romankozak.forwardappmobile.ui.components.listItemsRenderers.*
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.lang.Integer.max
import java.lang.Integer.min

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val listId = remember {
        navController.currentBackStackEntry?.arguments?.getString("listId") ?: ""
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    // --- ПОЧАТОК ЗМІН ---
    // 1. Отримуємо стан для RecentListsSheet з ViewModel
    val showRecentListsSheet by viewModel.showRecentListsSheet.collectAsStateWithLifecycle()
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    // --- КІНЕЦЬ ЗМІН ---

    val haptic = LocalHapticFeedback.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val localContext = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible by remember(imeBottom) {
        derivedStateOf { imeBottom > 0 }
    }

    LaunchedEffect(listContent) {
        val newItemId = uiState.newlyAddedItemId
        // Перевіряємо, чи є ID нового елемента, і чи цей елемент вже є першим у списку
        if (newItemId != null && listContent.firstOrNull()?.item?.id == newItemId) {
            // Запускаємо прокрутку
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
            // Скидаємо тригер, щоб прокрутка не повторювалася
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
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
                    // Встановлюємо результат для попереднього екрану
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("list_to_reveal", event.listId)
                    // Повертаємось назад
                    navController.popBackStack()
                }

                else -> {}
            }
        }
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
                                    onRelatedLinkClick = { handleRelatedLinkClick(it, localContext, navController) },
                                    dragHandleModifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                    ),
                                    contextMarkerToEmojiMap = contextMarkerToEmojiMap
                                )
                            }
                        }
                        is ListItemContent.NoteItem -> {
                            val isSelected = content.item.id in uiState.selectedItemIds
                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
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
                                onMoreActionsRequest = {},
                                onCreateInstanceRequest = {},
                                onMoveInstanceRequest = {},
                                onCopyGoalRequest = {}
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
                            val isSelected = content.item.id in uiState.selectedItemIds
                            SublistItemRow(
                                modifier = itemModifier,
                                sublistContent = content,
                                isSelected = isSelected,
                                onClick = { viewModel.onItemClick(content) },
                                onLongClick = { viewModel.onItemLongClick(content.item.id) }
                            )
                        }
                    }
                }
            }
        }

        // --- ПОЧАТОК ЗМІН ---
        // 2. Додаємо RecentListsSheet в Scaffold.
        // Він буде автоматично показуватись/ховатись залежно від стану showRecentListsSheet.
        RecentListsSheet(
            showSheet = showRecentListsSheet,
            recentLists = recentLists,
            onDismiss = { viewModel.onDismissRecentLists() },
            onListClick = { listId -> viewModel.onRecentListSelected(listId) }
        )
        // --- КІНЕЦЬ ЗМІН ---
    }
}

private fun handleRelatedLinkClick(link: RelatedLink, context: Context, navController: NavController) {
    when (link.type) {
        LinkType.URL -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }
        LinkType.GOAL_LIST -> navController.navigate("goal_detail_screen/${link.target}")
        LinkType.NOTE -> { /* TODO */ }
        LinkType.OBSIDIAN -> { /* TODO */ }
    }
}