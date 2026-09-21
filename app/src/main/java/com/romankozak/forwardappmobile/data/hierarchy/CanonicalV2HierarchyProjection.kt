package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacementViolation
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.validateProspectiveHierarchy

/**
 * Canonical presentation metadata admitted by the owning target domain before
 * H3 hierarchy projection begins.
 *
 * This deliberately carries no hierarchy parent/order fields. Placement owns
 * topology; target domains own identity and presentation eligibility.
 */
data class CanonicalV2HierarchyTargetPresentation(
    val target: HierarchyTargetRef,
    val title: String,
    /**
     * Stable identity exposed by the current presentation surface.
     *
     * Workspace presentation identity equals target.id. A CUT_OVER legacy
     * Beacon may expose its legacy Beacon id while [target] remains the
     * canonical MANAGED_SUBJECT identity persisted by HierarchyPlacement.
     */
    val presentationId: String = target.id,
)

/**
 * One durable V2 visible occurrence.
 *
 * [placementId] is the occurrence identity after H2 materialization.
 * Repeated [target] values are therefore valid and remain distinct.
 */
data class CanonicalV2HierarchyOccurrence(
    val placementId: PlacementId,
    val target: HierarchyTargetRef,
    val placementKind: PlacementKind,
    val parentPlacementId: PlacementId?,
    val occurrencePath: List<PlacementId>,
    val siblingOrder: Long,
    val rootOrder: Int,
    val depth: Int,
    val presentation: CanonicalV2HierarchyTargetPresentation,
)

data class CanonicalV2HierarchyProjection(
    val hierarchyId: HierarchyId,
    /**
     * Deterministic pre-order flattening of the occurrence graph.
     *
     * This order is intentionally part of H3 parity because CURRENT focus /
     * breadcrumb lookup selects the first visible ProjectLike occurrence for a
     * stable target id.
     */
    val occurrences: List<CanonicalV2HierarchyOccurrence>,
    val rootPlacementIds: List<PlacementId>,
) {
    private val byPlacementId = occurrences.associateBy { it.placementId }

    fun occurrence(placementId: PlacementId): CanonicalV2HierarchyOccurrence? =
        byPlacementId[placementId]
}

class CanonicalV2HierarchyProjectionException(
    val violations: List<HierarchyPlacementViolation>,
) : IllegalStateException(
        violations.joinToString(
            prefix = "Canonical V2 hierarchy projection rejected malformed state: ",
            separator = "; ",
        ) { violation ->
            "${violation.code}@${violation.path}: ${violation.message}"
        },
    )

/**
 * Pure H3 read projector.
 *
 * It consumes only:
 * - persisted HierarchyPlacement state;
 * - independently admitted canonical target presentation.
 *
 * It must never infer hierarchy topology from Workspace.parentWorkspaceId,
 * ContextParentLink, Beacon parent relations, operational-owner relations, or
 * the Canonical V1 snapshot.
 */
class CanonicalV2HierarchyProjector {
    fun project(
        placements: Collection<HierarchyPlacement>,
        admittedTargets: Map<HierarchyTargetRef, CanonicalV2HierarchyTargetPresentation>,
        hierarchyId: HierarchyId = HierarchyId.GENERAL,
    ): CanonicalV2HierarchyProjection {
        require(hierarchyId == HierarchyId.GENERAL) {
            "Canonical V2 H3 currently supports only hierarchy GENERAL"
        }

        val violations =
            validateProspectiveHierarchy(placements) { target ->
                admittedTargets[target]?.target == target
            }

        if (violations.isNotEmpty()) {
            throw CanonicalV2HierarchyProjectionException(violations)
        }

        val livePlacements =
            placements
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.hierarchyId == hierarchyId }
                .toList()

        if (livePlacements.isEmpty()) {
            return CanonicalV2HierarchyProjection(
                hierarchyId = hierarchyId,
                occurrences = emptyList(),
                rootPlacementIds = emptyList(),
            )
        }

        val byId = livePlacements.associateBy { it.id }
        val childrenByParentId =
            livePlacements
                .asSequence()
                .filter { it.parentPlacementId != null }
                .groupBy { requireNotNull(it.parentPlacementId) }
                .mapValues { (_, children) ->
                    children.sortedWith(hierarchyPlacementSiblingComparator)
                }

        val roots =
            livePlacements
                .filter { it.parentPlacementId == null }
                .sortedWith(hierarchyPlacementSiblingComparator)

        val flattened = mutableListOf<CanonicalV2HierarchyOccurrence>()

        fun append(
            placement: HierarchyPlacement,
            path: List<PlacementId>,
            rootOrder: Int,
        ) {
            val presentation =
                requireNotNull(admittedTargets[placement.target]) {
                    "Validated hierarchy target unexpectedly lost admission: ${placement.target}"
                }
            val occurrencePath = path + placement.id

            flattened +=
                CanonicalV2HierarchyOccurrence(
                    placementId = placement.id,
                    target = placement.target,
                    placementKind = placement.placementKind,
                    parentPlacementId = placement.parentPlacementId,
                    occurrencePath = occurrencePath,
                    siblingOrder = placement.siblingOrder,
                    rootOrder = rootOrder,
                    depth = occurrencePath.lastIndex,
                    presentation = presentation,
                )

            childrenByParentId[placement.id].orEmpty().forEach { child ->
                append(
                    placement = child,
                    path = occurrencePath,
                    rootOrder = rootOrder,
                )
            }
        }

        roots.forEachIndexed { rootOrder, root ->
            append(
                placement = root,
                path = emptyList(),
                rootOrder = rootOrder,
            )
        }

        check(flattened.size == byId.size) {
            "Validated live hierarchy did not flatten exactly once per placement"
        }

        return CanonicalV2HierarchyProjection(
            hierarchyId = hierarchyId,
            occurrences = flattened,
            rootPlacementIds = roots.map { it.id },
        )
    }
}
