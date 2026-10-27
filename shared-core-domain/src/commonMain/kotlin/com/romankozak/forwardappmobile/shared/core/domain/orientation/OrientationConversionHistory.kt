package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.*

/** Verifies one subject's immutable, version-ordered history; returns exact historical revision kinds. */
fun historicalAssessmentKinds(
    facts: List<SubjectConversionEvidence>, currentKind: OrientationKind, subjectVersion: Long,
): Map<String, OrientationKind> {
    if (facts.isEmpty()) return emptyMap()
    require(facts.map { it.id }.distinct().size == facts.size && facts.map { it.subjectId }.distinct().size == 1)
    val ordered = facts.sortedBy { it.toSubjectVersion }
    var previous: SubjectConversionEvidence? = null
    val revisions = linkedMapOf<String, OrientationKind>()
    ordered.forEach { fact ->
        fact.requireValidShape()
        previous?.let { old ->
            require(fact.formatVersion == 2 && fact.fromSubjectVersion >= old.toSubjectVersion && fact.fromOrientationKind == old.targetOrientationKind) { "Conflicting kind conversion branch" }
        }
        if (fact.formatVersion == 2) {
            val fromKind = OrientationKind.valueOf(requireNotNull(fact.fromOrientationKind))
            requireNotNull(fact.previousAssessmentRevisionIds).forEach { id ->
                require(revisions[id] == null || revisions[id] == fromKind) { "Revision claimed by divergent kinds" }
                revisions[id] = fromKind
            }
            val resultId = "${fact.subjectId}:kind:${fact.fromSubjectVersion}:assessment"
            require(resultId !in revisions) { "Kind result revision cannot precede its change" }
            revisions[resultId] = OrientationKind.valueOf(fact.targetOrientationKind)
        }
        previous = fact
    }
    val last = ordered.last()
    require(last.targetOrientationKind == currentKind.name && subjectVersion >= last.toSubjectVersion) { "Current kind/version contradicts conversion history" }
    return revisions
}

/** Preserve axes with identical applicability; reset only changed axes to the target's canonical default. */
fun assessmentAfterKindChange(from: OrientationKind, to: OrientationKind, current: OrientationAssessment): OrientationAssessment {
    require(from in SUBJECT_CONVERSION_TARGET_KINDS && to in SUBJECT_CONVERSION_TARGET_KINDS && from != to)
    require(validateOrientationAssessment(from, current).isEmpty())
    val defaults = initialOrientationAssessment(to)
    fun axis(axis: OrientationAxis) = if (axisApplicability(from, axis) == axisApplicability(to, axis)) current.valueFor(axis) else defaults.valueFor(axis)
    return OrientationAssessment(
        axis(OrientationAxis.IMPORTANCE), axis(OrientationAxis.IMPACT), axis(OrientationAxis.BREADTH),
        axis(OrientationAxis.EXPECTED_SPAN), axis(OrientationAxis.TARGET_WINDOW), axis(OrientationAxis.ATTENTION_TIER),
        axis(OrientationAxis.COMMITMENT), axis(OrientationAxis.CONFIDENCE), axis(OrientationAxis.EFFORT),
        axis(OrientationAxis.COST), axis(OrientationAxis.RISK), current.evaluationState,
    ).also { require(validateOrientationAssessment(to, it).isEmpty()) }
}
