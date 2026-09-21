package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType

/**
 * Deterministic H2 persistence projection for exact Workspace linked-presentation
 * provenance.
 *
 * This is intentionally a sparse positive side-stream:
 * - a live row means isLinkedAppearance=true for that exact occurrence;
 * - absence means ordinary presentation;
 * - PlacementKind.LINK alone never creates provenance;
 * - provenance is never inherited by structural descendants.
 */
internal fun CanonicalV1HierarchySnapshot.toDeterministicHierarchyPlacementLinkedAppearances(
    now: Long,
): List<HierarchyPlacementLinkedAppearanceEntity> =
    occurrences.mapNotNull { occurrence ->
        if (occurrence.target.type != HierarchyTargetType.WORKSPACE) {
            return@mapNotNull null
        }

        val linkedAppearance =
            occurrence.sourceAuthority ==
                CanonicalV1HierarchySourceAuthority.CONTEXT_PARENT_LINK ||
                occurrence.sourceAuthority ==
                CanonicalV1HierarchySourceAuthority.BEACON_OPERATIONAL_OWNER_PROJECTION

        if (!linkedAppearance) {
            return@mapNotNull null
        }

        HierarchyPlacementLinkedAppearanceEntity(
            placementId =
                CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                    hierarchyId = hierarchyId.value,
                    occurrenceKey = occurrence.occurrenceKey,
                ).value,
            hierarchyId = hierarchyId.value,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
    }

internal fun exactDeterministicHierarchyLinkedAppearanceMatch(
    existing: List<HierarchyPlacementLinkedAppearanceEntity>,
    desired: List<HierarchyPlacementLinkedAppearanceEntity>,
): Boolean {
    if (existing.size != desired.size) return false
    val existingByPlacement = existing.associateBy { it.placementId }

    return desired.all { wanted ->
        val current = existingByPlacement[wanted.placementId] ?: return@all false
        !current.isDeleted &&
            current.hierarchyId == wanted.hierarchyId
    }
}

internal fun buildLinkedAppearanceConflictMessage(
    existing: List<HierarchyPlacementLinkedAppearanceEntity>,
    desired: List<HierarchyPlacementLinkedAppearanceEntity>,
): String {
    val existingIds = existing.mapTo(sortedSetOf()) { it.placementId }
    val desiredIds = desired.mapTo(sortedSetOf()) { it.placementId }
    val extra = (existingIds - desiredIds).take(5)
    val missing = (desiredIds - existingIds).take(5)

    return buildString {
        append("H2 linked-appearance provenance requires an empty table or exact deterministic rerun; ")
        append("existing=${existing.size}, desired=${desired.size}")
        if (extra.isNotEmpty()) append(", extraPlacementIds=$extra")
        if (missing.isNotEmpty()) append(", missingPlacementIds=$missing")
        if (extra.isEmpty() && missing.isEmpty()) {
            append(", same placement ids carry incompatible durable identity")
        }
    }
}
