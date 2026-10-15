package com.romankozak.forwardappmobile.shared.core.models.orientation

data class ScoringCoordinate(
    val valueCode: String,
    val coordinate: Float,
)

data class ScoringAxisPolicy(
    val min: Float,
    val max: Float,
    val coordinates: List<ScoringCoordinate>,
)

data class ScoringPolicy(
    val id: String,
    val version: Int,
    val formulaVersion: Int,
    val effortWeight: Float,
    val costWeight: Float,
    val riskWeight: Float,
    val importance: ScoringAxisPolicy,
    val impact: ScoringAxisPolicy,
    val effort: ScoringAxisPolicy,
    val cost: ScoringAxisPolicy,
    val risk: ScoringAxisPolicy,
)

data class OrientationScoringOverride(
    val importanceCoordinate: Float? = null,
    val impactCoordinate: Float? = null,
    val effortWeight: Float? = null,
    val costWeight: Float? = null,
    val riskWeight: Float? = null,
)

data class OrientationScoringCoordinates(
    val importance: Float,
    val impact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
)

data class OrientationScore(
    val benefit: Float,
    val penalty: Float,
    val raw: Float,
    val display: Int,
) {
    companion object {
        val ZERO =
            OrientationScore(
                benefit = 0f,
                penalty = 0f,
                raw = 0f,
                display = 0,
            )
    }
}
