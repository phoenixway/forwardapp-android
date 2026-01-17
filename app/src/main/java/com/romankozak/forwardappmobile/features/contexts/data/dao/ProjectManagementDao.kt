package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectManagementDao {
    @Query("SELECT * FROM project_execution_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProjectStream(projectId: String): Flow<List<ProjectExecutionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProjectExecutionLog)

    @Update
    suspend fun updateLog(log: ProjectExecutionLog)

    @Delete
    suspend fun deleteLog(log: ProjectExecutionLog)

    @Query("SELECT * FROM project_execution_logs")
    suspend fun getAllLogs(): List<ProjectExecutionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ProjectExecutionLog>)

    @Query("DELETE FROM project_execution_logs")
    suspend fun deleteAllLogs()
}
