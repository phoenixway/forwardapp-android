package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayFocusesRepositoryCanonicalDeletionTest {
    @Test
    fun `delete everywhere for canonical focus uses canonical series identity`() = runTest {
        val dao = mockk<DayFocusItemDao>(relaxed = true)
        val repository = DayFocusesRepository(dao)
        val item = canonicalItem(
            type = DayFocusType.FOCUS,
            seriesId = "focus-series",
        )

        repository.deleteItemEverywhere(item)

        coVerify(exactly = 1) {
            dao.softDeleteCanonicalRecurrenceSeries(
                seriesId = "focus-series",
                updatedAt = any(),
            )
        }
    }

    @Test
    fun `delete everywhere for canonical responsibility uses canonical series identity`() = runTest {
        val dao = mockk<DayFocusItemDao>(relaxed = true)
        val repository = DayFocusesRepository(dao)
        val item = canonicalItem(
            type = DayFocusType.RESPONSIBILITY,
            seriesId = "responsibility-series",
        )

        repository.deleteItemEverywhere(item)

        coVerify(exactly = 1) {
            dao.softDeleteCanonicalRecurrenceSeries(
                seriesId = "responsibility-series",
                updatedAt = any(),
            )
        }
    }

    private fun canonicalItem(
        type: DayFocusType,
        seriesId: String,
    ): DayFocusItem =
        DayFocusItem(
            id = "recurrence:${type.name}:$seriesId:2026-08-20",
            dayPlanId = "plan",
            title = "Canonical item",
            type = type,
            isEveryday = false,
            recurringKey = null,
            recurrenceSeriesId = seriesId,
            recurrenceOccurrenceDayKey = "2026-08-20",
            recurrenceSourceSeriesVersion = 3,
        )
}
