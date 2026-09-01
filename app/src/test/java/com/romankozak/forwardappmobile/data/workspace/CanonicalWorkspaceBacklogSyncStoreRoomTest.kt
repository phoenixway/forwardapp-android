package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySyncVersion
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceBacklogSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical placement merges moves and uses exact version ack`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)
            val original = entry()

            store.mergeIncoming(listOf(original))
            assertEquals(listOf("placement"), store.loadUnsynced().map { it.id })
            assertEquals(listOf("placement"), store.loadChangedSince(9L).map { it.id })

            store.markSynced(listOf(WorkspaceBacklogEntrySyncVersion("placement", 2L)))
            assertNull(database.workspaceBacklogEntryDao().getById("placement")!!.syncedAt)

            store.markSynced(listOf(WorkspaceBacklogEntrySyncVersion("placement", 3L)))
            assertNotNull(database.workspaceBacklogEntryDao().getById("placement")!!.syncedAt)

            store.mergeIncoming(
                listOf(
                    original.copy(
                        workspaceId = "owner-b",
                        capabilityInstanceId = "backlog-owner-b",
                        order = 7L,
                        updatedAt = 20L,
                        version = 4L,
                    ),
                ),
            )

            val moved = requireNotNull(database.workspaceBacklogEntryDao().getById("placement"))
            assertEquals("owner-b", moved.workspaceId)
            assertEquals("backlog-owner-b", moved.capabilityInstanceId)
            assertEquals(7L, moved.entryOrder)
            assertNull(moved.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and live target must exist`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)
            val live = entry(version = 5L, updatedAt = 50L)

            store.mergeIncoming(listOf(live))
            store.mergeIncoming(listOf(live.copy(isDeleted = true)))
            assertTrue(database.workspaceBacklogEntryDao().getById("placement")!!.isDeleted)

            val failure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            entry(
                                id = "missing-target",
                                targetId = "missing-workspace",
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun store(database: AppDatabase) =
        CanonicalWorkspaceBacklogSyncStore(
            database = database,
            entryDao = database.workspaceBacklogEntryDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

    private suspend fun seedDependencies(database: AppDatabase) {
        database.workspaceDao().upsert(
            listOf(workspace("owner-a"), workspace("owner-b"), workspace("target")),
        )
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(capability("owner-a"), capability("owner-b")),
        )
    }

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )

    private fun capability(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "backlog-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = "BACKLOG",
            instanceKey = "default",
            capabilityOrder = 0L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun entry(
        id: String = "placement",
        targetId: String = "target",
        version: Long = 3L,
        updatedAt: Long = 10L,
    ) =
        WorkspaceBacklogEntrySnapshot(
            id = id,
            workspaceId = "owner-a",
            capabilityInstanceId = "backlog-owner-a",
            targetKind = "WORKSPACE",
            targetId = targetId,
            order = 0L,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = false,
        )
}
