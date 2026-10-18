package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot

/**
 * Selective presentation is driven by the already closed canonical H1 occurrences.
 *
 * A selected membership brings its container and every container ancestor.
 * No unrelated membership, sibling container or semantic owner is imported.
 * The complete source presentation is checked before narrowing the stream.
 */
internal fun SnapshotBundle.withCanonicalBigPictureSelectiveClosure(
    source: SnapshotBundle,
): SnapshotBundle {
    val presentation = source.bigPicturePresentation
        ?: return copy(bigPicturePresentation = null)

    val sourcePlacements = requireNotNull(source.hierarchyPlacements) {
        "Big Picture selective source requires canonical H1."
    }
    val selectedPlacements = requireNotNull(hierarchyPlacements) {
        "Big Picture selective closure requires filtered canonical H1."
    }

    validateSelectiveBigPictureSource(presentation, sourcePlacements)

    val sourceIds = sourcePlacements.mapTo(hashSetOf()) { it.id }
    val selectedIds = selectedPlacements.mapTo(linkedSetOf()) { it.id }
    require(sourceIds.containsAll(selectedIds)) {
        "Selective Big Picture H1 contains occurrences absent from source."
    }

    val containersById = presentation.containers.associateBy { it.id }
    val retainedMemberships =
        presentation.memberships.filter { it.placementId in selectedIds }

    val retainedContainerIds = linkedSetOf<String>()

    retainedMemberships.forEach { membership ->
        var cursor: String? = membership.containerId
        while (cursor != null) {
            if (!retainedContainerIds.add(cursor)) break
            cursor = requireNotNull(containersById[cursor]) {
                "Missing selective Big Picture container $cursor."
            }.parentContainerId
        }
    }

    val retainedContainers =
        presentation.containers.filter { it.id in retainedContainerIds }

    // Anchors represent an H1 location, never an independent import authority.
    // A filtered presentation must not silently depend on omitted H1 occurrences.
    retainedContainers.forEach { container ->
        require(container.anchorPlacementId == null ||
            container.anchorPlacementId in selectedIds) {
            "Selective Big Picture container ${container.id} has unselected H1 anchor."
        }
    }

    return copy(
        bigPicturePresentation = BigPicturePresentationSnapshot(
            containers = retainedContainers,
            memberships = retainedMemberships,
            formatVersion = presentation.formatVersion,
        ),
    )
}

/**
 * Validate the complete source, including nonselected branches, so malformed
 * presentation records cannot hide behind selection filtering.
 *
 * Mirrors the persisted presentation graph rules using wire H1 snapshots;
 * this sync module must not depend on the Android application's Room layer.
 */
private fun validateSelectiveBigPictureSource(
    presentation: BigPicturePresentationSnapshot,
    placements: List<HierarchyPlacementSnapshot>,
) {
    require(presentation.formatVersion == 1) {
        "Unsupported Big Picture presentation format."
    }

    val byPlacement = placements.associateBy { it.id }
    require(byPlacement.size == placements.size) {
        "Duplicate source H1 identity."
    }

    val byContainer = presentation.containers.associateBy { it.id }
    require(byContainer.size == presentation.containers.size) {
        "Duplicate Big Picture container identity."
    }

    require(
        presentation.memberships.map { it.placementId }.distinct().size ==
            presentation.memberships.size
    ) {
        "Duplicate Big Picture membership identity."
    }

    presentation.containers.forEach { container ->
        require(
            container.id.isNotBlank() &&
                container.hierarchyId == "GENERAL" &&
                container.title.isNotBlank() &&
                container.version >= 1L
        ) {
            "Invalid Big Picture container."
        }

        container.anchorPlacementId?.let { anchor ->
            require(byPlacement[anchor]?.hierarchyId == container.hierarchyId) {
                "Missing Big Picture container anchor."
            }
        }

        container.parentContainerId?.let { parentId ->
            val parent = requireNotNull(byContainer[parentId]) {
                "Missing Big Picture parent container."
            }
            require(
                parent.hierarchyId == container.hierarchyId &&
                    parent.anchorPlacementId == container.anchorPlacementId
            ) {
                "Big Picture container parent location differs."
            }
            require(container.isDeleted || !parent.isDeleted) {
                "Live Big Picture container below deleted parent."
            }
        }

        val seen = hashSetOf<String>()
        var cursor: String? = container.id
        while (cursor != null) {
            require(seen.add(cursor)) {
                "Big Picture container cycle."
            }
            cursor = byContainer[cursor]?.parentContainerId
        }
    }

    presentation.memberships.forEach { membership ->
        require(
            membership.placementId.isNotBlank() &&
                membership.containerId.isNotBlank() &&
                membership.hierarchyId == "GENERAL" &&
                membership.version >= 1L
        ) {
            "Invalid Big Picture membership."
        }

        val placement = requireNotNull(byPlacement[membership.placementId]) {
            "Missing Big Picture member occurrence."
        }
        val container = requireNotNull(byContainer[membership.containerId]) {
            "Missing Big Picture membership container."
        }

        require(
            placement.hierarchyId == membership.hierarchyId &&
                container.hierarchyId == membership.hierarchyId
        ) {
            "Big Picture membership hierarchy differs."
        }

        if (!membership.isDeleted &&
            !container.isDeleted &&
            !placement.isDeleted
        ) {
            require(placement.parentPlacementId == container.anchorPlacementId) {
                "Big Picture membership crosses an H1 sibling level."
            }
        }
    }
}
