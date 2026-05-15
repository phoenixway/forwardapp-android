package com.romankozak.forwardappmobile.features.daymanagement.runtime.platform

import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.runtime.engine.DayManagementRuntimeTriggerEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayManagementRuntimeNotifier
    @Inject
    constructor(
        private val alarmScheduler: AlarmScheduler,
        private val triggerEngine: DayManagementRuntimeTriggerEngine,
    ) {
        fun sync(state: DayManagementRuntimeState) {
            val now = System.currentTimeMillis()
            val trigger = triggerEngine.noPlanAfterWakeTrigger(state, now)
            if (trigger == null) {
                alarmScheduler.cancelNotification(WAKE_PLAN_REQUEST_CODE)
                return
            }

            val triggerAt =
                if (trigger.isOverdue) {
                    now + OVERDUE_NOTIFICATION_DELAY_MILLIS
                } else {
                    trigger.triggerAt
                }

            alarmScheduler.scheduleNotification(
                requestCode = WAKE_PLAN_REQUEST_CODE,
                triggerTime = triggerAt,
                title = trigger.title,
                message = trigger.message,
                extraInfo = "Фаза: ${state.currentPhase.name}",
            )
        }

        companion object {
            private const val WAKE_PLAN_REQUEST_CODE = 710001
            private const val OVERDUE_NOTIFICATION_DELAY_MILLIS = 5_000L
        }
    }
