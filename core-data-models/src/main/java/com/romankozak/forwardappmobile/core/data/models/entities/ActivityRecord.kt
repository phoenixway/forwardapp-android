package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "activity_records")
data class ActivityRecord(
    @PrimaryKey
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("text") val text: String,
    @SerializedName("rawNoteText") val rawNoteText: String? = null,
    @SerializedName("noteText") val noteText: String? = null,
    @SerializedName("stateEventType") val stateEventType: String? = null,
    @SerializedName("stateEventCrisisLevel") val stateEventCrisisLevel: Int? = null,
    @SerializedName("stateEventLabel") val stateEventLabel: String? = null,
    @SerializedName("stateEventApplied") val stateEventApplied: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("startTime") val startTime: Long? = null,
    @SerializedName("endTime") val endTime: Long? = null,
    @SerializedName("reminderTime") val reminderTime: Long? = null,
    @ColumnInfo(name = "target_id", index = true)
    @SerializedName("targetId") val targetId: String? = null,
    @ColumnInfo(name = "target_type")
    @SerializedName("targetType") val targetType: String? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "goal_id", index = true)
    @SerializedName("goalId") val goalId: String? = null,
    @SerializedName(value = "contextId", alternate = ["listId", "projectId"])
    @ColumnInfo(name = "context_id", index = true)
    val contextId: String? = null,
    @ColumnInfo(name = "xp_gained")
    @SerializedName("xpGained") val xpGained: Int? = null,
    @ColumnInfo(name = "anty_xp")
    @SerializedName("antyXp") val antyXp: Int? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0,
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
    @SerializedName("text") val text: String,
)
