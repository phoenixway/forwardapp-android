package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationScoringOverrideEntity
import com.romankozak.forwardappmobile.shared.core.domain.orientation.DEFAULT_ORIENTATION_SCORING_POLICY_V1
import com.romankozak.forwardappmobile.shared.core.domain.orientation.calculateOrientationScore
import com.romankozak.forwardappmobile.shared.core.domain.orientation.projectLegacyImpact
import com.romankozak.forwardappmobile.shared.core.domain.orientation.projectLegacyImportance
import com.romankozak.forwardappmobile.shared.core.domain.orientation.projectLegacyScoring
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScore
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringOverride
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueProvenance
import com.google.gson.Gson

data class CanonicalOrientationEvaluation(
    val orientationId: String,
    val assessment: OrientationAssessment,
    val scoringOverride: OrientationScoringOverride,
    val score: OrientationScore,
)

internal data class LegacyScoringInput(
    val importance: Float,
    val impact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    val effortWeight: Float,
    val costWeight: Float,
    val riskWeight: Float,
    val scoringStatus: String,
)

internal data class CanonicalScoringProjection(
    val assessment: OrientationAssessment,
    val scoringOverride: OrientationScoringOverride,
)

internal data class LegacyScoringCompatibilityProjection(
    val importance: Float,
    val impact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    val effortWeight: Float,
    val costWeight: Float,
    val riskWeight: Float,
    val rawScore: Float,
    val displayScore: Int,
    val scoringStatus: String,
)

internal fun projectLegacyScoringToCanonical(
    current: OrientationAssessment,
    source: LegacySubjectRef,
    input: LegacyScoringInput,
    preserveCanonicalImportanceImpact: Boolean,
): CanonicalScoringProjection {
    requireFiniteNonNegative(input.effortWeight, "effortWeight")
    requireFiniteNonNegative(input.costWeight, "costWeight")
    requireFiniteNonNegative(input.riskWeight, "riskWeight")
    val importance =
        projectLegacyImportance(input.importance, input.scoringStatus, source).withScoringPrecisionFallback(
            valueCode = input.importance.importanceValueCode().takeIf { input.scoringStatus.isLegacyAssessed() },
            fieldName = "valueImportance",
            rawValue = input.importance,
            source = source,
        )
    val impact =
        projectLegacyImpact(input.impact, input.scoringStatus, source).withScoringPrecisionFallback(
            valueCode = input.impact.impactValueCode().takeIf { input.scoringStatus.isLegacyAssessed() },
            fieldName = "valueImpact",
            rawValue = input.impact,
            source = source,
        )
    val scoring =
        projectLegacyScoring(
            scoringStatus = input.scoringStatus,
            importance = input.importance,
            impact = input.impact,
            effort = input.effort,
            cost = input.cost,
            risk = input.risk,
            effortWeight = input.effortWeight,
            costWeight = input.costWeight,
            riskWeight = input.riskWeight,
            source = source,
        )
    val targetImportance =
        if (preserveCanonicalImportanceImpact) current.importance else importance.assessment
    val targetImpact =
        if (preserveCanonicalImportanceImpact) current.impact else impact.assessment
    val assessed = scoring.evaluationState == OrientationEvaluationState.ASSESSED
    val override =
        scoring.override.copy(
            importanceCoordinate =
                input.importance.takeIf {
                    assessed && targetImportance.valueCode == importance.assessment.valueCode
                },
            impactCoordinate =
                input.impact.takeIf {
                    assessed && targetImpact.valueCode == impact.assessment.valueCode
                },
        )
    return CanonicalScoringProjection(
        assessment =
            current.copy(
                importance = targetImportance,
                impact = targetImpact,
                effort = scoring.effort,
                cost = scoring.cost,
                risk = scoring.risk,
                evaluationState = scoring.evaluationState,
            ),
        scoringOverride = override,
    )
}

