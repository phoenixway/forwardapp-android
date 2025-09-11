package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectManagementDao {

    @Query("SELECT * FROM project_execution_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProjectStream(projectId: String): Flow<List<ProjectExecutionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProjectExecutionLog)

    @Query("SELECT * FROM project_execution_logs")
    suspend fun getAllLogs(): List<ProjectExecutionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ProjectExecutionLog>)

    @Query("DELETE FROM project_execution_logs")
    suspend fun deleteAllLogs()
}

