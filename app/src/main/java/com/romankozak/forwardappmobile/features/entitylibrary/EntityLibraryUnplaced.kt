package com.romankozak.forwardappmobile.features.entitylibrary

import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType

/**
 * Unplaced is derived presentation state, not persisted entity state.
 *
 * The canonical catalog owns eligible entity identities.
 * Hierarchy placements own occurrence presence.
 */
fun deriveUnplacedCanonicalEntities(
    catalog: List<CanonicalEntitySummary>,
    placedTargets: Set<HierarchyTargetRef>,
): List<CanonicalEntitySummary> =
    catalog.filterNot { summary ->
        val target =
            when (val ref = summary.ref) {
                is CanonicalEntityRef.Orientation ->
                    HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, ref.id)

                is CanonicalEntityRef.Aspect ->
                    HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, ref.id)

                is CanonicalEntityRef.Workspace ->
                    HierarchyTargetRef(HierarchyTargetType.WORKSPACE, ref.id)
            }

        target in placedTargets
    }
