package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId

sealed class HierarchyPlacementMutationException(
    message: String,
) : IllegalArgumentException(message)

class HierarchyPlacementNotFoundException(
    val placementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Hierarchy placement does not exist or is not live: ${placementId.value}",
)

class HierarchyPlacementAlreadyExistsException(
    val placementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Hierarchy placement already exists: ${placementId.value}",
)

class UnsupportedHierarchyException(
    val hierarchyId: HierarchyId,
) : HierarchyPlacementMutationException(
    "Unsupported hierarchy: ${hierarchyId.value}",
)

class HierarchyTargetMissingException(
    val target: HierarchyTargetRef,
) : HierarchyPlacementMutationException(
    "Hierarchy target does not exist: ${target.type}:${target.id}",
)

class HierarchyTargetDeletedException(
    val target: HierarchyTargetRef,
) : HierarchyPlacementMutationException(
    "Hierarchy target is deleted: ${target.type}:${target.id}",
)

class HierarchyParentMissingException(
    val placementId: PlacementId,
    val parentPlacementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Parent ${parentPlacementId.value} does not exist for placement ${placementId.value}",
)

class HierarchyParentDeletedException(
    val placementId: PlacementId,
    val parentPlacementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Parent ${parentPlacementId.value} is deleted for placement ${placementId.value}",
)

class CrossHierarchyParentException(
    val placementId: PlacementId,
    val parentPlacementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Parent ${parentPlacementId.value} belongs to a different hierarchy for placement ${placementId.value}",
)

class PrimaryAppearanceConflictException(
    message: String,
) : HierarchyPlacementMutationException(message)

class HierarchyPlacementCycleException(
    message: String,
) : HierarchyPlacementMutationException(message)

class HierarchyChildPolicyRejectedException(
    val placementId: PlacementId,
) : HierarchyPlacementMutationException(
    "Placement ${placementId.value} has live children and cannot be removed",
)

class InvalidHierarchyPositionException(
    message: String,
) : HierarchyPlacementMutationException(message)

class InvalidHierarchyStateException(
    message: String,
) : HierarchyPlacementMutationException(message)

class HierarchyPlacementMergeConflictException(
    message: String,
) : HierarchyPlacementMutationException(message)

class CorruptHierarchyPlacementException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

sealed interface HierarchyPlacementPosition {
    data object FIRST : HierarchyPlacementPosition
    data object LAST : HierarchyPlacementPosition

    data class BEFORE(
        val placementId: PlacementId,
    ) : HierarchyPlacementPosition

    data class AFTER(
        val placementId: PlacementId,
    ) : HierarchyPlacementPosition
}

enum class HierarchyPlacementChildPolicy {
    REJECT_IF_HAS_LIVE_CHILDREN,
}

data class HierarchyPlacementMove(
    val placementId: PlacementId,
    val newParentPlacementId: PlacementId?,
    val position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
)
