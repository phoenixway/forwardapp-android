package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.ReminderInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderInfoDao {
    @Query("SELECT * FROM reminder_info WHERE goalId = :goalId")
    suspend fun getReminderInfo(goalId: String): ReminderInfo?

    @Query("SELECT * FROM reminder_info WHERE goalId = :goalId")
    fun getReminderInfoFlow(goalId: String): Flow<ReminderInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reminderInfo: ReminderInfo)

    @Query("DELETE FROM reminder_info WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: String)

    @Query("DELETE FROM reminder_info WHERE reminder_status IN ('COMPLETED', 'DISMISSED')")
    suspend fun clearCompletedAndDismissed()

    @Query("SELECT * FROM reminder_info")
    fun getAllReminderInfoFlow(): Flow<List<ReminderInfo>>
}