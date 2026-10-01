package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_HIERARCHY_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyBackupGeneration
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.classifyHierarchyBackupGeneration
import com.romankozak.forwardappmobile.core.data.models.sync.isDeprecatedLegacyCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HierarchyFormatVersionIngressTest {
    private val restore = SnapshotRestoreCanonicalizerImpl()

    @Test
    fun `hierarchy backup generations classify A B C D current and future explicitly`() {
        val preH1 =
            SnapshotBundle(
                hierarchyPlacements = null,
                hierarchyPlacementGroupScopes = null,
                hierarchyPlacementLinkedAppearances = null,
            )
        val earlyH1 =
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = null,
                hierarchyPlacementLinkedAppearances = null,
            )
        val preV177 =
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = null,
            )
        val historicalCanonical =
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )
        val current =
            historicalCanonical.copy(
                hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
            )
        val malformedCurrent =
            preV177.copy(
                hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
            )
        val future =
            historicalCanonical.copy(
                hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION + 1,
            )

        assertEquals(
            HierarchyBackupGeneration.LEGACY_PRE_H1,
            preH1.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.LEGACY_EARLY_H1,
            earlyH1.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.LEGACY_PRE_V177,
            preV177.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.HISTORICAL_CANONICAL,
            historicalCanonical.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.CURRENT_CANONICAL,
            current.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.INVALID_CURRENT_FORMAT,
            malformedCurrent.classifyHierarchyBackupGeneration(),
        )
        assertEquals(
            HierarchyBackupGeneration.UNSUPPORTED_EXPLICIT_VERSION,
            future.classifyHierarchyBackupGeneration(),
        )

        assertEquals(true, preH1.classifyHierarchyBackupGeneration().isDeprecatedLegacyCompatibility)
        assertEquals(true, earlyH1.classifyHierarchyBackupGeneration().isDeprecatedLegacyCompatibility)
        assertEquals(true, preV177.classifyHierarchyBackupGeneration().isDeprecatedLegacyCompatibility)
        assertEquals(false, historicalCanonical.classifyHierarchyBackupGeneration().isDeprecatedLegacyCompatibility)
        assertEquals(false, current.classifyHierarchyBackupGeneration().isDeprecatedLegacyCompatibility)
    }

    @Test
    fun `marker-less historical Restore remains admitted`() {
        val result = restore.canonicalize(SnapshotBundle())

        assertEquals(null, result.hierarchyFormatVersion)
    }

    @Test
    fun `current hierarchy format with complete triplet is admitted by Restore and merge`() {
        val bundle =
            SnapshotBundle(
                hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        restore.canonicalize(bundle)
        requireCanonicalMergeIngress(bundle)
    }

    @Test
    fun `current hierarchy format rejects incomplete triplet before compatibility routing`() {
        val cases =
            listOf(
                SnapshotBundle(
                    hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
                    hierarchyPlacements = emptyList(),
                    hierarchyPlacementGroupScopes = null,
                    hierarchyPlacementLinkedAppearances = emptyList(),
                ),
                SnapshotBundle(
                    hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
                    hierarchyPlacements = null,
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = emptyList(),
                ),
                SnapshotBundle(
                    hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
                    hierarchyPlacements = emptyList(),
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = null,
                ),
            )

        cases.forEach { bundle ->
            assertThrows(IllegalArgumentException::class.java) {
                restore.canonicalize(bundle)
            }
            assertThrows(IllegalArgumentException::class.java) {
                requireCanonicalMergeIngress(bundle)
            }
        }
    }

    @Test
    fun `future hierarchy format fails closed at Restore and merge ingress`() {
        val bundle =
            SnapshotBundle(
                hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION + 1,
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        assertThrows(IllegalArgumentException::class.java) {
            restore.canonicalize(bundle)
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(bundle)
        }
    }
}
