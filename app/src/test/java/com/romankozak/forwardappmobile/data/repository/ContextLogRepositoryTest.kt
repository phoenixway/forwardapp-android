package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ContextLogRepositoryTest {
    @Test
    fun `user authored Context log routes to canonical authoring boundary`() = runTest {
        val dao = mockk<ContextManagementDao>(relaxed = true)
        val ownershipBridge = mockk<ExecutionLogWorkspaceOwnershipBridge>()
        val canonicalRepository = mockk<CanonicalExecutionLogRepository>(relaxed = true)
        coEvery { ownershipBridge.resolveContextBackedWorkspaceId("context-1") } returns "workspace-1"
        val repository = ContextLogRepository(dao, ownershipBridge, canonicalRepository)

        repository.addContextLogEntry(
            contextId = "context-1",
            type = "COMMENT",
            description = "new",
        )

        coVerify(exactly = 1) {
            canonicalRepository.createLog(
                workspaceId = "workspace-1",
                type = "COMMENT",
                description = "new",
                details = null,
                timestamp = any(),
                now = any(),
            )
        }
        coVerify(exactly = 0) { dao.insertLog(any()) }
    }

    @Test
    fun `system Context log routes to canonical system audit boundary`() = runTest {
        val dao = mockk<ContextManagementDao>(relaxed = true)
        val ownershipBridge = mockk<ExecutionLogWorkspaceOwnershipBridge>()
        val canonicalRepository = mockk<CanonicalExecutionLogRepository>(relaxed = true)
        coEvery { ownershipBridge.resolveContextBackedWorkspaceId("context-1") } returns "workspace-1"
        val repository = ContextLogRepository(dao, ownershipBridge, canonicalRepository)

        repository.addSystemContextLogEntry(
            contextId = "context-1",
            type = "AUTOMATIC",
            description = "audit",
            details = "details",
        )

        coVerify(exactly = 1) {
            canonicalRepository.createSystemLog(
                workspaceId = "workspace-1",
                type = "AUTOMATIC",
                description = "audit",
                details = "details",
                timestamp = any(),
                now = any(),
            )
        }
        coVerify(exactly = 0) { dao.insertLog(any()) }
    }

    @Test
    fun `owner deletion routes through canonical content lifecycle`() = runTest {
        val dao = mockk<ContextManagementDao>(relaxed = true)
        val ownershipBridge = mockk<ExecutionLogWorkspaceOwnershipBridge>()
        val canonicalRepository = mockk<CanonicalExecutionLogRepository>(relaxed = true)
        val repository = ContextLogRepository(dao, ownershipBridge, canonicalRepository)

        repository.tombstoneOwnedContentForWorkspaces(
            workspaceIds = listOf("context-1", "context-2"),
            now = 100L,
        )

        coVerify(exactly = 1) {
            canonicalRepository.tombstoneOwnedContentForWorkspaces(
                workspaceIds = listOf("context-1", "context-2"),
                now = 100L,
            )
        }
    }
}
