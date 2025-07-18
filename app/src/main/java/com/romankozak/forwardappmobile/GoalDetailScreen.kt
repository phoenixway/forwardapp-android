@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt

enum class SwipeAction {
    Hidden,
    Revealed,
    Delete
}

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

    val reorderableState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to ->
            viewModel.moveGoal(from.index, to.index)
        }
    )

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
                onModeChangeRequest = { viewModel.onInputModeChangeRequest() },
                onTextChange = { viewModel.onInputTextChanged(it) },
                onSubmit = { viewModel.submitInput(it) }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (goals.isEmpty() && uiState.localSearchQuery.isBlank()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("У цьому списку ще немає цілей.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .reorderable(reorderableState)
            ) {
                items(goals, key = { it.instanceId }) { goalWithInstance ->
                    ReorderableItem(reorderableState, key = goalWithInstance.instanceId) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp, label = "elevationAnimation")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation.value),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)
                                .detectReorderAfterLongPress(reorderableState)
                            ) {
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
                                    obsidianVaultName = obsidianVaultName,
                                    onEdit = { viewModel.onEditGoal(goalWithInstance) },
                                    onDelete = { viewModel.deleteGoal(goalWithInstance) },
                                    onMore = { viewModel.onGoalActionInitiated(goalWithInstance) },
                                    onToggle = { viewModel.toggleGoalCompleted(goalWithInstance.goal) },
                                    onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                    onAssociatedListClick = { listId -> viewModel.onAssociatedListClicked(listId) }
                                )
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
fun SwipeableGoalItem(
    resetTrigger: Int,
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val revealPx = with(density) { (180.dp * -1).toPx() }
    val deletePx = with(density) { 120.dp.toPx() }

    val state = remember(key1 = resetTrigger) {
        AnchoredDraggableState(
            initialValue = SwipeAction.Hidden,
            anchors = DraggableAnchors {
                SwipeAction.Hidden at 0f
                SwipeAction.Revealed at revealPx
                SwipeAction.Delete at deletePx
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.6f },
            velocityThreshold = { Float.POSITIVE_INFINITY },
            animationSpec = tween(durationMillis = 300)
        )
    }

    LaunchedEffect(state.targetValue) {
        if (state.targetValue == SwipeAction.Delete) {
            onDelete()
        }
    }

    val itemBackgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 1500), label = "ItemBackgroundColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBackgroundColor)
    ) {
        Box(
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, "Видалити", tint = Color.White)
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.fillMaxHeight().width(90.dp).background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.SwapVert, "Дії", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        onEdit()
                        coroutineScope.launch { state.settle(0f) }
                    },
                    modifier = Modifier.fillMaxHeight().width(90.dp).background(Color.Gray)
                ) {
                    Icon(Icons.Default.Edit, "Редагувати", tint = Color.White)
                }
            }
        }

        val clampedOffset = state.requireOffset().coerceIn(revealPx, deletePx)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(clampedOffset.roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
            onClick = {
                if (state.currentValue != SwipeAction.Hidden) {
                    coroutineScope.launch { state.settle(0f) }
                } else {
                    onMore()
                }
            }
        ) {
            GoalItem(
                goal = goalWithInstance.goal,
                associatedLists = associatedLists,
                obsidianVaultName = obsidianVaultName,
                onToggle = onToggle,
                onItemClick = {},
                onTagClick = onTagClick,
                onAssociatedListClick = onAssociatedListClick,
                backgroundColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
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