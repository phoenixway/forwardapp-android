package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
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
        val now = System.currentTimeMillis()
        val logEntry = 
            ProjectExecutionLog(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                timestamp = now,
                type = type,
                description = description,
                details = details,
                updatedAt = now,
                syncedAt = null,
                version = 1,
            )
        projectManagementDao.insertLog(logEntry)
    }

    suspend fun updateProjectExecutionLog(log: ProjectExecutionLog) {
        projectManagementDao.updateLog(log.bumpSync())
    }

    suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        projectManagementDao.insertLog(log.softDelete())
    }
}
