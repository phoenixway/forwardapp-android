package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.CostValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffortValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScore
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringCoordinates
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringOverride
import com.romankozak.forwardappmobile.shared.core.models.orientation.RiskValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ScoringAxisPolicy
import com.romankozak.forwardappmobile.shared.core.models.orientation.ScoringCoordinate
import com.romankozak.forwardappmobile.shared.core.models.orientation.ScoringPolicy

val DEFAULT_ORIENTATION_SCORING_POLICY_V1 =
    ScoringPolicy(
        id = "orientation-default",
        version = 1,
        formulaVersion = 1,
        effortWeight = 1f,
        costWeight = 1f,
        riskWeight = 1f,
        importance =
            axisPolicy(
                min = 1f,
                max = 12f,
                ImportanceValue.LOW.name to 1f,
                ImportanceValue.MEDIUM.name to 4f,
                ImportanceValue.HIGH.name to 7f,
                ImportanceValue.CRITICAL.name to 10f,
            ),
        impact =
            axisPolicy(
                min = 1f,
                max = 13f,
                ImpactValue.TINY.name to 1f,
                ImpactValue.SMALL.name to 2f,
                ImpactValue.MEDIUM.name to 3f,
                ImpactValue.LARGE.name to 5f,
                ImpactValue.TRANSFORMATIVE.name to 8f,
            ),
        effort =
            axisPolicy(
                min = 0f,
                max = 21f,
                EffortValue.NONE.name to 0f,
                EffortValue.MINIMAL.name to 1f,
                EffortValue.LOW.name to 2f,
                EffortValue.MODERATE.name to 3f,
                EffortValue.HIGH.name to 5f,
                EffortValue.VERY_HIGH.name to 8f,
                EffortValue.EXTREME.name to 13f,
                EffortValue.MASSIVE.name to 21f,
            ),
        cost =
            axisPolicy(
                min = 0f,
                max = 5f,
                CostValue.NONE.name to 0f,
                CostValue.VERY_LOW.name to 1f,
                CostValue.LOW.name to 2f,
                CostValue.MODERATE.name to 3f,
                CostValue.HIGH.name to 4f,
                CostValue.VERY_HIGH.name to 5f,
            ),
        risk =
            axisPolicy(
                min = 0f,
                max = 21f,
                RiskValue.NONE.name to 0f,
                RiskValue.MINIMAL.name to 1f,
                RiskValue.LOW.name to 2f,
                RiskValue.MODERATE.name to 3f,
                RiskValue.HIGH.name to 5f,
                RiskValue.VERY_HIGH.name to 8f,
                RiskValue.EXTREME.name to 13f,
                RiskValue.CRITICAL.name to 21f,
            ),
    )

fun calculateOrientationScore(
    assessment: OrientationAssessment,
    policy: ScoringPolicy = DEFAULT_ORIENTATION_SCORING_POLICY_V1,
    override: OrientationScoringOverride = OrientationScoringOverride(),
): OrientationScore {
    if (assessment.evaluationState != OrientationEvaluationState.ASSESSED) {
        return OrientationScore.ZERO
    }

    val importance =
        override.importanceCoordinate
            ?: assessment.importance.coordinateIn(policy.importance)
            ?: return OrientationScore.ZERO
    val impact =
        override.impactCoordinate
            ?: assessment.impact.coordinateIn(policy.impact)
            ?: return OrientationScore.ZERO
    val effort = assessment.effort.coordinateIn(policy.effort) ?: return OrientationScore.ZERO
    val cost = assessment.cost.coordinateIn(policy.cost) ?: return OrientationScore.ZERO
    val risk = assessment.risk.coordinateIn(policy.risk) ?: return OrientationScore.ZERO

    return calculateOrientationScoreFromCoordinates(
        coordinates =
            OrientationScoringCoordinates(
                importance = importance,
                impact = impact,
                effort = effort,
                cost = cost,
                risk = risk,
            ),
        effortWeight = override.effortWeight ?: policy.effortWeight,
        costWeight = override.costWeight ?: policy.costWeight,
        riskWeight = override.riskWeight ?: policy.riskWeight,
        policy = policy,
    )
}

fun calculateOrientationScoreFromCoordinates(
    coordinates: OrientationScoringCoordinates,
    effortWeight: Float,
    costWeight: Float,
    riskWeight: Float,
    policy: ScoringPolicy = DEFAULT_ORIENTATION_SCORING_POLICY_V1,
): OrientationScore {
    val normImportance = normalize(coordinates.importance, policy.importance)
    val normImpact = normalize(coordinates.impact, policy.impact)
    val normEffort = normalize(coordinates.effort, policy.effort)
    val normCost = normalize(coordinates.cost, policy.cost)
    val normRisk = normalize(coordinates.risk, policy.risk)

    val benefit = normImportance * normImpact
    val totalWeight = effortWeight + costWeight + riskWeight

    val penalty =
        if (totalWeight > 0f) {
            (
                effortWeight * normEffort +
                    costWeight * normCost +
                    riskWeight * normRisk
            ) / totalWeight
        } else {
            0f
        }

    val raw = benefit - penalty
    val display = (((raw + 1f) / 2f) * 100f).toInt().coerceIn(0, 100)

    return OrientationScore(
        benefit = benefit,
        penalty = penalty,
        raw = raw,
        display = display,
    )
}

private fun AxisAssessment.coordinateIn(policy: ScoringAxisPolicy): Float? {
    val code = valueCode ?: return null
    return policy.coordinates.firstOrNull { it.valueCode == code }?.coordinate
}

private fun normalize(
    value: Float,
    policy: ScoringAxisPolicy,
): Float {
    if (policy.max <= policy.min) return 0f
    return ((value - policy.min) / (policy.max - policy.min)).coerceIn(0f, 1f)
}

private fun axisPolicy(
    min: Float,
    max: Float,
    vararg coordinates: Pair<String, Float>,
): ScoringAxisPolicy =
    ScoringAxisPolicy(
        min = min,
        max = max,
        coordinates =
            coordinates.map { (valueCode, coordinate) ->
                ScoringCoordinate(
                    valueCode = valueCode,
                    coordinate = coordinate,
                )
            },
    )
