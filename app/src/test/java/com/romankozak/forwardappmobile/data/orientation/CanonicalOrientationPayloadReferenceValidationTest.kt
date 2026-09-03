package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalOrientationPayloadReferenceValidationTest {
    @Test
    fun `complete empty payload remains valid through shared validator adapter`() {
        validateCanonicalPayloadReferences(emptyPayload())
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
                emptyPayload().copy(managedSubjects = listOf(subject), orientations = listOf(orientation)),
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
}
