package com.romankozak.forwardappmobile.shared.features.projects.logs.data.mappers

import com.romankozak.forwardappmobile.shared.database.Project_execution_logs
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog

fun Project_execution_logs.toSharedModel(): ProjectExecutionLog {
    return ProjectExecutionLog(
        id = this.id,
        projectId = this.projectId,
        timestamp = this.timestamp,
        type = this.type,
        description = this.description,
        details = this.details,
    )
}
