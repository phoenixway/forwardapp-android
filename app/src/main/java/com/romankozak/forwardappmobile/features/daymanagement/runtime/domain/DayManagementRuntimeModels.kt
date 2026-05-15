package com.romankozak.forwardappmobile.features.daymanagement.runtime.domain

import java.util.UUID

enum class DayManagementPhase {
    PREPARATION,
    IMPLEMENTATION,
    FINALIZATION,
    CLOSED,
}

enum class DayManagementRuntimeEventType {
    WOKE_UP,
    PLAN_FINALIZED,
    PHASE_ACTIVATED,
    WENT_TO_SLEEP,
}

data class DayManagementRuntimeState(
    val sessionId: String? = null,
    val calendarAnchorDate: Long? = null,
    val wokeAt: Long? = null,
    val sleepAt: Long? = null,
    val currentPhase: DayManagementPhase = DayManagementPhase.CLOSED,
    val phaseStartedAt: Long? = null,
    val dayPlanFinalizedAt: Long? = null,
    val activeAlarmIds: Set<String> = emptySet(),
    val riskFlags: Set<String> = emptySet(),
    val updatedAt: Long? = null,
) {
    val hasOpenOperationalDay: Boolean
        get() = wokeAt != null && sleepAt == null
}

sealed interface DayManagementRuntimeCommand {
    data class WakeUp(val now: Long) : DayManagementRuntimeCommand

    data class FinalizePlan(val now: Long) : DayManagementRuntimeCommand

    data class ActivatePhase(
        val phase: DayManagementPhase,
        val now: Long,
    ) : DayManagementRuntimeCommand

    data class Sleep(val now: Long) : DayManagementRuntimeCommand
}

data class DayManagementRuntimeEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: DayManagementRuntimeEventType,
    val timestamp: Long,
    val payload: Map<String, String> = emptyMap(),
)

data class DayManagementRuntimeDecision(
    val newState: DayManagementRuntimeState,
    val events: List<DayManagementRuntimeEvent>,
)
