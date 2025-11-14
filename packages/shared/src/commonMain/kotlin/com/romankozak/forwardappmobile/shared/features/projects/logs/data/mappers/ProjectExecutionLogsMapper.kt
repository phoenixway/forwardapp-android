package com.romankozak.forwardappmobile.shared.features.projects.logs.data.mappers

import com.romankozak.forwardappmobile.shared.features.projects.logs.ProjectExecutionLogs
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog

fun ProjectExecutionLogs.toDomain(): ProjectExecutionLog =
    ProjectExecutionLog(
        id = id,
        projectId = projectId,
        timestamp = timestamp,
        type = type,
        description = description,
        details = details,
    )
