package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionLogWorkspaceOwnershipBridgeTest {
    @Test
    fun `repair assigns only provenance-proven Context-backed owners`() = runTest {
        val workspaceDao = mockk<WorkspaceDao>()
        val logDao = mockk<ContextManagementDao>()
        val bridge = ExecutionLogWorkspaceOwnershipBridge(workspaceDao, logDao)

        coEvery { logDao.getContextIdsWithoutWorkspaceOwner() } returns
            listOf("safe-context", "collision-context")
        coEvery { workspaceDao.getContextBackedForContextId("safe-context") } returns
            contextBackedWorkspace("safe-context")
        coEvery { workspaceDao.getContextBackedForContextId("collision-context") } returns null
        coEvery {
            logDao.assignWorkspaceOwnerForContext("safe-context", "safe-context")
        } returns 3

        val report = bridge.repairUnresolved()

        assertEquals(3, report.assignedLogs)
        assertEquals(1, report.unresolvedContexts)
        coVerify(exactly = 1) {
            logDao.assignWorkspaceOwnerForContext("safe-context", "safe-context")
        }
        coVerify(exactly = 0) {
            logDao.assignWorkspaceOwnerForContext("collision-context", any())
        }
    }

    @Test
    fun `resolver fails closed without proven Context-backed Workspace`() = runTest {
        val workspaceDao = mockk<WorkspaceDao>()
        val logDao = mockk<ContextManagementDao>(relaxed = true)
        val bridge = ExecutionLogWorkspaceOwnershipBridge(workspaceDao, logDao)

        coEvery { workspaceDao.getContextBackedForContextId("collision") } returns null

        assertNull(bridge.resolveContextBackedWorkspaceId("collision"))
    }

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
