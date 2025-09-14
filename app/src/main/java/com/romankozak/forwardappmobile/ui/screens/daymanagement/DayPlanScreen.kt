// File: DayPlanScreen.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // LaunchedEffect для показу снекбару при виникненні помилки
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(
                message = uiState.error!!,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError() // Очищуємо помилку після показу
        }
    }

    // Завантажуємо дані один раз, коли змінюється dayPlanId
    LaunchedEffect(dayPlanId) {
        viewModel.loadDataForPlan(dayPlanId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("План дня") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Оновити дані") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { viewModel.loadDataForPlan(dayPlanId) }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Оновити")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddTaskDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "Додати завдання") },
                text = { Text("Додати завдання") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.tasks.isEmpty() -> { // Показуємо екран помилки, лише якщо немає даних
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.CloudOff, null, Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Помилка завантаження", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text(uiState.error!!, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDataForPlan(dayPlanId) }) {
                            Text("Спробувати ще раз")
                        }
                    }
                }
                else -> {
                    TaskList(
                        tasks = uiState.tasks,
                        dayPlan = uiState.dayPlan,
                        onToggleTask = viewModel::toggleTaskCompletion,
                        onTaskLongPress = viewModel::selectTask
                    )
                }
            }
        }
    }

    if (isAddTaskDialogOpen) {
        // Викликаємо діалог, який тепер передає всі необхідні дані
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
            onDelete = { viewModel.deleteTask(dayPlanId, it.id) },
            onSetReminder = { /* TODO: viewModel.setTaskReminder(it) */ }
        )
    }
}

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
        contentPadding = PaddingValues(bottom = 96.dp), // Відступ для FAB
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
                Column(
                    modifier = Modifier.fillParentMaxHeight(0.7f).fillParentMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Checklist, contentDescription = null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("Завдань ще немає", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onToggle = { onToggleTask(task.id) },
                    onLongPress = { onTaskLongPress(task) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DayPlanHeader(dayPlan: DayPlan?, completedTasks: Int, totalTasks: Int) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    Column(modifier = Modifier.padding(16.dp)) {
        val formattedDate = dayPlan?.date?.let {
            SimpleDateFormat("EEEE, d MMMM", Locale("uk", "UA")).format(Date(it))
        } ?: "План дня"

        Text(formattedDate.replaceFirstChar { it.uppercaseChar() }, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("$completedTasks з $totalTasks виконано (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium)

        AnimatedVisibility(visible = completedTasks == totalTasks && totalTasks > 0) {
            Text(
                "🎉 Вітаємо! Всі завдання виконані!",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(task: DayTask, onToggle: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if(task.completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if(task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TaskMetaInfo(task = task)
            }
            PriorityIndicator(priority = task.priority)
        }
    }
}

@Composable
fun TaskMetaInfo(task: DayTask) {
    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        task.estimatedDurationMinutes?.takeIf { it > 0 }?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = "Тривалість", Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("$it хв", style = MaterialTheme.typography.labelSmall)
            }
        }
        task.dueTime?.let { dueTimestamp ->
            val dueDate = Date(dueTimestamp)
            val isOverdue = dueDate.before(Date())
            val color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Alarm, contentDescription = "Термін", Modifier.size(16.dp), tint = color)
                Spacer(Modifier.width(4.dp))
                Text(
                    "до ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
fun PriorityIndicator(priority: TaskPriority) {
    val (color, icon) = when (priority) {
        TaskPriority.CRITICAL -> Pair(MaterialTheme.colorScheme.errorContainer, Icons.Outlined.PriorityHigh)
        TaskPriority.HIGH -> Pair(MaterialTheme.colorScheme.error, Icons.Outlined.Flag)
        TaskPriority.MEDIUM -> Pair(MaterialTheme.colorScheme.tertiary, Icons.Outlined.Flag)
        TaskPriority.LOW -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Outlined.Flag)
        TaskPriority.NONE -> return // Для NONE нічого не показуємо
    }
    Icon(icon, contentDescription = "Пріоритет", tint = color, modifier = Modifier.size(24.dp))
}