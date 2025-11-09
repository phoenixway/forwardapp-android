package com.romankozak.forwardappmobile.shared.features.activity_records.data

import com.romankozak.forwardappmobile.shared.database.ActivityRecords
import com.romankozak.forwardappmobile.shared.data.database.models.ActivityRecord as DomainActivityRecord

fun ActivityRecords.toDomain(): DomainActivityRecord {
    return DomainActivityRecord(
        id = this.id,
        name = this.text,
        description = null,
        createdAt = this.createdAt,
        startTime = this.startTime,
        endTime = this.endTime,
        totalTimeSpentMinutes = if (this.startTime != null && this.endTime != null) (this.endTime - this.startTime) / 60000 else null,
        tags = null,
        relatedLinks = null,
        isCompleted = this.endTime != null,
        activityType = this.targetType ?: "",
        parentProjectId = this.projectId,
    )
}

fun DomainActivityRecord.toSqlDelight(): ActivityRecords {
    return ActivityRecords(
        id = this.id,
        text = this.name,
        createdAt = this.createdAt,
        startTime = this.startTime,
        endTime = this.endTime,
        reminderTime = null,
        targetId = null,
        targetType = this.activityType,
        goalId = null,
        projectId = this.parentProjectId,
    )
}
