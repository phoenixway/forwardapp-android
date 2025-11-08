package com.romankozak.forwardappmobile.shared.features.projects.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueries
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.mappers.toSharedModel
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectLogRepository(
    private val projectExecutionLogQueries: ProjectExecutionLogQueries,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectExecutionLogQueries.selectByProjectId(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { logs -> logs.map { it.toSharedModel() } }

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
                id = uuid4().toString(),
                projectId = projectId,
                timestamp = System.currentTimeMillis(),
                type = type,
                description = description,
                details = details,
            )
        withContext(ioDispatcher) {
            projectExecutionLogQueries.insert(
                id = logEntry.id,
                projectId = logEntry.projectId,
                timestamp = logEntry.timestamp,
                type = logEntry.type,
                description = logEntry.description,
                details = logEntry.details
            )
        }
    }

    suspend fun updateProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            projectExecutionLogQueries.insert(
                id = log.id,
                projectId = log.projectId,
                timestamp = log.timestamp,
                type = log.type,
                description = log.description,
                details = log.details
            )
        }
    }

    suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            projectExecutionLogQueries.deleteById(log.id)
        }
    }
}