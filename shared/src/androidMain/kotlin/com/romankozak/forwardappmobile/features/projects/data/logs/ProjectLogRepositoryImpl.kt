package com.romankozak.forwardappmobile.shared.features.projects.data.logs

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueries
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.mappers.toSharedModel
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectLogRepositoryImpl(
    private val queries: ProjectExecutionLogQueries,
    private val ioDispatcher: CoroutineDispatcher
) : ProjectLogRepository {

    override fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        queries.selectByProjectId(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { logs -> logs.map { it.toSharedModel() } }

    override suspend fun addToggleProjectManagementLog(projectId: String, isEnabled: Boolean) {
        val status = if (isEnabled) "активовано" else "деактивовано"
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.AUTOMATIC,
            description = "Управління проєктом було $status."
        )
    }

    override suspend fun addUpdateProjectStatusLog(projectId: String, newStatus: String, statusText: String?) {
        val message = "Статус змінено на '$newStatus'." +
                (statusText?.let { "\nКоментар: $it" } ?: "")
        addProjectLogEntry(projectId, ProjectLogEntryTypeValues.STATUS_CHANGE, message)
    }

    override suspend fun addProjectComment(projectId: String, comment: String) {
        addProjectLogEntry(projectId, ProjectLogEntryTypeValues.COMMENT, comment)
    }

    override suspend fun addProjectLogEntry(projectId: String, type: String, description: String, details: String?) {
        withContext(ioDispatcher) {
            queries.insert(
                id = uuid4().toString(),
                projectId = projectId,
                timestamp = System.currentTimeMillis(),
                type = type,
                description = description,
                details = details
            )
        }
    }

    override suspend fun updateProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            queries.insert(
                id = log.id,
                projectId = log.projectId,
                timestamp = log.timestamp,
                type = log.type,
                description = log.description,
                details = log.details
            )
        }
    }

    override suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            queries.deleteById(log.id)
        }
    }
}
