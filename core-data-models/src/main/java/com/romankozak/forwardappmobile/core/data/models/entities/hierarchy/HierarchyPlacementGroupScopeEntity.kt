package com.romankozak.forwardappmobile.core.data.models.entities.hierarchy

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical occurrence-scoped Beacon Group presentation provenance.
 *
 * A live row exists only for a live root MANAGED_SUBJECT placement:
 * - groupSubjectId != null means that exact occurrence is shown in that Group;
 * - groupSubjectId == null means explicit NoGroup.
 *
 * Absence of a live row is never interpreted as NoGroup once this capability is
 * authoritative. Group remains synthetic and is never a hierarchy target or
 * parent placement.
 */
@Entity(
    tableName = "hierarchy_placement_group_scopes",
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
        Index(value = ["hierarchyId", "isDeleted", "groupSubjectId", "placementId"]),
        Index("updatedAt"),
        Index("syncedAt"),
    ],
)
data class HierarchyPlacementGroupScopeEntity(
    @PrimaryKey val placementId: String,
    val hierarchyId: String,
    val groupSubjectId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
