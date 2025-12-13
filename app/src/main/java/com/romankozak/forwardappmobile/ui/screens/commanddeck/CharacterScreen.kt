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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerViewModel
import java.util.Calendar

@Composable
fun CharacterScreen(
    activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(),
) {
    val activityLog by activityTrackerViewModel.activityLog.collectAsStateWithLifecycle()
    val statsState = rememberUpdatedState(activityLog.map { Triple(it.createdAt, it.xpGained ?: 0, it.antyXp ?: 0) })
    val (xpToday, antiXpToday) = calculateTodayStats(statsState.value)

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

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
