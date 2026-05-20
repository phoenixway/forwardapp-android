package com.romankozak.forwardappmobile.features.daymanagement.taskexecution.platform

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionReminderPolicy
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider

@Singleton
class TaskExecutionAlarmCoordinator
    @Inject
    constructor(
        private val alarmSchedulerProvider: Provider<AlarmScheduler>,
        private val reminderPolicy: TaskExecutionReminderPolicy,
    ) {
        fun sync(task: DayTask) {
            cancel(task.id)
            val alarmScheduler = alarmSchedulerProvider.get()
            reminderPolicy.build(task).forEach { spec ->
                alarmScheduler.scheduleNotification(
                    requestCode = spec.requestCode,
                    triggerTime = spec.triggerAt,
                    title = spec.title,
                    message = spec.message,
                    extraInfo = spec.extraInfo,
                )
            }
        }

        fun cancel(taskId: String) {
            val alarmScheduler = alarmSchedulerProvider.get()
            reminderPolicy.requestCodes(taskId).forEach(alarmScheduler::cancelNotification)
        }
    }
