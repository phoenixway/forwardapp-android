package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ReminderHandler
    @Inject
    constructor(
        private val alarmScheduler: AlarmScheduler,
        private val reminderRepository: ReminderRepository,
        private val scope: CoroutineScope,
    ) {
        @Suppress("UnusedParameter")
        fun onSetReminderForProject(
            projectId: String,
            projectName: String,
            time: Long,
        ) {
            scope.launch {
                val reminder =
                    Reminder(
                        id = UUID.randomUUID().toString(),
                        entityId = projectId,
                        entityType = "CONTEXT",
                        reminderTime = time,
                        status = "SCHEDULED",
                        creationTime = System.currentTimeMillis(), // Added creationTime
                    )
                reminderRepository.createReminder(reminder.entityId, reminder.entityType, reminder.reminderTime)
                alarmScheduler.schedule(reminder)
            }
        }

        fun onClearReminder(projectId: String) {
            scope.launch {
                val reminders = reminderRepository.getRemindersForEntityFlow(projectId).first() // Changed to .first()
                reminders.forEach {
                    alarmScheduler.cancel(it)
                }
                reminderRepository.clearRemindersForEntity(projectId)
            }
        }
    }
