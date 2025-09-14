// DayPlanScreen.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
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
    tasks: List<DayTask>,
    dayPlan: DayPlan?,
    onToggleTask: (String) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            DayPlanHeader(
                dayPlan = dayPlan,
                completedTasks = tasks.count { it.completed },
                totalTasks = tasks.size
            )
        }

        if (tasks.isEmpty()) {
            item {
                EmptyTasksState(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillParentMaxWidth()
                        .padding(16.dp)
                )
            }
        } else {
            items(
                items = tasks,
                key = { task -> task.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onToggle = { onToggleTask(task.id) },
                    onLongPress = { onTaskLongPress(task) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                 //       .animateItemPlacement()
                )
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
fun DayPlanHeader(
    dayPlan: DayPlan?,
    completedTasks: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    val formattedDate = remember(dayPlan?.date) {
        dayPlan?.date?.let { dateMillis ->
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk", "UA"))
            Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } ?: "План дня"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = formattedDate.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    progress >= 1.0f -> MaterialTheme.colorScheme.tertiary
                    progress >= 0.7f -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            AnimatedVisibility(
                visible = completedTasks == totalTasks && totalTasks > 0,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Вітаємо! Всі завдання виконані!",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: DayTask,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (task.completed) 0.7f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.completed) 1.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                task.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (task.completed) 0.5f else 0.8f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TaskMetaInfo(
                    task = task,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            PriorityIndicator(priority = task.priority)
        }
    }
}

@Composable
fun TaskMetaInfo(
    task: DayTask,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { duration ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = "Тривалість",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${duration} хв",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        task.dueTime?.let { dueTimestamp ->
            val isOverdue = dueTimestamp < System.currentTimeMillis() && !task.completed
            val color = if (isOverdue) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val formattedTime = remember(dueTimestamp) {
                val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                Instant.ofEpochMilli(dueTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isOverdue) Icons.Outlined.Warning else Icons.Outlined.Alarm,
                    contentDescription = if (isOverdue) "Прострочено" else "Термін",
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Text(
                    text = if (isOverdue) "до $formattedTime" else "до $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = if (isOverdue) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PriorityIndicator(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (priority) {
        TaskPriority.CRITICAL -> Pair(
            MaterialTheme.colorScheme.error,
            Icons.Outlined.PriorityHigh
        )
        TaskPriority.HIGH -> Pair(
            MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            Icons.Outlined.Flag
        )
        TaskPriority.MEDIUM -> Pair(
            MaterialTheme.colorScheme.primary,
            Icons.Outlined.Flag
        )
        TaskPriority.LOW -> Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            Icons.Outlined.Flag
        )
        TaskPriority.NONE -> return
    }

    Icon(
        icon,
        contentDescription = "Пріоритет: ${priority.getDisplayName()}",
        tint = color,
        modifier = modifier.size(20.dp)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanScreen(
    dayPlanId: String,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

   /* when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.tasks.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає задач")
            }
        }
        else -> {
            LazyColumn {
                items(uiState.tasks) { task ->
                    Text(task.title)
                }
            }
        }
    }*/


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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "План дня",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text("Оновити дані")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.loadDataForPlan(dayPlanId)
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Оновити",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
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
                    TaskList(
                        tasks = uiState.tasks,
                        dayPlan = uiState.dayPlan,
                        onToggleTask = { taskId ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTaskCompletion(taskId)
                        },
                        onTaskLongPress = { task ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.selectTask(task)
                        }
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