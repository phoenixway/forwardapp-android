package com.romankozak.forwardappmobile.core.data.models.sync

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
): HierarchyPlacementIngressDecision =
    when (boundary) {
        HierarchyPlacementIngressBoundary.NORMAL_MERGE ->
            HierarchyPlacementIngressDecision(
                canonicalH1RequiredAtIngress = true,
                legacyHierarchyTranslationAllowed = false,
            )

        HierarchyPlacementIngressBoundary.RESTORE_COMPATIBILITY ->
            HierarchyPlacementIngressDecision(
                canonicalH1RequiredAtIngress = false,
                legacyHierarchyTranslationAllowed = true,
            )
    }

/**
 * Under V2_AUTHORITY, hierarchy-bearing normal ingress must carry H1.
 * Presence is authoritative: [] is present/empty; null is absent.
 */
fun requireHierarchyPlacementIngress(
    boundary: HierarchyPlacementIngressBoundary,
    canonicalH1Present: Boolean,
    legacyHierarchyBearing: Boolean,
) {
    val policy = hierarchyPlacementIngressDecision(boundary)
    require(
        !policy.canonicalH1RequiredAtIngress ||
            !legacyHierarchyBearing ||
            canonicalH1Present,
    ) {
        "Canonical Hierarchy H1 is required for hierarchy-bearing normal ingress " +
            "after V2 hierarchy authority activation"
    }
}

fun requireCanonicalHierarchyRestoreOutput(canonicalH1Present: Boolean) {
    require(canonicalH1Present) {
        "Canonicalized Restore must contain Canonical Hierarchy H1 after V2 authority activation"
    }
}
