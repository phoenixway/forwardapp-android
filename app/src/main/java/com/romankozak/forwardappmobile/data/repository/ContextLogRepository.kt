package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextLogRepository @Inject constructor(
    private val contextManagementDao: ContextManagementDao
) {
    fun getProjectLogsStream(projectId: String): Flow<List<ContextLog>> =
        contextManagementDao.getLogsForProjectStream(projectId)

    suspend fun addToggleProjectManagementLog(projectId: String, isEnabled: Boolean) {
        val status = if (isEnabled) "активовано" else "деактивовано"
        addProjectLogEntry(
            projectId = projectId,
            type = ContextLogEntryTypeValues.AUTOMATIC,
            description = "Управління проектом було $status.",
        )
    }

    suspend fun addUpdateProjectStatusLog(projectId: String, newStatus: String, statusText: String?) {
        val logDescription =
            "Статус змінено на '$newStatus'." +
                (statusText?.let { "\nКоментар: $it" } ?: "")
        addProjectLogEntry(
            projectId = projectId,
            type = ContextLogEntryTypeValues.STATUS_CHANGE,
            description = logDescription,
        )
    }

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    ) {
        addProjectLogEntry(
            projectId = projectId,
            type = ContextLogEntryTypeValues.COMMENT,
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
            ContextLog(
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
        contextManagementDao.insertLog(logEntry)
    }

    suspend fun updateProjectExecutionLog(log: ContextLog) {
        contextManagementDao.updateLog(log.bumpSync())
    }

    suspend fun deleteProjectExecutionLog(log: ContextLog) {
        contextManagementDao.insertLog(log.softDelete())
    }
}
