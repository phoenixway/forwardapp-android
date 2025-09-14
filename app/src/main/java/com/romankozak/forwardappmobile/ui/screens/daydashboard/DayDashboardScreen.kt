package com.romankozak.forwardappmobile.ui.screens.daydashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.database.models.DayTask
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDashboardScreen(
    dayPlanId: String,
    viewModel: DayDashboardViewModel = hiltViewModel(),
    onNavigateToPlan: (String) -> Unit,
    onNavigateToAnalytics: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Завантажуємо дані один раз при вході на екран
    LaunchedEffect(key1 = dayPlanId) {
        viewModel.loadDataForDay(dayPlanId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.dayPlan?.name ?: "Дашборд дня") },
                actions = {
                    IconButton(onClick = { onNavigateToPlan(dayPlanId) }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Перейти до плану")
                    }
                    IconButton(onClick = { onNavigateToAnalytics(dayPlanId) }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Перейти до аналітики")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Помилка: ${uiState.error}",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DaySummaryCard(state = uiState)
                    }

                    item {
                        QuickActionsSection()
                    }

                    item {
                        Text(
                            "Завдання на сьогодні",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.tasks.isEmpty()) {
                        item {
                            Text(
                                "Завдань на сьогодні ще немає. Час їх додати!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(uiState.tasks, key = { it.id }) { task ->
                            DayTaskItem(task = task, onTaskCompleted = { taskId, isCompleted ->
                                // Тут буде виклик viewModel.updateTaskStatus(taskId, isCompleted)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DaySummaryCard(state: DayDashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val date = state.dayPlan?.date?.let { Date(it) }
            val formattedDate = date?.let { SimpleDateFormat("EEEE, d MMMM", Locale("uk", "UA")).format(it) } ?: ""

            Text(
                text = formattedDate.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Прогрес бар
            LinearProgressIndicator(
                progress = state.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(
                "Виконано ${state.tasksCompleted} з ${state.tasksTotal} завдань",
                style = MaterialTheme.typography.bodyMedium
            )

            // Ключові метрики
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem("Ранкова енергія", state.metrics?.morningEnergyLevel?.toString() ?: "N/A")
                MetricItem("Заплановано часу", "${state.metrics?.totalPlannedTime ?: 0} хв")
                MetricItem("Активного часу", "${state.metrics?.totalActiveTime ?: 0} хв")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun QuickActionsSection() {
    // TODO: Розширити функціонал
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { /* TODO: Add new task */ }, modifier = Modifier.weight(1f)) {
            Text("Нове завдання")
        }
        OutlinedButton(onClick = { /* TODO: Start break */ }, modifier = Modifier.weight(1f)) {
            Text("Почати перерву")
        }
    }
}

@Composable
fun DayTaskItem(task: DayTask, onTaskCompleted: (String, Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = task.completed,
                onCheckedChange = { isChecked -> onTaskCompleted(task.id, isChecked) }
            )
        }
    }
}