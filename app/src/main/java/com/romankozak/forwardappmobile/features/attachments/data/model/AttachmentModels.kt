package com.romankozak.forwardappmobile.features.attachments.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.data.database.models.Project
import java.util.UUID

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["attachment_type"]),
        Index(value = ["entity_id"]),
    ],
)
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "attachment_type") val attachmentType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "owner_project_id") val ownerProjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "project_attachment_cross_ref",
    primaryKeys = ["project_id", "attachment_id"],
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["attachment_id"]),
    ],
)
data class ProjectAttachmentCrossRef(
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "attachment_id") val attachmentId: String,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long = -System.currentTimeMillis(),
)

data class AttachmentWithProject(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long?,
)
