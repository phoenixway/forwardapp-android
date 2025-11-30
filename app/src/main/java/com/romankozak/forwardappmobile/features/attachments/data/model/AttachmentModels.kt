package com.romankozak.forwardappmobile.features.attachments.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    @PrimaryKey @SerializedName(value = "id", alternate = ["a"]) val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "attachment_type") @SerializedName(value = "attachmentType", alternate = ["b"]) val attachmentType: String,
    @ColumnInfo(name = "entity_id") @SerializedName(value = "entityId", alternate = ["c"]) val entityId: String,
    @ColumnInfo(name = "owner_project_id") @SerializedName(value = "ownerProjectId", alternate = ["d"]) val ownerProjectId: String? = null,
    @SerializedName(value = "createdAt", alternate = ["e"]) val createdAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "updatedAt", alternate = ["f"]) val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
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
    @ColumnInfo(name = "project_id") @SerializedName(value = "projectId", alternate = ["a"]) val projectId: String,
    @ColumnInfo(name = "attachment_id") @SerializedName(value = "attachmentId", alternate = ["b"]) val attachmentId: String,
    @ColumnInfo(name = "attachment_order") @SerializedName(value = "attachmentOrder", alternate = ["c"]) val attachmentOrder: Long = -System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)

data class AttachmentWithProject(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long?,
)
