package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_HIERARCHY_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureRecordVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BigPictureWifiDeltaTest {
    @Test
    fun `presentation only mutation triggers push and transports dependencies`() {
        val dirtyRow = section("dirty", 4)
        val unchanged = section("unchanged", 2)
        val full = canonicalFull(
            BigPicturePresentationSnapshot(
                containers = listOf(dirtyRow, unchanged),
            ),
        )
        val dirty = BigPicturePresentationSnapshot(
            containers = listOf(dirtyRow),
        )

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyBigPicturePresentation = dirty,
            ),
        )

        val plan = buildCanonicalWifiPushPlan(
            selection = LocalSyncSelection(),
            fullSnapshot = full,
            dirtyCanonicalSeries = emptyList(),
            dirtyBigPicturePresentation = dirty,
        )

        assertEquals(full.bigPicturePresentation, plan.snapshotDelta.bigPicturePresentation)
        assertNotNull(plan.snapshotDelta.hierarchyPlacements)
        assertNotNull(plan.snapshotDelta.hierarchyPlacementGroupScopes)
        assertNotNull(plan.snapshotDelta.hierarchyPlacementLinkedAppearances)
        assertEquals(
            listOf(BigPictureRecordVersion("dirty", 4)),
            plan.bigPicturePresentationAck.containers,
        )
        assertFalse(
            plan.bigPicturePresentationAck.containers.any { it.id == "unchanged" },
        )
    }

    @Test
    fun `unrelated delta never inherits full presentation`() {
        val full = canonicalFull(
            BigPicturePresentationSnapshot(
                containers = listOf(section("unrelated", 1)),
            ),
        )
        val delta = buildCanonicalSnapshotDelta(
            baseDelta = SnapshotBundle(),
            fullSnapshot = full,
        )
        assertNull(delta.bigPicturePresentation)
        assertFalse(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
            ),
        )
    }

    @Test
    fun `tombstone travels while unchanged dependencies stay outside ACK`() {
        val deleted = section("deleted", 7).copy(isDeleted = true)
        val dependency = section("dependency", 3)
        val dirty = BigPicturePresentationSnapshot(containers = listOf(deleted))
        val full = canonicalFull(
            BigPicturePresentationSnapshot(
                containers = listOf(deleted, dependency),
            ),
        )
        val plan = buildCanonicalWifiPushPlan(
            selection = LocalSyncSelection(),
            fullSnapshot = full,
            dirtyCanonicalSeries = emptyList(),
            dirtyBigPicturePresentation = dirty,
        )
        assertTrue(
            plan.snapshotDelta.bigPicturePresentation!!.containers
                .first { it.id == "deleted" }.isDeleted,
        )
        assertEquals(
            listOf(BigPictureRecordVersion("deleted", 7)),
            plan.bigPicturePresentationAck.containers,
        )
    }

    @Test
    fun `concurrent version change rejects mixed snapshot`() {
        val old = section("section", 2)
        val new = old.copy(version = 3)
        assertThrows(IllegalArgumentException::class.java) {
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(),
                fullSnapshot = canonicalFull(
                    BigPicturePresentationSnapshot(containers = listOf(new)),
                ),
                explicitBigPicturePresentation =
                    BigPicturePresentationSnapshot(containers = listOf(old)),
            )
        }
    }

    private fun section(id: String, version: Long) =
        BigPictureContainerSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            title = id,
            anchorPlacementId = null,
            parentContainerId = null,
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = version,
        )

    private fun canonicalFull(presentation: BigPicturePresentationSnapshot) =
        SnapshotBundle(
            version = 2,
            hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
            bigPicturePresentation = presentation,
            hierarchyPlacements = emptyList(),
            hierarchyPlacementGroupScopes = emptyList(),
            hierarchyPlacementLinkedAppearances = emptyList(),
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            orientationScoringOverrides = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = emptyList(),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
        )
}
