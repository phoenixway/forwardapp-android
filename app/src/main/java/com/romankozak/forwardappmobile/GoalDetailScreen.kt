// ВИПРАВЛЕННЯ: Анотація застосовується до всього файлу, вирішуючи проблему доступу.
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.State

// Стани для свайпу
enum class SwipeAction {
    Hidden,
    Revealed
}

@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.filteredGoals.collectAsState()
    val goalToEdit by viewModel.goalToEdit.collectAsState()
    val goalActionState by viewModel.goalActionDialogState.collectAsState()
    val showInputModeDialog by viewModel.showInputModeDialog.collectAsState()

    val lazyListState = rememberLazyListState()
    // ЗМІНА: Використовуємо тригери для перекомпозиції замість контролерів
    val resetTriggers = remember { mutableMapOf<String, MutableState<Int>>() }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is UiEvent.ResetSwipeState -> {
                    // ЗМІНА: Інкрементуємо тригер, щоб викликати перекомпозицію стану свайпу
                    val trigger = resetTriggers.getOrPut(event.instanceId) { mutableStateOf(0) }
                    trigger.value++
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
                lazyListState.animateScrollToItem(index)
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
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                items(goals, key = { it.instanceId }) { goalWithInstance ->
                    val isHighlighted by remember(uiState.goalToHighlight) {
                        derivedStateOf { uiState.goalToHighlight == goalWithInstance.goal.id }
                    }
                    SwipeableGoalItem(
                        goalWithInstance = goalWithInstance,
                        isHighlighted = isHighlighted,
                        // ЗМІНА: Передаємо тригер для скидання
                        resetTrigger = resetTriggers.getOrPut(goalWithInstance.instanceId) { mutableStateOf(0) },
                        onEdit = { viewModel.onEditGoal(goalWithInstance) },
                        onDelete = { viewModel.deleteGoal(goalWithInstance) },
                        onMore = { viewModel.onGoalActionInitiated(goalWithInstance) },
                        onToggle = { viewModel.toggleGoalCompleted(goalWithInstance.goal) },
                        onTagClick = { tag -> viewModel.onTagClicked(tag) }
                    )
                }
            }
        }
    }

    if (goalToEdit != null) {
        EditGoalDialog(
            goal = goalToEdit!!,
            onDismiss = { viewModel.onDismissEditGoalDialog() },
            onConfirm = { newText -> viewModel.updateGoalText(goalToEdit!!, newText) }
        )
    }

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
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    resetTrigger: State<Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val density = LocalDensity.current

    val anchors = remember {
        val revealPx = with(density) { -240.dp.toPx() } // 3 actions * 80.dp
        DraggableAnchors {
            SwipeAction.Hidden at 0f
            SwipeAction.Revealed at revealPx
        }
    }

    val state = remember(anchors, resetTrigger.value) {
        AnchoredDraggableState(
            initialValue = SwipeAction.Hidden,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(durationMillis = 300)
        )
    }

    val itemBackgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 1500), label = "ItemBackgroundColor"
    )

    // Box автоматично приймає висоту свого найвищого елемента (в нашому випадку - Surface з GoalItem)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Запобігає появі проміжків між елементами
    ) {
        // Фон з кнопками дій.
        // `fillMaxHeight()` змушує цей Row розтягнутися на всю висоту Box,
        // тобто він завжди буде такої ж висоти, як і GoalItem.
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End
        ) {
            // Кожна кнопка тепер також заповнює всю висоту і має фіксовану ширину.
            IconButton(
                onClick = onMore,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            ) { Icon(Icons.Default.MoreVert, "Більше дій", tint = Color.White) }

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(Color.Gray)
            ) { Icon(Icons.Default.Edit, "Редагувати", tint = Color.White) }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(MaterialTheme.colorScheme.error)
            ) { Icon(Icons.Default.Delete, "Видалити", tint = Color.White) }
        }

        // Основний контент, що рухається. Він визначає загальну висоту.
        Surface(
            modifier = Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
            color = itemBackgroundColor
        ) {
            GoalItem(
                goal = goalWithInstance.goal,
                onToggle = onToggle,
                onItemClick = {},
                onTagClick = onTagClick,
                backgroundColor = Color.Transparent, // Surface тепер контролює колір
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

    LaunchedEffect(inputMode) {
        if (inputMode != InputMode.AddGoal) {
            focusRequester.requestFocus()
        }
    }

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onModeChangeRequest) {
                Icon(
                    imageVector = when (inputMode) {
                        InputMode.AddGoal -> Icons.Default.Add
                        InputMode.SearchInList -> Icons.Default.Search
                        InputMode.SearchGlobal -> Icons.Default.Search
                    },
                    contentDescription = "Change Input Mode"
                )
            }
            OutlinedTextField(
                value = textState,
                onValueChange = {
                    textState = it
                    onTextChange(it.text)
                },
                placeholder = {
                    Text(
                        when (inputMode) {
                            InputMode.AddGoal -> "Add a new goal..."
                            InputMode.SearchInList -> "Search in this list..."
                            InputMode.SearchGlobal -> "Search globally..."
                        }
                    )
                },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                trailingIcon = {
                    if (textState.text.isNotEmpty()) {
                        IconButton(onClick = {
                            onSubmit(textState.text)
                            textState = TextFieldValue("")
                        }) {
                            Icon(Icons.Default.ArrowUpward, "Submit")
                        }
                    }
                }
            )
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