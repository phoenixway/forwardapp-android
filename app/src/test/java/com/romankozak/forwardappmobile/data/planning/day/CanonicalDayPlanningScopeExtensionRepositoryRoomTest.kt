package com.romankozak.forwardappmobile.data.planning.day

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalDayPlanningScopeExtensionRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `save read tombstone and explicit restore preserve typed fields`() = runBlocking {
        val db = database()
        try {
            seedScope(db, kind = "DAY")
            val repository = repository(db)
            repository.save(extension())

            assertEquals(DayPlanningOutcome.COMPLETED, repository.get(SCOPE_ID)?.outcome)
            assertEquals(480L, repository.get(SCOPE_ID)?.plannedBudgetMinutes)

            val tombstone = repository.tombstone(SCOPE_ID, now = 30L)
            assertTrue(tombstone.isDeleted)
            assertEquals(2L, tombstone.version)

            repository.restore(
                tombstone.copy(
                    updatedAt = 40L,
                    version = 3L,
                    isDeleted = false,
                ),
            )
            assertFalse(requireNotNull(repository.get(SCOPE_ID)).isDeleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `live extension rejects missing non DAY and tombstoned scope`() = runBlocking {
        val missingDb = database()
        try {
            assertFails { repository(missingDb).save(extension()) }
        } finally {
            missingDb.close()
        }

        val wrongKindDb = database()
        try {
            seedScope(wrongKindDb, kind = "TACTICAL_CYCLE")
            assertFails { repository(wrongKindDb).save(extension()) }
        } finally {
            wrongKindDb.close()
        }

        val deletedDb = database()
        try {
            seedScope(deletedDb, kind = "DAY", isDeleted = true)
            assertFails { repository(deletedDb).save(extension()) }
        } finally {
            deletedDb.close()
        }
    }

    @Test
    fun `sync merge is deterministic and exact ACK cannot acknowledge newer version`() = runBlocking {
        val db = database()
        try {
            seedScope(db, kind = "DAY")
            val store = CanonicalDayPlanningScopeExtensionSyncStore(db)
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(
                listOf(
                    extension().copy(
                        version = 2L,
                        updatedAt = 21L,
                        syncedAt = 999L,
                        plannedBudgetMinutes = 360L,
                    ).toSnapshot(),
                ),
            )
            store.mergeIncoming(listOf(extension().copy(version = 1L, plannedBudgetMinutes = 60L).toSnapshot()))
            assertEquals(360L, store.loadAll().single().plannedBudgetMinutes)
            assertNull(store.loadAll().single().syncedAt)

            assertFails {
                store.mergeIncoming(
                    listOf(
                        extension().copy(
                            version = 2L,
                            updatedAt = 21L,
                            plannedBudgetMinutes = 300L,
                        ).toSnapshot(),
                    ),
                )
            }

            db.dayPlanningScopeExtensionDao().upsert(
                extension().copy(version = 3L, updatedAt = 22L, syncedAt = null).toEntity(),
            )
            store.markSynced(
                listOf(
                    com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.DayPlanningScopeExtensionSyncVersion(
                        scopeId = SCOPE_ID.value,
                        version = 2L,
                    ),
                ),
            )
            assertNull(store.loadAll().single().syncedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `unknown outcome wire code fails closed`() = runBlocking {
        val db = database()
        try {
            seedScope(db, kind = "DAY")
            assertFails {
                CanonicalDayPlanningScopeExtensionSyncStore(db).mergeIncoming(
                    listOf(extension().toSnapshot().copy(outcome = "UNKNOWN")),
                )
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `sync merge rejects missing and non DAY scope dependencies`() = runBlocking {
        val missingDb = database()
        try {
            assertFails {
                CanonicalDayPlanningScopeExtensionSyncStore(missingDb).mergeIncoming(
                    listOf(extension().toSnapshot()),
                )
            }
        } finally {
            missingDb.close()
        }

        val wrongKindDb = database()
        try {
            seedScope(wrongKindDb, kind = "STRATEGIC_ARC")
            assertFails {
                CanonicalDayPlanningScopeExtensionSyncStore(wrongKindDb).mergeIncoming(
                    listOf(extension().toSnapshot()),
                )
            }
        } finally {
            wrongKindDb.close()
        }
    }

    @Test
    fun `legacy DayPlan never synthesizes dormant canonical extension`() = runBlocking {
        val db = database()
        try {
            db.dayPlanDao().insert(DayPlan(id = "legacy-day", date = 1_780_000_000_000L))

            assertTrue(CanonicalDayPlanningScopeExtensionSyncStore(db).loadAll().isEmpty())
            assertTrue(db.planningDao().getAllScopes().isEmpty())
        } finally {
            db.close()
        }
    }

    private fun repository(db: AppDatabase) =
        CanonicalDayPlanningScopeExtensionRepository(
            database = db,
            dao = db.dayPlanningScopeExtensionDao(),
            planningDao = db.planningDao(),
        )

    private suspend fun seedScope(
        db: AppDatabase,
        kind: String,
        isDeleted: Boolean = false,
    ) {
        db.planningDao().upsertScope(
            PlanningScopeEntity(
                id = SCOPE_ID.value,
                kind = kind,
                lifecycle = "ACTIVE",
                title = null,
                startsAt = null,
                endsAt = null,
                createdAt = 1L,
                updatedAt = 2L,
                syncedAt = null,
                version = 1L,
                isDeleted = isDeleted,
            ),
        )
    }

    private fun extension() =
        DayPlanningScopeExtension(
            scopeId = SCOPE_ID,
            calendarDayStartMillis = 1_780_000_000_000L,
            outcome = DayPlanningOutcome.COMPLETED,
            plannedBudgetMinutes = 480L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun assertFails(block: suspend () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue("Expected fail-closed validation", failed)
    }

    private companion object {
        val SCOPE_ID = PlanningScopeId("day-scope")
    }
}
