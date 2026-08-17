package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayFocusesRepositoryRecurringDeletionTest {
    @Test
    fun `tombstoned everyday occurrence blocks regeneration for same recurring key and day`() = runTest {
        val recurringKey = "focus-series-1"
        val targetDayPlanId = "target-plan"

        val source = mockk<DayFocusItem>()
        every { source.id } returns "source-focus"
        every { source.dayPlanId } returns "source-plan"
        every { source.title } returns "Everyday focus"
        every { source.notes } returns null
        every { source.relatedLinks } returns emptyList()
        every { source.type } returns DayFocusType.FOCUS
        every { source.recurringKey } returns recurringKey
        every { source.budgetPercent } returns 25

        val tombstone = mockk<DayFocusItem>()
        every { tombstone.isDeleted } returns true
        every { tombstone.recurringKey } returns recurringKey

        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
        coEvery { dayFocusItemDao.getItemsForDayPlanSync(targetDayPlanId) } returns listOf(tombstone)

        val repository = DayFocusesRepository(dayFocusItemDao)

        repository.upsertEverydayItemForDayPlan(
            source = source,
            targetDayPlanId = targetDayPlanId,
        )

        coVerify(exactly = 0) { dayFocusItemDao.insert(any()) }
        coVerify(exactly = 0) { dayFocusItemDao.update(any()) }
    }

    @Test
    fun `tombstoned everyday responsibility also blocks regeneration`() = runTest {
        val recurringKey = "responsibility-series-1"
        val targetDayPlanId = "target-plan"
        val source =
            DayFocusItem(
                id = "source-responsibility",
                dayPlanId = "source-plan",
                title = "Everyday responsibility",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.RESPONSIBILITY,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 30,
                order = 0,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )
        val tombstone =
            DayFocusItem(
                id = "deleted-responsibility",
                dayPlanId = targetDayPlanId,
                title = "Everyday responsibility",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.RESPONSIBILITY,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 30,
                order = 0,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                isDeleted = true,
                version = 2,
            )

        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
        coEvery { dayFocusItemDao.getItemsForDayPlanSync(targetDayPlanId) } returns listOf(tombstone)

        val repository = DayFocusesRepository(dayFocusItemDao)

        val result =
            repository.upsertEverydayItemForDayPlan(
                source = source,
                targetDayPlanId = targetDayPlanId,
            )

        assert(result.id == tombstone.id)
        assert(result.isDeleted)
        coVerify(exactly = 0) { dayFocusItemDao.insert(any()) }
        coVerify(exactly = 0) { dayFocusItemDao.update(any()) }
    }

    @Test
    fun `live everyday occurrence for same recurring key is still updated`() = runTest {
        val recurringKey = "focus-series-live"
        val targetDayPlanId = "target-plan"
        val source =
            DayFocusItem(
                id = "source-focus",
                dayPlanId = "source-plan",
                title = "Updated title",
                notes = "Updated notes",
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 40,
                order = 0,
                createdAt = 1_000L,
                updatedAt = 2_000L,
                syncedAt = null,
                isDeleted = false,
                version = 5,
            )
        val existing =
            DayFocusItem(
                id = "existing-focus",
                dayPlanId = targetDayPlanId,
                title = "Old title",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 10,
                order = 3,
                createdAt = 1_500L,
                updatedAt = 1_500L,
                syncedAt = 1_500L,
                isDeleted = false,
                version = 7,
            )

        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
        coEvery { dayFocusItemDao.getItemsForDayPlanSync(targetDayPlanId) } returns listOf(existing)

        val repository = DayFocusesRepository(dayFocusItemDao)

        val result =
            repository.upsertEverydayItemForDayPlan(
                source = source,
                targetDayPlanId = targetDayPlanId,
            )

        assert(result.id == existing.id)
        assert(result.dayPlanId == targetDayPlanId)
        assert(result.title == source.title)
        assert(result.notes == source.notes)
        assert(result.budgetPercent == source.budgetPercent)
        assert(result.recurringKey == recurringKey)
        assert(result.version == existing.version + 1)
        assert(!result.isDeleted)

        coVerify(exactly = 0) { dayFocusItemDao.insert(any()) }
        coVerify(exactly = 1) {
            dayFocusItemDao.update(
                match { item ->
                    item.id == existing.id &&
                        item.title == source.title &&
                        item.version == existing.version + 1 &&
                        !item.isDeleted
                },
            )
        }
    }
}
