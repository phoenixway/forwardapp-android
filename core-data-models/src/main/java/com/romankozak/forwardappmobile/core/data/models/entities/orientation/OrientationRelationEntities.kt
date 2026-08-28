package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orientation_relations",
    indices = [
        Index("fromOrientationId"),
        Index("toOrientationId"),
        Index(value = ["fromOrientationId", "toOrientationId", "relationType"], unique = true),
    ],
)
data class OrientationRelationEntity(
    @PrimaryKey val id: String,
    val fromOrientationId: String,
    val toOrientationId: String,
    val relationType: String,
    val relationOrder: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "aspect_orientation_refs",
    indices = [
        Index("aspectId"),
        Index("orientationId"),
        Index(value = ["aspectId", "orientationId", "relationType"], unique = true),
    ],
)
data class AspectOrientationRefEntity(
    @PrimaryKey val id: String,
    val aspectId: String,
    val orientationId: String,
    val relationType: String,
    val isPrimary: Boolean,
    val refOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "workspace_bindings",
    indices = [
        Index("workspaceId"),
        Index("subjectId"),
        Index(value = ["workspaceId", "subjectId", "bindingType"], unique = true),
    ],
)
data class WorkspaceBindingEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val subjectId: String,
    val bindingType: String,
    val isPrimary: Boolean,
    val bindingOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "workspace_capability_instances",
    indices = [
        Index("workspaceId"),
        Index(value = ["workspaceId", "capabilityType", "instanceKey"], unique = true),
    ],
)
data class WorkspaceCapabilityInstanceEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityType: String,
    val instanceKey: String,
    val capabilityOrder: Long,
    val state: String,
    val configurationVersion: Int,
    val configuration: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(tableName = "saved_orientation_views", indices = [Index("updatedAt"), Index("isDeleted")])
data class SavedOrientationViewEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filterAstVersion: Int,
    val filterJson: String,
    val sortSpecification: String,
    val grouping: String?,
    val visibleFieldsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
