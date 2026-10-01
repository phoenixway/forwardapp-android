package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementIngressBoundary
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.hasLegacyGeneralHierarchyEvidence
import com.romankozak.forwardappmobile.core.data.models.sync.hierarchyPlacementIngressDecision
import com.romankozak.forwardappmobile.core.data.models.sync.requireCanonicalHierarchyRestoreOutput
import com.romankozak.forwardappmobile.core.data.models.sync.requireHierarchyPlacementIngress
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupMemberSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyIngressPolicyTest {
    @Test
    fun `normal merge always requires canonical H1 for legacy hierarchy bearing ingress`() {
        val decision =
            hierarchyPlacementIngressDecision(
                boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            )

        assertTrue(decision.canonicalH1RequiredAtIngress)
        assertFalse(decision.legacyHierarchyTranslationAllowed)

        assertThrows(IllegalArgumentException::class.java) {
            requireHierarchyPlacementIngress(
                boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                canonicalH1Present = false,
                legacyHierarchyBearing = true,
            )
        }
    }

    @Test
    fun `normal merge accepts present H1 including authoritative empty`() {
        listOf(
            listOf(h1()),
            emptyList(),
        ).forEach { h1 ->
            val bundle = SnapshotBundle(hierarchyPlacements = h1)
            requireHierarchyPlacementIngress(
                boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                canonicalH1Present = bundle.hierarchyPlacements != null,
                legacyHierarchyBearing = true,
            )
        }
    }

    @Test
    fun `semantic only legacy relations do not require H1`() {
        val bundle =
            SnapshotBundle(
                mainBeaconGroups =
                    listOf(
                        MainBeaconGroupSnapshot(
                            id = "group",
                            title = "Group",
                            description = null,
                            order = 4L,
                            updatedAt = 2L,
                            createdAt = 1L,
                        ),
                    ),
                mainBeaconGroupMembers =
                    listOf(MainBeaconGroupMemberSnapshot("group", "beacon", 0L)),
                mainBeaconContextCrossRefs =
                    listOf(MainBeaconContextCrossRefSnapshot("beacon", "workspace", 0L)),
            )

        assertFalse(bundle.hasLegacyGeneralHierarchyEvidence())
        requireHierarchyPlacementIngress(
            boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
            authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            canonicalH1Present = false,
            legacyHierarchyBearing = bundle.hasLegacyGeneralHierarchyEvidence(),
        )
    }

    @Test
    fun `restore is sole compatibility translation boundary and canonical output must contain H1`() {
        val restore =
            hierarchyPlacementIngressDecision(
                boundary = HierarchyPlacementIngressBoundary.RESTORE_COMPATIBILITY,
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            )
        val merge =
            hierarchyPlacementIngressDecision(
                boundary = HierarchyPlacementIngressBoundary.NORMAL_MERGE,
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            )

        assertTrue(restore.legacyHierarchyTranslationAllowed)
        assertFalse(restore.canonicalH1RequiredAtIngress)
        assertFalse(merge.legacyHierarchyTranslationAllowed)
        assertTrue(merge.canonicalH1RequiredAtIngress)

        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalHierarchyRestoreOutput(
                authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                canonicalH1Present = false,
            )
        }
        requireCanonicalHierarchyRestoreOutput(
            authorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            canonicalH1Present = true,
        )
    }

    private fun h1() =
        HierarchyPlacementSnapshot(
            id = "p",
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = "w",
            parentPlacementId = null,
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
