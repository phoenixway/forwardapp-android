package com.romankozak.forwardappmobile.shared.features.projects.data.logs

import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import kotlinx.coroutines.flow.Flow

interface ProjectLogRepository {
    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>>

    suspend fun addToggleProjectManagementLog(projectId: String, isEnabled: Boolean)

    suspend fun addUpdateProjectStatusLog(projectId: String, newStatus: String, statusText: String?)

    suspend fun addProjectComment(projectId: String, comment: String)

    suspend fun addProjectLogEntry(
        projectId: String,
        type: String,
        description: String,
        details: String? = null,
    )

    suspend fun updateProjectExecutionLog(log: ProjectExecutionLog)

    suspend fun deleteProjectExecutionLog(log: ProjectExecutionLog)
}
