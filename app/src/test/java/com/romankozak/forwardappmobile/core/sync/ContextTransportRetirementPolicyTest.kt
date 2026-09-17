package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextTransportRetirementPolicyTest {
    @Test
    fun `live canonical-only Workspace retires same-id Context`() {
        val ids =
            canonicalRetiredContextIds(
                localWorkspaces = listOf(workspace("retired")),
            )

        assertEquals(setOf("retired"), ids)
    }

    @Test
    fun `canonical Workspace tombstone still prevents legacy Context resurrection`() {
        val ids =
            canonicalRetiredContextIds(
                localWorkspaces = listOf(workspace("retired", deleted = true)),
            )

        assertEquals(setOf("retired"), ids)
    }

    @Test
    fun `incoming canonical ownership participates in retirement before persistence`() {
        val ids =
            canonicalRetiredContextIds(
                localWorkspaces = emptyList(),
                incomingWorkspaces = listOf(workspace("incoming")),
            )

        assertEquals(setOf("incoming"), ids)
    }

    @Test
    fun `context-backed Workspace does not retire legacy Context transport`() {
        val ids =
            canonicalRetiredContextIds(
                localWorkspaces =
                    listOf(
                        workspace(
                            id = "legacy",
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = "legacy",
                        ),
                    ),
            )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `live retired row is blocked but tombstone evidence is preserved`() {
        data class Row(
            val id: String,
            val deleted: Boolean,
        )

        val filtered =
            listOf(
                Row("retired", deleted = false),
                Row("retired", deleted = true),
                Row("legacy", deleted = false),
            ).withoutLiveRetiredContexts(
                retiredContextIds = setOf("retired"),
                id = { it.id },
                isDeleted = { it.deleted },
            )

        assertFalse(filtered.any { it.id == "retired" && !it.deleted })
        assertTrue(filtered.any { it.id == "retired" && it.deleted })
        assertTrue(filtered.any { it.id == "legacy" && !it.deleted })
    }

    private fun workspace(
        id: String,
        deleted: Boolean = false,
        provenance: String = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId: String? = null,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = null,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = deleted,
        version = 1L,
        provenance = provenance,
        sourceContextId = sourceContextId,
    )
}
