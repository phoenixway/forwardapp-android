package com.romankozak.forwardappmobile.features.reminders.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.features.reminders.util.ReminderTextUtil
import kotlinx.coroutines.delay

private const val OVERDUE_UPDATE_INTERVAL_MILLIS = 60_000L
private const val URGENT_UPDATE_INTERVAL_MILLIS = 1_000L
private const val DEFAULT_BADGE_UPDATE_INTERVAL_MILLIS = 60_000L
private const val BADGE_CORNER_RADIUS_DP = 16
private const val BADGE_ICON_SIZE_DP = 14
private const val BADGE_FONT_SIZE_SP = 10

@Composable
fun ReminderBadge(reminder: Reminder) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(reminder.reminderTime) {
        val reminderTime = reminder.reminderTime
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = now

            val diff = reminderTime - now
            val delayMillis =
                when {
                    diff <= 0 -> OVERDUE_UPDATE_INTERVAL_MILLIS
                    diff < OVERDUE_UPDATE_INTERVAL_MILLIS -> URGENT_UPDATE_INTERVAL_MILLIS
                    else ->
                        (diff % OVERDUE_UPDATE_INTERVAL_MILLIS).takeIf { it > 0 }
                            ?: DEFAULT_BADGE_UPDATE_INTERVAL_MILLIS
                }
            delay(delayMillis)
        }
    }

    val isCompleted = reminder.status == "COMPLETED"
    val isSnoozed = reminder.status == "SNOOZED"
    val isDismissed = reminder.status == "DISMISSED"
    val isPastDue = reminder.reminderTime < currentTime && !isCompleted && !isSnoozed && !isDismissed

    val reminderText =
        when {
            isCompleted -> "Виконано"
            isSnoozed -> "Відкладено"
            isDismissed -> "Пропущено"
            isPastDue -> "Прострочено"
            else ->
                remember(reminder.reminderTime, currentTime) {
                    ReminderTextUtil.formatReminderTime(reminder.reminderTime, currentTime)
                }
        }

    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                isSnoozed -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                isDismissed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isPastDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isSnoozed -> MaterialTheme.colorScheme.secondary
                isDismissed -> MaterialTheme.colorScheme.onSurfaceVariant
                isPastDue -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            },
        label = "reminder_badge_content",
    )

    Surface(
        shape = RoundedCornerShape(BADGE_CORNER_RADIUS_DP.dp),
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
                imageVector =
                    when {
                        isCompleted -> Icons.Default.CheckCircle
                        isSnoozed -> Icons.Default.Snooze
                        isDismissed -> Icons.Default.AlarmOff
                        isPastDue -> Icons.Default.AlarmOff
                        else -> Icons.Default.AlarmOn
                    },
                contentDescription = "Нагадування",
                tint = contentColor,
                modifier = Modifier.size(BADGE_ICON_SIZE_DP.dp),
            )
            Text(
                text = reminderText,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = BADGE_FONT_SIZE_SP.sp,
                    ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
