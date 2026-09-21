package com.romankozak.forwardappmobile.core.data.models.entities.hierarchy

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exact occurrence-scoped presentation provenance for Workspace linked badges.
 *
 * A live row means that exact Workspace occurrence is presented as a linked
 * appearance. Absence of a live row means ordinary presentation.
 *
 * This is deliberately independent of PlacementKind. A structurally LINK
 * occurrence is not necessarily a linked presentation appearance, and the
 * presentation flag is never inherited by structural descendants.
 */
@Entity(
    tableName = "hierarchy_placement_linked_appearances",
    foreignKeys = [
        ForeignKey(
            entity = HierarchyPlacementEntity::class,
            parentColumns = ["id", "hierarchyId"],
            childColumns = ["placementId", "hierarchyId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
            deferred = true,
        ),
    ],
    indices = [
        Index(value = ["placementId", "hierarchyId"], unique = true),
        Index(value = ["hierarchyId", "isDeleted", "placementId"]),
        Index("updatedAt"),
        Index("syncedAt"),
    ],
)
data class HierarchyPlacementLinkedAppearanceEntity(
    @PrimaryKey val placementId: String,
    val hierarchyId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
