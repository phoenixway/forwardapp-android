package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.database.models.DayTask
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanScreen(
    dayPlanId: String,
    viewModel: DayPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()

    LaunchedEffect(key1 = dayPlanId) {
        viewModel.loadDataForPlan(dayPlanId)
    }

    // UI ОНОВЛЕННЯ: Додано Scaffold з TopAppBar та FAB.
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val date = uiState.dayPlan?.date?.let { Date(it) }
                    val formattedDate = date?.let {
                        SimpleDateFormat("EEEE, d MMMM", Locale("uk", "UA")).format(it)
                    } ?: "План дня"
                    Text(text = formattedDate.replaceFirstChar { it.uppercaseChar() })
                }
            )
        },
        // UX ОНОВЛЕННЯ: FAB для інтуїтивного додавання нових завдань.
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openAddTaskDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Додати завдання")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "Помилка: ${uiState.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    TaskList(
                        tasks = uiState.tasks,
                        onToggleTask = { taskId -> viewModel.toggleTaskCompletion(taskId) }
                    )
                }
            }
        }
    }

    // UX ОНОВЛЕННЯ: Діалогове вікно для створення нового завдання.
    if (isAddTaskDialogOpen) {
        AddTaskDialog(
            onDismissRequest = { viewModel.dismissAddTaskDialog() },
            onConfirm = { title ->
                viewModel.addTask(title)
                viewModel.dismissAddTaskDialog()
            }
        )
    }
}

@Composable
fun TaskList(tasks: List<DayTask>, onToggleTask: (String) -> Unit) {
    if (tasks.isEmpty()) {
        // UI ОНОВЛЕННЯ: Покращений вигляд "пустого стану" з іконкою та текстом.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Checklist,
                    contentDescription = "Немає завдань",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "Завдань на сьогодні ще немає.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "Натисніть '+' щоб додати перше.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskItem(task = task, onToggle = { onToggleTask(task.id) })
            }
        }
    }
}

@Composable
fun TaskItem(task: DayTask, onToggle: () -> Unit) {
    // UI ОНОВЛЕННЯ: Картка завдання з покращеним візуальним стилем.
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = task.title,
                style = if (task.completed) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = Color.Gray
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val isTitleValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Нове завдання") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва завдання") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                enabled = isTitleValid
            ) {
                Text("Створити")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Скасувати")
            }
        }
    )
}