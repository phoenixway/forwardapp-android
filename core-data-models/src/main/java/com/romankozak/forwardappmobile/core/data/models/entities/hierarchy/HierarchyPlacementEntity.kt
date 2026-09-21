package com.romankozak.forwardappmobile.core.data.models.entities.hierarchy

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical V2 structural hierarchy placement persistence.
 *
 * targetType + targetId is deliberately polymorphic and therefore has no SQL
 * foreign key. Target existence/liveness remains a domain/repository/import
 * invariant.
 */
@Entity(
    tableName = "hierarchy_placements",
    foreignKeys = [
        ForeignKey(
            entity = HierarchyPlacementEntity::class,
            parentColumns = ["id", "hierarchyId"],
            childColumns = ["parentPlacementId", "hierarchyId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
            deferred = true,
        ),
    ],
    indices = [
        // Candidate key required by SQLite/Room for the composite self-FK.
        Index(value = ["id", "hierarchyId"], unique = true),
        // FK child lookup and ordered live-child reads.
        Index(value = ["parentPlacementId", "hierarchyId", "isDeleted", "siblingOrder", "id"]),
        // Whole-hierarchy live reads in deterministic logical order.
        Index(value = ["hierarchyId", "isDeleted", "parentPlacementId", "siblingOrder", "id"]),
        // Typed-target appearance lookup.
        Index(value = ["hierarchyId", "targetType", "targetId", "isDeleted"]),
        Index("updatedAt"),
        Index("syncedAt"),
    ],
)
data class HierarchyPlacementEntity(
    @PrimaryKey val id: String,
    val hierarchyId: String,
    val targetType: String,
    val targetId: String,
    val parentPlacementId: String?,
    val placementKind: String,
    val siblingOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
