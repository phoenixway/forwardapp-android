package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun ActivityRecordTimeEditor(
    startTime: Long,
    endTime: Long?,
    isTimed: Boolean,
    canRemainOngoing: Boolean,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onMakeOngoing: () -> Unit,
) {
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    val invalidRange = isTimed && endTime != null && endTime < startTime

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = if (isTimed) "Часові межі" else "Час події",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        TimeBoundaryCard(
            label = "Початок",
            timestamp = startTime,
            icon = Icons.Default.PlayArrow,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
            onClick = onStartClick,
        )
        if (isTimed) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (invalidRange) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text =
                        when {
                            invalidRange -> "Завершення не може бути раніше початку"
                            endTime == null -> "Активність триває"
                            else -> "Тривалість: ${formatActivityDuration(endTime - startTime)}"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (invalidRange) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TimeBoundaryCard(
                label = "Завершення",
                timestamp = endTime,
                icon = Icons.Default.StopCircle,
                dateFormatter = dateFormatter,
                timeFormatter = timeFormatter,
                onClick = onEndClick,
            )
            if (canRemainOngoing && endTime != null) {
                TextButton(onClick = onMakeOngoing, modifier = Modifier.align(Alignment.End)) {
                    Text("Залишити активність незавершеною")
                }
            }
        }
    }
}

@Composable
private fun TimeBoundaryCard(
    label: String,
    timestamp: Long?,
    icon: ImageVector,
    dateFormatter: DateFormat,
    timeFormatter: DateFormat,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = timestamp?.let { dateFormatter.format(Date(it)) } ?: "Ще триває",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = timestamp?.let { timeFormatter.format(Date(it)) } ?: "Зараз",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatActivityDuration(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours год $minutes хв"
        hours > 0 -> "$hours год"
        else -> "$minutes хв"
    }
}
