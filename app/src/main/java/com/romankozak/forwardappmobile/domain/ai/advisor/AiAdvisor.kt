package com.romankozak.forwardappmobile.domain.ai.advisor

import com.romankozak.forwardappmobile.domain.ai.state.LifeSystemState
import javax.inject.Inject

interface AiAdvisor {
    suspend fun explain(state: LifeSystemState, context: AdvisorContext): AdvisorInsight
}

data class AdvisorContext(val notes: String? = null)

data class AdvisorInsight(
    val summary: String,
    val recommendations: List<String>,
)

class NoOpAiAdvisor @Inject constructor() : AiAdvisor {
    override suspend fun explain(state: LifeSystemState, context: AdvisorContext): AdvisorInsight =
        AdvisorInsight(
            summary = "LLM advisor disabled",
            recommendations = emptyList(),
        )
}
