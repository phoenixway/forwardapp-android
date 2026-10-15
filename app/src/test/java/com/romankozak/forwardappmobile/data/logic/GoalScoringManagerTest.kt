package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GoalScoringManagerTest {
    @Test
    fun `assessed Goal delegates exact raw legacy coordinates and weights`() {
        val goal =
            Goal(
                id = "goal",
                text = "Scored goal",
                completed = false,
                createdAt = 1L,
                updatedAt = 2L,
                valueImportance = 2.5f,
                valueImpact = 11f,
                effort = 7.3f,
                cost = 4.4f,
                risk = 17f,
                weightEffort = 0.7f,
                weightCost = 1.3f,
                weightRisk = 2.1f,
                scoringStatus = ScoringStatusValues.ASSESSED,
                rawScore = 99f,
                displayScore = 99,
            )

        val actual = GoalScoringManager.calculateScores(goal)
        val expected =
            legacyScore(
                importance = goal.valueImportance,
                impact = goal.valueImpact,
                effort = goal.effort,
                cost = goal.cost,
                risk = goal.risk,
                effortWeight = goal.weightEffort,
                costWeight = goal.weightCost,
                riskWeight = goal.weightRisk,
            )

        assertTrue(abs(expected.raw - actual.rawScore) < 0.000001f)
        assertEquals(expected.display, actual.displayScore)
    }

    @Test
    fun `non assessed Goal preserves legacy zero score behavior`() {
        val goal =
            Goal(
                id = "goal",
                text = "Unassessed goal",
                completed = false,
                createdAt = 1L,
                updatedAt = 2L,
                valueImportance = 12f,
                valueImpact = 13f,
                effort = 21f,
                cost = 5f,
                risk = 21f,
                scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                rawScore = 0.75f,
                displayScore = 88,
            )

        val actual = GoalScoringManager.calculateScores(goal)

        assertEquals(0f, actual.rawScore)
        assertEquals(0, actual.displayScore)
    }

    private fun legacyScore(
        importance: Float,
        impact: Float,
        effort: Float,
        cost: Float,
        risk: Float,
        effortWeight: Float,
        costWeight: Float,
        riskWeight: Float,
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

    private data class LegacyScore(
        val raw: Float,
        val display: Int,
    )
}
