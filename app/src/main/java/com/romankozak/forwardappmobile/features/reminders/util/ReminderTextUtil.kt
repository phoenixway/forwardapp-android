package com.romankozak.forwardappmobile.features.reminders.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

    fun formatDateTime(timeMillis: Long): String =
    SimpleDateFormat("dd.MM.yyyy 'о' HH:mm", Locale.getDefault()).format(Date(timeMillis))
}