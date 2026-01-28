package com.romankozak.forwardappmobile.core.data.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
    @ColumnInfo(
        name = "owner_context_id",
    ) @SerializedName(value = "ownerContextId", alternate = ["d", "ownerProjectId"]) val ownerContextId: String? = null,
    @ColumnInfo(name = "role_code") val roleCode: String? = null,
    @ColumnInfo(name = "is_system", defaultValue = "0") val isSystem: Boolean = false,
    @SerializedName(value = "createdAt", alternate = ["crAt"]) val createdAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "updatedAt", alternate = ["upAt"]) val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)

@Entity(
    tableName = "context_attachment_cross_ref",
    primaryKeys = ["context_id", "attachment_id"],
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
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
data class ContextAttachmentCrossRef(
    @ColumnInfo(name = "context_id") @SerializedName(value = "contextId", alternate = ["a"]) val contextId: String,
    @ColumnInfo(name = "attachment_id") @SerializedName(value = "attachmentId", alternate = ["b"]) val attachmentId: String,
    @ColumnInfo(
        name = "attachment_order",
    ) @SerializedName(value = "attachmentOrder", alternate = ["c"]) val attachmentOrder: Long = -System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)

data class AttachmentWithContext(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "context_id") val contextId: String?,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long?,
)
