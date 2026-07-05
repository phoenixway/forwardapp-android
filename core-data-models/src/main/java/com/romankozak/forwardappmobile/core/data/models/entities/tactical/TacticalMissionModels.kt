package com.romankozak.forwardappmobile.core.data.models.entities.tactical

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Converters

const val NO_DEADLINE = Long.MAX_VALUE
const val GENERAL_MISSION_STREAM_ID = "general"

@Entity(
    tableName = "tactical_missions",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["week_key"]),
        Index(value = ["mission_stream_id"]),
        Index(value = ["activity_slot_context_id"]),
        Index(value = ["source_backlog_item_id", "week_key"]),
    ],
)
@TypeConverters(Converters::class)
data class TacticalMission(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("a")
    val id: Long = 0,
    @SerializedName("b")
    val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("startTime") val startTime: Long? = null,
    @SerializedName("e")
    val deadline: Long,
    @SerializedName("f")
    val status: MissionStatus = MissionStatus.ACTIVE,
    @SerializedName("g")
    val priority: MissionPriority = MissionPriority.MEDIUM,
    @SerializedName(value = "projectId", alternate = ["contextId", "c"])
    val projectId: String?,
    @SerializedName(value = "linkedProjectIds", alternate = ["i", "j"])
    val linkedProjectIds: List<String>? = emptyList(),
    @SerializedName("linkedAttachmentIds") val linkedAttachmentIds: List<String>? = emptyList(),
    @ColumnInfo(name = "mission_order", defaultValue = "0")
    @SerializedName("order")
    val order: Long = 0L,
    @ColumnInfo(name = "week_key", defaultValue = "''")
    @SerializedName("weekKey")
    val weekKey: String = "",
    @ColumnInfo(name = "order_in_week", defaultValue = "0")
    @SerializedName("orderInWeek")
    val orderInWeek: Long = order,
    @ColumnInfo(name = "order_in_slot")
    @SerializedName("orderInSlot")
    val orderInSlot: Long? = null,
    @ColumnInfo(name = "mission_stream_id")
    @SerializedName("missionStreamId")
    val missionStreamId: String? = null,
    @ColumnInfo(name = "activity_slot_context_id")
    @SerializedName("activitySlotContextId")
    val activitySlotContextId: String? = null,
    @ColumnInfo(name = "source_type", defaultValue = "'MANUAL'")
    @SerializedName("sourceType")
    val sourceType: MissionSourceType = MissionSourceType.MANUAL,
    @ColumnInfo(name = "source_context_id")
    @SerializedName("sourceContextId")
    val sourceContextId: String? = null,
    @ColumnInfo(name = "source_backlog_item_id")
    @SerializedName("sourceBacklogItemId")
    val sourceBacklogItemId: String? = null,
    @ColumnInfo(name = "source_arc_quest_id")
    @SerializedName("sourceArcQuestId")
    val sourceArcQuestId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    @SerializedName("createdAt")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
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

fun TacticalMission.hasDeadline(): Boolean = deadline != NO_DEADLINE

enum class MissionSourceType {
    MANUAL,
    CONTEXT_BACKLOG_ITEM,
    SLOT_BACKLOG_ITEM,
    ARC_QUEST,
    PREVIOUS_WEEK,
}

@Entity(
    tableName = "tactical_mission_attachment_cross_ref",
    primaryKeys = ["missionId", "attachmentId"],
    foreignKeys = [
        ForeignKey(
            entity = TacticalMission::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["missionId"]),
        Index(value = ["attachmentId"]),
    ],
)
data class TacticalMissionAttachmentCrossRef(
    @SerializedName("missionId") val missionId: Long,
    @ColumnInfo(name = "attachmentId") @SerializedName("attachmentId")
    val attachmentId: String,
)

@Entity(
    tableName = "tactical_activity_slots",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["context_id"], unique = true),
    ],
)
data class TacticalActivitySlot(
    @PrimaryKey
    @SerializedName("id")
    val id: String,
    @ColumnInfo(name = "context_id")
    @SerializedName("contextId")
    val contextId: String,
    @ColumnInfo(name = "slot_order", defaultValue = "0")
    @SerializedName("slotOrder")
    val slotOrder: Long = 0L,
    @ColumnInfo(name = "created_at")
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
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

@Entity(
    tableName = "mission_streams",
    indices = [
        Index(value = ["stream_order"]),
        Index(value = ["is_default"]),
    ],
)
data class MissionStream(
    @PrimaryKey
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @ColumnInfo(name = "color_key")
    @SerializedName("colorKey")
    val colorKey: String? = null,
    @ColumnInfo(name = "icon_key")
    @SerializedName("iconKey")
    val iconKey: String? = null,
    @ColumnInfo(name = "budget_percent")
    @SerializedName("budgetPercent")
    val budgetPercent: Int? = null,
    @ColumnInfo(name = "stream_order", defaultValue = "0")
    @SerializedName("streamOrder")
    val streamOrder: Long = 0L,
    @ColumnInfo(name = "is_default", defaultValue = "0")
    @SerializedName("isDefault")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    @SerializedName("isArchived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
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
