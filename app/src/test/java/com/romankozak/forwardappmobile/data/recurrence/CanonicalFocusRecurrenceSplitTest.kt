package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.CanonicalFocusSplitSourceVersion
import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
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

class CanonicalFocusRecurrenceSplitTest {
    @Test
    fun `focus split preserves overrides and tombstones across new logical series`() = runTest {
        verifySplit(DayFocusType.FOCUS)
    }

    @Test
    fun `responsibility split preserves overrides and tombstones across new logical series`() = runTest {
        verifySplit(DayFocusType.RESPONSIBILITY)
    }

    private suspend fun verifySplit(type: DayFocusType) {
        val appDatabase = mockk<AppDatabase>()
        val seriesDao = mockk<CanonicalRecurringSeriesDao>(relaxed = true)
        val materializer = mockk<CanonicalRecurrenceMaterializationAdapter>(relaxed = true)
        every { appDatabase.canonicalRecurringSeriesDao() } returns seriesDao

        val oldSeriesId = if (type == DayFocusType.FOCUS) "old-focus" else "old-responsibility"
        val oldSeries = oldSeries(type, oldSeriesId)
        val selected = occurrence(type, oldSeriesId, "2026-08-20", "Old title", isDeleted = false)
        val cleanExcluded = occurrence(type, oldSeriesId, "2026-08-21", "Old title", isDeleted = false)
        val customIncluded = occurrence(type, oldSeriesId, "2026-08-22", "Custom included", isDeleted = false)
        val customExcluded = occurrence(type, oldSeriesId, "2026-08-23", "Custom excluded", isDeleted = false)
        val deletedIncluded = occurrence(type, oldSeriesId, "2026-08-24", "Old title", isDeleted = true)
        val deletedExcluded = occurrence(type, oldSeriesId, "2026-08-25", "Old title", isDeleted = true)
        val occurrences =
            listOf(
                selected,
                cleanExcluded,
                customIncluded,
                customExcluded,
                deletedIncluded,
                deletedExcluded,
            )

        coEvery { seriesDao.getById(oldSeriesId) } returns oldSeries.toAndroidEntity()
        coEvery { seriesDao.getFocusOccurrencesForSeries(oldSeriesId) } returns occurrences

        val newSeriesSlot = slot<CanonicalRecurringSeriesEntity>()
        val sourceVersionsSlot = slot<List<CanonicalFocusSplitSourceVersion>>()
        val replacementsSlot = slot<List<DayFocusItem>>()

        val adapter =
            CanonicalFocusRecurrenceAuthoringAdapter(
                appDatabase = appDatabase,
                materializationAdapter = materializer,
            )

        val returned =
            adapter.splitSeriesFromOccurrence(
                item = selected,
                title = "New title",
                notes = " new notes ",
                relatedLinks = emptyList(),
                budgetPercent = 40,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 2,
                        daysOfWeek = null,
                    ),
            )

        coVerify(exactly = 1) {
            seriesDao.splitCanonicalFocusSeries(
                oldSeriesId = oldSeriesId,
                oldSeriesExpectedVersion = 5,
                oldSeriesEndDayKey = "2026-08-19",
                newSeries = capture(newSeriesSlot),
                liveSourceOccurrences = capture(sourceVersionsSlot),
                replacementOccurrences = capture(replacementsSlot),
                updatedAt = any(),
            )
        }
        coVerify(exactly = 0) { materializer.materializeForDate(any(), any()) }

        val newSeries = newSeriesSlot.captured.toCanonicalSeries()
        assertEquals("2026-08-20", newSeries.startDayKey)
        assertEquals(RecurrenceFrequency.DAILY, newSeries.rule.frequency)
        assertEquals(2, newSeries.rule.interval)
        assertEquals(1L, newSeries.version)
        assertFalse(newSeries.isDeleted)

        when (type) {
            DayFocusType.FOCUS -> assertTrue(newSeries is RecurringFocusSeries)
            DayFocusType.RESPONSIBILITY -> assertTrue(newSeries is RecurringResponsibilitySeries)
            else -> error("Unexpected test type: $type")
        }

        assertEquals(
            setOf(selected.id, cleanExcluded.id, customIncluded.id, customExcluded.id),
            sourceVersionsSlot.captured.mapTo(mutableSetOf()) { it.itemId },
        )

        val replacements = replacementsSlot.captured
        assertEquals(4, replacements.size)

        val newSeriesId = newSeries.id
        val kindName = if (type == DayFocusType.FOCUS) "FOCUS" else "RESPONSIBILITY"
        val selectedReplacement =
            replacements.single {
                it.recurrenceOccurrenceDayKey == "2026-08-20" && it.recurrenceSeriesId == newSeriesId
            }
        assertEquals("recurrence:$kindName:$newSeriesId:2026-08-20", selectedReplacement.id)
        assertEquals("New title", selectedReplacement.title)
        assertEquals("new notes", selectedReplacement.notes)
        assertEquals(40, selectedReplacement.budgetPercent)
        assertFalse(selectedReplacement.isDeleted)
        assertEquals(selectedReplacement, returned)

        assertTrue(
            replacements.none {
                it.recurrenceOccurrenceDayKey == "2026-08-21"
            },
        )

        val migratedCustom =
            replacements.single {
                it.recurrenceOccurrenceDayKey == "2026-08-22" && it.recurrenceSeriesId == newSeriesId
            }
        assertEquals("Custom included", migratedCustom.title)
        assertFalse(migratedCustom.isDeleted)

        val detachedCustom =
            replacements.single {
                it.recurrenceSeriesId == null && it.title == "Custom excluded"
            }
        assertNull(detachedCustom.recurrenceOccurrenceDayKey)
        assertNull(detachedCustom.recurrenceSourceSeriesVersion)
        assertFalse(detachedCustom.isDeleted)

        val migratedTombstone =
            replacements.single {
                it.recurrenceOccurrenceDayKey == "2026-08-24" && it.recurrenceSeriesId == newSeriesId
            }
        assertTrue(migratedTombstone.isDeleted)
        assertEquals("recurrence:$kindName:$newSeriesId:2026-08-24", migratedTombstone.id)

        assertTrue(
            replacements.none {
                it.recurrenceOccurrenceDayKey == "2026-08-25"
            },
        )
    }

    private fun oldSeries(
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
                    version = 5,
                    rule = rule,
                    startDayKey = "2026-08-10",
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
                    version = 5,
                    rule = rule,
                    startDayKey = "2026-08-10",
                    endDayKey = null,
                    template = template,
                )

            else -> error("Unexpected test type: $type")
        }
    }

    private fun occurrence(
        type: DayFocusType,
        seriesId: String,
        dayKey: String,
        title: String,
        isDeleted: Boolean,
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
            recurrenceSourceSeriesVersion = 5,
            budgetPercent = 20,
            order = 1,
            createdAt = 1_000,
            updatedAt = 2_000,
            syncedAt = 2_000,
            isDeleted = isDeleted,
            version = 3,
        )
}
