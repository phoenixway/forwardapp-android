// DayPlanScreen.kt - Updated with Drag-and-Drop functionality
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemType
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Помилка завантаження",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Спробувати ще раз")
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTask>, // Це початковий список з ViewModel
    dayPlan: DayPlan?,
    onToggleTask: (String) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onTasksReordered: (List<DayTask>) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Єдиний локальний стан, який змінюється лише тут
    var internalTasks by remember { mutableStateOf(tasks) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Миттєво оновлюємо локальний стан
        internalTasks = internalTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }

        // Надсилаємо оновлений список у ViewModel
        onTasksReordered(internalTasks)

        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
        state = lazyListState,

        ) {
        items(
            items = internalTasks,
            key = { task -> task.id }
        ) { task ->
            ReorderableItem(
                reorderableLazyListState,
                key = task.id
            ) { isDragging ->

                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                Surface(shadowElevation = elevation) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        IconButton(
                            modifier = Modifier.draggableHandle(
                                onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                onDragStopped = { hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                            ),
                            onClick = {},
                        ) {
                            Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyTasksState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Checklist,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Завдань ще немає",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Натисніть кнопку '+', щоб додати перше завдання",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompactDayPlanHeader(
    dayPlan: DayPlan?,
    completedTasks: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val formattedDate = remember(dayPlan?.date) {
        dayPlan?.date?.let { dateMillis ->
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk", "UA"))
            Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } ?: "План дня"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = formattedDate.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$completedTasks з $totalTasks виконано",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalTasks > 0) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (completedTasks == totalTasks && totalTasks > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Вітаємо! Всі завдання виконані!",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Convert DayTask to Goal-like structure for reusing GoalItem component
private fun DayTask.toGoal(): Goal {
    return Goal(
        id = this.id,
        text = this.title,
        description = this.description,
        completed = this.completed,
        scoringStatus = ScoringStatus.NOT_ASSESSED,
        displayScore = 0,
        reminderTime = this.dueTime,
        relatedLinks = null,
        createdAt = this.createdAt,
        updatedAt = this.createdAt
    )
}

fun DayTask.toListItem(): ListItem {
    return ListItem(
        id = this.id,
        listId = this.projectId ?: this.dayPlanId,
        itemType = this.taskType ?: ListItemType.GOAL,
        entityId = this.entityId ?: this.goalId ?: this.id,
        order = this.order
    )
}
/*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskGoalItem(
    task: DayTask,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    isDragging: Boolean = false,
    reorderableState: ReorderableLazyListState? = null,
    modifier: Modifier = Modifier
) {
    val goalContent = ListItemContent.GoalItem(
        goal = task.toGoal(),
        item = task.toListItem()
    )

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        // Your existing card content

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            if (reorderableState != null) {
                Icon(
                    Icons.Outlined.DragHandle,
                    contentDescription = "Перетягнути для зміни порядку",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }


            // Goal item content
            Box(modifier = Modifier.weight(1f)) {
                GoalItem(
                    goalContent = goalContent,
                    onCheckedChange = { onToggle() },
                    onClick = { */
/* Handle click if needed *//*
 },
                    onLongClick = onLongPress,
                    isSelected = false,
                    modifier = Modifier.fillMaxWidth(),
                    currentTimeMillis = System.currentTimeMillis()
                )
            }
        }
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanScreen(
    dayPlanId: String,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }

    LaunchedEffect(dayPlanId) {
        viewModel.loadDataForPlan(dayPlanId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.openAddTaskDialog()
                },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Додати завдання"
                    )
                },
                text = { Text("Додати завдання") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.error != null && uiState.tasks.isEmpty() -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadDataForPlan(dayPlanId) }
                    )
                }

                else -> {
                    val uiState by viewModel.uiState.collectAsState()

                    // 2. Отримуємо список завдань з об'єкта uiState
                    val tasks = uiState.tasks
                    TaskList(
                        tasks = tasks,
                        dayPlan = uiState.dayPlan,
                        onToggleTask = { taskId ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTaskCompletion(taskId)
                        },
                        onTaskLongPress = { task ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.selectTask(task)
                        },
                        onTasksReordered = { reorderedList ->
                            // Переконайтесь, що у вас є dayPlanId для цього виклику
                            uiState.dayPlan?.let { dayPlan ->
                                viewModel.updateTasksOrder(dayPlan.id, reorderedList)
                            }
                        },
                    )
                }
            }
        }
    }

    if (isAddTaskDialogOpen) {
        AddTaskDialog(
            onDismissRequest = viewModel::dismissAddTaskDialog,
            onConfirm = { title, description, duration, priority ->
                viewModel.addTask(dayPlanId, title, description, duration, priority)
            },
            initialPriority = TaskPriority.MEDIUM
        )
    }

    selectedTask?.let { task ->
        TaskOptionsBottomSheet(
            task = task,
            onDismiss = viewModel::clearSelectedTask,
            onEdit = { /* TODO: viewModel.openEditTaskDialog(it) */ },
            onDelete = { taskToDelete ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteTask(dayPlanId, taskToDelete.id)
            },
            onSetReminder = { /* TODO: viewModel.setTaskReminder(it) */ }
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Завантаження плану...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// DayPlanScreen.kt - TaskGoalItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskGoalItem(
    task: DayTask,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    isDragging: Boolean = false,
    reorderableState: ReorderableLazyListState? = null,
    modifier: Modifier = Modifier
) {
    val goalContent = ListItemContent.GoalItem(
        goal = task.toGoal(),
        item = task.toListItem()
    )

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Контейнер для чекбокса і GoalItem
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Мануально доданий чекбокс
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Goal item content
                Box(modifier = Modifier.weight(1f)) {
                    GoalItem(
                        goalContent = goalContent,
                        onCheckedChange = {}, // Залишаємо порожнім, бо чекбокс вже є
                        onClick = { /* Handle click if needed */ },
                        onLongClick = onLongPress,
                        isSelected = false,
                        modifier = Modifier.fillMaxWidth(),
                        currentTimeMillis = System.currentTimeMillis()
                    )
                }
            }

            // Ручка перетягування та меню опцій
            if (reorderableState != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DragHandle,
                        contentDescription = "Перетягнути для зміни порядку",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                        ,                        tint = MaterialTheme.colorScheme.outline
                    )

                    IconButton(
                        onClick = onLongPress,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Більше опцій",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}