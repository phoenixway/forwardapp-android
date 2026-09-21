package com.romankozak.forwardappmobile.core.data.models.sync

/**
 * Hierarchy V2 authority state.
 *
 * Production remains CURRENT_PRE_CUTOVER until P2 explicitly switches the
 * coherent hierarchy slice. V2_AUTHORITY is readiness-only for now.
 */
enum class HierarchyPlacementAuthorityMode {
    CURRENT_PRE_CUTOVER,
    V2_AUTHORITY,
}

/**
 * Single dormant production authority seam for H4 readiness.
 *
 * H4.0c must not activate P2. A future change here is not sufficient by
 * itself: P2 still requires the coordinated reader, writer, and transport
 * authority cutover.
 */
fun currentHierarchyPlacementAuthorityMode(): HierarchyPlacementAuthorityMode =
    HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER

enum class HierarchyPlacementIngressBoundary {
    NORMAL_MERGE,
    RESTORE_COMPATIBILITY,
}

enum class LegacyGeneralHierarchyEvidence {
    WORKSPACES,
    CONTEXT_COMPATIBILITY_ROWS,
    CONTEXT_PARENT_LINKS,
    MAIN_BEACONS,
    MAIN_BEACON_PARENT_LINKS,
}

/**
 * V1 transport capable of mutating visible GENERAL occurrence topology.
 *
 * Deliberately excluded: Group PART_OF membership, Beacon operational-owner
 * association, WorkspaceBinding, and OrientationRelation.
 */
fun SnapshotBundle.legacyGeneralHierarchyEvidence(): Set<LegacyGeneralHierarchyEvidence> =
    buildSet {
        if (workspaces.orEmpty().isNotEmpty()) {
            add(LegacyGeneralHierarchyEvidence.WORKSPACES)
        }
        if (contexts.isNotEmpty()) {
            add(LegacyGeneralHierarchyEvidence.CONTEXT_COMPATIBILITY_ROWS)
        }
        if (contextParentLinks.isNotEmpty()) {
            add(LegacyGeneralHierarchyEvidence.CONTEXT_PARENT_LINKS)
        }
        if (mainBeacons.isNotEmpty()) {
            add(LegacyGeneralHierarchyEvidence.MAIN_BEACONS)
        }
        if (mainBeaconParentLinks.isNotEmpty()) {
            add(LegacyGeneralHierarchyEvidence.MAIN_BEACON_PARENT_LINKS)
        }
    }

fun SnapshotBundle.hasLegacyGeneralHierarchyEvidence(): Boolean =
    legacyGeneralHierarchyEvidence().isNotEmpty()

data class HierarchyPlacementIngressDecision(
    val canonicalH1RequiredAtIngress: Boolean,
    val legacyHierarchyTranslationAllowed: Boolean,
)

fun hierarchyPlacementIngressDecision(
    boundary: HierarchyPlacementIngressBoundary,
    authorityMode: HierarchyPlacementAuthorityMode,
): HierarchyPlacementIngressDecision =
    when (boundary) {
        HierarchyPlacementIngressBoundary.NORMAL_MERGE ->
            HierarchyPlacementIngressDecision(
                canonicalH1RequiredAtIngress =
                    authorityMode == HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                legacyHierarchyTranslationAllowed = false,
            )

        HierarchyPlacementIngressBoundary.RESTORE_COMPATIBILITY ->
            HierarchyPlacementIngressDecision(
                canonicalH1RequiredAtIngress = false,
                legacyHierarchyTranslationAllowed =
                    authorityMode == HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            )
    }

/**
 * In dormant V2_AUTHORITY mode, hierarchy-bearing normal ingress must carry H1.
 * Presence is authoritative: [] is present/empty; null is absent.
 */
fun requireHierarchyPlacementIngress(
    boundary: HierarchyPlacementIngressBoundary,
    authorityMode: HierarchyPlacementAuthorityMode,
    canonicalH1Present: Boolean,
    legacyHierarchyBearing: Boolean,
) {
    val policy = hierarchyPlacementIngressDecision(boundary, authorityMode)
    require(
        !policy.canonicalH1RequiredAtIngress ||
            !legacyHierarchyBearing ||
            canonicalH1Present,
    ) {
        "Canonical Hierarchy H1 is required for hierarchy-bearing normal ingress " +
            "after V2 hierarchy authority activation"
    }
}

fun requireCanonicalHierarchyRestoreOutput(
    authorityMode: HierarchyPlacementAuthorityMode,
    canonicalH1Present: Boolean,
) {
    require(
        authorityMode != HierarchyPlacementAuthorityMode.V2_AUTHORITY ||
            canonicalH1Present,
    ) {
        "Canonicalized Restore must contain Canonical Hierarchy H1 after V2 authority activation"
    }
}
