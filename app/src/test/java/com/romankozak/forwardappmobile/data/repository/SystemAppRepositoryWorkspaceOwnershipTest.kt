package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAppRepositoryWorkspaceOwnershipTest {
    @Test
    fun `ensure note app requires a live Workspace and does not consult Context ownership`() = runTest {
        val systemAppDao = mockk<SystemAppDao>(relaxed = true)
        val workspaceDao = mockk<WorkspaceDao>()
        val noteDocumentDao = mockk<NoteDocumentDao>(relaxed = true)
        val attachmentDao = mockk<AttachmentDao>(relaxed = true)
        val canonicalConnectionsRepository = mockk<CanonicalConnectionsRepository>(relaxed = true)
        val created = slot<SystemAppEntity>()
        coEvery { workspaceDao.getById("sys_strategic") } returns workspace()
        coEvery { systemAppDao.getBySystemKey("my-life-current-state") } returns null

        val result =
            SystemAppRepository(
                systemAppDao,
                workspaceDao,
                noteDocumentDao,
                attachmentDao,
                canonicalConnectionsRepository,
            ).ensureNoteApp(
                systemKey = "my-life-current-state",
                projectSystemKey = "strategic",
                documentName = "Current state",
            )

        assertEquals("sys_strategic", result.workspaceId)
        coVerify(exactly = 1) { systemAppDao.upsert(capture(created)) }
        assertEquals("sys_strategic", created.captured.workspaceId)
        coVerify(exactly = 0) {
            canonicalConnectionsRepository.linkAttachment(any(), any(), any())
        }
    }

    @Test
    fun `ensure note app fails closed for missing or deleted Workspace`() = runTest {
        val systemAppDao = mockk<SystemAppDao>(relaxed = true)
        val workspaceDao = mockk<WorkspaceDao>()
        val noteDocumentDao = mockk<NoteDocumentDao>(relaxed = true)
        val attachmentDao = mockk<AttachmentDao>(relaxed = true)
        val canonicalConnectionsRepository = mockk<CanonicalConnectionsRepository>(relaxed = true)
        coEvery { workspaceDao.getById("sys_strategic") } returns workspace(isDeleted = true)

        var failed = false
        try {
            SystemAppRepository(
                systemAppDao,
                workspaceDao,
                noteDocumentDao,
                attachmentDao,
                canonicalConnectionsRepository,
            ).ensureNoteApp(
                systemKey = "my-life-current-state",
                projectSystemKey = "strategic",
                documentName = "Current state",
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
        coVerify(exactly = 0) { systemAppDao.upsert(any()) }
    }

    private fun workspace(isDeleted: Boolean = false) =
        WorkspaceEntity(
            id = "sys_strategic",
            nameOverride = null,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1L,
            provenance = "CONTEXT_BACKED",
            sourceContextId = "sys_strategic",
        )
}
