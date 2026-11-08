package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayFocusLinkRemovalRoomTest {
    private val oldLink = RelatedLink(LinkType.CONTEXT, "deleted-context", "Old context")
    private val original = DayFocusItem(id = "current", dayPlanId = "today", title = "Stored title", notes = "Stored notes",
        relatedLinks = listOf(oldLink), type = DayFocusType.FOCUS, budgetPercent = 40, order = 5,
        recurrenceSeriesId = "series", recurrenceOccurrenceDayKey = "2026-11-07", recurrenceSourceSeriesVersion = 4,
        createdAt = 1, updatedAt = 10, syncedAt = 20, version = 6)

    @Test fun `removing stale link survives reopen and preserves other fields days and recurrence provenance`() = runBlocking {
        fixture { db ->
            val before = db.dayFocusItemDao().getAllSync().filter { it.id != original.id }
            val updated = DayFocusesRepository(db.dayFocusItemDao()).removeRelatedLink(original.id, oldLink)
            assertEquals(emptyList<RelatedLink>(), updated.relatedLinks)
            assertEquals(original.copy(relatedLinks = emptyList(), updatedAt = updated.updatedAt, syncedAt = null, version = 7), updated)
            val reopened = DayFocusesRepository(db.dayFocusItemDao()).getItemsForDayPlan("today").first().single()
            assertEquals(updated, reopened)
            assertEquals(before, db.dayFocusItemDao().getAllSync().filter { it.id != original.id })
            assertNull(db.workspaceDao().getById(oldLink.target))
            assertNull(db.contextDao().getContextById(oldLink.target))
        }
    }

    @Test fun `exact typed reference removal preserves colliding raw IDs and repeated removal is inert`() = runBlocking {
        fixture { db ->
            val note = oldLink.copy(type = LinkType.NOTE_DOCUMENT, displayName = "Note")
            val otherVault = oldLink.copy(vault = "another-vault")
            db.dayFocusItemDao().update(original.copy(relatedLinks = listOf(oldLink, note, otherVault)))
            val repository = DayFocusesRepository(db.dayFocusItemDao())
            val updated = repository.removeRelatedLink(original.id, oldLink.copy(displayName = "Renamed label", vault = ""))
            assertEquals(listOf(note, otherVault), updated.relatedLinks)
            assertEquals(updated, repository.removeRelatedLink(original.id, oldLink))
        }
    }

    @Test fun `unlink reads current row rather than overwriting newer fields with editor snapshot`() = runBlocking {
        fixture { db ->
            val current = original.copy(title = "New stored title", notes = "New stored notes", budgetPercent = 25,
                order = 9, type = DayFocusType.RESPONSIBILITY, version = 10)
            db.dayFocusItemDao().update(current)
            val updated = DayFocusesRepository(db.dayFocusItemDao()).removeRelatedLink(original.id, oldLink)
            assertEquals(current.copy(relatedLinks = emptyList(), updatedAt = updated.updatedAt, syncedAt = null, version = 11), updated)
        }
    }

    @Test fun `missing deleted and unmatched focus link commands do not rewrite data`() = runBlocking {
        fixture { db ->
            val repository = DayFocusesRepository(db.dayFocusItemDao())
            assertEquals(original, repository.removeRelatedLink(original.id, oldLink.copy(target = "other")))
            val deleted = original.copy(isDeleted = true)
            db.dayFocusItemDao().update(deleted)
            assertTrue(runCatching { repository.removeRelatedLink(original.id, oldLink) }.isFailure)
            assertTrue(runCatching { repository.removeRelatedLink("missing", oldLink) }.isFailure)
            assertEquals(deleted, db.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(original.id))
        }
    }

    @Test fun `failed unlink leaves stored link and metadata unchanged`() = runBlocking {
        fixture { db ->
            val before = db.dayFocusItemDao().getAllSync()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_unlink BEFORE UPDATE ON day_focus_items BEGIN SELECT RAISE(ABORT,'test'); END")
            assertTrue(runCatching { DayFocusesRepository(db.dayFocusItemDao()).removeRelatedLink(original.id, oldLink) }.isFailure)
            assertEquals(before, db.dayFocusItemDao().getAllSync())
        }
    }

    private suspend fun fixture(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            db.dayPlanDao().insertAll(listOf(DayPlan(id = "today", date = 1), DayPlan(id = "history", date = 0), DayPlan(id = "future", date = 2)))
            db.dayFocusItemDao().insertAll(listOf(original,
                original.copy(id = "history", dayPlanId = "history", recurrenceOccurrenceDayKey = "2026-11-06"),
                original.copy(id = "future", dayPlanId = "future", recurrenceOccurrenceDayKey = "2026-11-08")))
            block(db)
        } finally { db.close() }
    }
}
