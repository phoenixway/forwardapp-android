package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders ORDER BY reminderTime DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE entityId = :entityId")
    fun getReminderForEntity(entityId: String): Flow<Reminder?>

    @Query("DELETE FROM reminders WHERE entityId = :entityId")
    suspend fun deleteByEntityId(entityId: String)
}