package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspacePickerCreationTest {
    @Test
    fun `root picker creation returns canonical Workspace identity`() = runBlocking {
        val workspaceRepository = mockk<CanonicalWorkspaceRepository>()
        coEvery {
            workspaceRepository.create(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = null,
                now = any(),
            )
        } returns "workspace-id"

        val id = workspaceRepository.createRootWorkspaceForPicker("  Operations  ")

        assertEquals("workspace-id", id)
        coVerify(exactly = 1) {
            workspaceRepository.create(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = null,
                now = any(),
            )
        }
    }

    @Test
    fun `blank root picker creation authors nothing`() = runBlocking {
        val workspaceRepository = mockk<CanonicalWorkspaceRepository>()

        assertNull(workspaceRepository.createRootWorkspaceForPicker("   "))

        coVerify(exactly = 0) {
            workspaceRepository.create(any(), any(), any(), any(), any())
        }
    }
}
