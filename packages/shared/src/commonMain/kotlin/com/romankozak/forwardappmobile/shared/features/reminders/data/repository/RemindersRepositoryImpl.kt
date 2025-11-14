package com.romankozak.forwardappmobile.shared.features.reminders.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.reminders.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RemindersRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : RemindersRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        database.remindersQueries.getAllReminders()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRemindersForEntity(entityId: String): Flow<List<Reminder>> =
        database.remindersQueries.getRemindersForEntity(entityId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getReminderById(id: String): Reminder? = withContext(dispatcher) {
        database.remindersQueries.getReminderById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertReminder(reminder: Reminder) = withContext(dispatcher) {
        database.remindersQueries.insertReminder(
            id = reminder.id,
            entityId = reminder.entityId,
            entityType = reminder.entityType,
            reminderTime = reminder.reminderTime,
            status = reminder.status,
            creationTime = reminder.creationTime,
            snoozeUntil = reminder.snoozeUntil,
        )
    }

    override suspend fun deleteReminder(id: String) = withContext(dispatcher) {
        database.remindersQueries.deleteReminder(id)
    }

    override suspend fun deleteRemindersByEntity(entityId: String) = withContext(dispatcher) {
        database.remindersQueries.deleteRemindersByEntity(entityId)
    }

    override suspend fun clearReminders() = withContext(dispatcher) {
        database.remindersQueries.clearReminders()
    }
}
