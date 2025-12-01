package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
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
    private val dayManagementRepository: DayManagementRepository,
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
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            entityId = entityId,
            entityType = entityType,
            reminderTime = reminderTime,
            status = "SCHEDULED",
            creationTime = now,
            updatedAt = now,
            syncedAt = null,
            version = 1
        )
        reminderDao.insert(reminder)
        alarmScheduler.schedule(reminder)
    }

    suspend fun removeReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        reminderDao.insert(reminder.softDelete(now))
        alarmScheduler.cancel(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        val bumped = reminder.bumpSync(now)
        reminderDao.update(bumped)
        alarmScheduler.schedule(reminder)
    }

    suspend fun clearRemindersForEntity(entityId: String) {
        val reminders = reminderDao.getRemindersForEntity(entityId).first()
        reminders.forEach { reminder ->
            alarmScheduler.cancel(reminder)
        }
        val now = System.currentTimeMillis()
        reminders.forEach { reminder ->
            reminderDao.insert(reminder.softDelete(now))
        }
    }

    suspend fun snoozeReminder(reminderId: String) {
            val reminder = reminderDao.getReminderById(reminderId)
            if (reminder != null) {
                val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutes
                val snoozedReminder = reminder.copy(
                    status = "SNOOZED",
                    snoozeUntil = snoozeTime,
                    reminderTime = snoozeTime,
                    updatedAt = snoozeTime,
                    syncedAt = null,
                    version = reminder.version + 1
                )
                reminderDao.update(snoozedReminder)
                alarmScheduler.schedule(snoozedReminder)
            }
        }

    suspend fun dismissReminder(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            val now = System.currentTimeMillis()
            val dismissedReminder = reminder.copy(
                status = "DISMISSED",
                reminderTime = now,
                updatedAt = now,
                syncedAt = null,
                version = reminder.version + 1
            )
            reminderDao.update(dismissedReminder)
            alarmScheduler.cancel(reminder)
        }
    }

    suspend fun markAsCompleted(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            val isRecurringTaskReminder =
                reminder.entityType == "TASK" &&
                    dayManagementRepository.getTaskById(reminder.entityId)?.recurringTaskId != null

            if (isRecurringTaskReminder) {
                val completedReminder = reminder.copy(
                    status = "COMPLETED",
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = reminder.version + 1
                )
                reminderDao.update(completedReminder)
                alarmScheduler.cancel(reminder)
            } else {
                val now = System.currentTimeMillis()
                reminderDao.insert(reminder.softDelete(now))
                alarmScheduler.cancel(reminder)
            }
        }
    }

    fun getRemindersForEntityFlow(entityId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForEntity(entityId)
    }

    suspend fun clearAllReminders() {
        val allReminders = reminderDao.getAllReminders().first()
        allReminders.forEach { reminder ->
            alarmScheduler.cancel(reminder)
        }
        val now = System.currentTimeMillis()
        allReminders.forEach { reminder ->
            reminderDao.insert(reminder.softDelete(now))
        }
    }
}
