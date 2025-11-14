package com.romankozak.forwardappmobile.shared.features.projects.logs.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectExecutionLogsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ProjectExecutionLogsRepository {

    override fun observeLogs(projectId: String): Flow<List<ProjectExecutionLog>> =
        database.projectExecutionLogsQueries.selectAllByProjectId(projectId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertLog(log: ProjectExecutionLog) = withContext(dispatcher) {
        database.projectExecutionLogsQueries.insertProjectExecutionLog(
            id = log.id,
            projectId = log.projectId,
            timestamp = log.timestamp,
            type = log.type,
            description = log.description,
            details = log.details,
        )
    }

    override suspend fun deleteLog(logId: String) = withContext(dispatcher) {
        database.projectExecutionLogsQueries.deleteProjectExecutionLog(logId)
    }

    override suspend fun updateLogDetails(
        logId: String,
        description: String,
        details: String?,
    ) = withContext(dispatcher) {
        database.projectExecutionLogsQueries.updateProjectExecutionLog(
            id = logId,
            description = description,
            details = details,
        )
    }
}
