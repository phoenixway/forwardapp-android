package com.romankozak.forwardappmobile.shared.features.projects.logs.data

import com.romankozak.forwardappmobile.shared.database.Project_execution_logs
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog

// File: ProjectExecutionLogMapper.kt
fun Project_execution_logs.toDomain(): ProjectExecutionLog = ProjectExecutionLog(
    id = id,
    projectId = projectId,
    timestamp = timestamp,
    type = type,
    description = description,
    details = details
)
