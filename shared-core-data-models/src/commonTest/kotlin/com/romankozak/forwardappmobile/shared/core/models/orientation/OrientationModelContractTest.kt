package com.romankozak.forwardappmobile.shared.core.models.orientation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrientationModelContractTest {
    @Test
    fun emptyAssessmentDistinguishesUnsetFromNotApplicable() {
        val assessment = emptyApplicableAssessment().copy(targetWindow = notApplicableAxis())

        assertEquals(ValueOrigin.UNSET, assessment.importance.origin)
        assertNull(assessment.importance.valueCode)
        assertEquals(ValueOrigin.NOT_APPLICABLE, assessment.targetWindow.origin)
        assertEquals(ValueOrigin.UNSET, assessment.effort.origin)
        assertEquals(ValueOrigin.UNSET, assessment.cost.origin)
        assertEquals(ValueOrigin.UNSET, assessment.risk.origin)
        assertEquals(OrientationEvaluationState.UNASSESSED, assessment.evaluationState)
        assertEquals(ORIENTATION_MODEL_VERSION, 2)
    }

    @Test
    fun contractContainsAcceptedKindAndAxisSets() {
        assertEquals(12, OrientationKind.entries.size)
        assertEquals("INTENT", OrientationKind.INTENT.name)
        assertEquals("QUEST", OrientationKind.QUEST.name)
        assertEquals(11, OrientationAxis.entries.size)
        assertEquals(11, WorkspaceCapabilityType.entries.size)
        assertEquals(1, ORIENTATION_FILTER_AST_VERSION)
    }
}
