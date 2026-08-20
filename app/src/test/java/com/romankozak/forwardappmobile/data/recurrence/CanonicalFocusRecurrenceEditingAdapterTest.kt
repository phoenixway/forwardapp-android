package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalFocusRecurrenceEditingAdapterTest {
    @Test
    fun `focus series edit propagates selected and clean occurrences but preserves override and tombstone`() = runTest {
        verifySeriesEdit(DayFocusType.FOCUS)
    }

    @Test
    fun `responsibility series edit propagates selected and clean occurrences but preserves override and tombstone`() = runTest {
        verifySeriesEdit(DayFocusType.RESPONSIBILITY)
    }

    private suspend fun verifySeriesEdit(type: DayFocusType) {
        val appDatabase = mockk<AppDatabase>()
        val seriesDao = mockk<CanonicalRecurringSeriesDao>(relaxed = true)
        val focusDao = mockk<DayFocusItemDao>(relaxed = true)
        val materializer = mockk<CanonicalRecurrenceMaterializationAdapter>(relaxed = true)

        every { appDatabase.canonicalRecurringSeriesDao() } returns seriesDao
        every { appDatabase.dayFocusItemDao() } returns focusDao

        val seriesId = if (type == DayFocusType.FOCUS) "focus-series" else "responsibility-series"
        val series = canonicalSeries(type = type, seriesId = seriesId)
        val selected = occurrence(type, seriesId, "2026-08-20", "Old title", 3)
        val cleanFuture = occurrence(type, seriesId, "2026-08-21", "Old title", 3)
        val customizedFuture = occurrence(type, seriesId, "2026-08-22", "Custom title", 3)
        val tombstone =
            occurrence(type, seriesId, "2026-08-23", "Old title", 3).copy(
                isDeleted = true,
            )

        coEvery { seriesDao.getById(seriesId) } returns series.toAndroidEntity()
        coEvery { seriesDao.getFocusOccurrencesForSeries(seriesId) } returns
            listOf(selected, cleanFuture, customizedFuture, tombstone)

        val updatedSeriesSlot = slot<CanonicalRecurringSeriesEntity>()
        val updatedOccurrencesSlot = slot<List<DayFocusItem>>()

        val adapter =
            CanonicalFocusRecurrenceAuthoringAdapter(
                appDatabase = appDatabase,
                materializationAdapter = materializer,
            )

        val updatedSelected =
            adapter.updateSeriesTemplate(
                item = selected,
                title = "New title",
                notes = " new notes ",
                relatedLinks = emptyList(),
                budgetPercent = 40,
            )

        coVerify(exactly = 1) {
            seriesDao.updateSeriesAndFocusOccurrences(
                series = capture(updatedSeriesSlot),
                occurrences = capture(updatedOccurrencesSlot),
            )
        }

        val updatedSeries = updatedSeriesSlot.captured.toCanonicalSeries()
        assertEquals(4L, updatedSeries.version)
        assertNull(updatedSeries.syncedAt)
        assertFalse(updatedSeries.isDeleted)

        val template =
            when (updatedSeries) {
                is RecurringFocusSeries -> updatedSeries.template
                is RecurringResponsibilitySeries -> updatedSeries.template
                else -> error("Unexpected series kind: ${updatedSeries.kind}")
            }
        assertEquals("New title", template.title)
        assertEquals("new notes", template.notes)
        assertEquals(40, template.budgetPercent)

        val changedById = updatedOccurrencesSlot.captured.associateBy { it.id }
        assertEquals(2, changedById.size)
        assertTrue(selected.id in changedById)
        assertTrue(cleanFuture.id in changedById)
        assertFalse(customizedFuture.id in changedById)
        assertFalse(tombstone.id in changedById)

        changedById.values.forEach { occurrence ->
            assertEquals("New title", occurrence.title)
            assertEquals("new notes", occurrence.notes)
            assertEquals(40, occurrence.budgetPercent)
            assertEquals(4L, occurrence.recurrenceSourceSeriesVersion)
            assertNull(occurrence.syncedAt)
        }

        assertEquals("New title", updatedSelected.title)
        assertEquals(4L, updatedSelected.recurrenceSourceSeriesVersion)
    }

    private fun canonicalSeries(
        type: DayFocusType,
        seriesId: String,
    ): RecurringSeries {
        val template =
            RecurringFocusTemplate(
                title = "Old title",
                notes = null,
                relatedLinks = emptyList(),
                budgetPercent = 20,
            )
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = 1,
                daysOfWeek = null,
            )

        return when (type) {
            DayFocusType.FOCUS ->
                RecurringFocusSeries(
                    id = seriesId,
                    createdAt = 1_000,
                    updatedAt = 2_000,
                    syncedAt = 2_000,
                    isDeleted = false,
                    version = 3,
                    rule = rule,
                    startDayKey = "2026-08-20",
                    endDayKey = null,
                    template = template,
                )

            DayFocusType.RESPONSIBILITY ->
                RecurringResponsibilitySeries(
                    id = seriesId,
                    createdAt = 1_000,
                    updatedAt = 2_000,
                    syncedAt = 2_000,
                    isDeleted = false,
                    version = 3,
                    rule = rule,
                    startDayKey = "2026-08-20",
                    endDayKey = null,
                    template = template,
                )

            else -> error("Unsupported test type: $type")
        }
    }

    private fun occurrence(
        type: DayFocusType,
        seriesId: String,
        dayKey: String,
        title: String,
        sourceSeriesVersion: Long,
    ): DayFocusItem =
        DayFocusItem(
            id = "recurrence:${type.name}:$seriesId:$dayKey",
            dayPlanId = "plan-$dayKey",
            title = title,
            notes = null,
            relatedLinks = emptyList(),
            type = type,
            isEveryday = false,
            recurringKey = null,
            recurrenceSeriesId = seriesId,
            recurrenceOccurrenceDayKey = dayKey,
            recurrenceSourceSeriesVersion = sourceSeriesVersion,
            budgetPercent = 20,
            version = 2,
        )
}
