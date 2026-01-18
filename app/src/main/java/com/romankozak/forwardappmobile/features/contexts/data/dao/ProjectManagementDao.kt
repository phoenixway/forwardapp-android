package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectManagementDao {
    @Query("SELECT * FROM project_execution_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProjectStream(projectId: String): Flow<List<ContextLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ContextLog)

    @Update
    suspend fun updateLog(log: ContextLog)

    @Delete
    suspend fun deleteLog(log: ContextLog)

    @Query("SELECT * FROM project_execution_logs")
    suspend fun getAllLogs(): List<ContextLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ContextLog>)

    @Query("DELETE FROM project_execution_logs")
    suspend fun deleteAllLogs()
}
