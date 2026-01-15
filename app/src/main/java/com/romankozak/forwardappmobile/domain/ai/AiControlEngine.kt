package com.romankozak.forwardappmobile.domain.ai

import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import com.romankozak.forwardappmobile.data.repository.LifeSystemStateRepository
import com.romankozak.forwardappmobile.domain.ai.actuators.AiActuator
import com.romankozak.forwardappmobile.domain.ai.inference.LifeStateInferencer
import com.romankozak.forwardappmobile.domain.ai.policy.AiPolicy
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiControlEngine @Inject constructor(
    private val eventRepo: AiEventRepository,
    private val stateRepo: LifeSystemStateRepository,
    private val inferencer: LifeStateInferencer,
    private val policies: Set<@JvmSuppressWildcards AiPolicy>,
    private val actuators: Set<@JvmSuppressWildcards AiActuator>,
) {
    suspend fun tick() {
        val previousState = stateRepo.get()
        val since = previousState?.updatedAt ?: Instant.EPOCH
        val events = eventRepo.getEvents(since)
        if (events.isEmpty()) return

        val newState = inferencer.infer(previousState, events)
        stateRepo.save(newState)

        val decisions = policies.flatMap { it.evaluate(newState) }
        decisions.forEach { decision ->
            actuators.forEach { it.apply(decision) }
        }
    }
}
