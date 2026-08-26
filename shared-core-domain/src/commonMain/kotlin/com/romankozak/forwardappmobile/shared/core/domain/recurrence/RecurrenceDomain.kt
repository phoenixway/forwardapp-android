package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.recurrence.LocalDayKey
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import kotlin.js.JsExport

private data class LocalDayParts(
    val year: Int,
    val month: Int,
    val day: Int,
)

private val LOCAL_DAY_KEY_PATTERN = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun daysInMonth(
    year: Int,
    month: Int,
): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }

private fun parseLocalDayKey(dayKey: LocalDayKey): LocalDayParts {
    val match = LOCAL_DAY_KEY_PATTERN.matchEntire(dayKey)
        ?: throw IllegalArgumentException("Invalid LocalDayKey: $dayKey")

    val year = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val day = match.groupValues[3].toInt()

    // JavaScript Date.UTC, used by the original Desktop v2 oracle, treats
    // years 0..99 specially. Those values therefore fail its canonical
    // round-trip validation as well.
    if (year < 100 || month !in 1..12 || day !in 1..daysInMonth(year, month)) {
        throw IllegalArgumentException("Invalid calendar date: $dayKey")
    }

    return LocalDayParts(year = year, month = month, day = day)
}

private fun daysBeforeYear(year: Int): Long {
    val previousYear = year.toLong() - 1L
    return 365L * previousYear +
        previousYear / 4L -
        previousYear / 100L +
        previousYear / 400L
}

private fun daysBeforeMonth(
    year: Int,
    month: Int,
): Long {
    val commonYearOffsets = intArrayOf(
        0,
        0,
        31,
        59,
        90,
        120,
        151,
        181,
        212,
        243,
        273,
        304,
        334,
    )

    var result = commonYearOffsets[month].toLong()
    if (month > 2 && isLeapYear(year)) result += 1L
    return result
}

private fun absoluteDay(parts: LocalDayParts): Long =
    daysBeforeYear(parts.year) +
        daysBeforeMonth(parts.year, parts.month) +
        (parts.day - 1L)

private fun floorMod(
    value: Long,
    divisor: Int,
): Int {
    val raw = (value % divisor).toInt()
    return if (raw < 0) raw + divisor else raw
}

fun requireLocalDayKey(dayKey: LocalDayKey): LocalDayKey {
    parseLocalDayKey(dayKey)
    return dayKey
}

fun localDayKeyOf(
    year: Int,
    month: Int,
    day: Int,
): LocalDayKey =
    requireLocalDayKey(
        year.toString().padStart(4, '0') + "-" +
            month.toString().padStart(2, '0') + "-" +
            day.toString().padStart(2, '0'),
    )

fun previousLocalDayKey(dayKey: LocalDayKey): LocalDayKey {
    val parts = parseLocalDayKey(dayKey)

    if (parts.day > 1) {
        return localDayKeyOf(
            year = parts.year,
            month = parts.month,
            day = parts.day - 1,
        )
    }

    if (parts.month > 1) {
        val previousMonth = parts.month - 1
        return localDayKeyOf(
            year = parts.year,
            month = previousMonth,
            day = daysInMonth(parts.year, previousMonth),
        )
    }

    return localDayKeyOf(
        year = parts.year - 1,
        month = 12,
        day = 31,
    )
}

fun compareLocalDayKeys(
    left: LocalDayKey,
    right: LocalDayKey,
): Long = absoluteDay(parseLocalDayKey(left)) - absoluteDay(parseLocalDayKey(right))

fun localDayKeyDayOfWeek(dayKey: LocalDayKey): RecurrenceDayOfWeek {
    val parts = parseLocalDayKey(dayKey)

    // 0001-01-01 in the proleptic Gregorian calendar is Monday.
    val mondayBasedIndex = floorMod(absoluteDay(parts), 7)

    return when (mondayBasedIndex) {
        0 -> RecurrenceDayOfWeek.MONDAY
        1 -> RecurrenceDayOfWeek.TUESDAY
        2 -> RecurrenceDayOfWeek.WEDNESDAY
        3 -> RecurrenceDayOfWeek.THURSDAY
        4 -> RecurrenceDayOfWeek.FRIDAY
        5 -> RecurrenceDayOfWeek.SATURDAY
        6 -> RecurrenceDayOfWeek.SUNDAY
        else -> error("Unreachable weekday index: $mondayBasedIndex")
    }
}

fun recurrenceOccurrenceKey(
    seriesId: String,
    dayKey: LocalDayKey,
): String {
    requireLocalDayKey(dayKey)
    return "$seriesId@$dayKey"
}

fun recurrenceOccurrenceId(
    kind: RecurringSeriesKind,
    seriesId: String,
    dayKey: LocalDayKey,
): String {
    requireLocalDayKey(dayKey)
    return "recurrence:${kind.name}:$seriesId:$dayKey"
}

fun recurrenceOrigin(
    series: RecurringSeries,
    dayKey: LocalDayKey,
): RecurrenceOrigin {
    requireLocalDayKey(dayKey)
    return RecurrenceOrigin(
        seriesId = series.id,
        occurrenceDayKey = dayKey,
        sourceSeriesVersion = series.version,
    )
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun recurrenceRuleMatchesDay(
    rule: RecurrenceRule,
    startDayKey: LocalDayKey,
    targetDayKey: LocalDayKey,
): Boolean {
    val start = parseLocalDayKey(startDayKey)
    val target = parseLocalDayKey(targetDayKey)
    val startDay = absoluteDay(start)
    val targetDay = absoluteDay(target)

    if (targetDay < startDay) return false
    require(rule.interval >= 1) { "Invalid recurrence interval: ${rule.interval}" }

    val elapsedDays = targetDay - startDay

    return when (rule.frequency) {
        com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency.DAILY ->
            elapsedDays % rule.interval == 0L

        com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency.WEEKLY -> {
            val elapsedWeeks = elapsedDays / 7L
            if (elapsedWeeks % rule.interval != 0L) {
                false
            } else {
                val daysOfWeek =
                    rule.daysOfWeek ?: listOf(localDayKeyDayOfWeek(startDayKey))
                localDayKeyDayOfWeek(targetDayKey) in daysOfWeek
            }
        }

        com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency.MONTHLY -> {
            val elapsedMonths =
                (target.year - start.year) * 12 + (target.month - start.month)

            elapsedMonths % rule.interval == 0 && target.day == start.day
        }

        com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency.YEARLY -> {
            val elapsedYears = target.year - start.year

            elapsedYears % rule.interval == 0 &&
                target.month == start.month &&
                target.day == start.day
        }
    }
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun recurrenceScheduleMatchesDay(
    rule: RecurrenceRule,
    startDayKey: LocalDayKey,
    endDayKey: LocalDayKey?,
    isDeleted: Boolean,
    dayKey: LocalDayKey,
): Boolean {
    if (isDeleted) return false
    if (compareLocalDayKeys(dayKey, startDayKey) < 0L) return false
    if (endDayKey != null && compareLocalDayKeys(dayKey, endDayKey) > 0L) {
        return false
    }

    return recurrenceRuleMatchesDay(
        rule = rule,
        startDayKey = startDayKey,
        targetDayKey = dayKey,
    )
}

fun recurringSeriesMatchesDay(
    series: RecurringSeries,
    dayKey: LocalDayKey,
): Boolean =
    recurrenceScheduleMatchesDay(
        rule = series.rule,
        startDayKey = series.startDayKey,
        endDayKey = series.endDayKey,
        isDeleted = series.isDeleted,
        dayKey = dayKey,
    )
