package com.romankozak.forwardappmobile.features.mainscreen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val MILLIS_PER_DAY =
    HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLIS_PER_SECOND

@Composable
fun CharacterScreen(activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel()) {
    val activityLog by activityTrackerViewModel.activityLog.collectAsStateWithLifecycle()
    val entries = activityLog.map { Triple(it.createdAt, it.xpGained ?: 0, it.antyXp ?: 0) }
    val (xpToday, antiXpToday) = calculateTodayStats(entries)
    val dailyStats = calculateDailyStats(entries)
    val maxPositiveDay = dailyStats.maxByOrNull { entry -> entry.value.first - entry.value.second }
    val maxNegativeDay = dailyStats.minByOrNull { entry -> entry.value.first - entry.value.second }
    val daysCount = dailyStats.size.coerceAtLeast(1)
    val avgXp = dailyStats.values.sumOf { it.first } / daysCount
    val avgAntiXp = dailyStats.values.sumOf { it.second } / daysCount
    val dateFormatter = rememberDateFormatter()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Character",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TodayStatsCard(
                xpToday = xpToday,
                antiXpToday = antiXpToday,
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DailyHighlightsCardContent(
                        maxPositiveDay = maxPositiveDay,
                        maxNegativeDay = maxNegativeDay,
                        avgXp = avgXp,
                        avgAntiXp = avgAntiXp,
                        dateFormatter = dateFormatter,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayStatsCard(
    xpToday: Int,
    antiXpToday: Int,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Today stats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "XP: +$xpToday / -$antiXpToday",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun calculateTodayStats(entries: List<Triple<Long, Int, Int>>): Pair<Int, Int> {
    val todayStart = startOfDay(System.currentTimeMillis())
    val todayEnd = todayStart + MILLIS_PER_DAY - 1
    val filtered = entries.filter { (timestamp, _, _) -> timestamp in todayStart..todayEnd }
    val xp = filtered.sumOf { it.second }
    val anti = filtered.sumOf { it.third }
    return xp to anti
}

@Composable
private fun DailyHighlightsCardContent(
    maxPositiveDay: Map.Entry<Long, Pair<Int, Int>>?,
    maxNegativeDay: Map.Entry<Long, Pair<Int, Int>>?,
    avgXp: Int,
    avgAntiXp: Int,
    dateFormatter: SimpleDateFormat,
) {
    Text(
        text = "Daily highlights",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = buildHighlightText("Максимально позитивний день", maxPositiveDay, dateFormatter),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = buildHighlightText("Максимально негативний день", maxNegativeDay, dateFormatter),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = "Середні показники за день: +$avgXp / -$avgAntiXp",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun buildHighlightText(
    label: String,
    entry: Map.Entry<Long, Pair<Int, Int>>?,
    dateFormatter: SimpleDateFormat,
): String {
    val value =
        entry?.let { (day, stats) ->
            "${dateFormatter.format(day)} (+${stats.first} / -${stats.second}, нетто ${stats.first - stats.second})"
        } ?: "—"
    return "$label: $value"
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
    return remember {
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
