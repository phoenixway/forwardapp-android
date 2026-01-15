package com.romankozak.forwardappmobile.domain.ai.policy

import com.romankozak.forwardappmobile.domain.ai.state.ExecutionMode
import com.romankozak.forwardappmobile.domain.ai.state.EntropyLevel
import com.romankozak.forwardappmobile.domain.ai.state.LifeSystemState
import com.romankozak.forwardappmobile.domain.ai.state.LoadLevel

import javax.inject.Inject

class OverloadPolicy @Inject constructor() : AiPolicy {
    override fun evaluate(state: LifeSystemState): List<AiDecision> {
        return if (state.loadLevel == LoadLevel.HIGH || state.loadLevel == LoadLevel.CRITICAL) {
            listOf(
                AiDecision.AdaptUi(UiAdaptationMode.REDUCED_COMPLEXITY),
                AiDecision.ShowRecommendation(id = "stabilize", priority = 10),
            )
        } else emptyList()
    }
}

class StuckPolicy @Inject constructor() : AiPolicy {
    override fun evaluate(state: LifeSystemState): List<AiDecision> {
        return if (state.executionMode == ExecutionMode.STUCK) {
            listOf(
                AiDecision.AdaptUi(UiAdaptationMode.FOCUS),
                AiDecision.ShowRecommendation(id = "micro_intervention", priority = 9),
            )
        } else emptyList()
    }
}

class EntropyPolicy @Inject constructor() : AiPolicy {
    override fun evaluate(state: LifeSystemState): List<AiDecision> {
        return if (state.entropy == EntropyLevel.HIGH) {
            listOf(
                AiDecision.ModifyDefaults(key = "planner_mode", value = "cleanup"),
                AiDecision.ScheduleWorker(worker = "CleanupWorker", delayMinutes = 30),
            )
        } else emptyList()
    }
}
