package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureMembershipSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementNotFoundException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementPosition
import com.romankozak.forwardappmobile.data.hierarchy.HierarchySiblingSetMismatchException
import com.romankozak.forwardappmobile.data.hierarchy.InvalidHierarchyPositionException
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*

internal data class H1ProspectiveStructuralState(
    val placements: List<HierarchyPlacement>,
    val presentation: BigPicturePresentationSnapshot,
)

internal class H1StructuralPlan(
    private val originalPlacements: List<HierarchyPlacement>,
    private val originalPresentation: BigPicturePresentationSnapshot,
    originalGraph: H1UnifiedStructuralGraph,
) {
    private val parents: MutableMap<H1TypedNode, H1TypedNode?> =
        originalGraph.edges.associate { it.node to it.parent }.toMutableMap()

    private val siblings =
        mutableMapOf<H1TypedNode?, MutableList<H1TypedNode>>()

    private val touchedParents = linkedSetOf<H1TypedNode?>()
    private val createdOccurrences = linkedMapOf<String, HierarchyPlacement>()
    private val createdContainers = linkedMapOf<String, BigPictureContainerSnapshot>()
    private val deletedContainers = linkedSetOf<String>()
    private val deletedOccurrences = linkedSetOf<String>()

    fun retiredOccurrenceIds(): List<PlacementId> =
        deletedOccurrences.sorted().map(::PlacementId)

    init {
        (originalGraph.edges.map { it.parent }.toSet() + setOf(null))
            .forEach { parent ->
                siblings[parent] = originalGraph.childrenOf(parent)
                    .map { it.node }
                    .toMutableList()
            }
    }

    private fun requireNode(node: H1TypedNode) {
        require(node in parents) { "Unavailable H1 typed node: $node" }
    }

    private fun resolveParent(destination: H1StructuralParent): H1TypedNode? =
        when (destination) {
            H1StructuralParent.Root -> null
            is H1StructuralParent.Node -> destination.ref.also(::requireNode)
        }

    private fun children(parent: H1TypedNode?): MutableList<H1TypedNode> =
        siblings.getOrPut(parent) { mutableListOf() }

    private fun insertionIndex(position: Int?, size: Int): Int {
        val index = position ?: size
        require(index in 0..size) { "H1 insertion position out of bounds" }
        return index
    }

    private fun requireAcyclicMove(
        node: H1TypedNode,
        destination: H1TypedNode?,
    ) {
        val seen = hashSetOf<H1TypedNode>()
        var cursor = destination
        while (cursor != null) {
            require(cursor != node) { "Composite H1 structural cycle" }
            require(seen.add(cursor)) { "Existing H1 structural cycle" }
            cursor = parents.getValue(cursor)
        }
    }

    fun move(
        node: H1TypedNode,
        destination: H1StructuralParent,
        position: Int?,
    ) {
        requireNode(node)
        val newParent = resolveParent(destination)
        requireAcyclicMove(node, newParent)

        val oldParent = parents.getValue(node)
        require(children(oldParent).remove(node)) {
            "H1 node missing from its original sibling namespace"
        }

        val destinationChildren = children(newParent)
        destinationChildren.add(
            insertionIndex(position, destinationChildren.size),
            node,
        )
        parents[node] = newParent

        touchedParents += oldParent
        touchedParents += newParent
    }

    fun moveOccurrence(
        id: String,
        parentId: String?,
        position: HierarchyPlacementPosition,
    ) {
        val node = H1TypedNode.Occurrence(id)
        requireNode(node)

        val destination = parentId?.let {
            H1StructuralParent.Node(H1TypedNode.Occurrence(it))
        } ?: H1StructuralParent.Root

        val parent = resolveParent(destination)
        val availableSiblings = children(parent).filterNot { it == node }

        fun requiredSiblingIndex(anchorId: String): Int {
            val index = availableSiblings.indexOf(
                H1TypedNode.Occurrence(anchorId),
            )
            require(index >= 0) {
                "Relative H1 occurrence anchor is not an immediate typed sibling"
            }
            return index
        }

        val index = when (position) {
            HierarchyPlacementPosition.FIRST -> 0
            HierarchyPlacementPosition.LAST -> availableSiblings.size
            is HierarchyPlacementPosition.BEFORE ->
                requiredSiblingIndex(position.placementId.value)
            is HierarchyPlacementPosition.AFTER ->
                requiredSiblingIndex(position.placementId.value) + 1
        }

        move(node, destination, index)
    }

    fun reorderOccurrenceOnly(
        destination: H1StructuralParent,
        orderedPlacementIds: List<String>,
    ) {
        val parent = resolveParent(destination)

        require(children(parent).all { it is H1TypedNode.Occurrence }) {
            "Mixed H1 sibling set requires typed reorder"
        }

        reorder(
            destination,
            orderedPlacementIds.map(H1TypedNode::Occurrence),
        )
    }

    fun reorder(
        destination: H1StructuralParent,
        orderedChildren: List<H1TypedNode>,
    ) {
        val parent = resolveParent(destination)
        val actual = children(parent)

        require(orderedChildren.size == actual.size) {
            "H1 reorder must include every immediate typed child"
        }
        require(orderedChildren.size == orderedChildren.toSet().size) {
            "Duplicate H1 reorder node"
        }
        require(orderedChildren.toSet() == actual.toSet()) {
            "H1 reorder contains missing or foreign children"
        }

        siblings[parent] = orderedChildren.toMutableList()
        touchedParents += parent
    }

    fun createLegacyOccurrence(
        id: PlacementId,
        target: HierarchyTargetRef,
        placementKind: PlacementKind,
        parentPlacementId: PlacementId?,
        position: HierarchyPlacementPosition,
        now: Long,
    ) {
        val parent = parentPlacementId?.let {
            H1TypedNode.Occurrence(it.value)
        }
        if (parent != null) requireNode(parent)

        val immediateSiblings = children(parent)

        if (immediateSiblings.any {
                it !is H1TypedNode.Occurrence
            }) {
            throw HierarchySiblingSetMismatchException(
                "Legacy create requires a pure occurrence sibling set",
            )
        }

        fun anchorIndex(id: PlacementId): Int {
            val index = immediateSiblings.indexOf(
                H1TypedNode.Occurrence(id.value),
            )
            if (index < 0) {
                throw InvalidHierarchyPositionException(
                    "Relative create anchor is not an immediate occurrence sibling: ${id.value}",
                )
            }
            return index
        }

        val index = when (position) {
            HierarchyPlacementPosition.FIRST -> 0
            HierarchyPlacementPosition.LAST -> immediateSiblings.size
            is HierarchyPlacementPosition.BEFORE ->
                anchorIndex(position.placementId)
            is HierarchyPlacementPosition.AFTER ->
                anchorIndex(position.placementId) + 1
        }

        createOccurrence(
            id = id,
            target = target,
            placementKind = placementKind,
            destination = parent?.let(H1StructuralParent::Node)
                ?: H1StructuralParent.Root,
            position = index,
            now = now,
        )
    }

    fun addAppearance(
        id: PlacementId,
        target: HierarchyTargetRef,
        destination: H1StructuralParent,
        now: Long,
    ) {
        val primaries = (originalPlacements + createdOccurrences.values).count {
            !it.isDeleted && it.hierarchyId == HierarchyId.GENERAL &&
                it.target == target && it.placementKind == PlacementKind.PRIMARY
        }
        require(primaries <= 1) { "Ambiguous PRIMARY appearance" }
        createOccurrence(
            id, target,
            if (primaries == 0) PlacementKind.PRIMARY else PlacementKind.LINK,
            destination, null, now,
        )
    }

    fun createOccurrence(
        id: PlacementId,
        target: HierarchyTargetRef,
        placementKind: PlacementKind,
        destination: H1StructuralParent,
        position: Int?,
        now: Long,
    ) {
        require(id.value.isNotBlank()) { "Occurrence identity must not be blank" }
        require(originalPlacements.none { it.id == id }) {
            "Occurrence identity already exists, including historical identity"
        }
        require(id.value !in createdOccurrences) {
            "Duplicate new occurrence identity"
        }

        val parent = resolveParent(destination)
        val node = H1TypedNode.Occurrence(id.value)
        require(node !in parents) { "Duplicate typed occurrence node" }

        createdOccurrences[id.value] = HierarchyPlacement(
            id = id,
            hierarchyId = HierarchyId.GENERAL,
            target = target,
            parentPlacementId = null,
            placementKind = placementKind,
            siblingOrder = 0L,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

        parents[node] = parent

        val destinationChildren = children(parent)
        destinationChildren.add(
            insertionIndex(position, destinationChildren.size),
            node,
        )
        touchedParents += parent
    }

    fun createContainer(
        title: String,
        destination: H1StructuralParent,
        position: Int?,
        id: String,
        now: Long,
    ) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotBlank()) { "Container title must not be blank" }
        require(id.isNotBlank()) { "Container identity must not be blank" }
        require(originalPresentation.containers.none { it.id == id }) {
            "Container identity already exists, including historical identity"
        }
        require(id !in createdContainers) { "Duplicate new container identity" }

        val parent = resolveParent(destination)
        val node = H1TypedNode.Container(id)

        createdContainers[id] = BigPictureContainerSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            title = normalizedTitle,
            anchorPlacementId = null,
            parentContainerId = null,
            siblingOrder = 0L,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

        parents[node] = parent
        val destinationChildren = children(parent)
        destinationChildren.add(
            insertionIndex(position, destinationChildren.size),
            node,
        )
        touchedParents += parent
    }

    fun removeOccurrenceSubtree(
        placementId: PlacementId,
    ): List<PlacementId> {
        val root = H1TypedNode.Occurrence(placementId.value)
        requireNode(root)

        require(placementId.value !in createdOccurrences) {
            "New occurrence cannot be retired as a subtree in this plan"
        }

        val traversal = mutableListOf<H1TypedNode>()
        val visited = hashSetOf<H1TypedNode>()

        fun collect(node: H1TypedNode) {
            require(visited.add(node)) {
                "Cycle or duplicate node in typed retirement subtree"
            }
            traversal += node
            children(node).toList().forEach(::collect)
        }

        collect(root)

        val parent = parents.getValue(root)
        require(children(parent).remove(root)) {
            "Subtree root missing from typed sibling namespace"
        }

        val retiredOccurrences = mutableListOf<PlacementId>()

        traversal.forEach { node ->
            when (node) {
                is H1TypedNode.Occurrence -> {
                    require(node.id !in createdOccurrences) {
                        "Cannot retire an uncommitted occurrence subtree"
                    }
                    deletedOccurrences += node.id
                    retiredOccurrences += PlacementId(node.id)
                }

                is H1TypedNode.Container -> {
                    require(node.id !in createdContainers) {
                        "Cannot retire an uncommitted container subtree"
                    }
                    deletedContainers += node.id
                }
            }
        }

        traversal.asReversed().forEach { node ->
            parents.remove(node)
            siblings.remove(node)
        }

        touchedParents += parent
        return retiredOccurrences
    }

    fun removeOccurrence(placementId: PlacementId) {
        val node = H1TypedNode.Occurrence(placementId.value)
        requireNode(node)

        require(placementId.value !in createdOccurrences) {
            "New occurrence cannot be independently retired in this plan"
        }

        if (children(node).isNotEmpty()) {
            throw HierarchyChildPolicyRejectedException(placementId)
        }

        val parent = parents.getValue(node)
        require(children(parent).remove(node)) {
            "Retired occurrence missing from typed sibling namespace"
        }

        parents.remove(node)
        siblings.remove(node)
        deletedOccurrences += placementId.value
        touchedParents += parent
    }

    fun deleteContainer(id: String) {
        val node = H1TypedNode.Container(id)
        requireNode(node)
        require(id !in createdContainers) {
            "New container deletion is not a standalone mutation"
        }

        val parent = parents.getValue(node)
        val originalSiblings = children(parent)
        val index = originalSiblings.indexOf(node)
        require(index >= 0) { "Container missing from its parent" }

        val promoted = children(node).toList()
        originalSiblings.removeAt(index)
        originalSiblings.addAll(index, promoted)

        promoted.forEach { child -> parents[child] = parent }
        parents.remove(node)
        siblings.remove(node)

        deletedContainers += id
        touchedParents += parent
    }

    /**
     * Returns the nearest occurrence ABOVE the node. A container directly
     * below an occurrence is anchored at that occurrence; an occurrence below
     * another occurrence stores it as parentPlacementId.
     */
    private fun nearestOccurrenceAncestor(node: H1TypedNode): String? {
        requireNode(node)
        val seen = hashSetOf(node)
        var cursor = parents.getValue(node)
        while (cursor != null) {
            require(seen.add(cursor)) { "Composite H1 structural cycle" }
            if (cursor is H1TypedNode.Occurrence) return cursor.id
            cursor = parents.getValue(cursor)
        }
        return null
    }

    fun materialize(now: Long): H1ProspectiveStructuralState {
        val normalizedOrders = mutableMapOf<H1TypedNode, Long>()
        touchedParents.forEach { parent ->
            children(parent).forEachIndexed { index, node ->
                normalizedOrders[node] = index.toLong()
            }
        }

        val allPlacementRows =
            originalPlacements + createdOccurrences.values

        val originalPlacementById =
            allPlacementRows.associateBy { it.id.value }

        val placements = allPlacementRows.map { row ->
            when {
                row.id.value in deletedOccurrences -> row.copy(
                    isDeleted = true,
                    version = nextH1StructuralVersion(row.version),
                    updatedAt = nextH1StructuralTime(row.updatedAt, now),
                    syncedAt = null,
                )
                row.isDeleted || row.hierarchyId != HierarchyId.GENERAL -> row
                else -> {
                    val node = H1TypedNode.Occurrence(row.id.value)
                    val nearest = nearestOccurrenceAncestor(node)
                    val desiredParent = nearest?.let { ancestor ->
                        requireNotNull(originalPlacementById[ancestor]) {
                            "Missing ancestor occurrence"
                        }.id
                    }

                    val desiredOrder = normalizedOrders[node] ?: row.siblingOrder
                    val desired = row.copy(
                        parentPlacementId = desiredParent,
                        siblingOrder = desiredOrder,
                    )
                    when {
                        row.id.value in createdOccurrences -> desired
                        desired == row -> row
                        else -> desired.copy(
                            version = nextH1StructuralVersion(row.version),
                            updatedAt = nextH1StructuralTime(row.updatedAt, now),
                            syncedAt = null,
                        )
                    }
                }
            }
        }

        val liveContainerRows =
            originalPresentation.containers.filterNot { it.isDeleted || it.id in deletedContainers } +
                createdContainers.values

        val materializedLiveContainers = liveContainerRows.associate { row ->
            val node = H1TypedNode.Container(row.id)
            val parent = parents.getValue(node)
            val desired = row.copy(
                anchorPlacementId = nearestOccurrenceAncestor(node),
                parentContainerId = (parent as? H1TypedNode.Container)?.id,
                siblingOrder = normalizedOrders[node] ?: row.siblingOrder,
            )
            val final = when {
                row.id in createdContainers -> desired
                desired == row -> row
                else -> desired.copy(
                    version = nextH1StructuralVersion(row.version),
                    updatedAt = nextH1StructuralTime(row.updatedAt, now),
                    syncedAt = null,
                )
            }
            row.id to final
        }

        val containerRows = (
            originalPresentation.containers.map { row ->
                when {
                    row.id in deletedContainers -> row.copy(
                        isDeleted = true,
                        version = nextH1StructuralVersion(row.version),
                        updatedAt = nextH1StructuralTime(row.updatedAt, now),
                        syncedAt = null,
                    )
                    !row.isDeleted ->
                        materializedLiveContainers.getValue(row.id)
                    else -> row
                }
            } + createdContainers.keys.map(materializedLiveContainers::getValue)
        ).associateBy { it.id }.toMutableMap()

        /*
         * Historical nested containers are retained for transport.
         * Their anchor must continue matching their persisted container
         * parent, even when that parent changes occurrence ancestry.
         * This updates historical references without resurrecting them.
         */
        fun convergeHistoricalAnchor(
            id: String,
            visited: MutableSet<String>,
        ): String? {
            require(visited.add(id)) { "Historical container ancestry cycle" }
            val current = containerRows.getValue(id)
            val parentId = current.parentContainerId
            if (parentId == null) {
                visited.remove(id)
                return current.anchorPlacementId
            }

            val parentAnchor = convergeHistoricalAnchor(parentId, visited)
            if (current.anchorPlacementId != parentAnchor) {
                val updated = current.copy(anchorPlacementId = parentAnchor)
                containerRows[id] = if (id in createdContainers) updated
                else updated.copy(
                    version = nextH1StructuralVersion(current.version),
                    updatedAt = nextH1StructuralTime(current.updatedAt, now),
                    syncedAt = null,
                )
            }
            visited.remove(id)
            return containerRows.getValue(id).anchorPlacementId
        }

        containerRows.keys.toList().forEach {
            convergeHistoricalAnchor(it, hashSetOf())
        }

        val originalLiveContainers = originalPresentation.containers
            .filterNot { it.isDeleted }
            .mapTo(hashSetOf()) { it.id }

        val memberships = originalPresentation.memberships
            .associateBy { it.placementId }
            .toMutableMap()

        // Historical membership rows remain durable. Active rows owned by
        // retired occurrences become tombstones, never hard deletes.
        deletedOccurrences.forEach { id ->
            val current = memberships[id]
            if (current != null && !current.isDeleted) {
                memberships[id] = current.copy(
                    isDeleted = true,
                    version = nextH1StructuralVersion(current.version),
                    updatedAt = nextH1StructuralTime(current.updatedAt, now),
                    syncedAt = null,
                )
            }
        }

        placements.filter {
            !it.isDeleted && it.hierarchyId == HierarchyId.GENERAL
        }.forEach { placement ->
            val node = H1TypedNode.Occurrence(placement.id.value)
            val parent = parents.getValue(node)
            val desiredContainer = (parent as? H1TypedNode.Container)?.id
            val current = memberships[placement.id.value]

            when {
                desiredContainer != null -> {
                    require(
                        containerRows[desiredContainer]?.isDeleted == false,
                    ) { "Unavailable membership destination" }

                    if (current == null) {
                        memberships[placement.id.value] =
                            BigPictureMembershipSnapshot(
                                placementId = placement.id.value,
                                hierarchyId = "GENERAL",
                                containerId = desiredContainer,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1L,
                            )
                    } else if (
                        current.isDeleted ||
                        current.containerId != desiredContainer
                    ) {
                        memberships[placement.id.value] = current.copy(
                            containerId = desiredContainer,
                            isDeleted = false,
                            version = nextH1StructuralVersion(current.version),
                            updatedAt = nextH1StructuralTime(current.updatedAt, now),
                            syncedAt = null,
                        )
                    }
                }

                current != null &&
                    !current.isDeleted &&
                    current.containerId in originalLiveContainers -> {
                    memberships[placement.id.value] = current.copy(
                        isDeleted = true,
                        version = nextH1StructuralVersion(current.version),
                        updatedAt = nextH1StructuralTime(current.updatedAt, now),
                        syncedAt = null,
                    )
                }
            }
        }

        return H1ProspectiveStructuralState(
            placements = placements,
            presentation = originalPresentation.copy(
                containers = containerRows.values.sortedBy { it.id },
                memberships = memberships.values.sortedBy { it.placementId },
            ),
        )
    }
}

internal fun nextH1StructuralVersion(previous: Long): Long {
    require(previous in 1 until Long.MAX_VALUE) {
        "Cannot advance H1 structural record version"
    }
    return previous + 1
}

internal fun nextH1StructuralTime(previous: Long, requested: Long): Long {
    require(previous < Long.MAX_VALUE) {
        "Cannot advance H1 structural modification timestamp"
    }
    return maxOf(requested, previous + 1)
}
