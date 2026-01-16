package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.backlogitems

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Reminder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal object ReminderTextUtil {
    private const val ONE_MINUTE_MILLIS = 60 * 1000L
    private const val ONE_HOUR_MILLIS = 60 * ONE_MINUTE_MILLIS

    fun formatReminderTime(
        reminderTime: Long,
        now: Long,
    ): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = reminderTime

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(calendar.time)

        if (reminderTime < now) {
            val relativeTime =
                DateUtils
                    .getRelativeTimeSpanString(
                        reminderTime,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                    ).toString()
            return "Пропущено ($relativeTime)"
        }

        val diff = reminderTime - now
        if (diff < ONE_MINUTE_MILLIS) {
            return "Через хвилину"
        }
        if (diff < ONE_HOUR_MILLIS) {
            val minutes = (diff / ONE_MINUTE_MILLIS).toInt()
            return "Через $minutes хв"
        }

        if (DateUtils.isToday(reminderTime)) {
            return "Сьогодні о $formattedTime"
        }

        if (isTomorrow(reminderTime)) {
            return "Завтра о $formattedTime"
        }

        val dateFormat = SimpleDateFormat("d MMM о HH:mm", Locale.forLanguageTag("uk-UA"))
        return dateFormat.format(calendar.time)
    }

    private fun isTomorrow(time: Long): Boolean {
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)

        val target = Calendar.getInstance()
        target.timeInMillis = time

        return tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
}

@Composable
internal fun EnhancedReminderBadge(
    reminder: Reminder,
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val reminderText = if (reminder.status == "COMPLETED") {
        "Completed"
    } else if (reminder.status == "SNOOZED") {
        "Snoozed"
    } else {
        remember(reminder.reminderTime, currentTime) {
            ReminderTextUtil.formatReminderTime(reminder.reminderTime, currentTime)
        }
    }
    val isPastDue = reminder.reminderTime < currentTime && reminder.status != "COMPLETED"

    val backgroundColor by animateColorAsState(
        targetValue = when {
            reminder.status == "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isPastDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            reminder.status == "SNOOZED" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            reminder.status == "COMPLETED" -> MaterialTheme.colorScheme.primary
            isPastDue -> MaterialTheme.colorScheme.error
            reminder.status == "SNOOZED" -> MaterialTheme.colorScheme.secondary
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
                    isPastDue -> Icons.Default.AlarmOff
                    reminder.status == "SNOOZED" -> Icons.Default.Snooze
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
