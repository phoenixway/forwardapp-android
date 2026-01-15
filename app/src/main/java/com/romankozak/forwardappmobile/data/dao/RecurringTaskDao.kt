package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.RecurringTask

@Dao
interface RecurringTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTask: RecurringTask)

    @Update
    suspend fun update(recurringTask: RecurringTask)

    @Query("SELECT * FROM recurring_tasks WHERE id = :id")
    suspend fun getById(id: String): RecurringTask?

    @Query("SELECT * FROM recurring_tasks ORDER BY startDate DESC")
    suspend fun getAll(): List<RecurringTask>

    @Query("DELETE FROM recurring_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    // --- Backup Methods ---
    @Query("DELETE FROM recurring_tasks")
    suspend fun deleteAll()
}
