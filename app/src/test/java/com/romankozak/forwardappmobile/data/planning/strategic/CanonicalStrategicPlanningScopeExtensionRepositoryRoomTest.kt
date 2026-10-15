package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.StrategicPlanningScopeExtensionSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalStrategicPlanningScopeExtensionRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `save read tombstone and explicit restore preserve exact arc key`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            val repository = repository(db)
            repository.save(extension())

            assertEquals(" 2026/Alpha ", repository.get(SCOPE_ID)?.legacyArcKey)
            val tombstone = repository.tombstone(SCOPE_ID, now = 30L)
            assertTrue(tombstone.isDeleted)
            assertEquals(2L, tombstone.version)

            repository.restore(tombstone.copy(updatedAt = 40L, version = 3L, isDeleted = false))
            assertFalse(requireNotNull(repository.get(SCOPE_ID)).isDeleted)
        }
    }

    @Test
    fun `live extension rejects missing non strategic and tombstoned scope`() = runBlocking {
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
    fun `strict stored and wire mapping reject malformed arc keys`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            db.strategicPlanningScopeExtensionDao().upsert(extension().toEntity().copy(legacyArcKey = ""))
            assertFailsState { repository(db).get(SCOPE_ID) }
        }
        withDatabase { db ->
            seedScope(db)
            assertFails {
                CanonicalStrategicPlanningScopeExtensionSyncStore(db).mergeIncoming(
                    listOf(extension().toSnapshot().copy(legacyArcKey = "")),
                )
            }
        }
    }

    @Test
    fun `merge is deterministic and exact stale ACK does not acknowledge newer version`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            val store = CanonicalStrategicPlanningScopeExtensionSyncStore(db)
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(listOf(extension().toSnapshot()))
            store.mergeIncoming(listOf(extension().copy(version = 2L, updatedAt = 21L).toSnapshot()))
            store.mergeIncoming(listOf(extension().copy(version = 1L, updatedAt = 20L).toSnapshot()))
            assertEquals(2L, store.loadAll().single().version)
            assertNull(store.loadAll().single().syncedAt)

            assertFails {
                store.mergeIncoming(
                    listOf(extension().copy(version = 2L, updatedAt = 21L, legacyArcKey = "divergent").toSnapshot()),
                )
            }

            db.strategicPlanningScopeExtensionDao().upsert(
                extension().copy(version = 3L, updatedAt = 22L).toEntity(),
            )
            store.markSynced(listOf(StrategicPlanningScopeExtensionSyncVersion(SCOPE_ID.value, 2L)))
            assertNull(store.loadAll().single().syncedAt)
            store.markSynced(listOf(StrategicPlanningScopeExtensionSyncVersion(SCOPE_ID.value, 3L)))
            assertTrue(store.loadAll().single().syncedAt != null)
        }
    }

    @Test
    fun `merge rejects immutable createdAt and duplicate scope ids`() = runBlocking {
        withDatabase { db ->
            seedScope(db)
            val store = CanonicalStrategicPlanningScopeExtensionSyncStore(db)
            store.mergeIncoming(listOf(extension().toSnapshot()))
            assertFails {
                store.mergeIncoming(listOf(extension().copy(createdAt = 11L, version = 2L).toSnapshot()))
            }
            assertFails {
                store.mergeIncoming(listOf(extension().toSnapshot(), extension().toSnapshot()))
            }
        }
    }

    private fun repository(db: AppDatabase) =
        CanonicalStrategicPlanningScopeExtensionRepository(
            database = db,
            dao = db.strategicPlanningScopeExtensionDao(),
            planningDao = db.planningDao(),
        )

    private suspend fun seedScope(
        db: AppDatabase,
        kind: String = "STRATEGIC_ARC",
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
        StrategicPlanningScopeExtension(
            scopeId = SCOPE_ID,
            legacyArcKey = " 2026/Alpha ",
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

    private suspend fun assertFailsState(block: suspend () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue("Expected corrupt persisted state to fail closed", failed)
    }

    private companion object {
        val SCOPE_ID = PlanningScopeId("strategic-scope")
    }
}
