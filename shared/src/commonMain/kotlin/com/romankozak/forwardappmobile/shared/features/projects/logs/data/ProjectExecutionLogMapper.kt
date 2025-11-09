package com.romankozak.forwardappmobile.shared.features.projects.logs.data

import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogs
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog

fun ProjectExecutionLogs.toDomain(): ProjectExecutionLog {
    return ProjectExecutionLog(
        id = id,
        projectId = projectId,
        timestamp = timestamp,
        type = type,
        description = description,
        details = details
    )
}