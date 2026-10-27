package com.romankozak.forwardappmobile.data.entitybrowser

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationScoringOverrideEntity
import com.romankozak.forwardappmobile.data.orientation.toCanonicalAssessment
import com.romankozak.forwardappmobile.data.orientation.toModel
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalScoreDimension
import com.romankozak.forwardappmobile.shared.core.domain.orientation.DEFAULT_ORIENTATION_SCORING_POLICY_V1
import com.romankozak.forwardappmobile.shared.core.domain.orientation.calculateOrientationScore
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringOverride

/** Current Orientation-owned evaluation only. Unknown policy/values are absent, never coerced to zero. */
internal fun canonicalQueryScores(
    assessment: OrientationAssessmentEntity,
    override: OrientationScoringOverrideEntity?,
): Map<CanonicalScoreDimension, Double> {
    if (assessment.isDeleted || override?.isDeleted == true) return emptyMap()
    val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
    if (override != null && (override.orientationId != assessment.orientationId || override.policyId != policy.id ||
        override.policyVersion != policy.version || override.formulaVersion != policy.formulaVersion)) return emptyMap()
    val values = try { assessment.toCanonicalAssessment() } catch (_: IllegalArgumentException) { return emptyMap() }
    if (values.evaluationState != OrientationEvaluationState.ASSESSED) return emptyMap()
    val overrides = override?.toModel() ?: OrientationScoringOverride()
    fun coordinate(code: String?, coordinates: List<com.romankozak.forwardappmobile.shared.core.models.orientation.ScoringCoordinate>) =
        coordinates.firstOrNull { it.valueCode == code }?.coordinate
    // Preserve the canonical Float's decimal representation for inclusive user-entered bounds (e.g. 7.1).
    val dimensions = mapOf(
        CanonicalScoreDimension.IMPORTANCE to (overrides.importanceCoordinate ?: coordinate(values.importance.valueCode, policy.importance.coordinates)),
        CanonicalScoreDimension.IMPACT to (overrides.impactCoordinate ?: coordinate(values.impact.valueCode, policy.impact.coordinates)),
        CanonicalScoreDimension.EFFORT to coordinate(values.effort.valueCode, policy.effort.coordinates),
        CanonicalScoreDimension.COST to coordinate(values.cost.valueCode, policy.cost.coordinates),
        CanonicalScoreDimension.RISK to coordinate(values.risk.valueCode, policy.risk.coordinates),
    ).mapNotNull { (key, value) -> value?.takeIf { it.isFinite() }?.let { key to it.toString().toDouble() } }.toMap()
    return if (dimensions.size == 5) dimensions + (CanonicalScoreDimension.SCORE to calculateOrientationScore(values, policy, overrides).display.toDouble()) else dimensions
}
