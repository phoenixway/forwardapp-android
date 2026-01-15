package com.romankozak.forwardappmobile.domain.ai.policy

import com.romankozak.forwardappmobile.domain.ai.state.LifeSystemState

interface AiPolicy {
    fun evaluate(state: LifeSystemState): List<AiDecision>
}
