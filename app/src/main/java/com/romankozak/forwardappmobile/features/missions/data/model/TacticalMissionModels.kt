package com.romankozak.forwardappmobile.features.missions.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.data.database.models.Project

@Entity(
    tableName = "tactical_missions",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class TacticalMission(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val deadline: Long, // Unix timestamp
    val isCompleted: Boolean = false,
    val projectId: String
)

@Entity(
    tableName = "tactical_mission_attachment_cross_ref",
    primaryKeys = ["missionId", "attachmentId"],
    foreignKeys = [
        ForeignKey(
            entity = TacticalMission::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["missionId"]),
        Index(value = ["attachmentId"])
    ]
)
data class TacticalMissionAttachmentCrossRef(
    val missionId: Long,
    @ColumnInfo(name = "attachmentId")
    val attachmentId: String
)
