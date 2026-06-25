package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "arc_quests",
    indices = [
        Index(value = ["arc_key", "quest_order"]),
        Index(value = ["linked_context_id"]),
        Index(value = ["linked_mission_id"]),
    ],
)
data class ArcQuestEntity(
    @PrimaryKey
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "arc_key")
    @SerializedName("arcKey")
    val arcKey: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @ColumnInfo(name = "linked_context_id")
    @SerializedName("linkedContextId")
    val linkedContextId: String? = null,
    @ColumnInfo(name = "linked_mission_id")
    @SerializedName("linkedMissionId")
    val linkedMissionId: Long? = null,
    @ColumnInfo(name = "source_type")
    @SerializedName("sourceType")
    val sourceType: String = ArcQuestSourceType.MANUAL.name,
    @ColumnInfo(name = "source_id")
    @SerializedName("sourceId")
    val sourceId: String? = null,
    @SerializedName("status")
    val status: String = ArcQuestStatus.ACTIVE.name,
    @ColumnInfo(name = "quest_order")
    @SerializedName("order")
    val order: Long = 0L,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at")
    @SerializedName("syncedAt")
    val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    @SerializedName("isDeleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0")
    @SerializedName("version")
    val version: Long = 0L,
)

enum class ArcQuestSourceType {
    MANUAL,
    CONTEXT,
    MISSION,
    BEACON,
    BEACON_GROUP,
}

enum class ArcQuestStatus {
    ACTIVE,
    PAUSED,
    DONE,
}
