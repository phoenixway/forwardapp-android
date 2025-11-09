package com.romankozak.forwardappmobile.shared.features.projects.logs.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.ProjectExecutionLogRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectExecutionLogRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ProjectExecutionLogRepository {

    private val queries = db.projectExecutionLogsQueries

    override fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> {
        return queries
            .selectAllByProjectId(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addProjectLogEntry(log: ProjectExecutionLog) {
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

    override suspend fun updateProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            queries.update(
                description = log.description,
                details = log.details,
                id = log.id
            )
        }
    }

    override suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            queries.deleteById(log.id)
        }
    }
}