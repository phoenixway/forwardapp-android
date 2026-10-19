package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationEntry
import com.romankozak.forwardappmobile.data.hierarchy.container.bigPicturePresentationEntries
import com.romankozak.forwardappmobile.data.hierarchy.container.validateBigPicturePresentation
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId

/** Read-only mixed presentation rows. No semantic target or H1 mutation authority. */
sealed interface BigPictureReadEntry {
    val siblingOrder: Long
    val stableId: String

    data class Container(
        val section: BigPictureContainer,
        override val siblingOrder: Long,
    ) : BigPictureReadEntry {
        override val stableId: String get() = "container:${section.id}"
    }

    data class Occurrence(
        val node: BigPictureNode,
    ) : BigPictureReadEntry {
        override val siblingOrder: Long get() = node.placement.siblingOrder
        override val stableId: String get() = "occurrence:${node.placement.id.value}"
    }
}

/**
 * Only persisted typed H1 parents participate in Big Picture structure.
 * Historical GroupScope is provenance, never an additional visual parent.
 */
class BigPictureReadComposition(
    val nodes: List<BigPictureNode>,
    historicalScopes: List<BigPictureContainer>,
    persisted: BigPicturePresentationSnapshot,
) {
    private val byNode = nodes.associateBy { it.placement.id.value }
    private val placements = nodes.map { it.placement }

    // Only live rows participate in display. Historical/tombstone rows remain
    // intact in persistence, Merge and Sync/ACK.
    private val liveContainers = persisted.containers.filter {
        !it.isDeleted &&
            (it.anchorPlacementId == null || it.anchorPlacementId in byNode)
    }
    private val liveContainerIds = liveContainers.mapTo(hashSetOf()) { it.id }
    private val liveMemberships = persisted.memberships.filter {
        !it.isDeleted &&
            it.placementId in byNode &&
            it.containerId in liveContainerIds
    }
    private val visiblePresentation = persisted.copy(
        containers = liveContainers,
        memberships = liveMemberships,
    )

    init {
        require(byNode.size == nodes.size) { "Duplicate live Big Picture occurrence" }
        validateBigPicturePresentation(visiblePresentation, placements)
    }

    // The same admitted live projection drives both rows and mutation affordances.
    // Tombstoned/inert transport history is deliberately not re-admitted as UI structure.
    val structuralGraph = projectH1UnifiedStructuralGraph(placements, visiblePresentation)
    val genericContainerTitles = liveContainers.associate { it.id to it.title }

    private val membershipsByContainer = liveMemberships.groupBy { it.containerId }
    private val genericById = liveContainers.associate { row ->
        val section = BigPictureContainer(
            id = genericSectionId(row.id),
            title = row.title,
            rootPlacementIds =
                membershipsByContainer[row.id].orEmpty()
                    .mapTo(linkedSetOf()) { PlacementId(it.placementId) },
            origin = BigPictureContainerOrigin.PRESENTATION,
        )
        row.id to section
    }

    init {
        require(historicalScopes.isEmpty()) { "Synthetic Big Picture structural parents are retired" }
    }

    val rootScopes: List<BigPictureContainer> =
        liveContainers.filter {
            it.anchorPlacementId == null && it.parentContainerId == null
        }.map { genericById.getValue(it.id) }

    val allScopes: List<BigPictureContainer> = liveContainers.map { genericById.getValue(it.id) }

    fun section(uiId: String): BigPictureContainer? =
        allScopes.singleOrNull { it.id == uiId }

    fun genericSectionRecord(uiId: String) =
        liveContainers.singleOrNull { genericSectionId(it.id) == uiId }

    fun containingGenericSection(placementId: String): BigPictureContainer? =
        liveMemberships.singleOrNull { it.placementId == placementId }
            ?.containerId?.let(genericById::get)

    fun rootEntries(): List<BigPictureReadEntry> = genericEntries(null, null)

    fun occurrenceEntries(placementId: String): List<BigPictureReadEntry> {
        require(placementId in byNode) { "Unknown focused occurrence" }
        return genericEntries(anchorPlacementId = placementId, parentContainerId = null)
    }

    fun sectionEntries(uiId: String): List<BigPictureReadEntry> {
        val row = requireNotNull(genericSectionRecord(uiId)) {
            "Unknown presentation section"
        }
        return genericEntries(row.anchorPlacementId, row.id)
    }

    fun genericAncestors(uiId: String): List<BigPictureContainer> {
        val path = mutableListOf<BigPictureContainer>()
        var cursor = requireNotNull(genericSectionRecord(uiId))
        val seen = hashSetOf<String>()
        while (true) {
            require(seen.add(cursor.id)) { "Presentation section cycle" }
            path.add(genericById.getValue(cursor.id))
            cursor = cursor.parentContainerId
                ?.let { parentId ->
                    requireNotNull(liveContainers.singleOrNull { it.id == parentId })
                } ?: break
        }
        return path.asReversed()
    }

    private fun genericEntries(
        anchorPlacementId: String?,
        parentContainerId: String?,
    ): List<BigPictureReadEntry> =
        bigPicturePresentationEntries(
            snapshot = visiblePresentation,
            placements = placements,
            anchorPlacementId = anchorPlacementId,
            parentContainerId = parentContainerId,
        ).map { entry ->
            when (entry) {
                is BigPicturePresentationEntry.Container ->
                    BigPictureReadEntry.Container(
                        section = genericById.getValue(entry.id),
                        siblingOrder = entry.siblingOrder,
                    )
                is BigPicturePresentationEntry.Occurrence ->
                    BigPictureReadEntry.Occurrence(
                        requireNotNull(byNode[entry.id]) {
                            "Missing projected occurrence: ${entry.id}"
                        },
                    )
            }
        }
}

private fun genericSectionId(id: String): String = "presentation:$id"
