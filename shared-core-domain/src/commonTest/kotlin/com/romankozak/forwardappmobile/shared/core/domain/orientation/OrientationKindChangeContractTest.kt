package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.test.*

class OrientationKindChangeContractTest {
    private fun fact(from: OrientationKind, to: OrientationKind, version: Long, prior: List<String>) = SubjectConversionEvidence(
        "id:ORIENTATION_KIND:$version", "id", 2, "ORIENTATION", "ORIENTATION", to.name, version, version + 1,
        null, 0, false, 10, 10, 1, null, from.name, prior)

    @Test fun allOrdinaryPairsHaveValidTargetAssessmentAndPreserveCommonAxes() {
        for (from in SUBJECT_CONVERSION_TARGET_KINDS) for (to in SUBJECT_CONVERSION_TARGET_KINDS.filter { it != from }) {
            val source = initialOrientationAssessment(from).copy(importance = AxisAssessment("HIGH", ValueOrigin.EXPLICIT))
            val result = assessmentAfterKindChange(from, to, source)
            assertTrue(validateOrientationAssessment(to, result).isEmpty())
            assertEquals(source.importance, result.importance)
            assertEquals(source.effort, result.effort)
        }
    }
    @Test fun sourceSpecificDefaultsAreNotInferredAsUserValuesInNextKind() {
        val result = assessmentAfterKindChange(OrientationKind.ONGOING_STANDARD, OrientationKind.PROJECT, initialOrientationAssessment(OrientationKind.ONGOING_STANDARD))
        assertEquals(ValueOrigin.UNSET, result.expectedSpan.origin)
        assertEquals(ValueOrigin.UNSET, result.targetWindow.origin)
    }
    @Test fun historyUsesExactRevisionIdsAndVersionOrderedKindChain() {
        val first = fact(OrientationKind.INTENT, OrientationKind.PROJECT, 1, listOf("initial"))
        val second = fact(OrientationKind.PROJECT, OrientationKind.ONGOING_STANDARD, 5, listOf("id:kind:1:assessment", "edited"))
        val result = historicalAssessmentKinds(listOf(second, first), OrientationKind.ONGOING_STANDARD, 6)
        assertEquals(OrientationKind.INTENT, result["initial"])
        assertEquals(OrientationKind.PROJECT, result["edited"])
        assertEquals(OrientationKind.ONGOING_STANDARD, result["id:kind:5:assessment"])
    }
    @Test fun divergentBranchMissingClosureAndUnsupportedKindsFailClosed() {
        val first = fact(OrientationKind.INTENT, OrientationKind.PROJECT, 1, listOf("initial"))
        val branch = fact(OrientationKind.MILESTONE, OrientationKind.ONGOING_STANDARD, 2, listOf("other"))
        assertFails { historicalAssessmentKinds(listOf(first, branch), OrientationKind.ONGOING_STANDARD, 3) }
        assertFails { historicalAssessmentKinds(listOf(first), OrientationKind.ONGOING_STANDARD, 3) }
        assertFails { first.copy(previousAssessmentRevisionIds = emptyList()).requireValidShape() }
        assertFails { first.copy(fromOrientationKind = "DAY_THEME").requireValidShape() }
        assertFails { assessmentAfterKindChange(OrientationKind.GOAL, OrientationKind.PROJECT, initialOrientationAssessment(OrientationKind.GOAL)) }
    }
}
