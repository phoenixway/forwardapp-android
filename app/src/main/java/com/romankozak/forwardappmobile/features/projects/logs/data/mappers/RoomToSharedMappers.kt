package com.romankozak.forwardappmobile.features.projects.logs.data.mappers

import com.romankozak.forwardappmobile.core.database.models.ProjectExecutionLog as RoomProjectExecutionLog
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog as SharedProjectExecutionLog

fun RoomProjectExecutionLog.toSharedModel(): SharedProjectExecutionLog {
    return SharedProjectExecutionLog(
        id = this.id,
        projectId = this.projectId,
        timestamp = this.timestamp,
        type = this.type,
        description = this.description,
        details = this.details,
    )
}
