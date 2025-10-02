package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.ProjectReminderInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectReminderInfoDao {
    @Query("SELECT * FROM project_reminder_info")
    fun getAllProjectReminderInfoFlow(): Flow<List<ProjectReminderInfo>>

    @Query("SELECT * FROM project_reminder_info WHERE projectId = :projectId")
    suspend fun getProjectReminderInfo(projectId: String): ProjectReminderInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(projectReminderInfo: ProjectReminderInfo)

    @Query("DELETE FROM project_reminder_info WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String)
}
