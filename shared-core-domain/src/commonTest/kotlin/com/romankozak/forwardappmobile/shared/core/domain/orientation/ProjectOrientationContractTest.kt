package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.test.*

class ProjectOrientationContractTest {
    @Test fun projectUsesExistingAxesAndStandardCompletionLifecycle() {
        assertEquals(OrientationKind.PROJECT, resolveOrientationKind("PROJECT").known)
        assertEquals(OrientationLifecycle.REALIZED, resolveOrientationLifecycle("REALIZED").known)
        val assessment = initialOrientationAssessment(OrientationKind.PROJECT)
        OrientationAxis.entries.forEach {
            assertEquals(AxisApplicability.APPLICABLE, axisApplicability(OrientationKind.PROJECT, it))
            assertEquals(ValueOrigin.UNSET, assessment.valueFor(it).origin)
        }
        assertTrue(validateOrientationAssessment(OrientationKind.PROJECT, assessment).isEmpty())
    }

    @Test fun projectScoringUsesExistingCanonicalDimensionsAndFormula() {
        val assessment = initialOrientationAssessment(OrientationKind.PROJECT).copy(
            importance = AxisAssessment("HIGH", ValueOrigin.EXPLICIT),
            impact = AxisAssessment("LARGE", ValueOrigin.EXPLICIT),
            effort = AxisAssessment("LOW", ValueOrigin.EXPLICIT),
            cost = AxisAssessment("LOW", ValueOrigin.EXPLICIT),
            risk = AxisAssessment("LOW", ValueOrigin.EXPLICIT),
            evaluationState = OrientationEvaluationState.ASSESSED)
        assertTrue(validateOrientationAssessment(OrientationKind.PROJECT, assessment).isEmpty())
        assertNotEquals(OrientationScore.ZERO, calculateOrientationScore(assessment))
    }

    @Test fun unknownKindRetainsRawCodeRatherThanBecomingProject() {
        val unknown = resolveOrientationKind("future-unknown-kind")
        assertNull(unknown.known)
        assertEquals("future-unknown-kind", unknown.rawCode)
        assertEquals(OrientationKind.INTENT, resolveOrientationKind("INTENT").known)
        assertEquals(OrientationKind.DAY_THEME, resolveOrientationKind("DAY_THEME").known)
    }
}
