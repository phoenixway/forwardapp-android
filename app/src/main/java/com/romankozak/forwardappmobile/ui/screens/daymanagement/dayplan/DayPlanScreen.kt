package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan
// /home/romankozak/studio/public/forwardapp-suit/forwardapp-android/app/src/main/java/com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan/DayPlanScreen.kt

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.ui.common.MatrixRainView
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.AddTaskDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.TaskList
import kotlinx.coroutines.delay

const val TAG = "NAV_DEBUG"

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Помилка завантаження",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Спробувати ще раз") }
    }
}

@Composable
fun DayPlanScreen(
    initialDayPlanId: String,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel, // Modified line
    onNavigateToProject: (projectId: String) -> Unit,
    onNavigateToBacklog: (task: DayTask) -> Unit,
    onNavigateToSettings: () -> Unit,
    addTaskTrigger: Int,
    navController: NavController,
) {

    val systemUiController = rememberSystemUiController()
    val isLight = !isSystemInDarkTheme()

    LaunchedEffect(isLight) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = isLight,
            isNavigationBarContrastEnforced = false,
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val isEditTaskDialogOpen by viewModel.isEditTaskDialogOpen.collectAsState()
    var showReminderDialog by remember { mutableStateOf(false) }
    val taskToDelete by viewModel.showDeleteConfirmationDialog.collectAsState()
    val taskToEdit by viewModel.showEditConfirmationDialog.collectAsState()

    DisposableEffect(Unit) { onDispose { viewModel.clearSelectedTask() } }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is DayPlanUiEvent.NavigateToEditTask -> {
                    navController.navigate("edit_task_screen/${it.taskId}")
                }
            }
        }
    }

    var showMatrixSplash by remember { mutableStateOf(true) }
    var matrixView by remember { mutableStateOf<MatrixRainView?>(null) }
    var isContentReady by remember { mutableStateOf(false) }

    // Enhanced timing with content preparation
    LaunchedEffect(Unit) {
        // Pre-load content
        delay(100)
        isContentReady = true

        // Show matrix longer, then smooth fade
        delay(600)
        matrixView?.startFadeOut()

        // Wait for fade to complete
        delay(500)
        showMatrixSplash = false
    }

    if (taskToEdit != null) {
        EditRecurringTaskDialog(
            taskWithReminder = taskToEdit!!,
            onDismiss = { viewModel.dismissEditConfirmationDialog() },
            onConfirmEditSingle = { viewModel.editSingleInstanceOfRecurringTask(taskToEdit!!) },
            onConfirmEditAll = { viewModel.editAllFutureInstancesOfRecurringTask(taskToEdit!!) },
        )
    }

    if (taskToDelete != null) {
        DeleteRecurringTaskDialog(
            taskWithReminder = taskToDelete!!,
            onDismiss = { viewModel.dismissDeleteConfirmationDialog() },
            onConfirmDeleteSingle = { viewModel.deleteSingleInstanceOfRecurringTask(taskToDelete!!) },
            onConfirmDeleteAll = { viewModel.deleteAllFutureInstancesOfRecurringTask(taskToDelete!!) },
        )
    }

    LaunchedEffect(addTaskTrigger) {
        if (addTaskTrigger > 0) {
            viewModel.openAddTaskDialog()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(Unit) { viewModel.loadDataForPlan(initialDayPlanId) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content with conditional visibility for smoother transition
        AnimatedVisibility(
            visible = isContentReady,
            enter = fadeIn(animationSpec = tween(300, delayMillis = 400)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount: Float ->
                        when {
                            dragAmount < -50 && !uiState.isToday -> viewModel.navigateToNextDay()
                            dragAmount > 50 -> viewModel.navigateToPreviousDay()
                        }
                    }
                }
            ) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.error != null && uiState.tasks.isEmpty() -> {
                        ErrorState(
                            error = uiState.error!!,
                            onRetry = { viewModel.loadDataForPlan(initialDayPlanId) },
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val tasks = uiState.tasks
                            val totalPointsEarned =
                                tasks.filter { it.dayTask.completed }.sumOf { it.dayTask.points.coerceAtLeast(0) }
                            val totalPointsAvailable = tasks.sumOf { it.dayTask.points.coerceAtLeast(0) }
                            val completedTasksCount = tasks.count { it.dayTask.completed }
                            val totalTasksCount = tasks.size

                            TaskList(
                                tasks = tasks,
                                onTaskLongPress = { taskWithReminder ->
                                    viewModel.onTaskLongPressed(taskWithReminder)
                                },
                                onTasksReordered = { reorderedList ->
                                    uiState.dayPlan?.let { dayPlan ->
                                        viewModel.updateTasksOrder(dayPlan.id, reorderedList)
                                    }
                                },
                                onToggleTask = { taskId -> viewModel.toggleTaskCompletion(taskId) },
                                onSublistClick = onNavigateToProject,
                                modifier = Modifier.fillMaxSize(),
                                onParentInfoClick = { parentInfo ->
                                    when (parentInfo.type) {
                                        ParentType.PROJECT -> {
                                            navController.navigate("goal_detail_screen/${parentInfo.id}")
                                        }
                                        ParentType.GOAL -> {
                                            parentInfo.projectId?.let { listId ->
                                                navController.navigate(
                                                    "goal_detail_screen/${listId}?goalId=${parentInfo.id}"
                                                )
                                            }
                                                ?: run {
                                                    Log.e(
                                                        TAG,
                                                        "Goal parentInfo has null projectId for goalId: ${parentInfo.id}",
                                                    )
                                                }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // Enhanced Matrix splash screen with better integration
        AnimatedVisibility(visible = showMatrixSplash, exit = fadeOut(animationSpec = tween(300))) {
            AndroidView(
                factory = { context -> MatrixRainView(context).also { view -> matrixView = view } },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (isAddTaskDialogOpen) {
        AddTaskDialog(
            onDismissRequest = viewModel::dismissAddTaskDialog,
            onConfirm = { title, description, duration, priority, recurrenceRule, points ->
                viewModel.addTask(
                    initialDayPlanId,
                    title,
                    description,
                    duration,
                    priority,
                    recurrenceRule,
                    points,
                )
            },
            initialPriority = TaskPriority.MEDIUM,
        )
    }

    selectedTask?.let { selectedTaskWithReminder ->
        TaskOptionsBottomSheet(
            taskWithReminder = selectedTaskWithReminder,
            onDismiss = viewModel::clearSelectedTask,
            onEdit = { viewModel.onEditTaskClicked(selectedTaskWithReminder) },
            onDelete = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onDeleteTaskClicked(it)
            },
            onSetReminder = { showReminderDialog = true },
            showAddToTodayOption = !uiState.isToday,
            onAddToToday = { viewModel.copyTaskToTodaysPlan(selectedTaskWithReminder) },
            onShowInBacklog = { onNavigateToBacklog(selectedTaskWithReminder.dayTask) },
            onMoveToTop = { viewModel.moveTaskToTop(selectedTaskWithReminder) },
            onMoveToTomorrow = { viewModel.moveTaskToTomorrow(selectedTaskWithReminder) },
        )
    }

    if (showReminderDialog && selectedTask != null) {
        ReminderPropertiesDialog(
            onDismiss = {
                showReminderDialog = false
                viewModel.clearSelectedTask()
            },
            onSetReminder = { reminderTime ->
                selectedTask?.let { viewModel.setTaskReminder(it.dayTask.id, reminderTime) }
                showReminderDialog = false
                viewModel.clearSelectedTask()
            },
            onRemoveReminder = {
                selectedTask?.let { viewModel.clearTaskReminder(it.dayTask.id) }
                showReminderDialog = false
                viewModel.clearSelectedTask()
            },
            currentReminders = listOfNotNull(selectedTask?.reminder).map { it },
        )
    }
}

@Composable
fun EditRecurringTaskDialog(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    onConfirmEditSingle: (DayTaskWithReminder) -> Unit,
    onConfirmEditAll: (DayTaskWithReminder) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати повторюване завдання") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Це завдання є частиною серії. Виберіть, що саме потрібно змінити.")

                Row(
                    modifier =
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onConfirmEditSingle(taskWithReminder) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Тільки це завдання")
                }

                Row(
                    modifier =
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onConfirmEditAll(taskWithReminder) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Це та всі наступні завдання")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
fun DeleteRecurringTaskDialog(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    onConfirmDeleteSingle: (DayTaskWithReminder) -> Unit,
    onConfirmDeleteAll: (DayTaskWithReminder) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видалити повторюване завдання?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Це завдання є частиною серії. Виберіть, що саме потрібно видалити.")

                Button(
                    onClick = { onConfirmDeleteSingle(taskWithReminder) },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("Тільки це завдання")
                }

                Button(
                    onClick = { onConfirmDeleteAll(taskWithReminder) },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("Це та всі наступні")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Завантаження плану...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (DayTaskWithReminder) -> Unit,
    onSetReminder: () -> Unit,
    showAddToTodayOption: Boolean,
    onAddToToday: () -> Unit,
    onShowInBacklog: (DayTask) -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToTomorrow: () -> Unit,
) {
    val task = taskWithReminder.dayTask
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text("Редагувати") },
                leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                modifier = Modifier.clickable { onEdit() },
            )
            ListItem(
                headlineContent = { Text("Підняти на вершину списку") },
                leadingContent = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null) },
                modifier = Modifier.clickable { onMoveToTop() },
            )
            ListItem(
                headlineContent = { Text("Перенести на завтра") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                },
                modifier =
                Modifier.clickable {
                    onMoveToTomorrow()
                    onDismiss()
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Встановити нагадування") },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                modifier = Modifier.clickable { onSetReminder() },
            )
            if (showAddToTodayOption) {
                ListItem(
                    headlineContent = { Text("Додати в план на сьогодні") },
                    leadingContent = { Icon(Icons.Outlined.Today, contentDescription = null) },
                    modifier =
                    Modifier.clickable {
                        onAddToToday()
                        onDismiss()
                    },
                )
            }
            if (task.projectId != null || task.goalId != null) {
                ListItem(
                    headlineContent = { Text("Показати в беклозі проекту") },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) },
                    modifier =
                    Modifier.clickable {
                        Log.d(TAG, "1. КЛІК: 'Показати в беклозі'.")
                        Log.d(TAG, "   - Task Title: ${task.title}")
                        Log.d(TAG, "   - Task ProjectID: ${task.projectId}") // ДУЖЕ ВАЖЛИВИЙ ЛОГ
                        Log.d(TAG, "   - Task GoalID: ${task.goalId}")
                        Log.d(TAG, "   - Task ID: ${task.id}")

                        onShowInBacklog(task)
                        onDismiss()
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                leadingContent = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier =
                Modifier.clickable {
                    onDelete(taskWithReminder)
                    onDismiss()
                },
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
