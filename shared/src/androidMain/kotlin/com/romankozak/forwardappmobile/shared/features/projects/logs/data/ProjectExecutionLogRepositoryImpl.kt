package com.romankozak.forwardappmobile.shared.features.projects.logs.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.ProjectExecutionLogRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectExecutionLogRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ProjectExecutionLogRepository {

    override fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> {
        return db.projectExecutionLogsQueries
            .selectAllByProjectId(projectId) // ✅ правильний метод з SQLDelight
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addProjectLogEntry(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            db.projectExecutionLogsQueries.insert(
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
            db.projectExecutionLogsQueries.update(
                description = log.description,
                details = log.details,
                id = log.id
            )
        }
    }

    override suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog) {
        withContext(ioDispatcher) {
            db.projectExecutionLogsQueries.deleteById(log.id)
        }
    }
}
