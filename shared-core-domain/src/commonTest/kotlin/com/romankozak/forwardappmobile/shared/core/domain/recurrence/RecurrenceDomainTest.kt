package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecurrenceDomainTest {
    private fun taskSeries(
        startDayKey: String = "2026-08-17",
        endDayKey: String? = null,
        isDeleted: Boolean = false,
        rule: RecurrenceRule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = 1,
                daysOfWeek = null,
            ),
    ): RecurringTaskSeries =
        RecurringTaskSeries(
            id = "series:task:daily",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 3L,
            rule = rule,
            startDayKey = startDayKey,
            endDayKey = endDayKey,
            template =
                RecurringTaskTemplate(
                    title = "Daily task",
                    description = null,
                    goalId = null,
                    linkedProjectIds = emptyList(),
                    linkedAttachmentIds = emptyList(),
                    priority = TaskPriority.MEDIUM,
                    estimatedDurationMinutes = 30L,
                    points = 5,
                ),
        )

    @Test
    fun validatesLocalDayKeyStrictly() {
        assertEquals("2026-08-19", requireLocalDayKey("2026-08-19"))

        assertFailsWith<IllegalArgumentException> { requireLocalDayKey("2026-8-19") }
        assertFailsWith<IllegalArgumentException> { requireLocalDayKey("2026-02-30") }
        assertFailsWith<IllegalArgumentException> { requireLocalDayKey("0099-01-01") }
    }

    @Test
    fun calculatesCanonicalWeekdays() {
        assertEquals(RecurrenceDayOfWeek.MONDAY, localDayKeyDayOfWeek("2024-01-01"))
        assertEquals(RecurrenceDayOfWeek.WEDNESDAY, localDayKeyDayOfWeek("2026-08-19"))
    }

    @Test
    fun buildsCanonicalLogicalAndPhysicalOccurrenceIdentity() {
        assertEquals(
            "series:task:daily@2026-08-17",
            recurrenceOccurrenceKey("series:task:daily", "2026-08-17"),
        )
        assertEquals(
            "recurrence:TASK:series:task:daily:2026-08-17",
            recurrenceOccurrenceId(
                RecurringSeriesKind.TASK,
                "series:task:daily",
                "2026-08-17",
            ),
        )
    }

    @Test
    fun treatsSeriesBoundariesAsInclusive() {
        val series = taskSeries(
            startDayKey = "2026-08-17",
            endDayKey = "2026-08-18",
        )

        assertFalse(recurringSeriesMatchesDay(series, "2026-08-16"))
        assertTrue(recurringSeriesMatchesDay(series, "2026-08-17"))
        assertTrue(recurringSeriesMatchesDay(series, "2026-08-18"))
        assertFalse(recurringSeriesMatchesDay(series, "2026-08-19"))
    }

    @Test
    fun appliesDailyInterval() {
        val series = taskSeries(
            startDayKey = "2024-01-01",
            rule = RecurrenceRule(RecurrenceFrequency.DAILY, 3, null),
        )

        assertTrue(recurringSeriesMatchesDay(series, "2024-01-01"))
        assertFalse(recurringSeriesMatchesDay(series, "2024-01-02"))
        assertFalse(recurringSeriesMatchesDay(series, "2024-01-03"))
        assertTrue(recurringSeriesMatchesDay(series, "2024-01-04"))
    }

    @Test
    fun appliesWeeklyIntervalAndDaysOfWeek() {
        val series = taskSeries(
            startDayKey = "2024-01-01",
            rule =
                RecurrenceRule(
                    frequency = RecurrenceFrequency.WEEKLY,
                    interval = 2,
                    daysOfWeek = listOf(RecurrenceDayOfWeek.WEDNESDAY),
                ),
        )

        assertTrue(recurringSeriesMatchesDay(series, "2024-01-03"))
        assertFalse(recurringSeriesMatchesDay(series, "2024-01-10"))
        assertTrue(recurringSeriesMatchesDay(series, "2024-01-17"))
    }

    @Test
    fun weeklyNullDaysUsesStartWeekday() {
        val series = taskSeries(
            startDayKey = "2024-01-01",
            rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, 1, null),
        )

        assertTrue(recurringSeriesMatchesDay(series, "2024-01-01"))
        assertTrue(recurringSeriesMatchesDay(series, "2024-01-08"))
        assertFalse(recurringSeriesMatchesDay(series, "2024-01-02"))
    }

    @Test
    fun monthlyRecurrenceDoesNotClampDay31() {
        val series = taskSeries(
            startDayKey = "2024-01-31",
            rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, 2, null),
        )

        assertFalse(recurringSeriesMatchesDay(series, "2024-02-29"))
        assertTrue(recurringSeriesMatchesDay(series, "2024-03-31"))
        assertFalse(recurringSeriesMatchesDay(series, "2024-04-30"))
        assertTrue(recurringSeriesMatchesDay(series, "2024-05-31"))
    }

    @Test
    fun yearlyRecurrenceHonorsInterval() {
        val series = taskSeries(
            startDayKey = "2024-06-15",
            rule = RecurrenceRule(RecurrenceFrequency.YEARLY, 2, null),
        )

        assertFalse(recurringSeriesMatchesDay(series, "2025-06-15"))
        assertTrue(recurringSeriesMatchesDay(series, "2026-06-15"))
    }

    @Test
    fun leapDayYearlyRecurrenceDoesNotClamp() {
        val series = taskSeries(
            startDayKey = "2024-02-29",
            rule = RecurrenceRule(RecurrenceFrequency.YEARLY, 1, null),
        )

        assertFalse(recurringSeriesMatchesDay(series, "2025-02-28"))
        assertTrue(recurringSeriesMatchesDay(series, "2028-02-29"))
    }

    @Test
    fun deletedSeriesNeverMatches() {
        assertFalse(
            recurringSeriesMatchesDay(
                taskSeries(isDeleted = true),
                "2026-08-17",
            ),
        )
    }

    @Test
    fun invalidIntervalFails() {
        val series = taskSeries(
            rule = RecurrenceRule(RecurrenceFrequency.DAILY, 0, null),
        )

        assertFailsWith<IllegalArgumentException> {
            recurringSeriesMatchesDay(series, "2026-08-17")
        }
    }
}
