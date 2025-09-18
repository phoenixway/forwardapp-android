package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_records")
data class ActivityRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val reminderTime: Long? = null,
    @ColumnInfo(name = "target_id", index = true)
    val targetId: String? = null,
    @ColumnInfo(name = "target_type")
    val targetType: String? = null,
    @ColumnInfo(name = "goal_id", index = true)
    val goalId: String? = null,
    @ColumnInfo(name = "project_id", index = true)
    val projectId: String? = null,
) {
    val isTimeless: Boolean
        get() = startTime == null && endTime == null

    val isOngoing: Boolean
        get() = startTime != null && endTime == null

    val durationInMillis: Long?
        get() = if (startTime != null && endTime != null) endTime - startTime else null
}