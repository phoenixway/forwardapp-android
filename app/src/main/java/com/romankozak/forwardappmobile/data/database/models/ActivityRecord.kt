package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    val updatedAt: Long? = null,
    @ColumnInfo(name = "goal_id", index = true)
    val goalId: String? = null,
    @SerializedName(value = "projectId", alternate = ["listId"])
    @ColumnInfo(name = "project_id", index = true)
    val projectId: String? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
) {
    val isTimeless: Boolean
        get() = startTime == null && endTime == null

    val isOngoing: Boolean
        get() = startTime != null && endTime == null

    val durationInMillis: Long?
        get() = if (startTime != null && endTime != null) endTime - startTime else null
}


@Fts4(contentEntity = ActivityRecord::class)
@Entity(tableName = "activity_records_fts")
data class ActivityRecordFts(
    val text: String,
)
