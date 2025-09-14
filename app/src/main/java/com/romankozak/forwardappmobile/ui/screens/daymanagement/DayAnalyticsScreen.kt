package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.WeeklyInsights
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAnalyticsScreen(
    navController: NavController,
    viewModel: DayAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналітика та Інсайти") }
            )
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
                    AnalyticsContent(
                        state = uiState,
                        onTimeRangeSelected = { range -> viewModel.selectTimeRange(range) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsContent(state: DayAnalyticsUiState, onTimeRangeSelected: (TimeRange) -> Unit) {
    val insights = state.insights

    // UI ОНОВЛЕННЯ: Покращено повідомлення про відсутність даних.
    if (insights == null || insights.totalDays == 0) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimeRangeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = onTimeRangeSelected
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Недостатньо даних для аналітики за цей період.")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TimeRangeSelector(
                selectedRange = state.selectedRange,
                onRangeSelected = onTimeRangeSelected
            )
        }

        item {
            KeyMetricsGrid(insights = insights)
        }

        item {
            ProductivityInsights(insights = insights)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp).height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Тут буде графік продуктивності", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(selectedRange: TimeRange, onRangeSelected: (TimeRange) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TimeRange.values().forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.displayName) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun KeyMetricsGrid(insights: WeeklyInsights) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "Сер. успішність",
                value = "${(insights.averageCompletionRate * 100).roundToInt()}%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Всього активного часу",
                value = "${insights.totalActiveTime / 60} год ${insights.totalActiveTime % 60} хв",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "Всього днів",
                value = insights.totalDays.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Сер. завдань/день",
                value = "%.1f".format(insights.averageTasksPerDay),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ProductivityInsights(insights: WeeklyInsights) {
    val bestDay = insights.bestDay
    val worstDay = insights.worstDay
    val locale = Locale("uk", "UA")
    val dayFormat = SimpleDateFormat("EEEE, d MMMM", locale)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bestDay != null) {
            val dateString = dayFormat.format(Date(bestDay.date)).replaceFirstChar { it.uppercaseChar() }
            InsightCard(
                // UI ОНОВЛЕННЯ: Додано іконку для візуального акценту.
                icon = Icons.Outlined.AutoAwesome,
                title = "Найкращий день",
                description = "$dateString, успішність: ${(bestDay.completionRate * 100).roundToInt()}% (${bestDay.tasksCompleted}/${bestDay.tasksPlanned})"
            )
        }
        if (worstDay != null && insights.totalDays > 1) {
            val dateString = dayFormat.format(Date(worstDay.date)).replaceFirstChar { it.uppercaseChar() }
            InsightCard(
                // UI ОНОВЛЕННЯ: Додано іконку для візуального акценту.
                icon = Icons.Outlined.TrendingDown,
                title = "День для покращення",
                description = "$dateString, успішність: ${(worstDay.completionRate * 100).roundToInt()}% (${worstDay.tasksCompleted}/${worstDay.tasksPlanned})"
            )
        }
    }
}

@Composable
fun InsightCard(title: String, description: String, icon: ImageVector) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}