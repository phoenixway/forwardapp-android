package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

data class CanonicalHierarchyParityOccurrence(
    val placementId: PlacementId,
    val target: HierarchyTargetRef,
    val parentPlacementId: PlacementId?,
    val occurrencePath: List<PlacementId>,
    val placementKind: PlacementKind,
    val siblingOrder: Long,
    val rootOrder: Int,
    val depth: Int,
)

internal object CanonicalV1ToV2HierarchyParityNormalizer {
    fun expected(
        snapshot: CanonicalV1HierarchySnapshot,
    ): List<CanonicalHierarchyParityOccurrence> {
        val byKey = snapshot.occurrences.associateBy { it.occurrenceKey }
        require(byKey.size == snapshot.occurrences.size) {
            "Parity oracle contains duplicate occurrence keys"
        }

        fun placementId(key: String): PlacementId =
            CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                hierarchyId = snapshot.hierarchyId.value,
                occurrenceKey = key,
            )

        val childrenByParentKey =
            snapshot.occurrences
                .filter { it.parentOccurrenceKey != null }
                .groupBy { requireNotNull(it.parentOccurrenceKey) }

        val occurrenceComparator =
            compareBy<CanonicalV1HierarchyOccurrence> { it.siblingOrder }
                .thenBy { placementId(it.occurrenceKey).value }

        val roots =
            snapshot.occurrences
                .filter { it.parentOccurrenceKey == null }
                .sortedWith(occurrenceComparator)

        val normalized = mutableListOf<CanonicalHierarchyParityOccurrence>()

        fun append(
            occurrence: CanonicalV1HierarchyOccurrence,
            path: List<PlacementId>,
            rootOrder: Int,
        ) {
            val id = placementId(occurrence.occurrenceKey)
            val occurrencePath = path + id

            normalized +=
                CanonicalHierarchyParityOccurrence(
                    placementId = id,
                    target = occurrence.target,
                    parentPlacementId =
                        occurrence.parentOccurrenceKey?.let(::placementId),
                    occurrencePath = occurrencePath,
                    placementKind = occurrence.placementKind,
                    siblingOrder = occurrence.siblingOrder,
                    rootOrder = rootOrder,
                    depth = occurrencePath.lastIndex,
                )

            childrenByParentKey[occurrence.occurrenceKey]
                .orEmpty()
                .sortedWith(occurrenceComparator)
                .forEach { child ->
                    append(
                        occurrence = child,
                        path = occurrencePath,
                        rootOrder = rootOrder,
                    )
                }
        }

        roots.forEachIndexed { rootOrder, root ->
            append(
                occurrence = root,
                path = emptyList(),
                rootOrder = rootOrder,
            )
        }

        return normalized
    }

    fun actual(
        projection: CanonicalV2HierarchyProjection,
    ): List<CanonicalHierarchyParityOccurrence> =
        projection.occurrences.map { occurrence ->
            CanonicalHierarchyParityOccurrence(
                placementId = occurrence.placementId,
                target = occurrence.target,
                parentPlacementId = occurrence.parentPlacementId,
                occurrencePath = occurrence.occurrencePath,
                placementKind = occurrence.placementKind,
                siblingOrder = occurrence.siblingOrder,
                rootOrder = occurrence.rootOrder,
                depth = occurrence.depth,
            )
        }
}
