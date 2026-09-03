package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.test.Test
import kotlin.test.assertTrue

class CanonicalOrientationReferenceValidationTest {
    @Test
    fun goalLikeFactoryGraphIsValid() {
        assertTrue(validateCanonicalOrientationReferences(goalGraph()).isEmpty())
    }

    @Test
    fun orientationRequiresOrientationSubject() {
        val graph = goalGraph().copy(subjects = listOf(CanonicalSubjectReference("subject", ManagedSubjectType.ASPECT)))
        assertCode(graph, "ORIENTATION_SUBJECT")
    }

    @Test
    fun assessmentRequiresKnownOrientation() {
        val graph = goalGraph().copy(assessments = goalGraph().assessments.map { it.copy(orientationId = "missing") })
        assertCode(graph, "ASSESSMENT_ORIENTATION")
    }

    @Test
    fun currentAssessmentRequiresMatchingRevision() {
        val graph = goalGraph().copy(revisions = goalGraph().revisions.map { it.copy(orientationId = "other") })
        assertCode(graph, "ASSESSMENT_REVISION")
    }

    @Test
    fun mappingRequiresKnownSubject() {
        val graph = goalGraph().copy(mappings = listOf(CanonicalLegacyMappingReference("mapping", "missing")))
        assertCode(graph, "MAPPING_SUBJECT")
    }

    @Test
    fun relationRequiresKnownEndpoints() {
        val graph = goalGraph().copy(relations = listOf(OrientationRelation(
            id = "relation", createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
            fromOrientationId = "subject", toOrientationId = "missing",
            relationType = OrientationRelationType.SUPPORTS, order = null,
        )))
        assertCode(graph, "ORIENTATION_RELATIONS")
    }

    @Test
    fun workspaceReferencesAndSavedViewVersionRemainValidated() {
        val binding = WorkspaceBinding(
            id = "binding", createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
            workspaceId = "missing", subjectId = "subject", bindingType = WorkspaceBindingType.SUPPORTS,
            isPrimary = false, order = 0,
        )
        val graph = goalGraph().copy(
            workspaces = emptyList(), bindings = listOf(binding),
            savedViews = listOf(CanonicalSavedViewReference("view", 0)),
        )
        assertCode(graph, "BINDING_WORKSPACE")
        assertCode(graph, "SAVED_VIEW_VERSION")
    }

    @Test
    fun validCompleteGoalLikeGraphSucceeds() {
        val graph = goalGraph().copy(workspaces = emptyList())
        assertTrue(validateCanonicalOrientationReferences(graph).isEmpty())
    }

    private fun goalGraph(): CanonicalOrientationValidationGraph {
        val created = createGoalLikeCanonicalSubject(
            goalId = "goal", subjectId = "subject", assessmentId = "assessment", mappingId = "mapping",
            title = "Goal", description = null, completed = false,
            createdAt = 1, updatedAt = 1, migrationVersion = 1,
        )
        return CanonicalOrientationValidationGraph(
            subjects = listOf(CanonicalSubjectReference(created.subject.id, created.subject.subjectType)),
            orientations = listOf(CanonicalOrientationReference(created.orientation.subjectId, created.orientation.kind)),
            aspects = emptyList(),
            assessments = listOf(CanonicalCurrentAssessmentReference(
                created.currentAssessment.orientationId,
                created.currentAssessment.revisionId,
                created.currentAssessment.assessment,
            )),
            revisions = listOf(CanonicalAssessmentRevisionReference(
                created.revision.id, created.revision.orientationId, created.revision.assessment,
            )),
            mappings = listOf(CanonicalLegacyMappingReference(created.mapping.id, created.mapping.subjectId)),
            relations = emptyList(), aspectRefs = emptyList(), workspaces = null,
            bindings = emptyList(), capabilities = emptyList(), savedViews = emptyList(),
        )
    }

    private fun assertCode(graph: CanonicalOrientationValidationGraph, code: String) {
        assertTrue(validateCanonicalOrientationReferences(graph).any { it.code == code })
    }
}
