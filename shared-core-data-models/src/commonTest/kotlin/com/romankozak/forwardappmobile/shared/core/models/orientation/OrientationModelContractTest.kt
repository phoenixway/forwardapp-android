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
        assertEquals(ORIENTATION_MODEL_VERSION, 1)
    }

    @Test
    fun contractContainsAcceptedKindAndAxisSets() {
        assertEquals(9, OrientationKind.entries.size)
        assertEquals(8, OrientationAxis.entries.size)
        assertEquals(13, WorkspaceCapabilityType.entries.size)
        assertEquals(1, ORIENTATION_FILTER_AST_VERSION)
    }
}
