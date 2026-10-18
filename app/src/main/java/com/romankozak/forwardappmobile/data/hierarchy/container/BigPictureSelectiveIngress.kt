package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle

/**
 * Admission boundary for presentation transported with a selective H1 delta.
 *
 * A member must belong to an incoming H1 occurrence. Its container and all
 * container ancestors must be present, with anchors inside that same H1 set.
 * No unrelated source-wide container may enter via a selective payload.
 *
 * Final geometric / version validation remains inside the atomic Room merge.
 */
internal fun requireSelectiveBigPictureDependencyClosure(bundle: SnapshotBundle) {
    val presentation = bundle.bigPicturePresentation ?: return

    require(presentation.formatVersion == 1) {
        "Unsupported selective Big Picture presentation format"
    }

    val placements = requireNotNull(bundle.hierarchyPlacements) {
        "Selective Big Picture presentation requires H1 occurrences"
    }
    require(bundle.hierarchyPlacementGroupScopes != null &&
        bundle.hierarchyPlacementLinkedAppearances != null) {
        "Selective Big Picture presentation requires complete H1 provenance"
    }

    val selectedIds = placements.mapTo(linkedSetOf()) { it.id }
    require(selectedIds.size == placements.size) {
        "Duplicate selective Big Picture H1 occurrence"
    }

    val containersById = presentation.containers.associateBy { it.id }
    require(containersById.size == presentation.containers.size) {
        "Duplicate selective Big Picture container"
    }

    val membershipIds = presentation.memberships.map { it.placementId }
    require(membershipIds.distinct().size == membershipIds.size) {
        "Duplicate selective Big Picture membership"
    }

    val requiredContainers = linkedSetOf<String>()

    presentation.memberships.forEach { membership ->
        require(membership.placementId in selectedIds) {
            "Selective Big Picture membership references unselected H1 occurrence"
        }

        val path = hashSetOf<String>()
        var cursor: String? = membership.containerId

        while (cursor != null) {
            require(path.add(cursor)) {
                "Selective Big Picture container cycle"
            }

            val container = requireNotNull(containersById[cursor]) {
                "Selective Big Picture missing container dependency"
            }

            require(container.anchorPlacementId == null ||
                container.anchorPlacementId in selectedIds) {
                "Selective Big Picture container references unselected H1 anchor"
            }

            requiredContainers += cursor
            cursor = container.parentContainerId
        }
    }

    require(requiredContainers == containersById.keys) {
        "Selective Big Picture payload contains unrelated containers"
    }
}
