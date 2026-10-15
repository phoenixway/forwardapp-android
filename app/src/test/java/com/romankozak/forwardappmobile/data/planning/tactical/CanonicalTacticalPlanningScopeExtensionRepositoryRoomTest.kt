package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.TacticalPlanningScopeExtensionSyncVersion
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalTacticalPlanningScopeExtensionRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `save read tombstone and explicit restore preserve typed fields`() = runBlocking {
        val db = database()
        try {
            seedScope(db)
            val repository = repository(db)
            repository.save(extension())

            assertEquals(TacticalPlanningScopeType.TIMEBOXED, repository.get(SCOPE_ID)?.type)
            assertEquals("2026-W41", repository.get(SCOPE_ID)?.weekKey)

            val tombstone = repository.tombstone(SCOPE_ID, now = 30L)
            assertTrue(tombstone.isDeleted)
            assertEquals(2L, tombstone.version)

            repository.restore(tombstone.copy(updatedAt = 40L, version = 3L, isDeleted = false))
            assertFalse(requireNotNull(repository.get(SCOPE_ID)).isDeleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `live extension rejects missing non tactical and tombstoned scope`() = runBlocking {
        withDatabase { db -> assertFails { repository(db).save(extension()) } }
        withDatabase { db ->
            seedScope(db, kind = "DAY")
            assertFails { repository(db).save(extension()) }
        }
        withDatabase { db ->
            seedScope(db, isDeleted = true)
            assertFails { repository(db).save(extension()) }
        }
    }

    @Test
    fun `sync merge is deterministic and exact ACK cannot acknowledge newer version`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            val store = CanonicalTacticalPlanningScopeExtensionSyncStore(db)
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(
                listOf(extension().copy(version = 2L, updatedAt = 21L, closedAt = 20L).toSnapshot()),
            )
            store.mergeIncoming(listOf(extension().copy(version = 1L, closedAt = 10L).toSnapshot()))
            assertEquals(20L, store.loadAll().single().closedAt)
            assertNull(store.loadAll().single().syncedAt)

            assertFails {
                store.mergeIncoming(
                    listOf(extension().copy(version = 2L, updatedAt = 21L, closedAt = 22L).toSnapshot()),
                )
            }

            db.tacticalPlanningScopeExtensionDao().upsert(
                extension().copy(version = 3L, updatedAt = 22L).toEntity(),
            )
            store.markSynced(listOf(TacticalPlanningScopeExtensionSyncVersion(SCOPE_ID.value, 2L)))
            assertNull(store.loadAll().single().syncedAt)
        }
    }

    @Test
    fun `wire validation rejects unknown type and invalid dependencies`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            assertFails {
                CanonicalTacticalPlanningScopeExtensionSyncStore(db).mergeIncoming(
                    listOf(extension().toSnapshot().copy(iterationType = "UNKNOWN")),
                )
            }
        }
        withDatabase { db ->
            assertFails {
                CanonicalTacticalPlanningScopeExtensionSyncStore(db).mergeIncoming(
                    listOf(extension().toSnapshot()),
                )
            }
        }
        withDatabase { db ->
            seedScope(db, kind = "STRATEGIC_ARC")
            assertFails {
                CanonicalTacticalPlanningScopeExtensionSyncStore(db).mergeIncoming(
                    listOf(extension().toSnapshot()),
                )
            }
        }
    }

    @Test
    fun `legacy TacticalIteration never synthesizes dormant canonical state`() = runBlocking {
        withDatabase { db ->
            db.tacticalIterationDao().insert(
                TacticalIteration(
                    id = "legacy-cycle",
                    title = "Legacy cycle",
                    startedAt = 1L,
                    weekKey = "2026-W41",
                ),
            )

            assertTrue(CanonicalTacticalPlanningScopeExtensionSyncStore(db).loadAll().isEmpty())
            assertTrue(db.planningDao().getAllScopes().isEmpty())
            assertTrue(db.planningDao().getAllCommitments().isEmpty())
        }
    }

    private fun repository(db: AppDatabase) =
        CanonicalTacticalPlanningScopeExtensionRepository(
            database = db,
            dao = db.tacticalPlanningScopeExtensionDao(),
            planningDao = db.planningDao(),
        )

    private suspend fun seedScope(
        db: AppDatabase,
        kind: String = "TACTICAL_CYCLE",
        isDeleted: Boolean = false,
    ) {
        db.planningDao().upsertScope(
            PlanningScopeEntity(
                id = SCOPE_ID.value,
                kind = kind,
                lifecycle = "ACTIVE",
                title = null,
                startsAt = 1L,
                endsAt = 2L,
                createdAt = 1L,
                updatedAt = 2L,
                syncedAt = null,
                version = 1L,
                isDeleted = isDeleted,
            ),
        )
    }

    private fun extension() =
        TacticalPlanningScopeExtension(
            scopeId = SCOPE_ID,
            type = TacticalPlanningScopeType.TIMEBOXED,
            weekKey = "2026-W41",
            closedAt = null,
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

    private suspend fun <T> withDatabase(block: suspend (AppDatabase) -> T): T {
        val db = database()
        return try {
            block(db)
        } finally {
            db.close()
        }
    }

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
        val SCOPE_ID = PlanningScopeId("tactical-scope")
    }
}
