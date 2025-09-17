package com.romankozak.forwardappmobile.ui.screens.daymanagement.daydashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDashboardScreen(
    dayPlanId: String,
    viewModel: DayDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dayPlanId) {
        viewModel.loadDataForDay(dayPlanId)
    }

    Scaffold(
        topBar = {
            // UI ОНОВЛЕННЯ: Заголовок став більш інформативним.
            // Зайві кнопки навігації видалено, оскільки тепер є вкладки.
            TopAppBar(
                title = {
                    val date = uiState.dayPlan?.date?.let { Date(it) }
                    val formattedDate = date?.let {
                        SimpleDateFormat("d MMMM yyyy", Locale("uk", "UA")).format(it)
                    } ?: ""
                    Text("Дашборд за $formattedDate")
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // UI ОНОВЛЕННЯ: Контент тепер організовано в інформативні картки.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgressCard(
                    tasksCompleted = uiState.tasksCompleted,
                    tasksTotal = uiState.tasksTotal,
                    progress = uiState.progress
                )

                uiState.metrics?.let { metrics ->
                    MetricCard(
                        icon = Icons.Outlined.Timer, // ВИПРАВЛЕНО: Замінено іконку на 'Timer'
                        label = "Активний час",
                        value = "${metrics.totalActiveTime} хв"
                    )
                }


            }
        }
    }
}

@Composable
fun ProgressCard(tasksCompleted: Int, tasksTotal: Int, progress: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.PieChart, contentDescription = "Прогрес")
                Text("Прогрес дня", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$tasksCompleted з $tasksTotal завдань виконано (${(progress * 100).roundToInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MetricCard(icon: ImageVector, label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = label)
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}