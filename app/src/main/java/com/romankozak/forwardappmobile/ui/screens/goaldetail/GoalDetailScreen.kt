// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/GoalDetailScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.FilterableListChooser
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.components.SwipeableGoalItem
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import kotlinx.coroutines.flow.filterNotNull
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

    val uiState by viewModel.uiState.collectAsState()
    Log.d("COMPOSE_DEBUG", "GoalDetailScreen recomposing. Current mode is: ${uiState.inputMode}")

    val goals by viewModel.filteredGoals.collectAsState()
    val goalActionState by viewModel.goalActionDialogState.collectAsState()
    val list by viewModel.goalList.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val allContexts by viewModel.allContextNames.collectAsState()
    val associatedListsMap by viewModel.associatedListsMap.collectAsState()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val emojiToHide by viewModel.currentListContextEmojiToHide.collectAsState()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsState()
    val filteredListHierarchy by viewModel.filteredListHierarchyForDialog.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val showRecentSheet by viewModel.showRecentListsSheet.collectAsState()
    val recentLists by viewModel.recentLists.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSuggestions by remember { mutableStateOf(value = false) }
    var filteredContexts by remember { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val isInputBarVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 ||
                    listState.firstVisibleItemScrollOffset < listState.layoutInfo.visibleItemsInfo.lastOrNull()?.offset ?: 0
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        viewModel.moveGoal(from.index, to.index, afterApiUpdate = false)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        val scrollThreshold = 2
        coroutineScope.launch {
            val firstVisible = listState.firstVisibleItemIndex
            val lastVisible = firstVisible + listState.layoutInfo.visibleItemsInfo.size - 1

            if (to.index < (firstVisible + scrollThreshold)) {
                listState.animateScrollToItem(max(0, to.index - scrollThreshold))
            } else if (to.index > (lastVisible - scrollThreshold)) {
                listState.animateScrollToItem(
                    min(
                        listState.layoutInfo.totalItemsCount - 1,
                        to.index + scrollThreshold,
                    ),
                )
            }
        }
    }

    fun getCurrentWord(textValue: TextFieldValue): String? {
        val cursorPosition = textValue.selection.start
        if (cursorPosition == 0) return null
        val textUpToCursor = textValue.text.substring(0, cursorPosition)
        val lastSpaceIndex = textUpToCursor.lastIndexOf(' ')
        val startIndex = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1
        val currentWord = textUpToCursor.substring(startIndex)
        return currentWord.takeIf { it.startsWith("@") }
    }

    LaunchedEffect(uiState.inputValue) {
        val currentWord = getCurrentWord(uiState.inputValue)
        if ((currentWord != null) && (currentWord.length > 1)) {
            val query = currentWord.substring(1)
            filteredContexts = allContexts.filter { it.startsWith(query, ignoreCase = true) }
            showSuggestions = filteredContexts.isNotEmpty()
        } else {
            showSuggestions = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
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
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ResetSwipeState -> { /* Handled in VM */
                }
                is UiEvent.ScrollTo -> {
                    coroutineScope.launch {
                        listState.animateScrollToItem(event.index.coerceAtLeast(0))
                    }
                }
                is UiEvent.NavigateBackAndReveal -> {
                    try {
                        val goalListBackStackEntry = navController.getBackStackEntry("goal_lists_screen")
                        goalListBackStackEntry.savedStateHandle["list_to_reveal"] = event.listId
                        navController.popBackStack("goal_lists_screen", inclusive = false)
                    } catch (_: Exception) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.goalToHighlight) {
        val goalId = uiState.goalToHighlight
        if (goalId != null && !isSelectionModeActive) {
            val index = goals.indexOfFirst { it.goal.id == goalId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onHighlightShown()
            }
        }
    }

    LaunchedEffect(uiState.newlyAddedGoalInstanceId, goals) {
        val newGoalId = uiState.newlyAddedGoalInstanceId
        if (newGoalId != null) {
            val index = goals.indexOfFirst { it.instanceId == newGoalId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onScrolledToNewGoal()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedInstanceIds.size,
                    areAllSelected = goals.isNotEmpty() && (uiState.selectedInstanceIds.size == goals.size),
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllGoals() },
                    onDelete = { viewModel.deleteSelectedGoals() },
                    onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                    onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) },
                )
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: "Завантаження...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onRevealInExplorer(listId) }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = "Reveal in Backlogs Explorer",
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSelectionModeActive && isInputBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SuggestionChipsRow(
                        visible = showSuggestions,
                        contexts = filteredContexts,
                        onContextClick = { context ->
                            val currentText = uiState.inputValue.text
                            val cursorPosition = uiState.inputValue.selection.start
                            val wordStart = currentText.substring(0, cursorPosition)
                                .lastIndexOf(' ')
                                .let { if (it == -1) 0 else it + 1 }
                                .takeIf {
                                    currentText.substring(it, cursorPosition).startsWith("@")
                                } ?: -1

                            if (wordStart != -1) {
                                val textBefore = currentText.substring(0, wordStart)
                                val textAfter = currentText.substring(cursorPosition)
                                val newText = "$textBefore@$context $textAfter"
                                val newCursorPosition = wordStart + context.length + 2

                                viewModel.onInputTextChanged(
                                    TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newCursorPosition),
                                    ),
                                )
                            }
                        },
                    )
                    GoalInputBar(
                        modifier = Modifier.fillMaxWidth(),
                        inputValue = uiState.inputValue,
                        inputMode = uiState.inputMode,
                        onValueChange = viewModel::onInputTextChanged,
                        onSubmit = viewModel::submitInput,
                        onInputModeSelected = viewModel::onInputModeSelected,
                        onRecentsClick = viewModel::onShowRecentLists
                    )
                }
            }
        },
    ) { paddingValues ->
        if (list == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (goals.isEmpty() && uiState.localSearchQuery.isBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("У цьому списку ще немає цілей.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                itemsIndexed(
                    goals,
                    key = { _, item -> item.instanceId },
                ) { index, goalWithInstanceInfo ->
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = goalWithInstanceInfo.instanceId,
                        enabled = !isSelectionModeActive,
                    ) { isDragging ->
                        val scale by animateFloatAsState(
                            if (isDragging) 1.05f else 1f,
                            label = "scale",
                        )
                        val elevation by animateDpAsState(
                            if (isDragging) 8.dp else 0.dp,
                            label = "elevation",
                        )

                        val associatedLists = associatedListsMap.getOrDefault(
                            goalWithInstanceInfo.goal.id,
                            emptyList(),
                        )
                        val isSelected =
                            goalWithInstanceInfo.instanceId in uiState.selectedInstanceIds
                        val itemBackgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        SwipeableGoalItem(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(300),
                                    fadeOutSpec = tween(300),
                                )
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .shadow(
                                    elevation = elevation,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                            resetTrigger = uiState.resetTriggers.getOrDefault(
                                goalWithInstanceInfo.instanceId,
                                0,
                            ),
                            goalWithInstance = goalWithInstanceInfo,
                            isDragging = isDragging,
                            associatedLists = associatedLists,
                            obsidianVaultName = obsidianVaultName,
                            emojiToHide = emojiToHide,
                            onDelete = { viewModel.deleteGoal(goalWithInstanceInfo) },
                            backgroundColor = itemBackgroundColor,
                            onToggle = { viewModel.toggleGoalCompleted(goalWithInstanceInfo.goal) },
                            onTagClick = { tag: String -> viewModel.onTagClicked(tag) },
                            onAssociatedListClick = { listId: String ->
                                viewModel.onAssociatedListClicked(
                                    listId,
                                )
                            },
                            onItemClick = { viewModel.onGoalClick(goalWithInstanceInfo) },
                            onLongClick = { viewModel.onGoalLongClick(goalWithInstanceInfo.instanceId) },
                            onSwipeStart = { viewModel.onSwipeStart(goalWithInstanceInfo.instanceId) },
                            isAnotherItemSwiped = (uiState.swipedInstanceId != null) && (uiState.swipedInstanceId != goalWithInstanceInfo.instanceId),
                            onMoreActionsRequest = { viewModel.onGoalActionInitiated(goalWithInstanceInfo) },
                            onCreateInstanceRequest = { viewModel.onCreateInstanceRequest(goalWithInstanceInfo) },
                            onMoveInstanceRequest = { viewModel.onMoveInstanceRequest(goalWithInstanceInfo) },
                            onCopyGoalRequest = { viewModel.onCopyGoalRequest(goalWithInstanceInfo) },
                            contextMarkerToEmojiMap = contextMarkerToEmojiMap,

                            dragHandleModifier = if (!isSelectionModeActive) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                )
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }

    RecentListsSheet(
        showSheet = showRecentSheet,
        recentLists = recentLists,
        onDismiss = { viewModel.onDismissRecentLists() },
        onListClick = { listId -> viewModel.onRecentListSelected(listId) }
    )

    when (val state = goalActionState) {
        is GoalActionDialogState.Hidden -> {}
        is GoalActionDialogState.AwaitingActionChoice -> {
            GoalActionChoiceDialog(
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
            ) {
                viewModel.onGoalActionSelected(it)
            }
        }
        is GoalActionDialogState.AwaitingListChoice -> {
            val title = when (state.actionType) {
                GoalActionType.CreateInstance -> "Створити зв'язок у..."
                GoalActionType.MoveInstance -> "Перемістити до..."
                GoalActionType.CopyGoal -> "Копіювати до..."
                else -> "Виберіть список"
            }
            val currentListId = list?.id
            val disabledIds = currentListId?.let { setOf(it) } ?: emptySet()

            FilterableListChooser(
                title = title,
                filterText = listChooserFilterText,
                onFilterTextChanged = viewModel::onListChooserFilterChanged,
                topLevelLists = filteredListHierarchy.topLevelLists,
                childMap = filteredListHierarchy.childMap,
                expandedIds = listChooserFinalExpandedIds,
                onToggleExpanded = viewModel::onListChooserToggleExpanded,
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onConfirm = { confirmedListId ->
                    confirmedListId?.let { viewModel.confirmGoalAction(it) }
                },
                currentParentId = null,
                disabledIds = disabledIds,
                onAddNewList = { id, parentId, name ->
                    viewModel.addNewList(id, parentId, name)
                },
            )
        }
    }
}