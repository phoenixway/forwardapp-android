package com.romankozak.forwardappmobile.features.daymanagement.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DayManagementUtils {
    
    private val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    
    fun getDayStart(timestamp: Long): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return calendar.timeInMillis
    }

    
    fun getDayEnd(timestamp: Long): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        return calendar.timeInMillis
    }

    
    fun getCurrentDay(): Long = getDayStart(System.currentTimeMillis())

    
    fun getYesterday(): Long = getCurrentDay() - TimeUnit.DAYS.toMillis(1)

    
    fun getTomorrow(): Long = getCurrentDay() + TimeUnit.DAYS.toMillis(1)

    
    fun isToday(timestamp: Long): Boolean {
        return getDayStart(timestamp) == getCurrentDay()
    }

    
    fun isYesterday(timestamp: Long): Boolean {
        return getDayStart(timestamp) == getYesterday()
    }

    
    fun getDayName(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Понеділок"
            Calendar.TUESDAY -> "Вівторок"
            Calendar.WEDNESDAY -> "Середа"
            Calendar.THURSDAY -> "Четвер"
            Calendar.FRIDAY -> "П'ятниця"
            Calendar.SATURDAY -> "Субота"
            Calendar.SUNDAY -> "Неділя"
            else -> "Невідомо"
        }
    }

    
    fun formatDate(timestamp: Long): String = dayDateFormat.format(Date(timestamp))

    
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    
    fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        return when {
            hours > 0 -> "${hours}г ${minutes}хв"
            minutes > 0 -> "${minutes}хв ${seconds}с"
            else -> "${seconds}с"
        }
    }

    
    fun createTimeInDay(
        dayTimestamp: Long,
        hours: Int,
        minutes: Int,
    ): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = dayTimestamp
                set(Calendar.HOUR_OF_DAY, hours)
                set(Calendar.MINUTE, minutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return calendar.timeInMillis
    }

    
    private fun getDaysDifference(
        from: Long,
        to: Long,
    ): Int {
        return ((getDayStart(to) - getDayStart(from)) / TimeUnit.DAYS.toMillis(1)).toInt()
    }

    
    fun getDateDescription(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Сьогодні"
            isYesterday(timestamp) -> "Вчора"
            getDayStart(timestamp) == getTomorrow() -> "Завтра"
            else -> {
                val daysDifference = getDaysDifference(getCurrentDay(), timestamp)
                when {
                    daysDifference in 1..7 -> "Через $daysDifference д."
                    daysDifference in -7..-1 -> "${-daysDifference} д. тому"
                    else -> formatDate(timestamp)
                }
            }
        }
    }
}


fun Long.toDayStart(): Long = DayManagementUtils.getDayStart(this)

fun Long.isToday(): Boolean = DayManagementUtils.isToday(this)

fun Long.formatAsDate(): String = DayManagementUtils.formatDate(this)

fun Long.formatAsTime(): String = DayManagementUtils.formatTime(this)

fun Long.formatAsDateTime(): String = DayManagementUtils.formatDateTime(this)

fun Long.formatAsDuration(): String = DayManagementUtils.formatDuration(this)

fun Long.getDayName(): String = DayManagementUtils.getDayName(this)

fun Long.getDateDescription(): String = DayManagementUtils.getDateDescription(this)
