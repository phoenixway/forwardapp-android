package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextSettingsActionsTargetDeletionTest {
    private val contexts = mockk<ContextRepository>(relaxed = true)
    private val workspaces = mockk<CanonicalWorkspaceRepository>(relaxed = true)

    private fun actions() =
        ContextSettingsActions(contexts, workspaces)

    @Test
    fun `V2 settings deletes live Workspace target without subtree or Context fallback`() = runBlocking {
        coEvery { workspaces.hasLiveWorkspace("target") } returns true

        actions()
            .deleteCurrentProject("target")

        coVerify(exactly = 1) { workspaces.tombstone("target", any()) }
        coVerify(exactly = 0) { contexts.deleteContextsByIds(any()) }
    }

    @Test
    fun `V2 settings rejects absent Workspace rather than deleting legacy Context`() = runBlocking {
        coEvery { workspaces.hasLiveWorkspace("legacy") } returns false

        val failure = runCatching {
            actions()
                .deleteCurrentProject("legacy")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        coVerify(exactly = 0) { workspaces.tombstone(any(), any()) }
        coVerify(exactly = 0) { contexts.deleteContextsByIds(any()) }
    }

    @Test
    fun `V2 settings propagates nonleaf rejection without fallback`() = runBlocking {
        coEvery { workspaces.hasLiveWorkspace("target") } returns true
        coEvery { workspaces.tombstone("target", any()) } throws
            HierarchyChildPolicyRejectedException(PlacementId("link-parent"))

        val failure = runCatching {
            actions()
                .deleteCurrentProject("target")
        }.exceptionOrNull()

        assertTrue(failure is HierarchyChildPolicyRejectedException)
        coVerify(exactly = 0) { contexts.deleteContextsByIds(any()) }
    }

    @Test
    fun `reserved System target is rejected before any deletion`() = runBlocking {
        val failure = runCatching {
            actions()
                .deleteCurrentProject(SystemContexts.INBOX.raw)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        coVerify(exactly = 0) { workspaces.tombstone(any(), any()) }
        coVerify(exactly = 0) { contexts.deleteContextsByIds(any()) }
    }
}
