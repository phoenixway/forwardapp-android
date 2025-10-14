package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())


    fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders()
    }

    suspend fun createOrUpdateReminder(
        entityId: String,
        entityType: String,
        reminderTime: Long
    ) {
        val reminder = Reminder(
            entityId = entityId,
            entityType = entityType,
            reminderTime = reminderTime,
            status = "SCHEDULED",
            creationTime = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        repositoryScope.launch { alarmScheduler.schedule(reminder) }
    }

    suspend fun clearReminderForEntity(entityId: String) {
        try {
            val reminder = reminderDao.getReminderForEntity(entityId).first()
            if (reminder != null) {
                reminderDao.delete(reminder)
                alarmScheduler.cancel(reminder)
            }
        } catch (e: NoSuchElementException) {
            // No reminder found, do nothing
        }
    }

    suspend fun snoozeReminder(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutes
            val snoozedReminder = reminder.copy(status = "SNOOZED", snoozeUntil = snoozeTime)
            reminderDao.update(snoozedReminder)
            repositoryScope.launch { alarmScheduler.schedule(snoozedReminder) }
        }
    }

    suspend fun dismissReminder(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            val dismissedReminder = reminder.copy(status = "DISMISSED")
            reminderDao.update(dismissedReminder)
            alarmScheduler.cancel(reminder)
        }
    }

    suspend fun markAsCompleted(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            val completedReminder = reminder.copy(status = "COMPLETED")
            reminderDao.update(completedReminder)
            alarmScheduler.cancel(reminder)
        }
    }

    fun getReminderForEntityFlow(entityId: String): Flow<Reminder?> {
        return reminderDao.getReminderForEntity(entityId)
    }
}
