package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReminderHandler @Inject constructor(
    private val alarmScheduler: AlarmScheduler,
    private val scope: CoroutineScope
) {
    fun onSetReminderForProject(projectId: String, projectName: String, time: Long) {
        scope.launch {
            alarmScheduler.schedule(projectId, time, "Нагадування: $projectName", "Пора повернутися до проекту")
        }
    }

    fun onClearReminder(projectId: String) {
        scope.launch {
            alarmScheduler.cancel(projectId)
        }
    }
}
