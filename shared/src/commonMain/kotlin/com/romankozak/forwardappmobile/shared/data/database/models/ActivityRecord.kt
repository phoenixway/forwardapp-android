package com.romankozak.forwardappmobile.shared.data.database.models

data class ActivityRecord(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val startTime: Long?,
    val endTime: Long?,
    val totalTimeSpentMinutes: Long?,
    val tags: List<String>?,
    val relatedLinks: List<RelatedLink>?,
    val isCompleted: Boolean,
    val activityType: String,
    val parentProjectId: String?,
)
