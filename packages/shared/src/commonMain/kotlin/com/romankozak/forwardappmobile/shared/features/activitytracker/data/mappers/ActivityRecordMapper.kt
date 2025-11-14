package com.romankozak.forwardappmobile.shared.features.activitytracker.data.mappers

import com.romankozak.forwardappmobile.shared.features.activitytracker.ActivityRecords
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord

fun ActivityRecords.toDomain(): ActivityRecord =
    ActivityRecord(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        startTime = startTime,
        endTime = endTime,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        tags = tags,
        relatedLinks = relatedLinks,
        isCompleted = isCompleted,
        activityType = activityType,
        parentProjectId = parentProjectId,
    )
