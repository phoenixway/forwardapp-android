package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextLogRepositoryTest {
    @Test
    fun `retention turns overflow into syncable tombstone`() = runTest {
        val dao = mockk<ContextManagementDao>(relaxed = true)
        val ownershipBridge = mockk<ExecutionLogWorkspaceOwnershipBridge>()
        coEvery { ownershipBridge.resolveContextBackedWorkspaceId("context-1") } returns "context-1"
        val repository = ContextLogRepository(dao, ownershipBridge)
        val overflow =
            ContextLog(
                id = "old-log",
                contextId = "context-1",
                timestamp = 100L,
                type = "AUTOMATIC",
                description = "old",
                details = null,
                updatedAt = 200L,
                syncedAt = 250L,
                isDeleted = false,
                version = 7L,
            )

        coEvery {
            dao.getLogsForContextBeyondKeepCount(
                contextId = "context-1",
                keepCount = 40,
            )
        } returns listOf(overflow)

        val retainedChanges = slot<List<ContextLog>>()
        val insertedLog = slot<ContextLog>()
        coEvery { dao.insertLogs(capture(retainedChanges)) } returns Unit
        coEvery { dao.insertLog(capture(insertedLog)) } returns Unit

        repository.addContextLogEntry(
            contextId = "context-1",
            type = "COMMENT",
            description = "new",
        )

        assertEquals("context-1", insertedLog.captured.workspaceId)
        assertEquals(1, retainedChanges.captured.size)
        val tombstone = retainedChanges.captured.single()
        assertEquals(overflow.id, tombstone.id)
        assertTrue(tombstone.isDeleted)
        assertEquals(overflow.version + 1, tombstone.version)
        assertNull(tombstone.syncedAt)
        assertTrue((tombstone.updatedAt ?: 0L) > (overflow.updatedAt ?: 0L))

        coVerify(exactly = 1) {
            dao.getLogsForContextBeyondKeepCount(
                contextId = "context-1",
                keepCount = 40,
            )
        }
    }
}
