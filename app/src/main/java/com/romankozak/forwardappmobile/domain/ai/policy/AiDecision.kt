package com.romankozak.forwardappmobile.domain.ai.policy

sealed interface AiDecision {

    data class ShowRecommendation(
        val id: String,
        val priority: Int,
    ) : AiDecision

    data class AdaptUi(
        val mode: UiAdaptationMode,
    ) : AiDecision

    data class ModifyDefaults(
        val key: String,
        val value: String,
    ) : AiDecision

    data class ScheduleWorker(
        val worker: String,
        val delayMinutes: Long,
    ) : AiDecision
}

enum class UiAdaptationMode { NORMAL, REDUCED_COMPLEXITY, FOCUS }
