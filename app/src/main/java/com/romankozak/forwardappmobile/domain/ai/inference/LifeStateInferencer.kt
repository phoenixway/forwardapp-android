package com.romankozak.forwardappmobile.domain.ai.inference

import com.romankozak.forwardappmobile.domain.ai.events.AiEvent
import com.romankozak.forwardappmobile.domain.ai.events.ActivityLoggedEvent
import com.romankozak.forwardappmobile.domain.ai.events.IdleDetectedEvent
import com.romankozak.forwardappmobile.domain.ai.state.EntropyLevel
import com.romankozak.forwardappmobile.domain.ai.state.ExecutionMode
import com.romankozak.forwardappmobile.domain.ai.state.LifeSystemState
import com.romankozak.forwardappmobile.domain.ai.state.LoadLevel
import com.romankozak.forwardappmobile.domain.ai.state.StabilityLevel
import java.time.Instant
import kotlin.math.max

import javax.inject.Inject

interface LifeStateInferencer {
    suspend fun infer(previous: LifeSystemState?, events: List<AiEvent>): LifeSystemState
}

class DeterministicLifeStateInferencer @Inject constructor() : LifeStateInferencer {
    override suspend fun infer(previous: LifeSystemState?, events: List<AiEvent>): LifeSystemState {
        val now = Instant.now()
        val windowed = RollingWindows(events)

        val xp1h = windowed.sumXp(minutes = 60)
        val anti1h = windowed.sumAntiXp(minutes = 60)
        val idle1h = windowed.idleMinutes(minutes = 60)

        val loadLevel = when {
            anti1h > xp1h * 2 || idle1h > 40 -> LoadLevel.CRITICAL
            anti1h > xp1h || idle1h > 30 -> LoadLevel.HIGH
            xp1h > 0 -> LoadLevel.NORMAL
            else -> LoadLevel.LOW
        }.withHysteresis(previous?.loadLevel)

        val executionMode = when {
            idle1h > 45 -> ExecutionMode.STUCK
            xp1h > 0 && idle1h < 20 -> ExecutionMode.FOCUSED
            else -> ExecutionMode.SCATTERED
        }.withHysteresis(previous?.executionMode)

        val stability = when {
            loadLevel == LoadLevel.CRITICAL -> StabilityLevel.FRAGMENTED
            loadLevel == LoadLevel.HIGH || executionMode == ExecutionMode.SCATTERED -> StabilityLevel.UNSTABLE
            else -> StabilityLevel.STABLE
        }.withHysteresis(previous?.stability)

        val entropy = when {
            windowed.idleMinutes(minutes = 360) > 120 -> EntropyLevel.HIGH
            windowed.sumEvents(minutes = 360) > 50 -> EntropyLevel.MEDIUM
            else -> EntropyLevel.LOW
        }.withHysteresis(previous?.entropy)

        return LifeSystemState(
            loadLevel = loadLevel,
            executionMode = executionMode,
            stability = stability,
            entropy = entropy,
            updatedAt = now,
        )
    }
}

private class RollingWindows(events: List<AiEvent>) {
    private val now = Instant.now()
    private val events = events

    fun sumXp(minutes: Int): Int =
        events.filterIsInstance<ActivityLoggedEvent>()
            .filter { it.timestamp.isAfter(now.minusSeconds(minutes * 60L)) }
            .sumOf { it.xp }

    fun sumAntiXp(minutes: Int): Int =
        events.filterIsInstance<ActivityLoggedEvent>()
            .filter { it.timestamp.isAfter(now.minusSeconds(minutes * 60L)) }
            .sumOf { it.antiXp }

    fun idleMinutes(minutes: Int): Int =
        events.filterIsInstance<IdleDetectedEvent>()
            .filter { it.timestamp.isAfter(now.minusSeconds(minutes * 60L)) }
            .sumOf { it.idleMinutes }

    fun sumEvents(minutes: Int): Int =
        events.count { it.timestamp.isAfter(now.minusSeconds(minutes * 60L)) }
}

private fun LoadLevel.withHysteresis(previous: LoadLevel?): LoadLevel {
    if (previous == null) return this
    return when {
        this.ordinal - previous.ordinal >= 2 -> LoadLevel.values()[previous.ordinal + 1]
        previous.ordinal - this.ordinal >= 2 -> LoadLevel.values()[previous.ordinal - 1]
        else -> this
    }
}

private fun ExecutionMode.withHysteresis(previous: ExecutionMode?): ExecutionMode {
    if (previous == null) return this
    return if (this == ExecutionMode.STUCK && previous == ExecutionMode.FOCUSED) ExecutionMode.SCATTERED else this
}

private fun StabilityLevel.withHysteresis(previous: StabilityLevel?): StabilityLevel {
    if (previous == null) return this
    return if (previous == StabilityLevel.FRAGMENTED && this == StabilityLevel.STABLE) StabilityLevel.UNSTABLE else this
}

private fun EntropyLevel.withHysteresis(previous: EntropyLevel?): EntropyLevel {
    if (previous == null) return this
    return if (previous == EntropyLevel.HIGH && this == EntropyLevel.LOW) EntropyLevel.MEDIUM else this
}
