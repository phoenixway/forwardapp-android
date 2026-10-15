package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.shared.core.domain.orientation.calculateOrientationScoreFromCoordinates
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringCoordinates

object GoalScoringManager {
    fun calculateScores(goal: Goal): Goal {
        if (goal.scoringStatus != ScoringStatusValues.ASSESSED) {
            return goal.copy(
                rawScore = 0f,
                displayScore = 0,
            )
        }

        val score =
            calculateOrientationScoreFromCoordinates(
                coordinates =
                    OrientationScoringCoordinates(
                        importance = goal.valueImportance,
                        impact = goal.valueImpact,
                        effort = goal.effort,
                        cost = goal.cost,
                        risk = goal.risk,
                    ),
                effortWeight = goal.weightEffort,
                costWeight = goal.weightCost,
                riskWeight = goal.weightRisk,
            )

        return goal.copy(
            rawScore = score.raw,
            displayScore = score.display,
        )
    }
}
