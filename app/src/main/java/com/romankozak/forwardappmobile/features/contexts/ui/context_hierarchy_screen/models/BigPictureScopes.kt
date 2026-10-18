package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_BEACON_SCOPE_ID
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_GROUP_SCOPE_ID
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId

/**
 * Historical GroupScope/NoGroup/NoBeacon presentation adapter, NOT a generic group authority.
 * Missing GroupScope leaves a root unassigned; it never means inferred semantic membership.
 * Full legacy provenance completeness is enforced at its persistence/ingress boundary,
 * not promoted into a universal grammar for the Big Picture read surface.
 */
fun bigPictureScopes(
    structure: BigPictureStructure,
    nodes: List<BigPictureNode>,
    catalog: List<CanonicalEntitySummary>,
): List<BigPictureContainer> {
    val roots = nodes.filter { it.placement.parentPlacementId == null }
    val subjectRoots = roots.filter { it.placement.target.type == HierarchyTargetType.MANAGED_SUBJECT }
    val subjectRootIds = subjectRoots.mapTo(hashSetOf()) { it.placement.id }
    require(structure.rootGroupSubjects.keys.all { it in subjectRootIds }) {
        "Historical GroupScope references a non-root/non-subject occurrence"
    }
    val groups = catalog.filter { it.ref is CanonicalEntityRef.Orientation }.associateBy { it.ref.id }
    val grouped = subjectRoots.filter { it.placement.id in structure.rootGroupSubjects }
        .groupBy { structure.rootGroupSubjects[it.placement.id] }
    val groupIds = structure.groupOrder.keys + grouped.keys.filterNotNull()
    return buildList {
        groupIds.sortedWith(compareBy<String> { structure.groupOrder[it] ?: Long.MAX_VALUE }
            .thenBy { groups[it]?.title?.lowercase().orEmpty() }.thenBy { it })
            .forEach { subjectId ->
                add(BigPictureContainer(
                    id = "group:$subjectId",
                    title = groups[subjectId]?.title ?: "Група недоступна",
                    rootPlacementIds = grouped[subjectId].orEmpty().mapTo(linkedSetOf()) { it.placement.id },
                    origin = BigPictureContainerOrigin.HISTORICAL_GROUP,
                ))
            }
        grouped[null]?.let { ungrouped ->
            add(BigPictureContainer(CANONICAL_V2_NO_GROUP_SCOPE_ID, "Без групи",
                ungrouped.mapTo(linkedSetOf()) { it.placement.id }, BigPictureContainerOrigin.HISTORICAL_NO_GROUP))
        }
        roots.filter { it.placement.target.type == HierarchyTargetType.WORKSPACE }
            .takeIf { it.isNotEmpty() }?.let { workspaces ->
                add(BigPictureContainer(CANONICAL_V2_NO_BEACON_SCOPE_ID, "NoBeacon",
                    workspaces.mapTo(linkedSetOf()) { it.placement.id }, BigPictureContainerOrigin.HISTORICAL_NO_BEACON))
            }
    }
}
