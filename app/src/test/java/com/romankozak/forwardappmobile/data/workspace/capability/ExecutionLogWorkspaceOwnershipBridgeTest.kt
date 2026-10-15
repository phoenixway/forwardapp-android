package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionLogWorkspaceOwnershipBridgeTest {
    @Test
    fun `resolver fails closed without proven Context-backed Workspace`() = runTest {
        val workspaceDao = mockk<WorkspaceDao>()
        val bridge = ExecutionLogWorkspaceOwnershipBridge(workspaceDao)

        coEvery { workspaceDao.getContextBackedForContextId("collision") } returns null

        assertNull(bridge.resolveContextBackedWorkspaceId("collision"))
    }
}
