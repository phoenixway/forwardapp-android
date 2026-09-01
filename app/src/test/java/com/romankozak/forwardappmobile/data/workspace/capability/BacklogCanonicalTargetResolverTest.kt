package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogCanonicalTargetResolverTest {
    @Test
    fun `GOAL resolves only through existing CUT_OVER canonical Orientation mapping`() = runTest {
        val orientationDao = mockk<OrientationDao>()
        val workspaceDao = mockk<WorkspaceDao>()
        coEvery {
            orientationDao.getLegacyMapping(
                LegacyOrientationSourceType.GOAL.name,
                "goal-1",
            )
        } returns mapping(state = LegacySubjectMappingState.CUT_OVER.name)

        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val target = resolver.resolveLegacy("GOAL", "goal-1")

        assertEquals(WorkspaceBacklogTargetKind.ORIENTATION, target.kind)
        assertEquals("orientation-1", target.id)
        coVerify(exactly = 1) {
            orientationDao.getLegacyMapping(
                LegacyOrientationSourceType.GOAL.name,
                "goal-1",
            )
        }
    }

    @Test
    fun `GOAL fails closed when canonical mapping is missing`() = runTest {
        val orientationDao = mockk<OrientationDao>()
        val workspaceDao = mockk<WorkspaceDao>()
        coEvery {
            orientationDao.getLegacyMapping(
                LegacyOrientationSourceType.GOAL.name,
                "goal-1",
            )
        } returns null

        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val failure = runCatching { resolver.resolveLegacy("GOAL", "goal-1") }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `GOAL fails closed when canonical mapping is not CUT_OVER`() = runTest {
        val orientationDao = mockk<OrientationDao>()
        val workspaceDao = mockk<WorkspaceDao>()
        coEvery {
            orientationDao.getLegacyMapping(
                LegacyOrientationSourceType.GOAL.name,
                "goal-1",
            )
        } returns mapping(state = LegacySubjectMappingState.MATERIALIZED.name)

        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val failure = runCatching { resolver.resolveLegacy("GOAL", "goal-1") }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `SUBLIST resolves only through proven Context-backed Workspace`() = runTest {
        val orientationDao = mockk<OrientationDao>()
        val workspaceDao = mockk<WorkspaceDao>()
        coEvery { workspaceDao.getContextBackedForContextId("context-1") } returns
            contextBackedWorkspace("workspace-1", "context-1")

        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val target = resolver.resolveLegacy("SUBLIST", "context-1")

        assertEquals(WorkspaceBacklogTargetKind.WORKSPACE, target.kind)
        assertEquals("workspace-1", target.id)
        coVerify(exactly = 1) { workspaceDao.getContextBackedForContextId("context-1") }
    }

    @Test
    fun `PROJECT uses the same proven Workspace identity rule as SUBLIST`() = runTest {
        val orientationDao = mockk<OrientationDao>()
        val workspaceDao = mockk<WorkspaceDao>()
        coEvery { workspaceDao.getContextBackedForContextId("context-1") } returns
            contextBackedWorkspace("workspace-1", "context-1")

        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val target = resolver.resolveLegacy("PROJECT", "context-1")

        assertEquals(WorkspaceBacklogTargetKind.WORKSPACE, target.kind)
        assertEquals("workspace-1", target.id)
    }

    @Test
    fun `legacy NOTE preserves its distinct historical identity`() = runTest {
        val orientationDao = mockk<OrientationDao>(relaxed = true)
        val workspaceDao = mockk<WorkspaceDao>(relaxed = true)
        val resolver = BacklogCanonicalTargetResolver(orientationDao, workspaceDao)

        val target = resolver.resolveLegacy("NOTE", "note-1")

        assertEquals(WorkspaceBacklogTargetKind.LEGACY_NOTE, target.kind)
        assertEquals("note-1", target.id)
        coVerify(exactly = 0) { orientationDao.getLegacyMapping(any(), any()) }
        coVerify(exactly = 0) { workspaceDao.getContextBackedForContextId(any()) }
    }

    private fun mapping(state: String) =
        LegacySubjectMappingEntity(
            id = "mapping-1",
            sourceType = LegacyOrientationSourceType.GOAL.name,
            sourceId = "goal-1",
            subjectId = "orientation-1",
            migrationVersion = 3,
            state = state,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun contextBackedWorkspace(
        id: String,
        contextId: String,
    ) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Workspace",
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
            sourceContextId = contextId,
        )
}
