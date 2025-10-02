package com.romankozak.forwardappmobile.ui.common.components

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

object ReminderTextUtil {
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

        val dateFormat = SimpleDateFormat("d MMM о HH:mm", Locale("uk", "UA"))
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
fun EnhancedReminderBadge(
    reminderTime: Long,
    currentTimeMillis: Long,
    isCompleted: Boolean = false
) {
    val reminderText = if (isCompleted) {
        "Completed"
    } else {
        remember(reminderTime, currentTimeMillis) {
            ReminderTextUtil.formatReminderTime(reminderTime, currentTimeMillis)
        }
    }
    val isPastDue = reminderTime < currentTimeMillis && !isCompleted

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isPastDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
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
                imageVector = if (isPastDue) Icons.Default.AlarmOff else Icons.Default.AlarmOn,
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
