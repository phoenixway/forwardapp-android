package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertTrue

class RecurrenceJsFacadeTest {
    @Test
    fun `js recurrence origin factory converts safe number to canonical long`() {
        val origin =
            createRecurrenceOriginForJs(
                seriesId = "series-1",
                occurrenceDayKey = "2026-08-26",
                sourceSeriesVersion = 42.0,
            )

        assertEquals("series-1", origin.seriesId)
        assertEquals("2026-08-26", origin.occurrenceDayKey)
        assertEquals(42L, origin.sourceSeriesVersion)
    }

    @Test
    fun `js long conversion rejects fractional and unsafe numbers`() {
        assertFails {
            createRecurrenceOriginForJs(
                "series-1",
                "2026-08-26",
                1.5,
            )
        }

        assertFails {
            createRecurrenceOriginForJs(
                "series-1",
                "2026-08-26",
                9_007_199_254_740_992.0,
            )
        }
    }

    @Test
    fun `js recurrence rule factory creates canonical model`() {
        val rule =
            createRecurrenceRuleForJs(
                frequency = "weekly",
                interval = 2,
                daysOfWeek = arrayOf("MONDAY", "wednesday"),
            )

        assertEquals(RecurrenceFrequency.WEEKLY, rule.frequency)
        assertEquals(2, rule.interval)
        assertEquals(
            listOf(
                RecurrenceDayOfWeek.MONDAY,
                RecurrenceDayOfWeek.WEDNESDAY,
            ),
            rule.daysOfWeek,
        )

        val implicitWeekdayRule =
            createRecurrenceRuleForJs(
                frequency = "DAILY",
                interval = 1,
                daysOfWeek = emptyArray(),
            )

        assertEquals(null, implicitWeekdayRule.daysOfWeek)
    }

    @Test
    fun `js occurrence id facade delegates to canonical identity`() {
        assertEquals(
            "recurrence:TASK:series-1:2026-08-25",
            recurrenceOccurrenceIdForJs(
                kind = "task",
                seriesId = "series-1",
                dayKey = "2026-08-25",
            ),
        )
    }

    @Test
    fun `js series matcher delegates lifecycle boundaries and rule semantics`() {
        assertFalse(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs("DAILY", 1, null),
                startDayKey = "2026-08-17",
                endDayKey = "2026-08-18",
                isDeleted = false,
                dayKey = "2026-08-16",
            ),
        )

        assertTrue(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs("DAILY", 1, null),
                startDayKey = "2026-08-17",
                endDayKey = "2026-08-18",
                isDeleted = false,
                dayKey = "2026-08-18",
            ),
        )

        assertFalse(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs("DAILY", 1, null),
                startDayKey = "2026-08-17",
                endDayKey = "2026-08-18",
                isDeleted = false,
                dayKey = "2026-08-19",
            ),
        )

        assertFalse(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs("DAILY", 1, null),
                startDayKey = "2026-08-17",
                endDayKey = null,
                isDeleted = true,
                dayKey = "2026-08-17",
            ),
        )
    }

    @Test
    fun `js series matcher delegates weekly rule semantics`() {
        assertTrue(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs(
                    "WEEKLY",
                    2,
                    arrayOf("WEDNESDAY"),
                ),
                startDayKey = "2024-01-03",
                endDayKey = null,
                isDeleted = false,
                dayKey = "2024-01-17",
            ),
        )

        assertFalse(
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs(
                    "WEEKLY",
                    2,
                    arrayOf("WEDNESDAY"),
                ),
                startDayKey = "2024-01-03",
                endDayKey = null,
                isDeleted = false,
                dayKey = "2024-01-10",
            ),
        )
    }

    @Test
    fun `js facade preserves canonical validation`() {
        assertFails {
            requireLocalDayKeyForJs("2026-02-30")
        }

        assertFails {
            recurrenceScheduleMatchesDay(
                rule = createRecurrenceRuleForJs("DAILY", 0, null),
                startDayKey = "2026-08-25",
                endDayKey = null,
                isDeleted = false,
                dayKey = "2026-08-25",
            )
        }
    }
}
