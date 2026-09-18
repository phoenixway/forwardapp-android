package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyInboxFullBackupAdapterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `one way legacy Inbox import uses tombstone proven canonical Workspace owner`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            database.contextDao().insert(
                ContextEntity(
                    id = "owner",
                    name = "Legacy owner",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 2L,
                    isDeleted = true,
                    version = 3L,
                ),
            )
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = "owner", nameOverride = "Canonical owner", descriptionOverride = null,
                        parentWorkspaceId = null, roleCode = null, workspaceOrder = 0L,
                        createdAt = 1L, updatedAt = 2L, syncedAt = null, isDeleted = false,
                        version = 3L, provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )

            LegacyInboxFullBackupAdapter(database, database.workspaceDao(), database.workspaceInboxRecordDao())
                .materializeLegacyFullBackup(
                    listOf(
                        InboxRecord(
                            id = "record", contextId = "owner", text = "Recovered", createdAt = 4L,
                            order = 7L, updatedAt = 5L, syncedAt = 6L, version = 8L,
                        ),
                    ),
                )

            val record = database.workspaceInboxRecordDao().getAll().single()
            assertEquals("owner", record.workspaceId)
            assertEquals("record", record.id)
            assertEquals(7L, record.recordOrder)
            assertEquals(6L, record.syncedAt)
            assertEquals(8L, record.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `arbitrary canonical only Workspace without retirement evidence is rejected`() = runBlocking {
        assertRejected(provenance = WorkspaceProvenance.CANONICAL_ONLY.name)
    }

    @Test
    fun `standalone Workspace is not legacy Context Inbox ingress evidence`() = runBlocking {
        assertRejected(provenance = WorkspaceProvenance.STANDALONE.name)
    }

    private suspend fun assertRejected(provenance: String) {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = "owner", nameOverride = "owner", descriptionOverride = null,
                        parentWorkspaceId = null, roleCode = null, workspaceOrder = 0L,
                        createdAt = 1L, updatedAt = 1L, syncedAt = null, isDeleted = false,
                        version = 1L, provenance = provenance, sourceContextId = null,
                    ),
                ),
            )
            val failure =
                runCatching {
                    LegacyInboxFullBackupAdapter(database, database.workspaceDao(), database.workspaceInboxRecordDao())
                        .materializeLegacyFullBackup(
                            listOf(InboxRecord("record", "owner", "Rejected", 1L, 0L)),
                        )
                }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.workspaceInboxRecordDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }
}
