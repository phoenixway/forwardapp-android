package com.romankozak.forwardappmobile.features.daymanagement.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val MINUTES_PER_HOUR = 60
private const val RELATIVE_DAY_RANGE = 7

private val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

fun formatDayDate(timestamp: Long): String = dayDateFormat.format(Date(timestamp))

fun formatDayTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

fun formatDayDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

fun formatDayDuration(durationMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % MINUTES_PER_HOUR
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % MINUTES_PER_HOUR

    return when {
        hours > 0 -> "${hours}г ${minutes}хв"
        minutes > 0 -> "${minutes}хв ${seconds}с"
        else -> "${seconds}с"
    }
}

fun describeDayDate(timestamp: Long): String {
    return when {
        DayManagementUtils.isToday(timestamp) -> "Сьогодні"
        DayManagementUtils.isYesterday(timestamp) -> "Вчора"
        DayManagementUtils.getDayStart(timestamp) == getTomorrow() -> "Завтра"
        else -> {
            val daysDifference = getDaysDifference(getCurrentDay(), timestamp)
            when {
                daysDifference in 1..RELATIVE_DAY_RANGE -> "Через $daysDifference д."
                daysDifference in -RELATIVE_DAY_RANGE..-1 -> "${-daysDifference} д. тому"
                else -> formatDayDate(timestamp)
            }
        }
    }
}
