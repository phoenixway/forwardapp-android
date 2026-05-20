package com.romankozak.forwardappmobile.features.daymanagement.runtime.engine

import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementPhase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeCommand
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeDecision
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeEvent
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeEventType
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayManagementRuntime
    @Inject
    constructor() {
        fun handle(
            state: DayManagementRuntimeState,
            command: DayManagementRuntimeCommand,
        ): DayManagementRuntimeDecision =
            when (command) {
                is DayManagementRuntimeCommand.WakeUp -> wakeUp(command.now)
                is DayManagementRuntimeCommand.FinalizeFocus -> finalizeFocus(state, command.now)
                is DayManagementRuntimeCommand.FinalizePlan -> finalizePlan(state, command.now)
                is DayManagementRuntimeCommand.ActivatePhase -> activatePhase(state, command.phase, command.now)
                is DayManagementRuntimeCommand.Sleep -> sleep(state, command.now)
            }

        private fun wakeUp(now: Long): DayManagementRuntimeDecision {
            val newState =
                DayManagementRuntimeState(
                    sessionId = UUID.randomUUID().toString(),
                    calendarAnchorDate = now,
                    wokeAt = now,
                    sleepAt = null,
                    currentPhase = DayManagementPhase.PREPARATION,
                    phaseStartedAt = now,
                    dayFocusFinalizedAt = null,
                    dayPlanFinalizedAt = null,
                    implementationStartedAt = null,
                    finalizationStartedAt = null,
                    activeAlarmIds = emptySet(),
                    riskFlags = emptySet(),
                    updatedAt = now,
                )
            return DayManagementRuntimeDecision(
                newState = newState,
                events =
                    listOf(
                        DayManagementRuntimeEvent(
                            type = DayManagementRuntimeEventType.WOKE_UP,
                            timestamp = now,
                        ),
                    ),
            )
        }

        private fun finalizeFocus(
            state: DayManagementRuntimeState,
            now: Long,
        ): DayManagementRuntimeDecision {
            val ensuredState = ensureOpenSession(state, now)
            val newState =
                ensuredState.copy(
                    dayFocusFinalizedAt = now,
                    updatedAt = now,
                )
            return DayManagementRuntimeDecision(
                newState = newState,
                events =
                    listOf(
                        DayManagementRuntimeEvent(
                            type = DayManagementRuntimeEventType.FOCUS_FINALIZED,
                            timestamp = now,
                        ),
                    ),
            )
        }

        private fun finalizePlan(
            state: DayManagementRuntimeState,
            now: Long,
        ): DayManagementRuntimeDecision {
            val ensuredState = ensureOpenSession(state, now)
            val newState =
                ensuredState.copy(
                    dayPlanFinalizedAt = now,
                    riskFlags = ensuredState.riskFlags - WakePlanAlarmId,
                    activeAlarmIds = ensuredState.activeAlarmIds - WakePlanAlarmId,
                    updatedAt = now,
                )
            return DayManagementRuntimeDecision(
                newState = newState,
                events =
                    listOf(
                        DayManagementRuntimeEvent(
                            type = DayManagementRuntimeEventType.PLAN_FINALIZED,
                            timestamp = now,
                        ),
                    ),
            )
        }

        private fun activatePhase(
            state: DayManagementRuntimeState,
            phase: DayManagementPhase,
            now: Long,
        ): DayManagementRuntimeDecision {
            val ensuredState = ensureOpenSession(state, now)
            val newState =
                ensuredState.copy(
                    currentPhase = phase,
                    phaseStartedAt = now,
                    implementationStartedAt =
                        when (phase) {
                            DayManagementPhase.IMPLEMENTATION -> now
                            else -> ensuredState.implementationStartedAt
                        },
                    finalizationStartedAt =
                        when (phase) {
                            DayManagementPhase.FINALIZATION -> now
                            else -> ensuredState.finalizationStartedAt
                        },
                    updatedAt = now,
                )
            return DayManagementRuntimeDecision(
                newState = newState,
                events =
                    listOf(
                        DayManagementRuntimeEvent(
                            type = DayManagementRuntimeEventType.PHASE_ACTIVATED,
                            timestamp = now,
                            payload = mapOf("phase" to phase.name),
                        ),
                    ),
            )
        }

        private fun sleep(
            state: DayManagementRuntimeState,
            now: Long,
        ): DayManagementRuntimeDecision {
            if (!state.hasOpenOperationalDay) {
                return DayManagementRuntimeDecision(
                    newState = state,
                    events = emptyList(),
                )
            }
            val newState =
                state.copy(
                    sleepAt = now,
                    currentPhase = DayManagementPhase.CLOSED,
                    phaseStartedAt = now,
                    activeAlarmIds = emptySet(),
                    riskFlags = emptySet(),
                    updatedAt = now,
                )
            return DayManagementRuntimeDecision(
                newState = newState,
                events =
                    listOf(
                        DayManagementRuntimeEvent(
                            type = DayManagementRuntimeEventType.WENT_TO_SLEEP,
                            timestamp = now,
                        ),
                    ),
            )
        }

        private fun ensureOpenSession(
            state: DayManagementRuntimeState,
            now: Long,
        ): DayManagementRuntimeState =
            if (state.hasOpenOperationalDay) {
                state
            } else {
                DayManagementRuntimeState(
                    sessionId = UUID.randomUUID().toString(),
                    calendarAnchorDate = now,
                    wokeAt = now,
                    currentPhase = DayManagementPhase.PREPARATION,
                    phaseStartedAt = now,
                    updatedAt = now,
                )
            }

        companion object {
            const val WakePlanAlarmId = "wake_plan_overdue"
        }
    }
