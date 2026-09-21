package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Embedded
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity

/**
 * One coherent persisted structural read frame.
 *
 * H1 is mandatory. GroupScope and linked-presentation provenance are exact
 * occurrence side-streams joined to the same SQLite statement snapshot.
 */
data class CanonicalV2HierarchyStructuralReadRow(
    @Embedded
    val placement: HierarchyPlacementEntity,
    @Embedded(prefix = "groupScope_")
    val groupScope: HierarchyPlacementGroupScopeEntity?,
    @Embedded(prefix = "linkedAppearance_")
    val linkedAppearance: HierarchyPlacementLinkedAppearanceEntity?,
)
