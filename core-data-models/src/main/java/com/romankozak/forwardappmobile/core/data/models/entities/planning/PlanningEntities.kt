package com.romankozak.forwardappmobile.core.data.models.entities.planning

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity

@Entity(
    tableName = "planning_scopes",
    indices = [
        Index(value = ["kind", "isDeleted", "lifecycle"]),
        Index("updatedAt"),
        Index("syncedAt"),
    ],
)
data class PlanningScopeEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val lifecycle: String,
    val title: String?,
    val startsAt: Long?,
    val endsAt: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

@Entity(
    tableName = "planning_commitments",
    foreignKeys = [
        ForeignKey(
            entity = PlanningScopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["scopeId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = OrientationEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["orientationId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["scopeId", "role", "isDeleted", "commitmentOrder", "id"]),
        Index(value = ["scopeId", "orientationId", "role", "isDeleted"]),
        Index("orientationId"),
        Index("updatedAt"),
        Index("syncedAt"),
    ],
)
data class PlanningCommitmentEntity(
    @PrimaryKey val id: String,
    val scopeId: String,
    val orientationId: String,
    val role: String,
    val commitmentOrder: Long,
    val priorityLevel: String?,
    val status: String,
    val provenanceKind: String,
    val provenanceSourceType: String?,
    val provenanceSourceId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)
