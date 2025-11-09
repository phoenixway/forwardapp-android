package com.romankozak.forwardappmobile.shared.features.activity_records.data

import com.romankozak.forwardappmobile.shared.database.ActivityRecords
import com.romankozak.forwardappmobile.shared.data.database.models.ActivityRecord as DomainActivityRecord
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun ActivityRecords.toDomain(): DomainActivityRecord {
    return DomainActivityRecord(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        startTime = this.startTime,
        endTime = this.endTime,
        totalTimeSpentMinutes = this.totalTimeSpentMinutes,
        tags = this.tags?.let { Json.decodeFromString<List<String>>(it) },
        relatedLinks = this.relatedLinks,
        isCompleted = this.isCompleted,
        activityType = this.activityType,
        parentProjectId = this.parentProjectId
    )
}

fun DomainActivityRecord.toSqlDelight(): ActivityRecords {
    return ActivityRecords(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        startTime = this.startTime,
        endTime = this.endTime,
        totalTimeSpentMinutes = this.totalTimeSpentMinutes,
        tags = this.tags?.let { Json.encodeToString(it) },
        relatedLinks = this.relatedLinks,
        isCompleted = this.isCompleted,
        activityType = this.activityType,
        parentProjectId = this.parentProjectId
    )
}
