package com.romankozak.forwardappmobile.features.daymanagement.runtime.platform

import android.util.Log
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
                runCatching {
                    alarmScheduler.cancelNotification(WAKE_PLAN_REQUEST_CODE)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to cancel day runtime notification", error)
                }
                return
            }

            val triggerAt =
                if (trigger.isOverdue) {
                    now + OVERDUE_NOTIFICATION_DELAY_MILLIS
                } else {
                    trigger.triggerAt
                }

            runCatching {
                alarmScheduler.scheduleNotification(
                    requestCode = WAKE_PLAN_REQUEST_CODE,
                    triggerTime = triggerAt,
                    title = trigger.title,
                    message = trigger.message,
                    extraInfo = "Фаза: ${state.currentPhase.name}",
                )
            }.onFailure { error ->
                Log.w(TAG, "Failed to schedule day runtime notification", error)
            }
        }

        companion object {
            private const val TAG = "DayRuntimeNotifier"
            private const val WAKE_PLAN_REQUEST_CODE = 710001
            private const val OVERDUE_NOTIFICATION_DELAY_MILLIS = 5_000L
        }
    }
