package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectLogEntryTypeValues
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectLogRepository @Inject constructor(
    private val projectManagementDao: ProjectManagementDao
) {
    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectManagementDao.getLogsForProjectStream(projectId)

    suspend fun addToggleProjectManagementLog(projectId: String, isEnabled: Boolean) {
        val status = if (isEnabled) "активовано" else "деактивовано"
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.AUTOMATIC,
            description = "Управління проектом було $status.",
        )
    }

    suspend fun addUpdateProjectStatusLog(projectId: String, newStatus: String, statusText: String?) {
        val logDescription =
            "Статус змінено на '$newStatus'." +
                (statusText?.let { "\nКоментар: $it" } ?: "")
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.STATUS_CHANGE,
            description = logDescription,
        )
    }

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    ) {
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.COMMENT,
            description = comment,
        )
    }

    suspend fun addProjectLogEntry(
        projectId: String,
        type: String,
        description: String,
        details: String? = null,
    ) {
        val logEntry = 
            ProjectExecutionLog(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                timestamp = System.currentTimeMillis(),
                type = type,
                description = description,
                details = details,
            )
        projectManagementDao.insertLog(logEntry)
    }

    suspend fun updateProjectExecutionLog(log: ProjectExecutionLog) {
        projectManagementDao.updateLog(log)
    }

    suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        projectManagementDao.deleteLog(log)
    }
}
