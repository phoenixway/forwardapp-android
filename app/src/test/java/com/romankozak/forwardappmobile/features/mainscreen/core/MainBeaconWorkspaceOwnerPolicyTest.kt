package com.romankozak.forwardappmobile.features.mainscreen.core

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainBeaconWorkspaceOwnerPolicyTest {
    private val ordinaryId = "ordinary-main-beacon-owner"

    @Test
    fun `live ordinary Context admits proven context-backed Workspace`() {
        val workspace = workspace(
            provenance = WorkspaceProvenance.CONTEXT_BACKED,
            sourceContextId = ordinaryId,
        )

        assertSame(
            workspace,
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                workspace,
            ),
        )
    }

    @Test
    fun `live ordinary Context rejects same-id canonical-only collision`() {
        assertRejected(
            MainBeaconOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING,
        ) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                workspace(
                    provenance = WorkspaceProvenance.CANONICAL_ONLY,
                    sourceContextId = null,
                ),
            )
        }
    }

    @Test
    fun `live Context rejects divergent source mapping`() {
        assertRejected(
            MainBeaconOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING,
        ) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                workspace(
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = "another-context",
                ),
            )
        }
    }

    @Test
    fun `retired ordinary Context admits canonical-only owner`() {
        val candidate = workspace(
            provenance = WorkspaceProvenance.CANONICAL_ONLY,
            sourceContextId = null,
        )

        assertSame(
            candidate,
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(deleted = true),
                candidate,
            ),
        )
    }

    @Test
    fun `retired ordinary Context rejects context-backed owner`() {
        assertRejected(
            MainBeaconOwnerRejection.UNPROVEN_RETIRED_CONTEXT_MAPPING,
        ) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(deleted = true),
                workspace(
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = ordinaryId,
                ),
            )
        }
    }

    @Test
    fun `contextless canonical and standalone Workspaces are admitted`() {
        for (provenance in listOf(
            WorkspaceProvenance.CANONICAL_ONLY,
            WorkspaceProvenance.STANDALONE,
        )) {
            val candidate = workspace(
                provenance = provenance,
                sourceContextId = null,
            )

            assertSame(
                candidate,
                requireMainBeaconWorkspaceOwner(
                    ordinaryId,
                    null,
                    candidate,
                ),
            )
        }
    }

    @Test
    fun `contextless proven context-backed Workspace is admitted`() {
        val candidate = workspace(
            provenance = WorkspaceProvenance.CONTEXT_BACKED,
            sourceContextId = ordinaryId,
        )

        assertSame(
            candidate,
            requireMainBeaconWorkspaceOwner(ordinaryId, null, candidate),
        )
    }

    @Test
    fun `contextless divergent context-backed mapping is rejected`() {
        assertRejected(
            MainBeaconOwnerRejection.UNPROVEN_CONTEXTLESS_OWNER,
        ) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                null,
                workspace(
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = "another-context",
                ),
            )
        }
    }

    @Test
    fun `missing and deleted Workspaces fail closed`() {
        assertRejected(MainBeaconOwnerRejection.MISSING_WORKSPACE) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                null,
            )
        }

        assertRejected(MainBeaconOwnerRejection.DELETED_WORKSPACE) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                workspace(
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = ordinaryId,
                    deleted = true,
                ),
            )
        }
    }

    @Test
    fun `reserved System owner requires live canonical-only Workspace`() {
        val systemId = SystemContexts.PERSONAL_MANAGEMENT.raw
        val canonical = workspace(
            id = systemId,
            provenance = WorkspaceProvenance.CANONICAL_ONLY,
            sourceContextId = null,
        )

        assertSame(
            canonical,
            requireMainBeaconWorkspaceOwner(systemId, null, canonical),
        )

        assertRejected(MainBeaconOwnerRejection.INVALID_SYSTEM_OWNER) {
            requireMainBeaconWorkspaceOwner(
                systemId,
                null,
                workspace(
                    id = systemId,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = systemId,
                ),
            )
        }
    }

    @Test
    fun `mismatched identity and empty owner fail closed`() {
        assertRejected(MainBeaconOwnerRejection.DIVERGENT_ID) {
            requireMainBeaconWorkspaceOwner(
                ordinaryId,
                context(),
                workspace(
                    id = "another-workspace",
                    provenance = WorkspaceProvenance.CONTEXT_BACKED,
                    sourceContextId = ordinaryId,
                ),
            )
        }

        assertRejected(MainBeaconOwnerRejection.EMPTY_OWNER_ID) {
            requireMainBeaconWorkspaceOwner("", null, null)
        }
    }

    private fun assertRejected(
        reason: MainBeaconOwnerRejection,
        action: () -> Unit,
    ) {
        val error = runCatching(action).exceptionOrNull()
        assertTrue(error is MainBeaconWorkspaceOwnerRejected)
        assertEquals(
            reason,
            (error as MainBeaconWorkspaceOwnerRejected).reason,
        )
    }

    private fun context(deleted: Boolean = false) =
        Context(
            id = ordinaryId,
            name = ordinaryId,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = deleted,
        )

    private fun workspace(
        id: String = ordinaryId,
        provenance: WorkspaceProvenance,
        sourceContextId: String?,
        deleted: Boolean = false,
    ) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = deleted,
            version = 1L,
            provenance = provenance.name,
            sourceContextId = sourceContextId,
        )
}
