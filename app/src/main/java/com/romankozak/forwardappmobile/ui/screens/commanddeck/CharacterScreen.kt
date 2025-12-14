package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerViewModel
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

@Composable
fun CharacterScreen(
    activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(),
) {
    val activityLog by activityTrackerViewModel.activityLog.collectAsStateWithLifecycle()
    val entries = activityLog.map { Triple(it.createdAt, it.xpGained ?: 0, it.antyXp ?: 0) }
    val (xpToday, antiXpToday) = calculateTodayStats(entries)
    val dailyStats = calculateDailyStats(entries)
    val maxPositiveDay = dailyStats.maxByOrNull { (_, value) -> value.first - value.second }
    val maxNegativeDay = dailyStats.minByOrNull { (_, value) -> value.first - value.second }
    val daysCount = dailyStats.size.coerceAtLeast(1)
    val avgXp = dailyStats.values.sumOf { it.first } / daysCount
    val avgAntiXp = dailyStats.values.sumOf { it.second } / daysCount
    val dateFormatter = rememberDateFormatter()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Character",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Today stats",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "XP: +$xpToday / -$antiXpToday",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Daily highlights",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Максимально позитивний день: ${
                            maxPositiveDay?.let { "${dateFormatter.format(it.first)} (+${it.second.first} / -${it.second.second}, нетто ${it.second.first - it.second.second})" }
                                ?: "—"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Максимально негативний день: ${
                            maxNegativeDay?.let { "${dateFormatter.format(it.first)} (+${it.second.first} / -${it.second.second}, нетто ${it.second.first - it.second.second})" }
                                ?: "—"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Середні показники за день: +$avgXp / -$avgAntiXp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun calculateTodayStats(entries: List<Triple<Long, Int, Int>>): Pair<Int, Int> {
    val todayStart = startOfDay(System.currentTimeMillis())
    val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
    val filtered = entries.filter { (timestamp, _, _) -> timestamp in todayStart..todayEnd }
    val xp = filtered.sumOf { it.second }
    val anti = filtered.sumOf { it.third }
    return xp to anti
}

private fun calculateDailyStats(entries: List<Triple<Long, Int, Int>>): Map<Long, Pair<Int, Int>> {
    val grouped = mutableMapOf<Long, Pair<Int, Int>>()
    entries.forEach { (timestamp, xp, antiXp) ->
        val day = startOfDay(timestamp)
        val (prevXp, prevAnti) = grouped[day] ?: (0 to 0)
        grouped[day] = (prevXp + xp) to (prevAnti + antiXp)
    }
    return grouped
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return androidx.compose.runtime.remember {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
