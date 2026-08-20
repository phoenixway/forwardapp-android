package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalFocusRecurrenceConversionTest {
    @Test
    fun `one off focus converts to canonical series and occurrence atomically`() = runTest {
        val appDatabase = mockk<AppDatabase>()
        val dayPlanDao = mockk<DayPlanDao>()
        val seriesDao = mockk<CanonicalRecurringSeriesDao>(relaxed = true)
        val materializer = mockk<CanonicalRecurrenceMaterializationAdapter>(relaxed = true)
        val dayPlan = mockk<DayPlan>()

        every { appDatabase.dayPlanDao() } returns dayPlanDao
        every { appDatabase.canonicalRecurringSeriesDao() } returns seriesDao
        every { dayPlan.date } returns 1_777_000_000_000L
        coEvery { dayPlanDao.getPlanById("plan") } returns dayPlan

        val source =
            DayFocusItem(
                id = "one-off-focus",
                dayPlanId = "plan",
                title = "Old title",
                type = DayFocusType.FOCUS,
                isEveryday = false,
                recurringKey = null,
                order = 5,
                version = 7,
            )

        val seriesSlot = slot<com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity>()
        val occurrenceSlot = slot<DayFocusItem>()

        val adapter =
            CanonicalFocusRecurrenceAuthoringAdapter(
                appDatabase = appDatabase,
                materializationAdapter = materializer,
            )

        val returned =
            adapter.convertOneOffToSeries(
                item = source,
                title = "Recurring title",
                notes = " recurring notes ",
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                budgetPercent = 35,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 1,
                        daysOfWeek = null,
                    ),
            )

        coVerify(exactly = 1) {
            seriesDao.convertOneOffToCanonicalSeries(
                series = capture(seriesSlot),
                occurrence = capture(occurrenceSlot),
                sourceItemId = "one-off-focus",
                sourceExpectedVersion = 7,
                updatedAt = any(),
            )
        }
        coVerify(exactly = 0) { materializer.materializeForDate(any(), any()) }

        val series = seriesSlot.captured.toCanonicalSeries() as RecurringFocusSeries
        val occurrence = occurrenceSlot.captured
        val dayKey = requireNotNull(occurrence.recurrenceOccurrenceDayKey)

        assertEquals(series.id, occurrence.recurrenceSeriesId)
        assertEquals(series.startDayKey, dayKey)
        assertEquals("recurrence:FOCUS:${series.id}:$dayKey", occurrence.id)
        assertEquals(1L, occurrence.recurrenceSourceSeriesVersion)
        assertEquals("Recurring title", occurrence.title)
        assertEquals("recurring notes", occurrence.notes)
        assertEquals(35, occurrence.budgetPercent)
        assertEquals(5L, occurrence.order)
        assertEquals(1L, occurrence.version)
        assertFalse(occurrence.isDeleted)
        assertFalse(occurrence.isEveryday)
        assertNull(occurrence.recurringKey)
        assertEquals(occurrence, returned)
    }
}
