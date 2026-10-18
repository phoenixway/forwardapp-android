package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement

/** Non-authoritative proof model. No Room writes or semantic relations. */
sealed interface H1TypedNode {
    data class Occurrence(val id: String) : H1TypedNode
    data class Container(val id: String) : H1TypedNode
}

data class H1TypedEdge(
    val node: H1TypedNode,
    val parent: H1TypedNode?,
    val siblingOrder: Long,
)

class H1UnifiedStructuralGraph internal constructor(
    val edges: List<H1TypedEdge>,
) {
    private val byNode = edges.associateBy { it.node }

    fun parentOf(node: H1TypedNode): H1TypedNode? =
        requireNotNull(byNode[node]) { "Unknown H1 typed node: $node" }.parent

    fun childrenOf(parent: H1TypedNode?): List<H1TypedEdge> =
        edges.filter { it.parent == parent }
            .sortedWith(
                compareBy<H1TypedEdge> { it.siblingOrder }
                    .thenBy { if (it.node is H1TypedNode.Container) 0 else 1 }
                    .thenBy {
                        when (val node = it.node) {
                            is H1TypedNode.Container -> node.id
                            is H1TypedNode.Occurrence -> node.id
                        }
                    },
            )

    fun nearestOccurrenceAncestor(node: H1TypedNode): String? {
        var cursor = parentOf(node)
        while (cursor != null) {
            if (cursor is H1TypedNode.Occurrence) return cursor.id
            cursor = parentOf(cursor)
        }
        return null
    }
}

/**
 * Schema-199 -> typed graph proof projection.
 *
 * Historical Policy B memberships into tombstoned containers remain
 * untouched in the supplied snapshot, but do not form live parent edges.
 * Missing references and invalid active graph topology still fail closed.
 */
fun projectH1UnifiedStructuralGraph(
    placements: Collection<HierarchyPlacement>,
    presentation: BigPicturePresentationSnapshot,
): H1UnifiedStructuralGraph {
    require(placements.map { it.id.value }.distinct().size == placements.size) {
        "Duplicate H1 placement identity"
    }
    validateBigPicturePresentation(presentation, placements)

    val livePlacements = placements.filter {
        !it.isDeleted && it.hierarchyId.value == "GENERAL"
    }.associateBy { it.id.value }
    val liveContainers = presentation.containers
        .filter { !it.isDeleted }
        .associateBy { it.id }
    // validateBigPicturePresentation has already checked ALL references,
    // including historical/tombstoned records. Do not weaken that check.
    //
    // Policy B preserves nondeleted memberships targeting retired containers
    // as inert transport history. They are NOT active typed parent edges.
    // Memberships for retired occurrences are likewise not live graph edges.
    val liveMemberships = presentation.memberships
        .filter {
            !it.isDeleted &&
                it.placementId in livePlacements &&
                it.containerId in liveContainers
        }
        .associateBy { it.placementId }

    val edges = buildList {
        liveContainers.values.forEach { container ->
            require(container.anchorPlacementId == null ||
                container.anchorPlacementId in livePlacements) {
                "Live container has unavailable occurrence anchor"
            }
            val parent = container.parentContainerId?.let(H1TypedNode::Container)
                ?: container.anchorPlacementId?.let(H1TypedNode::Occurrence)
            add(H1TypedEdge(H1TypedNode.Container(container.id), parent, container.siblingOrder))
        }
        livePlacements.values.forEach { placement ->
            val parent = liveMemberships[placement.id.value]?.containerId
                ?.let(H1TypedNode::Container)
                ?: placement.parentPlacementId?.value?.let(H1TypedNode::Occurrence)
            add(H1TypedEdge(H1TypedNode.Occurrence(placement.id.value), parent, placement.siblingOrder))
        }
    }

    val byNode = edges.associateBy { it.node }
    require(byNode.size == edges.size) { "Duplicate typed H1 node" }

    edges.forEach { edge ->
        require(edge.parent == null || edge.parent in byNode) {
            "Missing immediate H1 structural parent"
        }
        val seen = hashSetOf<H1TypedNode>()
        var cursor: H1TypedNode? = edge.node
        while (cursor != null) {
            require(seen.add(cursor)) { "Composite H1 structural cycle" }
            cursor = byNode.getValue(cursor).parent
        }
    }

    val graph = H1UnifiedStructuralGraph(edges)
    livePlacements.values.forEach { placement ->
        require(
            graph.nearestOccurrenceAncestor(H1TypedNode.Occurrence(placement.id.value)) ==
                placement.parentPlacementId?.value
        ) { "Occurrence ancestry differs from typed graph" }
    }
    liveContainers.values.forEach { container ->
        require(
            graph.nearestOccurrenceAncestor(H1TypedNode.Container(container.id)) ==
                container.anchorPlacementId
        ) { "Container anchor differs from typed graph ancestry" }
    }
    return graph
}
