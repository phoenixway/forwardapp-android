// DayPlanScreen.kt - Updated with reminder dialog fix
package com.romankozak.forwardappmobile.ui.screens.dayplan.daymanagement

import TaskList
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
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
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.AddTaskDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.EditTaskDialog
import sh.calvin.reorderable.ReorderableLazyListState
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
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateToPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Попередній день"
                )
            }
            Text(
                text = formattedDate.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNavigateToNextDay, enabled = isNextDayNavigationEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Наступний день",
                    tint = if (isNextDayNavigationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
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

@Composable
fun DayPlanScreen(
    dayPlanId: String,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProject: (projectId: String) -> Unit,
    onNavigateToBacklog: (task: DayTask) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val isEditTaskDialogOpen by viewModel.isEditTaskDialogOpen.collectAsState()
    var showReminderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.tasksUpdated.collect {
            viewModel.loadDataForPlan(dayPlanId)
        }
    }
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
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.openAddTaskDialog()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Додати завдання")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null && uiState.tasks.isEmpty() -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadDataForPlan(dayPlanId) }
                    )
                }
                else -> {
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
                            uiState.dayPlan?.let { dayPlan ->
                                viewModel.updateTasksOrder(dayPlan.id, reorderedList)
                            }
                        },
                        onNavigateToPreviousDay = { viewModel.navigateToPreviousDay() },
                        onNavigateToNextDay = { viewModel.navigateToNextDay() },
                        isNextDayNavigationEnabled = !uiState.isToday,
                        onSublistClick = onNavigateToProject,
                        modifier = Modifier.fillMaxSize()
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
            onEdit = {
                viewModel.openEditTaskDialog()
                // Не очищуємо тут, щоб діалог редагування мав доступ до task
            },
            onDelete = { taskToDelete ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteTask(dayPlanId, taskToDelete.id)
            },
            onSetReminder = { showReminderDialog = true },
            showAddToTodayOption = !uiState.isToday,
            onAddToToday = {
                viewModel.copyTaskToTodaysPlan(task)
            },
            onShowInBacklog = {
                onNavigateToBacklog(task)
            }
        )
    }

    if (isEditTaskDialogOpen && selectedTask != null) {
        EditTaskDialog(
            task = selectedTask!!,
            onDismissRequest = {
                viewModel.dismissEditTaskDialog()
                viewModel.clearSelectedTask() // Очищуємо після закриття
            },
            onConfirm = { title, description, duration, priority ->
                viewModel.updateTask(
                    taskId = selectedTask!!.id,
                    title = title,
                    description = description,
                    duration = duration,
                    priority = priority
                )
            },
            onDelete = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteTask(dayPlanId, selectedTask!!.id)
                viewModel.dismissEditTaskDialog()
            }
        )
    }

    // --- ЗМІНЕНО: Додано логіку очищення стану в колбеки діалогу ---
    if (showReminderDialog && selectedTask != null) {
        ReminderPickerDialog(
            onDismiss = {
                showReminderDialog = false
                viewModel.clearSelectedTask() // Очищуємо стан
            },
            onSetReminder = { reminderTime ->
                viewModel.setTaskReminder(selectedTask!!.id, reminderTime)
                showReminderDialog = false
                viewModel.clearSelectedTask() // Очищуємо стан
            },
            onClearReminder = {
                viewModel.clearTaskReminder(selectedTask!!.id)
                showReminderDialog = false
                viewModel.clearSelectedTask() // Очищуємо стан
            },
            currentReminderTime = selectedTask!!.reminderTime
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    val goalContent = ListItemContent.GoalItem(goal = task.toGoal(), item = task.toListItem())
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    GoalItem(
                        goalContent = goalContent,
                        onCheckedChange = {},
                        onClick = { /* Handle click if needed */ },
                        onLongClick = onLongPress,
                        isSelected = false,
                        modifier = Modifier.fillMaxWidth(),
                        currentTimeMillis = System.currentTimeMillis()
                    )
                }
            }
            if (reorderableState != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DragHandle,
                        contentDescription = "Перетягнути для зміни порядку",
                        modifier = Modifier.padding(end = 8.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    IconButton(onClick = onLongPress, modifier = Modifier.size(48.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
    task: DayTask,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (DayTask) -> Unit,
    onSetReminder: () -> Unit, // Змінено: більше не приймає task
    showAddToTodayOption: Boolean,
    onAddToToday: () -> Unit,
    onShowInBacklog: (DayTask) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            ListItem(
                headlineContent = { Text("Редагувати") },
                leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                // --- ЗМІНЕНО: Не викликаємо onDismiss, щоб зберегти selectedTask ---
                modifier = Modifier.clickable {
                    onEdit()
                    // onDismiss() - видалено
                }
            )
            ListItem(
                headlineContent = { Text("Встановити нагадування") },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                // --- ЗМІНЕНО: Не викликаємо onDismiss, щоб зберегти selectedTask ---
                modifier = Modifier.clickable {
                    onSetReminder()
                    // onDismiss() - видалено
                }
            )
            if (showAddToTodayOption) {
                ListItem(
                    headlineContent = { Text("Додати в план на сьогодні") },
                    leadingContent = { Icon(Icons.Outlined.Today, contentDescription = null) },
                    modifier = Modifier.clickable { onAddToToday(); onDismiss() }
                )
            }
            if (task.projectId != null || task.goalId != null) {
                ListItem(
                    headlineContent = { Text("Показати в беклозі проекту") },
                    leadingContent = { Icon(Icons.Outlined.ListAlt, contentDescription = null) },
                    modifier = Modifier.clickable { onShowInBacklog(task); onDismiss() }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDelete(task); onDismiss() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}