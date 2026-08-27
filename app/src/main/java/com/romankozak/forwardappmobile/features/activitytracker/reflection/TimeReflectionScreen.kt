package com.romankozak.forwardappmobile.features.activitytracker.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReflectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimeReflectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reflection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            ReflectionContent(
                reflection = uiState.reflection,
                onPeriodSelected = viewModel::selectPeriod,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ReflectionContent(
    reflection: TimeReflection,
    onPeriodSelected: (ReflectionPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReflectionPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = reflection.period == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.title) },
                    )
                }
            }
        }

        if (reflection.rangeStart == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Немає зафіксованого початку дня", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Започаткуйте день у вкладці «День», щоб сформувати межу для рефлексії.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            return@LazyColumn
        }

        item { SummaryCard(reflection) }

        item {
            Text(
                text = "Час за тегами",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (reflection.tagStats.isEmpty()) {
            item { Text("За цей період немає відстежених активностей.") }
        } else {
            items(reflection.tagStats, key = TagTimeStat::tag) { stat -> TagStatRow(stat) }
            item {
                Text(
                    "Активність із кількома тегами враховується в кожному з них.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            TimeReflectionEntitySections(reflection.entityStats)
        }
    }
}

@Composable
private fun SummaryCard(reflection: TimeReflection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Всього відстежено", style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatReflectionDuration(reflection.totalTrackedMillis),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatReflectionRange(reflection.rangeStart!!, reflection.rangeEnd),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (reflection.recordedDayCount < reflection.period.operationalDayCount) {
                Text(
                    text = "Доступно початків днів: ${reflection.recordedDayCount} із ${reflection.period.operationalDayCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TagStatRow(stat: TagTimeStat) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stat.tag, fontWeight = FontWeight.Medium)
            Text(formatReflectionDuration(stat.durationMillis))
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { stat.share.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun formatReflectionDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours год $minutes хв"
        hours > 0 -> "$hours год"
        else -> "$minutes хв"
    }
}

private fun formatReflectionRange(start: Long, end: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return "${formatter.format(Date(start))} — ${formatter.format(Date(end))}"
}
