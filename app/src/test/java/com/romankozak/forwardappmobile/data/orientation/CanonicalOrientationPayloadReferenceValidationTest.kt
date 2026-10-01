package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalOrientationPayloadReferenceValidationTest {
    @Test
    fun `complete empty payload remains valid through shared validator adapter`() {
        validateCanonicalPayloadReferences(
            bundle = emptyPayload(),
            validateEmbeddedWorkspaceTopology = false,
        )
    }

    @Test
    fun `orientation with wrong subject type remains rejected`() {
        val subject = ManagedSubjectEntity(
            id = "subject", subjectType = "ASPECT", title = "Subject", description = null,
            createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
        )
        val orientation = OrientationEntity(
            subjectId = "subject", kind = "GOAL", lifecycle = "ACTIVE", lifecycleOrigin = "DERIVED",
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateCanonicalPayloadReferences(
                bundle =
                    emptyPayload().copy(
                        managedSubjects = listOf(subject),
                        orientations = listOf(orientation),
                    ),
                validateEmbeddedWorkspaceTopology = false,
            )
        }
    }

    @Test
    fun `V2 validation ignores embedded Workspace topology while pre-cutover validation retains it`() {
        val payload =
            emptyPayload().copy(
                workspaces =
                    listOf(
                        workspace(id = "first", parentId = "second"),
                        workspace(id = "second", parentId = "first"),
                    ),
            )

        validateCanonicalPayloadReferences(
            bundle = payload,
            validateEmbeddedWorkspaceTopology = false,
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateCanonicalPayloadReferences(
                bundle = payload,
                validateEmbeddedWorkspaceTopology = true,
            )
        }
    }

    private fun emptyPayload() = SnapshotBundle(
        managedSubjects = emptyList(), orientations = emptyList(), aspects = emptyList(),
        orientationAssessments = emptyList(), orientationAssessmentRevisions = emptyList(),
        legacySubjectMappings = emptyList(), orientationRelations = emptyList(),
        aspectOrientationRefs = emptyList(), workspaceBindings = emptyList(),
        workspaceCapabilityInstances = emptyList(), savedOrientationViews = emptyList(),
    )

    private fun workspace(
        id: String,
        parentId: String?,
    ) = WorkspaceSnapshot(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = parentId,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )
}
