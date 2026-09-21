package com.romankozak.forwardappmobile.shared.core.domain.hierarchy

data class HierarchyPlacementViolation(
    val path: String,
    val code: String,
    val message: String,
)

/**
 * Validates one complete prospective hierarchy state.
 *
 * Tombstoned placements remain part of identity/history validation, but do not
 * participate in live topology, PRIMARY uniqueness, or target-liveness checks.
 */
fun validateProspectiveHierarchy(
    placements: Collection<HierarchyPlacement>,
    isTargetLive: (HierarchyTargetRef) -> Boolean,
): List<HierarchyPlacementViolation> {
    val violations = mutableListOf<HierarchyPlacementViolation>()
    val placementsById = placements.groupBy { it.id }

    placementsById
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            violations +=
                violation(
                    path = "placements.${id.value}",
                    code = "DUPLICATE_PLACEMENT_ID",
                    message = "Placement identity ${id.value} occurs ${duplicates.size} times",
                )
        }

    placements.forEach { placement ->
        val path = "placements.${placement.id.value}"

        if (placement.hierarchyId != HierarchyId.GENERAL) {
            violations +=
                violation(
                    path = path,
                    code = "UNSUPPORTED_HIERARCHY",
                    message = "Only hierarchy GENERAL is supported in H1",
                )
        }

        if (placement.isDeleted) {
            return@forEach
        }

        if (!isTargetLive(placement.target)) {
            violations +=
                violation(
                    path = path,
                    code = "TARGET_NOT_LIVE",
                    message = "Live placement target must exist and be live",
                )
        }

        val parentId = placement.parentPlacementId ?: return@forEach

        if (parentId == placement.id) {
            violations +=
                violation(
                    path = path,
                    code = "SELF_PARENT",
                    message = "A placement cannot parent itself",
                )
            return@forEach
        }

        val parentCandidates = placementsById[parentId]
        when {
            parentCandidates == null -> {
                violations +=
                    violation(
                        path = path,
                        code = "MISSING_PARENT",
                        message = "Live placement parent ${parentId.value} does not exist",
                    )
            }

            parentCandidates.size != 1 -> {
                violations +=
                    violation(
                        path = path,
                        code = "PARENT_ID_NOT_UNIQUE",
                        message = "Live placement parent ${parentId.value} is not uniquely identified",
                    )
            }

            else -> {
                val parent = parentCandidates.single()
                if (parent.isDeleted) {
                    violations +=
                        violation(
                            path = path,
                            code = "TOMBSTONED_PARENT",
                            message = "Live placement parent ${parentId.value} is tombstoned",
                        )
                }
                if (parent.hierarchyId != placement.hierarchyId) {
                    violations +=
                        violation(
                            path = path,
                            code = "CROSS_HIERARCHY_PARENT",
                            message = "Parent and child placements must belong to the same hierarchy",
                        )
                }
            }
        }
    }

    placements
        .filterNot { it.isDeleted }
        .filter { it.placementKind == PlacementKind.PRIMARY }
        .groupBy { Triple(it.hierarchyId, it.target.type, it.target.id) }
        .filterValues { it.size > 1 }
        .forEach { (key, _) ->
            violations +=
                violation(
                    path = "placements",
                    code = "MULTIPLE_PRIMARY",
                    message = "Target ${key.second}:${key.third} has multiple live PRIMARY placements in ${key.first.value}",
                )
        }

    val uniqueLiveById =
        placementsById
            .mapNotNull { (id, rows) -> rows.singleOrNull()?.takeUnless { it.isDeleted }?.let { id to it } }
            .toMap()

    uniqueLiveById.values.forEach { start ->
        val visited = mutableSetOf<PlacementId>()
        var current: HierarchyPlacement? = start

        while (current != null) {
            if (!visited.add(current.id)) {
                violations +=
                    violation(
                        path = "placements.${start.id.value}",
                        code = "CYCLE",
                        message = "Live placement graph must be acyclic",
                    )
                break
            }

            val parentId = current.parentPlacementId ?: break
            val parent = uniqueLiveById[parentId]
            current =
                if (parent != null && parent.hierarchyId == current.hierarchyId) {
                    parent
                } else {
                    null
                }
        }
    }

    return violations.distinctBy { it.path to it.code }
}

private fun violation(
    path: String,
    code: String,
    message: String,
): HierarchyPlacementViolation =
    HierarchyPlacementViolation(
        path = path,
        code = code,
        message = message,
    )
