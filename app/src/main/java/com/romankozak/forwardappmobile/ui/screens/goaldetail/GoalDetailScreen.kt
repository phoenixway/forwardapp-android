@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.components.SwipeableGoalItem
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.dialogs.InputModeDialog
import com.romankozak.forwardappmobile.ui.dialogs.ListChooserDialog
import kotlinx.coroutines.launch
import sh.calvin.reorderable.rememberScroller

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.filteredGoals.collectAsState()
    val goalActionState by viewModel.goalActionDialogState.collectAsState()
    val showInputModeDialog by viewModel.showInputModeDialog.collectAsState()
    val listHierarchy by viewModel.listHierarchyForChooser.collectAsState()
    val list by viewModel.goalList.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val allContexts by viewModel.allContextNames.collectAsState()
    val associatedListsMap by viewModel.associatedListsMap.collectAsState()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSuggestions by remember { mutableStateOf(false) }
    var filteredContexts by remember { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()
    val slowScroller = rememberScroller(
        scrollableState = listState,
        pixelPerSecond = 10f // Зменшіть це значення для ще повільнішої прокрутки
    )

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            viewModel.moveGoal(from.index, to.index, false)
        },
        scroller = slowScroller // Передаємо створений Scroller
    )

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
        if (currentWord != null && currentWord.length > 1) {
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
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route)
                }
                is UiEvent.ResetSwipeState -> {
                    // Ця логіка тепер у ViewModel
                }
                is UiEvent.ScrollTo -> {
                    coroutineScope.launch {
                        listState.animateScrollToItem(event.index.coerceAtLeast(0))
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

    LaunchedEffect(uiState.newlyAddedGoalInstanceId) {
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
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedInstanceIds.size,
                    areAllSelected = goals.isNotEmpty() && uiState.selectedInstanceIds.size == goals.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllGoals() },
                    onDelete = { viewModel.deleteSelectedGoals() },
                    onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                    onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) }
                )
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: "Завантаження...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isSelectionModeActive) {
                Column {
                    SuggestionChipsRow(
                        visible = showSuggestions,
                        contexts = filteredContexts,
                        onContextClick = { context ->
                            val currentText = uiState.inputValue.text
                            val cursorPosition = uiState.inputValue.selection.start
                            val wordStart = currentText.substring(0, cursorPosition)
                                .lastIndexOf(' ')
                                .let { if (it == -1) 0 else it + 1 }
                                .takeIf { currentText.substring(it, cursorPosition).startsWith("@") } ?: -1

                            if (wordStart != -1) {
                                val textBefore = currentText.substring(0, wordStart)
                                val textAfter = currentText.substring(cursorPosition)
                                val newText = "$textBefore@$context $textAfter"
                                val newCursorPosition = wordStart + context.length + 2

                                viewModel.onInputTextChanged(
                                    TextFieldValue(text = newText, selection = TextRange(newCursorPosition))
                                )
                            }
                        }
                    )
                    GoalInputBar(
                        inputValue = uiState.inputValue,
                        inputMode = uiState.inputMode,
                        onModeChangeRequest = viewModel::onInputModeChangeRequest,
                        onSubmit = viewModel::submitInput,
                        onValueChange = viewModel::onInputTextChanged
                    )
                }
            }
        }
    ) { paddingValues ->
        val haptic = LocalHapticFeedback.current

        if (list == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (goals.isEmpty() && uiState.localSearchQuery.isBlank()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("У цьому списку ще немає цілей.")
            }
        } else {
            LazyColumn(
                state = listState, // Використовуємо оригінальний listState
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                itemsIndexed(
                    goals,
                    key = { _, item -> item.instanceId }
                ) { index, goalWithInstanceInfo ->
                    // Використовуйте ReorderableItem, якщо він доступний
                    // Перевірте документацію, чи він ще підтримується
                    // Якщо ні, вам потрібно використовувати модифікатор, але без isItemDragging
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = goalWithInstanceInfo.instanceId,
                        enabled = !isSelectionModeActive
                    ) { isDragging -> // Цей параметр містить стан перетягування
                        // Весь вміст елемента
                        val elevation = animateDpAsState(
                            if (isDragging) 8.dp else 0.dp,
                            label = "elevationAnimation"
                        )
                        val scale by animateFloatAsState(
                            if (isDragging) 1.02f else 1.0f,
                            label = "scaleAnimation"
                        )
                        val isSelected = goalWithInstanceInfo.instanceId in uiState.selectedInstanceIds

                        LaunchedEffect(isDragging) {
                            if (isDragging) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        val isHighlighted by remember(uiState.goalToHighlight) {
                            derivedStateOf { uiState.goalToHighlight == goalWithInstanceInfo.goal.id }
                        }
                        val associatedLists = associatedListsMap.getOrDefault(goalWithInstanceInfo.goal.id, emptyList())
                        val itemBackgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        SwipeableGoalItem(
                            modifier = Modifier
                                .scale(scale)
                                .animateItem(
                                )
                                .shadow(elevation.value, RoundedCornerShape(8.dp)

                                ),
                            resetTrigger = uiState.resetTriggers.getOrDefault(goalWithInstanceInfo.instanceId, 0),
                            goalWithInstance = goalWithInstanceInfo,
                            isHighlighted = isHighlighted,

                            isDragging = isDragging, // Передаємо isDragging безпосередньо з лямбди
                            associatedLists = associatedLists,
                            obsidianVaultName = obsidianVaultName,
                            onEdit = { viewModel.onEditGoal(goalWithInstanceInfo) },
                            onDelete = { viewModel.deleteGoal(goalWithInstanceInfo) },
                            onMore = { viewModel.onGoalActionInitiated(goalWithInstanceInfo) },
                            backgroundColor = itemBackgroundColor,
                            onToggle = { viewModel.toggleGoalCompleted(goalWithInstanceInfo.goal) },
                            onTagClick = { tag: String -> viewModel.onTagClicked(tag) },
                            onAssociatedListClick = { listId: String -> viewModel.onAssociatedListClicked(listId) },
                            onItemClick = { viewModel.onGoalClick(goalWithInstanceInfo) },
                            onLongClick = { viewModel.onGoalLongClick(goalWithInstanceInfo.instanceId) },
                            dragHandleModifier = if (!isSelectionModeActive) {
                                Modifier.longPressDraggableHandle()
                            } else {
                                Modifier // В режимі вибору ручка неактивна
                            }                        // Не передавайте dragHandle або indicator, якщо вони не підтримуються
                        )
                    }
                }
            }
        }
    }

    if (showInputModeDialog) {
        InputModeDialog(
            onDismiss = { viewModel.onDismissInputModeDialog() },
            onSelect = { viewModel.onInputModeSelected(it) }
        )
    }

    when (val state = goalActionState) {
        is GoalActionDialogState.Hidden -> {}
        is GoalActionDialogState.AwaitingActionChoice -> {
            GoalActionChoiceDialog(
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onActionSelected = { viewModel.onGoalActionSelected(it) }
            )
        }
        is GoalActionDialogState.AwaitingListChoice -> {
            ListChooserDialog(
                topLevelLists = listHierarchy.topLevelLists,
                childMap = listHierarchy.childMap,
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onConfirm = { viewModel.confirmGoalAction(it) }
            )
        }
    }
}