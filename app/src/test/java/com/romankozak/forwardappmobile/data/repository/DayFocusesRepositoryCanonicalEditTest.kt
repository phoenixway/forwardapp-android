package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DayFocusesRepositoryCanonicalEditTest {
    @Test
    fun `single occurrence edit preserves canonical provenance`() = runTest {
        val dao = mockk<DayFocusItemDao>(relaxed = true)
        val repository = DayFocusesRepository(dao)
        val original =
            DayFocusItem(
                id = "recurrence:FOCUS:series-1:2026-08-20",
                dayPlanId = "plan",
                title = "Original",
                type = DayFocusType.FOCUS,
                isEveryday = false,
                recurringKey = null,
                recurrenceSeriesId = "series-1",
                recurrenceOccurrenceDayKey = "2026-08-20",
                recurrenceSourceSeriesVersion = 7,
                version = 3,
            )

        val updated =
            repository.updateItem(
                item = original,
                title = "Customized",
                notes = "only today",
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                budgetPercent = 30,
            )

        assertEquals("Customized", updated.title)
        assertEquals("series-1", updated.recurrenceSeriesId)
        assertEquals("2026-08-20", updated.recurrenceOccurrenceDayKey)
        assertEquals(7L, updated.recurrenceSourceSeriesVersion)
        assertFalse(updated.isEveryday)
        assertNull(updated.recurringKey)

        coVerify(exactly = 1) {
            dao.update(
                match { stored ->
                    stored.id == original.id &&
                        stored.recurrenceSeriesId == "series-1" &&
                        stored.recurrenceOccurrenceDayKey == "2026-08-20" &&
                        stored.recurrenceSourceSeriesVersion == 7L
                },
            )
        }
    }
}
