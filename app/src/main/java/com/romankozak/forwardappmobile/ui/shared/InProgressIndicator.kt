package com.romankozak.forwardappmobile.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.FloatingActionButton
import com.romankozak.forwardappmobile.core.database.models.ActivityRecord

@Composable
fun InProgressIndicator(
    ongoingActivity: ActivityRecord?,
    onStopClick: () -> Unit,
    onReminderClick: () -> Unit,
    onIndicatorClick: () -> Unit,
    indicatorState: InProgressIndicatorState = remember { InProgressIndicatorState() }
) {
    AnimatedVisibility(
        visible = ongoingActivity != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        if (ongoingActivity != null) {
            var elapsedTime by remember { mutableLongStateOf(System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)) }

            LaunchedEffect(key1 = ongoingActivity.id) {
                while (true) {
                    elapsedTime = System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)
                    kotlinx.coroutines.delay(1000L)
                }
            }
            val timeString = formatElapsedTime(elapsedTime)

            if (indicatorState.isExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onIndicatorClick),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.HourglassTop, contentDescription = "В процесі", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ongoingActivity.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onReminderClick) {
                            Icon(Icons.Default.Notifications, contentDescription = "Встановити нагадування")
                        }
                        IconButton(onClick = onStopClick) {
                            Icon(Icons.Default.StopCircle, contentDescription = "Зупинити")
                        }
                        IconButton(onClick = { indicatorState.isExpanded = false }) {
                            Icon(Icons.Default.ExpandLess, contentDescription = "Згорнути")
                        }
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { indicatorState.isExpanded = true },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Icon(Icons.Default.HourglassTop, contentDescription = "В процесі")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = timeString)
                    }
                }
            }
        }
    }
}

@Composable
private fun formatElapsedTime(elapsedTime: Long): String {
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(elapsedTime)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
