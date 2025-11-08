package com.romankozak.forwardappmobile.shared.features.reminders.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ReminderQueriesQueries
import com.romankozak.forwardappmobile.shared.features.reminders.data.mappers.toKmpReminder
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class ReminderRepository(
    private val database: ForwardAppDatabase,
    private val alarmScheduler: AlarmScheduler,
    private val ioDispatcher: CoroutineDispatcher,
) {

    fun getAllReminders(): Flow<List<Reminder>> {
        return database.reminderQueriesQueries.getAllReminders()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toKmpReminder() } }
    }

    suspend fun createReminder(
        entityId: String,
        entityType: String,
        reminderTime: Long
    ) = withContext(ioDispatcher) {
        val reminder = Reminder(
            id = uuid4(),
            entityId = entityId,
            entityType = entityType,
            reminderTime = reminderTime,
            status = "SCHEDULED",
            creationTime = Clock.System.now().toEpochMilliseconds(),
            snoozeUntil = null
        )
        database.reminderQueriesQueries.insert(
            id = reminder.id,
            entityId = reminder.entityId,
            entityType = reminder.entityType,
            reminderTime = reminder.reminderTime,
            status = reminder.status,
            creationTime = reminder.creationTime,
            snoozeUntil = reminder.snoozeUntil
        )
        alarmScheduler.schedule(reminder)
    }

    suspend fun removeReminder(reminder: Reminder) = withContext(ioDispatcher) {
        database.reminderQueriesQueries.delete(reminder.id)
        alarmScheduler.cancel(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) = withContext(ioDispatcher) {
        database.reminderQueriesQueries.update(
            id = reminder.id,
            entityId = reminder.entityId,
            entityType = reminder.entityType,
            reminderTime = reminder.reminderTime,
            status = reminder.status,
            creationTime = reminder.creationTime,
            snoozeUntil = reminder.snoozeUntil
        )
        alarmScheduler.schedule(reminder)
    }

    suspend fun clearRemindersForEntity(entityId: String) = withContext(ioDispatcher) {
        val reminders = database.reminderQueriesQueries.getRemindersForEntity(entityId).executeAsList().map { it.toKmpReminder() }
        reminders.forEach { reminder ->
            alarmScheduler.cancel(reminder)
        }
        database.reminderQueriesQueries.deleteByEntityId(entityId)
    }

    suspend fun snoozeReminder(reminderId: String) = withContext(ioDispatcher) {
        val reminder = database.reminderQueriesQueries.getReminderById(reminderId).executeAsList().firstOrNull()?.toKmpReminder()
        if (reminder != null) {
            val snoozeTime = Clock.System.now().toEpochMilliseconds() + 15 * 60 * 1000 // 15 minutes
            val snoozedReminder = reminder.copy(status = "SNOOZED", snoozeUntil = snoozeTime, reminderTime = snoozeTime)
            updateReminder(snoozedReminder)
        }
    }

    suspend fun dismissReminder(reminderId: String) = withContext(ioDispatcher) {
        val reminder = database.reminderQueriesQueries.getReminderById(reminderId).executeAsList().firstOrNull()?.toKmpReminder()
        if (reminder != null) {
            val dismissedReminder = reminder.copy(status = "DISMISSED", reminderTime = Clock.System.now().toEpochMilliseconds())
            updateReminder(dismissedReminder)
            alarmScheduler.cancel(reminder)
        }
    }

    suspend fun markAsCompleted(reminderId: String) = withContext(ioDispatcher) {
        val reminder = database.reminderQueriesQueries.getReminderById(reminderId).executeAsList().firstOrNull()?.toKmpReminder()
        if (reminder != null) {
            val completedReminder = reminder.copy(status = "COMPLETED")
            updateReminder(completedReminder)
            alarmScheduler.cancel(reminder)
        }
    }

    fun getRemindersForEntityFlow(entityId: String): Flow<List<Reminder>> {
        return database.reminderQueriesQueries.getRemindersForEntity(entityId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toKmpReminder() } }
    }

    suspend fun clearAllReminders() = withContext(ioDispatcher) {
        val allReminders = database.reminderQueriesQueries.getAllReminders().executeAsList().map { it.toKmpReminder() }
        allReminders.forEach { reminder ->
            alarmScheduler.cancel(reminder)
        }
        database.reminderQueriesQueries.deleteAll()
    }
}

// A simple UUID generator. In a real KMP project, you'd use a library.
expect fun uuid4(): String
