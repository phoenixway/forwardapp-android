@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt

// Стани для свайпу
enum class SwipeAction {
    Hidden,
    Revealed,
    Delete
}

@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.filteredGoals.collectAsState()
    val goalActionState by viewModel.goalActionDialogState.collectAsState()
    val showInputModeDialog by viewModel.showInputModeDialog.collectAsState()
    val associatedListsMap by viewModel.associatedListsMap.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var resetTriggers by remember { mutableStateOf(mapOf<String, Int>()) }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.moveGoal(from.index, to.index) }
    )

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
                    val currentTrigger = resetTriggers[event.instanceId] ?: 0
                    resetTriggers = resetTriggers + (event.instanceId to currentTrigger + 1)
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    LaunchedEffect(uiState.goalToHighlight) {
        val goalId = uiState.goalToHighlight
        if (goalId != null) {
            val index = goals.indexOfFirst { it.goal.id == goalId }
            if (index != -1) {
                reorderableState.listState.animateScrollToItem(index)
                viewModel.onHighlightShown()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.goalList?.name ?: "Завантаження...") },
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
                onModeChangeRequest = { viewModel.onInputModeChangeRequest() },
                onTextChange = { viewModel.onInputTextChanged(it) },
                onSubmit = { viewModel.submitInput(it) }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (goals.isEmpty() && uiState.localSearchQuery.isBlank()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("У цьому списку ще немає цілей.")
            }
        } else {
            LazyColumn(
                state = reorderableState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .reorderable(reorderableState)
                    .detectReorderAfterLongPress(reorderableState)
            ) {
                items(goals, key = { it.instanceId }) { goalWithInstance ->
                    ReorderableItem(reorderableState, key = goalWithInstance.instanceId) {
                        val isHighlighted by remember(uiState.goalToHighlight) {
                            derivedStateOf { uiState.goalToHighlight == goalWithInstance.goal.id }
                        }
                        val trigger = resetTriggers[goalWithInstance.instanceId] ?: 0
                        val associatedLists = associatedListsMap[goalWithInstance.goal.id] ?: emptyList()

                        SwipeableGoalItem(
                            resetTrigger = trigger,
                            goalWithInstance = goalWithInstance,
                            isHighlighted = isHighlighted,
                            associatedLists = associatedLists,
                            onEdit = { viewModel.onEditGoal(goalWithInstance) },
                            onDelete = { viewModel.deleteGoal(goalWithInstance) },
                            onMore = { viewModel.onGoalActionInitiated(goalWithInstance) },
                            onToggle = { viewModel.toggleGoalCompleted(goalWithInstance.goal) },
                            onTagClick = { tag -> viewModel.onTagClicked(tag) },
                            onAssociatedListClick = { listId -> viewModel.onAssociatedListClicked(listId) },
                            onResetSwipe = { viewModel.resetSwipe(goalWithInstance.instanceId) }
                        )
                    }
                }
            }
        }
    }

    // Діалоги, що використовуються на цьому екрані
    if (showInputModeDialog) {
        InputModeDialog(
            onDismiss = { viewModel.onDismissInputModeDialog() },
            onSelect = { viewModel.onInputModeSelected(it) }
        )
    }

    val hierarchy by viewModel.listHierarchy.collectAsState()

    when (goalActionState) {
        is GoalActionDialogState.Hidden -> {}
        is GoalActionDialogState.AwaitingActionChoice -> {
            GoalActionChoiceDialog(
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onActionSelected = { viewModel.onActionSelected(it) }
            )
        }
        is GoalActionDialogState.AwaitingListChoice -> {
            ListChooserDialog(
                topLevelLists = hierarchy.topLevelLists,
                childMap = hierarchy.childMap,
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onConfirm = { listId -> viewModel.confirmGoalAction(listId) }
            )
        }
    }
}

@Composable
fun SwipeableGoalItem(
    resetTrigger: Int,
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    associatedLists: List<GoalList>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    onResetSwipe: () -> Unit
) {
    val density = LocalDensity.current

    val revealPx = with(density) { -180.dp.toPx() }
    val deletePx = with(density) { 120.dp.toPx() }

    val fullAnchors = remember {
        DraggableAnchors {
            SwipeAction.Hidden at 0f
            SwipeAction.Revealed at revealPx
            SwipeAction.Delete at deletePx
        }
    }

    val state = remember(key1 = resetTrigger) {
        AnchoredDraggableState(
            initialValue = SwipeAction.Hidden,
            anchors = fullAnchors,
            positionalThreshold = { totalDistance -> totalDistance * 0.6f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(durationMillis = 300)
        )
    }

    LaunchedEffect(state.currentValue) {
        val newAnchors = when (state.currentValue) {
            SwipeAction.Revealed -> DraggableAnchors {
                SwipeAction.Hidden at 0f
                SwipeAction.Revealed at revealPx
            }
            SwipeAction.Delete -> DraggableAnchors {
                SwipeAction.Hidden at 0f
                SwipeAction.Delete at deletePx
            }
            SwipeAction.Hidden -> fullAnchors
        }
        state.updateAnchors(newAnchors)
    }

    LaunchedEffect(state.targetValue) {
        if (state.targetValue == SwipeAction.Delete) {
            onDelete()
        }
    }

    val itemBackgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 1500), label = "ItemBackgroundColor"
    )

    Box(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMore, modifier = Modifier.fillMaxHeight().width(90.dp).background(MaterialTheme.colorScheme.secondary)) { Icon(Icons.Default.SwapVert, "Дії", tint = Color.White) }
                IconButton(onClick = onEdit, modifier = Modifier.fillMaxHeight().width(90.dp).background(Color.Gray)) { Icon(Icons.Default.Edit, "Редагувати", tint = Color.White) }
            }
            Box(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(120.dp).background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Видалити", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Surface(
            modifier = Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
            color = itemBackgroundColor,
            onClick = {
                if (state.currentValue != SwipeAction.Hidden) {
                    onResetSwipe()
                } else {
                    onMore()
                }
            }
        ) {
            GoalItem(
                goal = goalWithInstance.goal,
                associatedLists = associatedLists,
                onToggle = onToggle,
                onItemClick = {
                    if (state.currentValue != SwipeAction.Hidden) {
                        onResetSwipe()
                    } else {
                        onMore()
                    }
                },
                onTagClick = onTagClick,
                onAssociatedListClick = onAssociatedListClick,
                backgroundColor = Color.Transparent,
                modifier = Modifier
            )
        }
    }
}

// Решта файлу залишається без змін (GoalInputBar, Dialogs)
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
                Text("Create instance in another list", modifier = Modifier.fillMaxWidth().clickable { onActionSelected(GoalActionType.CreateInstance) }.padding(16.dp))
                HorizontalDivider()
                Text("Move instance to another list", modifier = Modifier.fillMaxWidth().clickable { onActionSelected(GoalActionType.MoveInstance) }.padding(16.dp))
                HorizontalDivider()
                Text("Copy goal to another list", modifier = Modifier.fillMaxWidth().clickable { onActionSelected(GoalActionType.CopyGoal) }.padding(16.dp))
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
                Text("Add Goal", modifier = Modifier.fillMaxWidth().clickable { onSelect(InputMode.AddGoal) }.padding(16.dp))
                HorizontalDivider()
                Text("Search in List", modifier = Modifier.fillMaxWidth().clickable { onSelect(InputMode.SearchInList) }.padding(16.dp))
                HorizontalDivider()
                Text("Search Globally", modifier = Modifier.fillMaxWidth().clickable { onSelect(InputMode.SearchGlobal) }.padding(16.dp))
            }
        }
    }
}