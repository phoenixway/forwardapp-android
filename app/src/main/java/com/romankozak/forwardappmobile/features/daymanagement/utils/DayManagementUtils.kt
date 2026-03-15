package com.romankozak.forwardappmobile.features.daymanagement.utils

import java.util.Calendar
import java.util.concurrent.TimeUnit

object DayManagementUtils {
    private const val START_OF_DAY_HOUR = 0
    private const val START_OF_DAY_MINUTE = 0
    private const val START_OF_DAY_SECOND = 0
    private const val START_OF_DAY_MILLISECOND = 0
    private const val END_OF_DAY_HOUR = 23
    private const val END_OF_DAY_MINUTE = 59
    private const val END_OF_DAY_SECOND = 59
    private const val END_OF_DAY_MILLISECOND = 999
    fun getDayStart(timestamp: Long): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, START_OF_DAY_HOUR)
                set(Calendar.MINUTE, START_OF_DAY_MINUTE)
                set(Calendar.SECOND, START_OF_DAY_SECOND)
                set(Calendar.MILLISECOND, START_OF_DAY_MILLISECOND)
            }
        return calendar.timeInMillis
    }

    fun getDayEnd(timestamp: Long): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
                set(Calendar.MINUTE, END_OF_DAY_MINUTE)
                set(Calendar.SECOND, END_OF_DAY_SECOND)
                set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
            }
        return calendar.timeInMillis
    }

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

}

fun getCurrentDay(): Long = DayManagementUtils.getDayStart(System.currentTimeMillis())

fun getYesterday(): Long = getCurrentDay() - TimeUnit.DAYS.toMillis(1)

fun getTomorrow(): Long = getCurrentDay() + TimeUnit.DAYS.toMillis(1)

fun getDaysDifference(
    from: Long,
    to: Long,
): Int {
    return (
        (DayManagementUtils.getDayStart(to) - DayManagementUtils.getDayStart(from)) /
            TimeUnit.DAYS.toMillis(1)
    ).toInt()
}
