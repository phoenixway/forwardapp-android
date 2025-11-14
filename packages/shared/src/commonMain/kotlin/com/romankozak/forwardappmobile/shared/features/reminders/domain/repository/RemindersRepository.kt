package com.romankozak.forwardappmobile.shared.features.reminders.domain.repository

import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface RemindersRepository {
    fun observeReminders(): Flow<List<Reminder>>

    fun observeRemindersForEntity(entityId: String): Flow<List<Reminder>>

    suspend fun getReminderById(id: String): Reminder?

    suspend fun upsertReminder(reminder: Reminder)

    suspend fun deleteReminder(id: String)

    suspend fun deleteRemindersByEntity(entityId: String)

    suspend fun clearReminders()
}
