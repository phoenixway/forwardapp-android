package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.validateProspectiveHierarchy

internal fun HierarchyPlacementEntity.toHierarchyPlacementStrict(): HierarchyPlacement =
    try {
        require(hierarchyId == HierarchyId.GENERAL.value) {
            "Unsupported persisted hierarchyId=$hierarchyId"
        }
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target =
                HierarchyTargetRef(
                    type = HierarchyTargetType.valueOf(targetType),
                    id = targetId,
                ),
            parentPlacementId = parentPlacementId?.let(::PlacementId),
            placementKind = PlacementKind.valueOf(placementKind),
            siblingOrder = siblingOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            isDeleted = isDeleted,
            version = version,
        )
    } catch (failure: RuntimeException) {
        throw CorruptHierarchyPlacementException(
            "Malformed hierarchy_placements row id=$id",
            failure,
        )
    }

internal fun HierarchyPlacement.toHierarchyPlacementEntity(): HierarchyPlacementEntity =
    HierarchyPlacementEntity(
        id = id.value,
        hierarchyId = hierarchyId.value,
        targetType = target.type.name,
        targetId = target.id,
        parentPlacementId = parentPlacementId?.value,
        placementKind = placementKind.name,
        siblingOrder = siblingOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

internal fun HierarchyPlacement.sameMutableState(other: HierarchyPlacement): Boolean =
    hierarchyId == other.hierarchyId &&
        target == other.target &&
        parentPlacementId == other.parentPlacementId &&
        placementKind == other.placementKind &&
        siblingOrder == other.siblingOrder &&
        isDeleted == other.isDeleted

private enum class TargetLiveness {
    LIVE,
    MISSING,
    DELETED,
}

internal suspend fun AppDatabase.requireValidProspectiveHierarchy(
    placements: Collection<HierarchyPlacement>,
) {
    val liveTargets =
        placements.asSequence()
            .filterNot { it.isDeleted }
            .map { it.target }
            .distinct()
            .toList()

    val targetStates =
        liveTargets.associateWith { target ->
            when (target.type) {
                HierarchyTargetType.MANAGED_SUBJECT -> {
                    val row = orientationDao().getManagedSubject(target.id)
                    when {
                        row == null -> TargetLiveness.MISSING
                        row.isDeleted -> TargetLiveness.DELETED
                        else -> TargetLiveness.LIVE
                    }
                }

                HierarchyTargetType.WORKSPACE -> {
                    val row = workspaceDao().getById(target.id)
                    when {
                        row == null -> TargetLiveness.MISSING
                        row.isDeleted -> TargetLiveness.DELETED
                        else -> TargetLiveness.LIVE
                    }
                }
            }
        }

    targetStates.entries
        .sortedWith(compareBy({ it.key.type.name }, { it.key.id }))
        .firstOrNull { it.value != TargetLiveness.LIVE }
        ?.let { (target, state) ->
            when (state) {
                TargetLiveness.MISSING -> throw HierarchyTargetMissingException(target)
                TargetLiveness.DELETED -> throw HierarchyTargetDeletedException(target)
                TargetLiveness.LIVE -> Unit
            }
        }

    val violations =
        validateProspectiveHierarchy(placements) { target ->
            targetStates[target] == TargetLiveness.LIVE
        }
    if (violations.isEmpty()) return

    val violation = violations.first()
    val placement =
        violation.path
            .removePrefix("placements.")
            .takeIf { it != violation.path }
            ?.let { id -> placements.firstOrNull { it.id.value == id } }

    throw when (violation.code) {
        "UNSUPPORTED_HIERARCHY" ->
            UnsupportedHierarchyException(requireNotNull(placement).hierarchyId)

        "MISSING_PARENT" ->
            HierarchyParentMissingException(
                placementId = requireNotNull(placement).id,
                parentPlacementId = requireNotNull(placement.parentPlacementId),
            )

        "TOMBSTONED_PARENT" ->
            HierarchyParentDeletedException(
                placementId = requireNotNull(placement).id,
                parentPlacementId = requireNotNull(placement.parentPlacementId),
            )

        "CROSS_HIERARCHY_PARENT" ->
            CrossHierarchyParentException(
                placementId = requireNotNull(placement).id,
                parentPlacementId = requireNotNull(placement.parentPlacementId),
            )

        "MULTIPLE_PRIMARY" ->
            PrimaryAppearanceConflictException(violation.message)

        "CYCLE", "SELF_PARENT" ->
            HierarchyPlacementCycleException(violation.message)

        else ->
            InvalidHierarchyStateException(
                "${violation.code}: ${violation.message}",
            )
    }
}
