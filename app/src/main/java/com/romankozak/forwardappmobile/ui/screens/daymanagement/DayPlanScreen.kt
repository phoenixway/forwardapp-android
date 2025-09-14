package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.ui.navigation.DAY_ANALYTICS_ROUTE
import com.romankozak.forwardappmobile.ui.navigation.DAY_DASHBOARD_ROUTE
import com.romankozak.forwardappmobile.ui.navigation.DAY_PLAN_LIST_ROUTE
import com.romankozak.forwardappmobile.utils.DayManagementUtils
import java.util.concurrent.TimeUnit

// Головний екран-контейнер для секції управління добою
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    viewModel: DayPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dayManagementNavController = rememberNavController()

    Scaffold(
        topBar = {
            DayPlanTopAppBar(
                currentDate = uiState.selectedDate,
                onNavigateBack = { mainNavController.popBackStack() }, // Використовуємо головний NavController для виходу
                onDateChange = { newDate ->
                    // Перезавантажуємо всю секцію з новою датою
                    mainNavController.navigate("day_management_screen/$newDate") {
                        popUpTo("day_management_screen/${uiState.selectedDate}") { inclusive = true }
                    }
                }
            )
        },
        bottomBar = {
            DayManagementBottomBar(navController = dayManagementNavController)
        }
    ) { padding ->
        // Вкладений NavHost для внутрішньої навігації
        NavHost(
            navController = dayManagementNavController,
            startDestination = DAY_PLAN_LIST_ROUTE,
            modifier = Modifier.padding(padding)
        ) {
            composable(DAY_PLAN_LIST_ROUTE) {
                DayPlanContent(viewModel = viewModel)
            }
            composable(DAY_DASHBOARD_ROUTE) {
                DayDashboardScreen(navController = dayManagementNavController)
            }
            composable(DAY_ANALYTICS_ROUTE) {
                DayAnalyticsScreen(navController = dayManagementNavController)
            }
        }
    }
}

// Контент для вкладки "План" (раніше це був весь DayPlanScreen)
@Composable
private fun DayPlanContent(viewModel: DayPlanViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.tasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Завдань на сьогодні ще немає. Додайте перше!")
                        }
                    }
                } else {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати завдання")
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title ->
                viewModel.addTask(title)
                showAddTaskDialog = false
            }
        )
    }
}


@Composable
private fun DayManagementBottomBar(navController: NavHostController) {
    val items = listOf(
        DayManagementScreenRoute.Plan,
        DayManagementScreenRoute.Dashboard,
        DayManagementScreenRoute.Analytics
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// Допоміжні класи для навігації
sealed class DayManagementScreenRoute(val route: String, val title: String, val icon: ImageVector) {
    object Plan : DayManagementScreenRoute(DAY_PLAN_LIST_ROUTE, "План", Icons.Default.ListAlt)
    object Dashboard : DayManagementScreenRoute(DAY_DASHBOARD_ROUTE, "Дашборд", Icons.Default.Dashboard)
    object Analytics : DayManagementScreenRoute(DAY_ANALYTICS_ROUTE, "Аналітика", Icons.Default.Analytics)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPlanTopAppBar(
    currentDate: Long,
    onNavigateBack: () -> Unit,
    onDateChange: (newDate: Long) -> Unit
) {
    TopAppBar(
        title = { Text(DayManagementUtils.getDateDescription(currentDate)) },
        navigationIcon = {
            IconButton(onClick = { onDateChange(currentDate - TimeUnit.DAYS.toMillis(1)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Попередній день")
            }
        },
        actions = {
            IconButton(onClick = { onDateChange(currentDate + TimeUnit.DAYS.toMillis(1)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Наступний день")
            }
        }
    )
}

@Composable
private fun TaskRow(task: DayTask, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Статус",
                tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нове завдання") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Що потрібно зробити?") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
