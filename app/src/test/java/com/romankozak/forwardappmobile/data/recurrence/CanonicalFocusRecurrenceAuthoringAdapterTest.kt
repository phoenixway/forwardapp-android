package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.RecurrenceMaterializationPlan
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
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

class CanonicalFocusRecurrenceAuthoringAdapterTest {
    @Test
    fun `everyday focus creates canonical daily focus series and materialized occurrence`() = runTest {
        verifyCreate(DayFocusType.FOCUS)
    }

    @Test
    fun `everyday responsibility creates canonical daily responsibility series and materialized occurrence`() = runTest {
        verifyCreate(DayFocusType.RESPONSIBILITY)
    }

    private suspend fun verifyCreate(type: DayFocusType) {
        val appDatabase = mockk<AppDatabase>()
        val dayPlanDao = mockk<DayPlanDao>()
        val seriesDao = mockk<CanonicalRecurringSeriesDao>(relaxed = true)
        val focusDao = mockk<DayFocusItemDao>()
        val materializer = mockk<CanonicalRecurrenceMaterializationAdapter>()
        val dayPlan = mockk<DayPlan>()
        val result = mockk<RecurrenceMaterializationPlan>()
        val occurrence = mockk<DayFocusItem>()
        val capturedSeries = slot<com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity>()

        every { appDatabase.dayPlanDao() } returns dayPlanDao
        every { appDatabase.canonicalRecurringSeriesDao() } returns seriesDao
        every { appDatabase.dayFocusItemDao() } returns focusDao
        every { dayPlan.date } returns 1_777_000_000_000L
        coEvery { dayPlanDao.getPlanById("plan") } returns dayPlan
        every { result.dayKey } returns "2026-04-25"
        coEvery { materializer.materializeForDate(1_777_000_000_000L, any()) } returns result
        coEvery { focusDao.getByIdForCanonicalRecurrenceSync(any()) } returns occurrence

        val adapter =
            CanonicalFocusRecurrenceAuthoringAdapter(
                appDatabase = appDatabase,
                materializationAdapter = materializer,
            )

        val returned =
            adapter.createDailySeriesForPlan(
                dayPlanId = "plan",
                title = "Canonical recurring item",
                notes = " notes ",
                relatedLinks = emptyList(),
                type = type,
                budgetPercent = 25,
            )

        coVerify(exactly = 1) { seriesDao.insert(capture(capturedSeries)) }
        coVerify(exactly = 1) { materializer.materializeForDate(1_777_000_000_000L, any()) }
        assertEquals(occurrence, returned)

        val series = capturedSeries.captured.toCanonicalSeries()
        assertEquals(RecurrenceFrequency.DAILY, series.rule.frequency)
        assertEquals(1, series.rule.interval)
        assertNull(series.rule.daysOfWeek)
        assertNull(series.endDayKey)
        assertFalse(series.isDeleted)
        assertEquals(1L, series.version)

        when (type) {
            DayFocusType.FOCUS -> {
                val focusSeries = series as RecurringFocusSeries
                assertEquals("Canonical recurring item", focusSeries.template.title)
                assertEquals("notes", focusSeries.template.notes)
                assertEquals(25, focusSeries.template.budgetPercent)
            }

            DayFocusType.RESPONSIBILITY -> {
                val responsibilitySeries = series as RecurringResponsibilitySeries
                assertEquals("Canonical recurring item", responsibilitySeries.template.title)
                assertEquals("notes", responsibilitySeries.template.notes)
                assertEquals(25, responsibilitySeries.template.budgetPercent)
            }

            else -> error("Unexpected test type: $type")
        }
    }
}
