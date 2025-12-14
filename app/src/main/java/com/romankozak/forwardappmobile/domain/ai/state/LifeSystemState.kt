package com.romankozak.forwardappmobile.domain.ai.state

import java.time.Instant

data class LifeSystemState(
    val loadLevel: LoadLevel,
    val executionMode: ExecutionMode,
    val stability: StabilityLevel,
    val entropy: EntropyLevel,
    val updatedAt: Instant,
)

enum class LoadLevel { LOW, NORMAL, HIGH, CRITICAL }
enum class ExecutionMode { FOCUSED, SCATTERED, STUCK }
enum class StabilityLevel { STABLE, UNSTABLE, FRAGMENTED }
enum class EntropyLevel { LOW, MEDIUM, HIGH }
