package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*

data class BigPictureNode(
    val placement: HierarchyPlacement,
    val entity: CanonicalEntitySummary?,
    val depth: Int,
    val childCount: Int,
)

/** GENERAL occurrences are structure; catalog identities provide presentation only. */
fun projectBigPicture(
    placements: List<HierarchyPlacement>,
    catalog: List<CanonicalEntitySummary>,
): List<BigPictureNode> {
    val live = placements.filter { it.hierarchyId == HierarchyId.GENERAL && !it.isDeleted }
    val violations = validateProspectiveHierarchy(live) { true }
    require(violations.isEmpty()) {
        "Invalid Big Picture hierarchy: ${violations.map { it.code }.distinct().joinToString()}"
    }
    val entities = catalog.groupBy { summary ->
        when (val ref = summary.ref) {
            is CanonicalEntityRef.Workspace -> HierarchyTargetRef(HierarchyTargetType.WORKSPACE, ref.id)
            else -> HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, ref.id)
        }
    }
    val children = live.groupBy { it.parentPlacementId }
        .mapValues { (_, siblings) -> siblings.sortedWith(hierarchyPlacementSiblingComparator) }
    val pending = ArrayDeque<Pair<HierarchyPlacement, Int>>()
    children[null].orEmpty().asReversed().forEach { pending.addLast(it to 0) }
    return buildList {
        while (pending.isNotEmpty()) {
            val (placement, depth) = pending.removeLast()
            val nested = children[placement.id].orEmpty()
            add(BigPictureNode(placement, entities[placement.target]?.singleOrNull(), depth, nested.size))
            nested.asReversed().forEach { pending.addLast(it to depth + 1) }
        }
    }
}

data class BigPictureFocus(
    val activeNode: BigPictureNode?,
    val children: List<BigPictureNode>,
    val breadcrumbs: List<BigPictureNode>,
)

/** Focus navigation shows one occurrence and its direct children, never a flattened tree. */
fun focusBigPicture(nodes: List<BigPictureNode>, placementId: String?): BigPictureFocus {
    val byId = nodes.associateBy { it.placement.id.value }
    val active = byId[placementId]
    val path = mutableListOf<BigPictureNode>()
    var ancestor = active
    while (ancestor != null) {
        path += ancestor
        ancestor = ancestor.placement.parentPlacementId?.value?.let(byId::get)
    }
    return BigPictureFocus(
        activeNode = active,
        children = nodes.filter { it.placement.parentPlacementId == active?.placement?.id },
        breadcrumbs = path.asReversed(),
    )
}

fun bigPictureDestination(node: BigPictureNode): NavTarget? =
    when (val ref = node.entity?.ref) {
        is CanonicalEntityRef.Orientation -> NavTarget.SemanticEntityEditor(ref.id)
        is CanonicalEntityRef.Aspect -> NavTarget.SemanticEntityEditor(ref.id)
        is CanonicalEntityRef.Workspace -> NavTarget.Workspace(ref.id)
        null -> null
    }
