package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import io.mockk.coEvery
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkspaceCompatibilityProjectionRepositoryTest {
    private val workspaceDao = mockk<WorkspaceDao>()
    private val orientationDao = mockk<OrientationDao>()
    private val contextDao = mockk<ContextDao>()
    private val repository =
        WorkspaceCompatibilityProjectionRepository(workspaceDao, orientationDao, contextDao)

    @Test
    fun `explicit sourceContextId resolves compatibility content`() = runTest {
        val workspace = workspace("context-backed", "context-backed")
        val context = context("context-backed")
        stubCommon(workspace)
        coEvery { contextDao.getContextById("context-backed") } returns context

        val projection = repository.get("context-backed")

        assertThat(projection?.workspace).isEqualTo(workspace)
        assertThat(projection?.sourceContext).isEqualTo(context)
        coVerify(exactly = 1) { contextDao.getContextById("context-backed") }
    }

    @Test
    fun `standalone Workspace never probes a same-id Context`() = runTest {
        stubCommon(workspace("workspace-only", null))

        val projection = repository.get("workspace-only")

        assertThat(projection?.sourceContext).isNull()
        coVerify(exactly = 0) { contextDao.getContextById(any()) }
    }

    @Test
    fun `destination scalar observation uses one canonical Workspace row without compatibility graph reads`() = runTest {
        val workspace = workspace("workspace-only", null)
        every { workspaceDao.observeById("workspace-only") } returns flowOf(workspace)

        assertThat(repository.observeWorkspace("workspace-only").first()).isEqualTo(workspace)

        verify(exactly = 1) { workspaceDao.observeById("workspace-only") }
        verify { contextDao wasNot Called; orientationDao wasNot Called }
    }

    private fun stubCommon(workspace: WorkspaceEntity) {
        coEvery { workspaceDao.getById(workspace.id) } returns workspace
        coEvery { workspaceDao.getOpenBootstrapIssues() } returns emptyList()
        coEvery { orientationDao.getAllWorkspaceCapabilities() } returns emptyList()
    }

    private fun workspace(id: String, sourceContextId: String?) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Workspace",
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = if (sourceContextId == null) "STANDALONE" else "CONTEXT_BACKED",
            sourceContextId = sourceContextId,
        )

    private fun context(id: String) =
        Context(
            id = id,
            name = "Legacy",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
