package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class BacklogReadQueueRoomTest {
    @Test fun `coherent backlog read completes while Room transaction executor is occupied`() = runBlocking {
        val transactions = Executors.newSingleThreadExecutor()
        val queries = Executors.newFixedThreadPool(2)
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .setTransactionExecutor(transactions).setQueryExecutor(queries).allowMainThreadQueries().build()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val exited = AtomicBoolean(false)
        try {
            db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "Workspace", null, null, 1, 1, null, false, 1, "CANONICAL_ONLY", null)))
            db.orientationDao().upsertWorkspaceCapabilities(listOf(WorkspaceCapabilityInstanceEntity(
                id = "cap", workspaceId = "w", capabilityType = "BACKLOG", instanceKey = "default", capabilityOrder = 0,
                state = "ACTIVE", configurationVersion = 1, configuration = "{}", createdAt = 1, updatedAt = 1,
                syncedAt = null, isDeleted = false, version = 1,
            )))
            db.workspaceBacklogEntryDao().upsert(listOf(WorkspaceBacklogEntryEntity(
                id = "placement", workspaceId = "w", capabilityInstanceId = "cap", targetKind = "LEGACY_NOTE", targetId = "note",
                entryOrder = 0, createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
            )))
            val reader = CanonicalBacklogCompatibilityReader(db)
            assertEquals("placement", reader.getItemsForContext("w").single().id) // warm Room/query setup
            transactions.execute {
                entered.countDown()
                try { release.await(30, TimeUnit.SECONDS) } finally { exited.set(true) }
            }
            assertTrue(withContext(Dispatchers.IO) { entered.await(5, TimeUnit.SECONDS) })
            val result = withTimeout(5_000) { reader.getItemsForContext("w") }
            assertEquals("note", result.single().entityId)
            assertFalse("Read must finish before the busy transaction executor is released", exited.get())
        } finally {
            release.countDown()
            db.close()
            transactions.shutdownNow()
            queries.shutdownNow()
        }
    }
}
