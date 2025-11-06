package com.romankozak.forwardappmobile.features.attachments.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.data.database.models.ProjectEntity
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentEntity as SharedAttachmentEntity
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.AttachmentWithProject as SharedAttachmentWithProject
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.ProjectAttachmentCrossRef as SharedProjectAttachmentCrossRef

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["attachment_type"]),
        Index(value = ["entity_id"]),
    ],
)
data class AttachmentRoomEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "attachment_type") val attachmentType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "owner_project_id") val ownerProjectId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "project_attachment_cross_ref",
    primaryKeys = ["project_id", "attachment_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["attachment_id"]),
    ],
)
data class ProjectAttachmentCrossRefRoom(
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "attachment_id") val attachmentId: String,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long,
)

data class AttachmentWithProjectRoom(
    @Embedded val attachment: AttachmentRoomEntity,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long?,
)

internal fun AttachmentRoomEntity.toShared(): SharedAttachmentEntity =
    SharedAttachmentEntity(
        id = id,
        attachmentType = attachmentType,
        entityId = entityId,
        ownerProjectId = ownerProjectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun SharedAttachmentEntity.toRoom(): AttachmentRoomEntity =
    AttachmentRoomEntity(
        id = id,
        attachmentType = attachmentType,
        entityId = entityId,
        ownerProjectId = ownerProjectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun ProjectAttachmentCrossRefRoom.toShared(): SharedProjectAttachmentCrossRef =
    SharedProjectAttachmentCrossRef(
        projectId = projectId,
        attachmentId = attachmentId,
        attachmentOrder = attachmentOrder,
    )

internal fun SharedProjectAttachmentCrossRef.toRoom(): ProjectAttachmentCrossRefRoom =
    ProjectAttachmentCrossRefRoom(
        projectId = projectId,
        attachmentId = attachmentId,
        attachmentOrder = attachmentOrder,
    )

internal fun AttachmentWithProjectRoom.toShared(): SharedAttachmentWithProject =
    SharedAttachmentWithProject(
        attachment = attachment.toShared(),
        projectId = projectId,
        attachmentOrder = attachmentOrder,
    )
