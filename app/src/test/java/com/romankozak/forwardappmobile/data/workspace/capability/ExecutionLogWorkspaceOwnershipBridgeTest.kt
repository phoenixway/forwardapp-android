package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExecutionLogWorkspaceOwnershipBridgeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `repair canonicalizes only provenance-proven Context-backed rows`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insertContexts(
                listOf(
                    contextEntity("safe-context"),
                    contextEntity("collision-context"),
                ),
            )
            database.workspaceDao().upsert(listOf(contextBackedWorkspace("safe-context")))
            database.contextManagementDao().insertLogs(
                listOf(
                    legacyLog(
                        id = "safe-1",
                        contextId = "safe-context",
                        version = 7L,
                        syncedAt = 100L,
                    ),
                    legacyLog(
                        id = "safe-2",
                        contextId = "safe-context",
                        version = 3L,
                    ),
                    legacyLog(
                        id = "blocked",
                        contextId = "collision-context",
                        version = 2L,
                    ),
                ),
            )

            val bridge =
                ExecutionLogWorkspaceOwnershipBridge(
                    database,
                    database.workspaceDao(),
                    database.contextManagementDao(),
                )

            val report = bridge.repairUnresolved()

            assertEquals(2, report.assignedLogs)
            assertEquals(1, report.unresolvedContexts)

            val safe = database.contextManagementDao().getLogById("safe-1")!!
            assertNull(safe.contextId)
            assertEquals("safe-context", safe.workspaceId)
            assertEquals(7L, safe.version)
            assertNull(safe.syncedAt)
            assertEquals("legacy-safe-1", safe.description)

            val blocked = database.contextManagementDao().getLogById("blocked")!!
            assertEquals("collision-context", blocked.contextId)
            assertNull(blocked.workspaceId)
            assertEquals(2L, blocked.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `resolver fails closed without proven Context-backed Workspace`() = runTest {
        val workspaceDao = mockk<WorkspaceDao>()
        val logDao = mockk<ContextManagementDao>(relaxed = true)
        val bridge = ExecutionLogWorkspaceOwnershipBridge(mockk(relaxed = true), workspaceDao, logDao)

        coEvery { workspaceDao.getContextBackedForContextId("collision") } returns null

        assertNull(bridge.resolveContextBackedWorkspaceId("collision"))
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun contextEntity(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
            roleCode = "management",
        )

    private fun legacyLog(
        id: String,
        contextId: String,
        version: Long,
        syncedAt: Long? = null,
    ) =
        ContextLog(
            id = id,
            contextId = contextId,
            timestamp = 10L,
            type = "COMMENT",
            description = "legacy-$id",
            updatedAt = 20L,
            syncedAt = syncedAt,
            isDeleted = false,
            version = version,
        )

    private fun contextBackedWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Legacy",
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )
}
