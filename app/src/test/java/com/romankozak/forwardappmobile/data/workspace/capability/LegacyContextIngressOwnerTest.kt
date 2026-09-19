package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyContextIngressOwnerTest {
    @Test
    fun `retirement evidence admits only matching live canonical owner`() {
        assertTrue(isAdmittedLegacyContextIngressOwner(ID, true, workspace(ID, WorkspaceProvenance.CANONICAL_ONLY, null)))
        assertFalse(isAdmittedLegacyContextIngressOwner(ID, null, workspace(ID, WorkspaceProvenance.CANONICAL_ONLY, null)))
        assertFalse(isAdmittedLegacyContextIngressOwner(ID, true, workspace(ID, WorkspaceProvenance.STANDALONE, null)))
    }

    @Test
    fun `live historical Context admits deleted context backed Workspace for tombstone migration`() {
        assertTrue(
            isAdmittedLegacyContextIngressOwner(
                ID,
                false,
                workspace(
                    ID,
                    WorkspaceProvenance.CONTEXT_BACKED,
                    ID,
                    isDeleted = true,
                ),
            ),
        )
    }

    @Test
    fun `live Context and exact System retain their bounded contracts`() {
        assertTrue(isAdmittedLegacyContextIngressOwner(ID, false, workspace(ID, WorkspaceProvenance.CONTEXT_BACKED, ID)))
        assertTrue(
            isAdmittedLegacyContextIngressOwner(
                SystemContexts.INBOX.raw,
                null,
                workspace(SystemContexts.INBOX.raw, WorkspaceProvenance.CANONICAL_ONLY, null),
            ),
        )
        assertFalse(
            isAdmittedLegacyContextIngressOwner(
                "sys_custom",
                null,
                workspace("sys_custom", WorkspaceProvenance.CANONICAL_ONLY, null),
            ),
        )
    }

    private fun workspace(
        id: String,
        provenance: WorkspaceProvenance,
        sourceContextId: String?,
        isDeleted: Boolean = false,
    ) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1L,
            provenance = provenance.name,
            sourceContextId = sourceContextId,
        )

    private companion object {
        const val ID = "ordinary"
    }
}
