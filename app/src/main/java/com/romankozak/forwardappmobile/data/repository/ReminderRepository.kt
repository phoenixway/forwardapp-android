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

    suspend fun createReminder(
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

    suspend fun removeReminder(reminder: Reminder) {
        reminderDao.delete(reminder)
        alarmScheduler.cancel(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.update(reminder)
        repositoryScope.launch { alarmScheduler.schedule(reminder) }
    }

    suspend fun clearRemindersForEntity(entityId: String) {
        val reminders = reminderDao.getRemindersForEntity(entityId).first()
        reminders.forEach { reminder ->
            alarmScheduler.cancel(reminder)
        }
        reminderDao.deleteByEntityId(entityId)
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

    fun getRemindersForEntityFlow(entityId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForEntity(entityId)
    }
}