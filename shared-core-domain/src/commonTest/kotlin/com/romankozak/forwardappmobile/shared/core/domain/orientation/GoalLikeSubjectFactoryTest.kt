package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalLikeSubjectFactoryTest {
    @Test
    fun createsConsistentCanonicalGraphFromCallerSuppliedIdentity() {
        val graph = createGoalLikeCanonicalSubject(
            goalId = "goal-1",
            subjectId = "subject-1",
            assessmentId = "assessment-1",
            mappingId = "mapping-1",
            title = "Prepare launch",
            description = "Details",
            completed = false,
            createdAt = 100L,
            updatedAt = 120L,
            migrationVersion = 7,
        )

        assertEquals("subject-1", graph.subject.id)
        assertEquals(ManagedSubjectType.ORIENTATION, graph.subject.subjectType)
        assertEquals("subject-1", graph.orientation.subjectId)
        assertEquals(OrientationKind.GOAL, graph.orientation.kind)
        assertEquals(OrientationLifecycle.ACTIVE, graph.orientation.lifecycle)
        assertEquals("assessment-1", graph.revision.id)
        assertEquals("subject-1", graph.revision.orientationId)
        assertEquals("subject-1", graph.currentAssessment.orientationId)
        assertEquals("assessment-1", graph.currentAssessment.revisionId)
        assertEquals(graph.orientation.assessment, graph.currentAssessment.assessment)
        assertEquals(AssessmentRevisionSource.USER, graph.revision.source)
        assertEquals("mapping-1", graph.mapping.id)
        assertEquals(LegacyOrientationSourceType.GOAL, graph.mapping.source.sourceType)
        assertEquals("goal-1", graph.mapping.source.sourceId)
        assertEquals("subject-1", graph.mapping.subjectId)
        assertEquals(LegacySubjectMappingState.CUT_OVER, graph.mapping.state)
        assertEquals("Prepare launch", graph.subject.title)
        assertEquals("Details", graph.subject.description)
        assertEquals(100L, graph.subject.createdAt)
        assertEquals(120L, graph.subject.updatedAt)
        assertEquals(1L, graph.subject.version)
        assertNull(graph.subject.syncedAt)
        assertTrue(validateOrientationAssessment(graph.orientation.kind, graph.orientation.assessment).isEmpty())
    }

    @Test
    fun completedGoalUsesRealizedLifecycleAndDeterministicDefaults() {
        val graph = createGoalLikeCanonicalSubject(
            goalId = "goal-2",
            subjectId = "subject-2",
            assessmentId = "assessment-2",
            mappingId = "mapping-2",
            title = "Done",
            description = null,
            completed = true,
            createdAt = 200L,
            updatedAt = 200L,
            migrationVersion = 7,
        )

        assertEquals(OrientationLifecycle.REALIZED, graph.orientation.lifecycle)
        assertEquals(200L, graph.revision.effectiveFrom)
        assertEquals(200L, graph.revision.recordedAt)
        assertEquals(1L, graph.revision.version)
        assertNull(graph.subject.description)
        assertNotNull(graph.orientation.assessment)
    }
}