internal fun CanonicalOrientationEvaluation.toLegacyProjection(): LegacyScoringCompatibilityProjection {
    val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
    return LegacyScoringCompatibilityProjection(
        importance =
            scoringOverride.importanceCoordinate
                ?: assessment.importance.coordinate(policy.importance.coordinates.associate { it.valueCode to it.coordinate }),
        impact =
            scoringOverride.impactCoordinate
                ?: assessment.impact.coordinate(policy.impact.coordinates.associate { it.valueCode to it.coordinate }),
        effort = assessment.effort.coordinate(policy.effort.coordinates.associate { it.valueCode to it.coordinate }),
        cost = assessment.cost.coordinate(policy.cost.coordinates.associate { it.valueCode to it.coordinate }),
        risk = assessment.risk.coordinate(policy.risk.coordinates.associate { it.valueCode to it.coordinate }),
        effortWeight = scoringOverride.effortWeight ?: policy.effortWeight,
        costWeight = scoringOverride.costWeight ?: policy.costWeight,
        riskWeight = scoringOverride.riskWeight ?: policy.riskWeight,
        rawScore = score.raw,
        displayScore = score.display,
        scoringStatus = assessment.evaluationState.toLegacyStatus(),
    )
}

internal fun OrientationAssessmentEntity.toCanonicalAssessment(): OrientationAssessment =
    OrientationAssessment(
        importance = AxisAssessment(importanceValue, ValueOrigin.valueOf(importanceOrigin)),
        impact = AxisAssessment(impactValue, ValueOrigin.valueOf(impactOrigin)),
        breadth = AxisAssessment(breadthValue, ValueOrigin.valueOf(breadthOrigin)),
        expectedSpan = AxisAssessment(expectedSpanValue, ValueOrigin.valueOf(expectedSpanOrigin)),
        targetWindow = AxisAssessment(targetWindowValue, ValueOrigin.valueOf(targetWindowOrigin)),
        attentionTier = AxisAssessment(attentionTierValue, ValueOrigin.valueOf(attentionTierOrigin)),
        commitment = AxisAssessment(commitmentValue, ValueOrigin.valueOf(commitmentOrigin)),
        confidence = AxisAssessment(confidenceValue, ValueOrigin.valueOf(confidenceOrigin)),
        effort = AxisAssessment(effortValue, ValueOrigin.valueOf(effortOrigin)),
        cost = AxisAssessment(costValue, ValueOrigin.valueOf(costOrigin)),
        risk = AxisAssessment(riskValue, ValueOrigin.valueOf(riskOrigin)),
        evaluationState = OrientationEvaluationState.valueOf(evaluationState),
    )

internal fun OrientationScoringOverrideEntity.toModel(): OrientationScoringOverride =
    OrientationScoringOverride(
        importanceCoordinate = importanceCoordinate,
        impactCoordinate = impactCoordinate,
        effortWeight = effortWeight,
        costWeight = costWeight,
        riskWeight = riskWeight,
    )

internal fun scoringOverrideEntity(
    orientationId: String,
    override: OrientationScoringOverride,
    current: OrientationScoringOverrideEntity?,
    now: Long,
): OrientationScoringOverrideEntity {
    val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
    return OrientationScoringOverrideEntity(
        orientationId = orientationId,
        policyId = policy.id,
        policyVersion = policy.version,
        formulaVersion = policy.formulaVersion,
        importanceCoordinate = override.importanceCoordinate,
        impactCoordinate = override.impactCoordinate,
        effortWeight = override.effortWeight,
        costWeight = override.costWeight,
        riskWeight = override.riskWeight,
        createdAt = current?.createdAt ?: now,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = (current?.version ?: 0L) + 1L,
    )
}

internal fun OrientationAssessmentEntity.withCanonicalAssessment(
    assessment: OrientationAssessment,
    revisionId: String,
    now: Long,
    gson: Gson,
): OrientationAssessmentEntity =
    copy(
        revisionId = revisionId,
        importanceValue = assessment.importance.valueCode,
        importanceOrigin = assessment.importance.origin.name,
        impactValue = assessment.impact.valueCode,
        impactOrigin = assessment.impact.origin.name,
        breadthValue = assessment.breadth.valueCode,
        breadthOrigin = assessment.breadth.origin.name,
        expectedSpanValue = assessment.expectedSpan.valueCode,
        expectedSpanOrigin = assessment.expectedSpan.origin.name,
        targetWindowValue = assessment.targetWindow.valueCode,
        targetWindowOrigin = assessment.targetWindow.origin.name,
        attentionTierValue = assessment.attentionTier.valueCode,
        attentionTierOrigin = assessment.attentionTier.origin.name,
        commitmentValue = assessment.commitment.valueCode,
        commitmentOrigin = assessment.commitment.origin.name,
        confidenceValue = assessment.confidence.valueCode,
        confidenceOrigin = assessment.confidence.origin.name,
        effortValue = assessment.effort.valueCode,
        effortOrigin = assessment.effort.origin.name,
        costValue = assessment.cost.valueCode,
        costOrigin = assessment.cost.origin.name,
        riskValue = assessment.risk.valueCode,
        riskOrigin = assessment.risk.origin.name,
        evaluationState = assessment.evaluationState.name,
        provenanceJson =
            gson.toJson(
                listOf(
                    assessment.importance,
                    assessment.impact,
                    assessment.breadth,
                    assessment.expectedSpan,
                    assessment.targetWindow,
                    assessment.attentionTier,
                    assessment.commitment,
                    assessment.confidence,
                    assessment.effort,
                    assessment.cost,
                    assessment.risk,
                ).mapNotNull { it.provenance },
            ),
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = version + 1L,
    )

