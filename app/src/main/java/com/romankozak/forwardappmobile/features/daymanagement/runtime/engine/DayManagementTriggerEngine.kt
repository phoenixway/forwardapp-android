package com.romankozak.forwardappmobile.features.daymanagement.runtime.engine

import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import javax.inject.Inject
import javax.inject.Singleton

data class DayManagementRuntimeTrigger(
    val id: String,
    val triggerAt: Long,
    val isOverdue: Boolean,
    val title: String,
    val message: String,
)

@Singleton
class DayManagementRuntimeTriggerEngine
    @Inject
    constructor() {
        fun noPlanAfterWakeTrigger(
            state: DayManagementRuntimeState,
            now: Long,
        ): DayManagementRuntimeTrigger? {
            val wokeAt = state.wokeAt ?: return null
            if (!state.hasOpenOperationalDay) return null
            if (state.dayPlanFinalizedAt != null) return null

            val deadline = wokeAt + NO_PLAN_AFTER_WAKE_MILLIS
            return DayManagementRuntimeTrigger(
                id = DayManagementRuntime.WakePlanAlarmId,
                triggerAt = deadline,
                isOverdue = now >= deadline,
                title = "План дня не завершено",
                message = "Минуло більше 2 годин після пробудження, а план дня ще не зафіксований.",
            )
        }

        companion object {
            const val NO_PLAN_AFTER_WAKE_MILLIS = 2 * 60 * 60 * 1000L
        }
    }
