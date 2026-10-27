package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.test.*

class SubjectConversionReferenceContractTest {
    private val fact = SubjectConversionEvidence("s:ASPECT:ORIENTATION:1", "s", 1, "ASPECT", "ORIENTATION", "PROJECT",
        1, 2, null, 0, false, 20, 20, 1, null)
    private val graph = CanonicalOrientationValidationGraph(
        subjects = listOf(CanonicalSubjectReference("s", ManagedSubjectType.ORIENTATION, 2), CanonicalSubjectReference("o", ManagedSubjectType.ORIENTATION)),
        orientations = listOf(CanonicalOrientationReference("s", OrientationKind.PROJECT), CanonicalOrientationReference("o", OrientationKind.INTENT)),
        aspects = emptyList(), assessments = emptyList(), revisions = emptyList(), mappings = emptyList(), relations = emptyList(),
        aspectRefs = listOf(AspectOrientationRef("r", 1, 20, null, true, 2, "s", "o", AspectOrientationRelationType.BELONGS_TO, false, 0)),
        workspaces = emptyList(), bindings = emptyList(), capabilities = emptyList(), savedViews = emptyList(), subjectConversions = listOf(fact))

    @Test fun onlyValidatedEvidenceAdmitsFormerAspectTombstones() {
        assertTrue(validateCanonicalOrientationReferences(graph).isEmpty())
        assertTrue(validateCanonicalOrientationReferences(graph.copy(subjectConversions = emptyList())).any { it.code == "ASPECT_REF_ENDPOINT" })
        assertTrue(validateCanonicalOrientationReferences(graph.copy(aspectRefs = graph.aspectRefs.map { it.copy(isDeleted = false) })).any { it.code == "ASPECT_REF_ENDPOINT" })
    }
    @Test fun duplicateWrongKindVersionOrCurrentSubtypeEvidenceFailsClosed() {
        for (bad in listOf(fact.copy(toSubjectVersion = 99), fact.copy(targetOrientationKind = "GOAL"), fact.copy(fromType = "ORIENTATION"))) {
            assertTrue(validateCanonicalOrientationReferences(graph.copy(subjectConversions = listOf(bad))).any { it.code == "SUBJECT_CONVERSION" })
        }
        assertTrue(validateCanonicalOrientationReferences(graph.copy(subjectConversions = listOf(fact, fact))).any { it.code == "SUBJECT_CONVERSION" })
        assertTrue(validateCanonicalOrientationReferences(graph.copy(aspects = listOf(CanonicalAspectReference("s", null)))).any { it.code == "SUBJECT_CONVERSION" })
    }
    @Test fun historicalDeletedChildMayRetainFormerParentButLiveChildMustBePromoted() {
        val child = CanonicalSubjectReference("child", ManagedSubjectType.ASPECT, 2, true)
        val historical = graph.copy(subjects = graph.subjects + child, aspects = listOf(CanonicalAspectReference("child", "s")))
        assertTrue(validateCanonicalOrientationReferences(historical).isEmpty())
        assertTrue(validateCanonicalOrientationReferences(historical.copy(subjects = graph.subjects + child.copy(isDeleted = false))).any { it.code == "ASPECT_HIERARCHY" })
    }
}
