package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toWorkspaceOwnedEntityOrNull
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.SystemAppSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemAppSnapshotWorkspaceOwnershipTest {
    @Test
    fun `canonical Workspace owner is accepted only when live`() {
        val entity =
            snapshot(workspaceId = "sys_strategic")
                .toWorkspaceOwnedEntityOrNull(
                    liveWorkspaceIds = setOf("sys_strategic"),
                    validDocumentIds = setOf("doc-1"),
                    allowLegacyContextOwner = false,
                )

        requireNotNull(entity)
        assertEquals("sys_strategic", entity.workspaceId)
        assertEquals("doc-1", entity.noteDocumentId)
    }

    @Test
    fun `legacy Context owner is bounded to full restore and the same live Workspace id`() {
        val historical = snapshot(legacyContextId = "sys_strategic")

        assertNull(
            historical.toWorkspaceOwnedEntityOrNull(
                liveWorkspaceIds = setOf("sys_strategic"),
                validDocumentIds = setOf("doc-1"),
                allowLegacyContextOwner = false,
            ),
        )
        assertEquals(
            "sys_strategic",
            historical
                .toWorkspaceOwnedEntityOrNull(
                    liveWorkspaceIds = setOf("sys_strategic"),
                    validDocumentIds = setOf("doc-1"),
                    allowLegacyContextOwner = true,
                )
                ?.workspaceId,
        )
        assertNull(
            historical.toWorkspaceOwnedEntityOrNull(
                liveWorkspaceIds = emptySet(),
                validDocumentIds = setOf("doc-1"),
                allowLegacyContextOwner = true,
            ),
        )
    }

    @Test
    fun `missing document is cleared without changing proven Workspace owner`() {
        val entity =
            snapshot(workspaceId = "sys_strategic", noteDocumentId = "missing")
                .toWorkspaceOwnedEntityOrNull(
                    liveWorkspaceIds = setOf("sys_strategic"),
                    validDocumentIds = emptySet(),
                    allowLegacyContextOwner = false,
                )

        requireNotNull(entity)
        assertEquals("sys_strategic", entity.workspaceId)
        assertNull(entity.noteDocumentId)
    }

    private fun snapshot(
        workspaceId: String? = null,
        legacyContextId: String? = null,
        noteDocumentId: String? = "doc-1",
    ) =
        SystemAppSnapshot(
            id = "app-1",
            systemKey = "my-life-current-state",
            appType = "NOTE_DOCUMENT",
            workspaceId = workspaceId,
            legacyContextId = legacyContextId,
            noteDocumentId = noteDocumentId,
            createdAt = 1L,
            updatedAt = 2L,
            version = 1L,
            isDeleted = false,
        )
}
