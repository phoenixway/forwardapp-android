package com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow

interface ProjectExecutionLogsRepository {
    fun observeLogs(projectId: String): Flow<List<ProjectExecutionLog>>

    suspend fun upsertLog(log: ProjectExecutionLog)

    suspend fun deleteLog(logId: String)

    suspend fun updateLogDetails(
        logId: String,
        description: String,
        details: String?,
    )
}
