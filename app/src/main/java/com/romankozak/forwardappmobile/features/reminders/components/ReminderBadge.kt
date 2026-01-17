package com.romankozak.forwardappmobile.features.reminders.components

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.features.reminders.util.ReminderTextUtil

@Composable
fun ReminderBadge(
    reminder: Reminder,
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(reminder.reminderTime) {
        val reminderTime = reminder.reminderTime
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = now

            val diff = reminderTime - now
            val delayMillis = when {
                diff <= 0 -> 60_000L // Оновлювати раз на хвилину, якщо пропущено
                diff < 60_000L -> 1_000L // Оновлювати кожну секунду, якщо менше хвилини
                else -> (diff % 60_000L).takeIf { it > 0 } ?: 60_000L // Оновлювати на початку наступної хвилини
            }
            delay(delayMillis)
        }
    }

    val isCompleted = reminder.status == "COMPLETED"
    val isSnoozed = reminder.status == "SNOOZED"
    val isDismissed = reminder.status == "DISMISSED"
    val isPastDue = reminder.reminderTime < currentTime && !isCompleted && !isSnoozed && !isDismissed

    val reminderText = when {
        isCompleted -> "Виконано"
        isSnoozed -> "Відкладено"
        isDismissed -> "Пропущено"
        isPastDue -> "Прострочено"
        else -> remember(reminder.reminderTime, currentTime) {
            ReminderTextUtil.formatReminderTime(reminder.reminderTime, currentTime)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isSnoozed -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            isDismissed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            isPastDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            isSnoozed -> MaterialTheme.colorScheme.secondary
            isDismissed -> MaterialTheme.colorScheme.onSurfaceVariant
            isPastDue -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        },
        label = "reminder_badge_content",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when {
                    isCompleted -> Icons.Default.CheckCircle
                    isSnoozed -> Icons.Default.Snooze
                    isDismissed -> Icons.Default.AlarmOff
                    isPastDue -> Icons.Default.AlarmOff
                    else -> Icons.Default.AlarmOn
                },
                contentDescription = "Нагадування",
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = reminderText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
