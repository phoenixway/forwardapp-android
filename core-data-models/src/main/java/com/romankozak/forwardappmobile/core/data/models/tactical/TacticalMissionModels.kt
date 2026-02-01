package com.romankozak.forwardappmobile.core.data.models.tactical

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.Converters

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
    indices = [Index(value = ["projectId"])],
)
@TypeConverters(Converters::class)
data class TacticalMission(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val startTime: Long? = null,
    val deadline: Long,
    val status: MissionStatus = MissionStatus.PENDING,
    val priority: MissionPriority = MissionPriority.MEDIUM,
    @SerializedName(value = "projectId", alternate = ["contextId"]) val projectId: String?,
    val linkedProjectIds: List<String>? = emptyList(),
    val linkedAttachmentIds: List<String>? = emptyList(),
)

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
    val missionId: Long,
    @ColumnInfo(name = "attachmentId")
    val attachmentId: String,
)
