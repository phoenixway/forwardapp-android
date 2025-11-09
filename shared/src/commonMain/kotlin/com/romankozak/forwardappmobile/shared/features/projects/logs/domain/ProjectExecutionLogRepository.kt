package com.romankozak.forwardappmobile.shared.features.projects.logs.domain

import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow

interface ProjectExecutionLogRepository {
    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>>
    suspend fun addProjectLogEntry(log: ProjectExecutionLog)
    suspend fun updateProjectExecutionLog(log: ProjectExecutionLog)
    suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog)
}
