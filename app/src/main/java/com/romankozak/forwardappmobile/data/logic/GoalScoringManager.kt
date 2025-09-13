package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import kotlin.math.roundToInt

object GoalScoringManager {
    private val effortScale = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    private val importanceScale = (1..12).map { it.toFloat() }
    private val impactScale = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    private val costScale = (0..5).map { it.toFloat() }
    private val riskScale = effortScale

    private fun normalize(
        value: Float,
        scale: List<Float>,
    ): Float {
        val min = scale.firstOrNull() ?: 0f
        val max = scale.lastOrNull() ?: 1f
        if (max <= min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    fun calculateScores(goal: Goal): Goal {
        if (goal.scoringStatus != ScoringStatus.ASSESSED) {
            return goal.copy(
                rawScore = 0f,
                displayScore = 0,
            )
        }

        val normImportance = normalize(goal.valueImportance, importanceScale)
        val normImpact = normalize(goal.valueImpact, impactScale)
        val normEffort = normalize(goal.effort, effortScale)
        val normCost = normalize(goal.cost, costScale)
        val normRisk = normalize(goal.risk, riskScale)

        val normBenefit = normImportance * normImpact

        val totalWeight = goal.weightEffort + goal.weightCost + goal.weightRisk
        val normTotalCost =
            if (totalWeight > 0f) {
                (goal.weightEffort * normEffort + goal.weightCost * normCost + goal.weightRisk * normRisk) / totalWeight
            } else {
                0f
            }

        val calculatedRawScore = normBenefit - normTotalCost
        val calculatedDisplayScore = (((calculatedRawScore + 1) / 2) * 100).toInt().coerceIn(0, 100)

        return goal.copy(
            rawScore = calculatedRawScore,
            displayScore = calculatedDisplayScore,
        )
    }

    fun calculateScoresForList(list: GoalList): GoalList {
        if (list.scoringStatus != ScoringStatus.ASSESSED) {
            return list.copy(
                rawScore = 0f,
                displayScore = 0,
            )
        }

        val normImportance = normalize(list.valueImportance, importanceScale)
        val normImpact = normalize(list.valueImpact, impactScale)
        val normEffort = normalize(list.effort, effortScale)
        val normCost = normalize(list.cost, costScale)
        val normRisk = normalize(list.risk, riskScale)

        val normBenefit = normImportance * normImpact

        val totalWeight = list.weightEffort + list.weightCost + list.weightRisk
        val normTotalCost =
            if (totalWeight > 0f) {
                (list.weightEffort * normEffort + list.weightCost * normCost + list.weightRisk * normRisk) / totalWeight
            } else {
                0f
            }

        val calculatedRawScore = normBenefit - normTotalCost
        val calculatedDisplayScore = (((calculatedRawScore + 1) / 2) * 100).toInt().coerceIn(0, 100)

        return list.copy(
            rawScore = calculatedRawScore,
            displayScore = calculatedDisplayScore,
        )
    }
}