@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.filteredGoals.collectAsState()
    val goalActionState by viewModel.goalActionDialogState.collectAsState()
    val showInputModeDialog by viewModel.showInputModeDialog.collectAsState()
    val associatedListsMap by viewModel.associatedListsMap.collectAsState()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val listHierarchy by viewModel.listHierarchyForChooser.collectAsState()
    val list by viewModel.goalList.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var resetTriggers by remember { mutableStateOf(mapOf<String, Int>()) }

    val listState = rememberLazyListState()
    val dragAndDropState = rememberDragAndDropState<GoalWithInstanceInfo>()

    val isLoading = list == null

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
                is UiEvent.ResetSwipeState -> {
                    val currentTrigger = resetTriggers.getOrDefault(event.instanceId, 0)
                    resetTriggers = resetTriggers + (event.instanceId to currentTrigger + 1)
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route)
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
        if (goalId != null) {
            val index = goals.indexOfFirst { it.goal.id == goalId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onHighlightShown()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Завантаження...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            GoalInputBar(
                inputMode = uiState.inputMode,
                onModeChangeRequest = { viewModel.onModeChangeRequest() },
                onTextChange = { viewModel.onInputTextChanged(it) },
                onSubmit = { viewModel.submitInput(it) }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            goals.isEmpty() && uiState.localSearchQuery.isBlank() -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("У цьому списку ще немає цілей.")
                }
            }
            else -> {
                var scrollDirection by remember { mutableStateOf(0) }

                LaunchedEffect(scrollDirection) {
                    if (scrollDirection != 0) {
                        while (true) {
                            listState.scrollBy(20f * scrollDirection)
                            delay(16)
                        }
                    }
                }

                LaunchedEffect(dragAndDropState.draggedItem) {
                    if (dragAndDropState.draggedItem == null) {
                        scrollDirection = 0
                    }
                }

                DragAndDropContainer(
                    state = dragAndDropState
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        itemsIndexed(goals, key = { _, it -> it.instanceId }) { _, goalWithInstanceInfo ->
                            val isCurrentlyDragging = dragAndDropState.draggedItem?.key == goalWithInstanceInfo.instanceId

                            val elevation = animateDpAsState(if (isCurrentlyDragging) 8.dp else 0.dp, label = "elevationAnimation")
                            val scale by animateFloatAsState(if (isCurrentlyDragging) 1.02f else 1.0f, label = "scaleAnimation")

                            val haptic = LocalHapticFeedback.current
                            LaunchedEffect(isCurrentlyDragging) {
                                if (isCurrentlyDragging) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dropTarget(
                                        state = dragAndDropState,
                                        key = goalWithInstanceInfo.instanceId,
                                        onDrop = { draggedItemState ->
                                            scrollDirection = 0
                                            val draggedData = draggedItemState.data
                                            val droppedOnData = goalWithInstanceInfo

                                            val fromIndex = goals.indexOfFirst { it.instanceId == draggedData.instanceId }
                                            val toIndex = goals.indexOfFirst { it.instanceId == droppedOnData.instanceId }

                                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                viewModel.moveGoal(fromIndex, toIndex)
                                                                           }
                                        },
                                        onDragEnter = {
                                            val hoveredIndex = goals.indexOfFirst { it.instanceId == goalWithInstanceInfo.instanceId }
                                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
                                                val firstVisibleIndex = visibleItems.first().index
                                                val lastVisibleIndex = visibleItems.last().index

                                                scrollDirection = when (hoveredIndex) {
                                                    firstVisibleIndex -> -1
                                                    lastVisibleIndex -> 1
                                                    else -> 0
                                                }
                                            }
                                        },
                                        onDragExit = {
                                            scrollDirection = 0
                                        }
                                    )
                            ) {
                                // ✨ ВИПРАВЛЕНО: Замінено isDragging на draggedItem != null
                                if (dragAndDropState.draggedItem != null && dragAndDropState.hoveredDropTargetKey == goalWithInstanceInfo.instanceId && dragAndDropState.draggedItem?.key != goalWithInstanceInfo.instanceId) {
                                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    val isHighlighted by remember(uiState.goalToHighlight) {
                                        derivedStateOf { uiState.goalToHighlight == goalWithInstanceInfo.goal.id }
                                    }
                                    val associatedLists = associatedListsMap.getOrDefault(goalWithInstanceInfo.goal.id, emptyList())

                                    SwipeableGoalItem(
                                        modifier = Modifier
                                            .scale(scale)
                                            .shadow(elevation.value, RoundedCornerShape(8.dp)),
                                        resetTrigger = resetTriggers.getOrDefault(goalWithInstanceInfo.instanceId, 0),
                                        goalWithInstance = goalWithInstanceInfo,
                                        isHighlighted = isHighlighted,
                                        isDragging = isCurrentlyDragging,
                                        associatedLists = associatedLists,
                                        obsidianVaultName = obsidianVaultName,
                                        onEdit = { viewModel.onEditGoal(goalWithInstanceInfo) },
                                        onDelete = { viewModel.deleteGoal(goalWithInstanceInfo) },
                                        onMore = { viewModel.onGoalActionInitiated(goalWithInstanceInfo) },
                                        onToggle = { viewModel.toggleGoalCompleted(goalWithInstanceInfo.goal) },
                                        onTagClick = { tag: String -> viewModel.onTagClicked(tag) },
                                        onAssociatedListClick = { listId: String -> viewModel.onAssociatedListClicked(listId) },
                                        dragHandle = {
                                            DraggableItem(
                                                state = dragAndDropState,
                                                key = goalWithInstanceInfo.instanceId,
                                                data = goalWithInstanceInfo,
                                                dropAnimationSpec = snap()
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(48.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DragHandle,
                                                        contentDescription = "Ручка для перетягування",
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            // ✨ ВИПРАВЛЕНО: Замінено isDragging на draggedItem != null
                            val isTargetedAtEnd = dragAndDropState.draggedItem != null && dragAndDropState.hoveredDropTargetKey == null
                            if(isTargetedAtEnd) {
                                val draggedItemIndex = goals.indexOfFirst { it.instanceId == dragAndDropState.draggedItem?.key }
                                if(draggedItemIndex != -1 && draggedItemIndex != goals.lastIndex) {
                                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                }
                            }
                        }
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

@Composable
fun GoalInputBar(
    inputMode: InputMode,
    onModeChangeRequest: () -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    var textState by remember(inputMode) { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(inputMode) {
        if (inputMode != InputMode.AddGoal) {
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 8.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onModeChangeRequest) {
                    Icon(
                        imageVector = when (inputMode) {
                            InputMode.AddGoal -> Icons.Default.Add
                            InputMode.SearchInList, InputMode.SearchGlobal -> Icons.Default.Search
                        },
                        contentDescription = "Change Input Mode"
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = textState,
                        onValueChange = {
                            textState = it
                            onTextChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textState.text.isNotBlank()) {
                                onSubmit(textState.text)
                                textState = TextFieldValue("")
                                focusManager.clearFocus()
                            }
                        }),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    if (textState.text.isEmpty()) {
                        Text(
                            text = when (inputMode) {
                                InputMode.AddGoal -> "Додати нову ціль..."
                                InputMode.SearchInList -> "Пошук в цьому списку..."
                                InputMode.SearchGlobal -> "Глобальний пошук..."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AnimatedVisibility(
                    visible = textState.text.isNotBlank(),
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
                ) {
                    FilledTonalIconButton(onClick = {
                        onSubmit(textState.text)
                        textState = TextFieldValue("")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Submit")
                    }
                }
            }
        }
    }
}


@Composable
fun GoalActionChoiceDialog(onDismiss: () -> Unit, onActionSelected: (GoalActionType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text("Create instance in another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.CreateInstance) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Move instance to another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.MoveInstance) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Copy goal to another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.CopyGoal) }
                    .padding(16.dp))
            }
        }
    }
}

@Composable
fun ListChooserDialog(
    topLevelLists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column {
                Text("Choose a list", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    fun renderList(list: GoalList, level: Int) {
                        item(key = list.id) {
                            Text(
                                text = list.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(list.id) }
                                    .padding(start = (level * 24 + 16).dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                            )
                        }
                        childMap[list.id]?.forEach { child ->
                            renderList(child, level + 1)
                        }
                    }
                    topLevelLists.forEach { renderList(it, 0) }
                }
            }
        }
    }
}

@Composable
fun InputModeDialog(onDismiss: () -> Unit, onSelect: (InputMode) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text("Add Goal", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(InputMode.AddGoal) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Search in List", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(InputMode.SearchInList) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Search Globally", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(InputMode.SearchGlobal) }
                    .padding(16.dp))
            }
        }
    }
}