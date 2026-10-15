package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.CostValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffortValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringCoordinates
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringOverride
import com.romankozak.forwardappmobile.shared.core.models.orientation.RiskValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrientationScoringTest {
    @Test
    fun rawCoordinateCalculatorMatchesLegacyFormulaForArbitraryLegacyFloats() {
        val actual =
            calculateOrientationScoreFromCoordinates(
                coordinates =
                    OrientationScoringCoordinates(
                        importance = 2.5f,
                        impact = 11f,
                        effort = 7.3f,
                        cost = 4.4f,
                        risk = 17f,
                    ),
                effortWeight = 0.7f,
                costWeight = 1.3f,
                riskWeight = 2.1f,
            )

        val expected =
            legacyScore(
                importance = 2.5f,
                impact = 11f,
                effort = 7.3f,
                cost = 4.4f,
                risk = 17f,
                effortWeight = 0.7f,
                costWeight = 1.3f,
                riskWeight = 2.1f,
            )

        assertFloatEquals(expected.raw, actual.raw)
        assertEquals(expected.display, actual.display)
    }

    @Test
    fun defaultPolicyMatchesLegacyFormulaForCanonicalCoordinates() {
        val assessment =
            assessed(
                importance = ImportanceValue.HIGH,
                impact = ImpactValue.LARGE,
                effort = EffortValue.VERY_HIGH,
                cost = CostValue.HIGH,
                risk = RiskValue.EXTREME,
            )
        val override =
            OrientationScoringOverride(
                effortWeight = 1.5f,
                costWeight = 0.5f,
                riskWeight = 2f,
            )

        val actual = calculateOrientationScore(assessment, override = override)
        val expected =
            legacyScore(
                importance = 7f,
                impact = 5f,
                effort = 8f,
                cost = 4f,
                risk = 13f,
                effortWeight = 1.5f,
                costWeight = 0.5f,
                riskWeight = 2f,
            )

        assertFloatEquals(expected.raw, actual.raw)
        assertEquals(expected.display, actual.display)
    }

    @Test
    fun legacyPrecisionOverridePreservesCollapsedImportanceAndImpactCoordinates() {
        val assessment =
            assessed(
                importance = ImportanceValue.LOW,
                impact = ImpactValue.TRANSFORMATIVE,
                effort = EffortValue.LOW,
                cost = CostValue.MODERATE,
                risk = RiskValue.HIGH,
            )

        val actual =
            calculateOrientationScore(
                assessment = assessment,
                override =
                    OrientationScoringOverride(
                        importanceCoordinate = 2f,
                        impactCoordinate = 13f,
                    ),
            )
        val expected =
            legacyScore(
                importance = 2f,
                impact = 13f,
                effort = 2f,
                cost = 3f,
                risk = 5f,
            )

        assertFloatEquals(expected.raw, actual.raw)
        assertEquals(expected.display, actual.display)
    }

    @Test
    fun semanticDefaultsUseAcceptedRepresentativeCoordinates() {
        val assessment =
            assessed(
                importance = ImportanceValue.CRITICAL,
                impact = ImpactValue.TRANSFORMATIVE,
                effort = EffortValue.NONE,
                cost = CostValue.NONE,
                risk = RiskValue.NONE,
            )

        val actual = calculateOrientationScore(assessment)
        val expected =
            legacyScore(
                importance = 10f,
                impact = 8f,
                effort = 0f,
                cost = 0f,
                risk = 0f,
            )

        assertFloatEquals(expected.raw, actual.raw)
        assertEquals(expected.display, actual.display)
    }

    @Test
    fun nonAssessedEvaluationDoesNotProduceScore() {
        val assessment =
            assessed(
                importance = ImportanceValue.HIGH,
                impact = ImpactValue.LARGE,
                effort = EffortValue.HIGH,
                cost = CostValue.HIGH,
                risk = RiskValue.HIGH,
            ).copy(evaluationState = OrientationEvaluationState.UNASSESSED)

        assertEquals(0f, calculateOrientationScore(assessment).raw)
        assertEquals(0, calculateOrientationScore(assessment).display)
    }

    @Test
    fun assessedButIncompleteEvaluationDoesNotTreatUnsetAsZeroCost() {
        val assessment =
            assessed(
                importance = ImportanceValue.HIGH,
                impact = ImpactValue.LARGE,
                effort = EffortValue.HIGH,
                cost = CostValue.HIGH,
                risk = RiskValue.HIGH,
            ).copy(risk = AxisAssessment(null, ValueOrigin.UNSET))

        assertEquals(0f, calculateOrientationScore(assessment).raw)
        assertEquals(0, calculateOrientationScore(assessment).display)
    }

    @Test
    fun zeroTotalWeightMatchesLegacyZeroPenaltyRule() {
        val assessment =
            assessed(
                importance = ImportanceValue.HIGH,
                impact = ImpactValue.LARGE,
                effort = EffortValue.MASSIVE,
                cost = CostValue.VERY_HIGH,
                risk = RiskValue.CRITICAL,
            )

        val actual =
            calculateOrientationScore(
                assessment = assessment,
                override =
                    OrientationScoringOverride(
                        effortWeight = 0f,
                        costWeight = 0f,
                        riskWeight = 0f,
                    ),
            )
        val expected =
            legacyScore(
                importance = 7f,
                impact = 5f,
                effort = 21f,
                cost = 5f,
                risk = 21f,
                effortWeight = 0f,
                costWeight = 0f,
                riskWeight = 0f,
            )

        assertFloatEquals(expected.raw, actual.raw)
        assertEquals(expected.display, actual.display)
    }

    private fun assessed(
        importance: ImportanceValue,
        impact: ImpactValue,
        effort: EffortValue,
        cost: CostValue,
        risk: RiskValue,
    ) =
        emptyApplicableAssessment().copy(
            importance = explicit(importance.name),
            impact = explicit(impact.name),
            effort = explicit(effort.name),
            cost = explicit(cost.name),
            risk = explicit(risk.name),
            evaluationState = OrientationEvaluationState.ASSESSED,
        )

    private fun explicit(valueCode: String) =
        AxisAssessment(
            valueCode = valueCode,
            origin = ValueOrigin.EXPLICIT,
        )

    private fun legacyScore(
        importance: Float,
        impact: Float,
        effort: Float,
        cost: Float,
        risk: Float,
        effortWeight: Float = 1f,
        costWeight: Float = 1f,
        riskWeight: Float = 1f,
    ): LegacyScore {
        fun normalize(
            value: Float,
            min: Float,
            max: Float,
        ): Float = ((value - min) / (max - min)).coerceIn(0f, 1f)

        val benefit =
            normalize(importance, 1f, 12f) *
                normalize(impact, 1f, 13f)

        val totalWeight = effortWeight + costWeight + riskWeight
        val penalty =
            if (totalWeight > 0f) {
                (
                    effortWeight * normalize(effort, 0f, 21f) +
                        costWeight * normalize(cost, 0f, 5f) +
                        riskWeight * normalize(risk, 0f, 21f)
                ) / totalWeight
            } else {
                0f
            }

        val raw = benefit - penalty
        return LegacyScore(
            raw = raw,
            display = (((raw + 1f) / 2f) * 100f).toInt().coerceIn(0, 100),
        )
    }

    private fun assertFloatEquals(
        expected: Float,
        actual: Float,
    ) {
        assertTrue(
            kotlin.math.abs(expected - actual) < 0.000001f,
            "Expected $expected but was $actual",
        )
    }

    private data class LegacyScore(
        val raw: Float,
        val display: Int,
    )
}