internal fun scoringAssessmentRevision(
    id: String,
    orientationId: String,
    assessment: OrientationAssessment,
    now: Long,
    reason: String,
    source: AssessmentRevisionSource,
    gson: Gson,
): OrientationAssessmentRevisionEntity =
    OrientationAssessmentRevisionEntity(
        id = id,
        orientationId = orientationId,
        effectiveFrom = now,
        recordedAt = now,
        source = source.name,
        reason = reason,
        assessmentJson = gson.toJson(assessment),
        createdAt = now,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

internal fun canonicalEvaluation(
    assessmentEntity: OrientationAssessmentEntity,
    overrideEntity: OrientationScoringOverrideEntity,
): CanonicalOrientationEvaluation {
    val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
    require(!assessmentEntity.isDeleted && !overrideEntity.isDeleted)
    require(assessmentEntity.orientationId == overrideEntity.orientationId)
    require(
        overrideEntity.policyId == policy.id &&
            overrideEntity.policyVersion == policy.version &&
            overrideEntity.formulaVersion == policy.formulaVersion,
    ) { "Unsupported Orientation scoring policy binding" }
    val assessment = assessmentEntity.toCanonicalAssessment()
    val override = overrideEntity.toModel()
    return CanonicalOrientationEvaluation(
        orientationId = assessmentEntity.orientationId,
        assessment = assessment,
        scoringOverride = override,
        score = calculateOrientationScore(assessment, policy, override),
    )
}

private fun AxisAssessment.coordinate(values: Map<String, Float>): Float =
    valueCode?.let(values::get) ?: 0f

private fun OrientationEvaluationState.toLegacyStatus(): String =
    when (this) {
        OrientationEvaluationState.UNASSESSED -> ScoringStatusValues.NOT_ASSESSED
        OrientationEvaluationState.ASSESSED -> ScoringStatusValues.ASSESSED
        OrientationEvaluationState.UNSCORABLE -> ScoringStatusValues.IMPOSSIBLE_TO_ASSESS
    }

private fun requireFiniteNonNegative(value: Float, name: String) {
    require(value.isFinite() && value >= 0f) { "$name must be finite and non-negative" }
}

private fun String.isLegacyAssessed(): Boolean = trim().uppercase() == ScoringStatusValues.ASSESSED

private fun com.romankozak.forwardappmobile.shared.core.domain.orientation.LegacyAxisProjection.withScoringPrecisionFallback(
    valueCode: String?,
    fieldName: String,
    rawValue: Float,
    source: LegacySubjectRef,
): com.romankozak.forwardappmobile.shared.core.domain.orientation.LegacyAxisProjection {
    if (assessment.valueCode != null || valueCode == null) return this
    return copy(
        assessment =
            AxisAssessment(
                valueCode = valueCode,
                origin = ValueOrigin.DERIVED,
                provenance =
                    ValueProvenance(
                        sourceType = source.sourceType.name,
                        sourceId = source.sourceId,
                        fieldName = fieldName,
                        rawValue = rawValue.toString(),
                    ),
            ),
        diagnostics = emptyList(),
    )
}

private fun Float.importanceValueCode(): String? =
    when {
        !isFinite() || this < 1f || this > 12f -> null
        this < 4f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue.LOW.name
        this < 7f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue.MEDIUM.name
        this < 10f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue.HIGH.name
        else -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue.CRITICAL.name
    }

private fun Float.impactValueCode(): String? =
    when {
        !isFinite() || this < 1f || this > 13f -> null
        this < 2f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue.TINY.name
        this < 3f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue.SMALL.name
        this < 5f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue.MEDIUM.name
        this < 8f -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue.LARGE.name
        else -> com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue.TRANSFORMATIVE.name
    }
