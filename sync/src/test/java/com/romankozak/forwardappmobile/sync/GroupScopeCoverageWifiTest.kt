package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import org.junit.Assert.*
import org.junit.Test

class GroupScopeCoverageWifiTest {
    @Test fun `wire roundtrip preserves modes and absent legacy coverage stays absent`() {
        val gson = com.google.gson.Gson()
        val snapshot = full().copy(groupScopeCoverage = GroupScopeCoverageSnapshot(records =
            listOf("NATIVE", "HISTORICAL", "LEGACY_UNKNOWN").mapIndexed { index, mode ->
                row("p$index", index + 1L).copy(mode = mode)
            }))
        assertEquals(snapshot.groupScopeCoverage,
            gson.fromJson(gson.toJson(snapshot), SnapshotBundle::class.java).groupScopeCoverage)
        assertNull(gson.fromJson("{}", SnapshotBundle::class.java).groupScopeCoverage)
    }

    private fun row(id: String, version: Long) =
        GroupScopeApplicabilityRecord(id, "GENERAL", "NATIVE", 1, 2, null, version)

    private fun full() = SnapshotBundle(
        version = 2,
        hierarchyFormatVersion = CURRENT_HIERARCHY_FORMAT_VERSION,
        groupScopeCoverage = GroupScopeCoverageSnapshot(records = listOf(row("dirty", 2), row("dependency", 1))),
        hierarchyPlacements = emptyList(), hierarchyPlacementGroupScopes = emptyList(),
        hierarchyPlacementLinkedAppearances = emptyList(),
        bigPicturePresentation = BigPicturePresentationSnapshot(),
        managedSubjects = emptyList(), orientations = emptyList(), aspects = emptyList(),
        orientationAssessments = emptyList(), orientationAssessmentRevisions = emptyList(),
        orientationScoringOverrides = emptyList(), legacySubjectMappings = emptyList(),
        orientationRelations = emptyList(), aspectOrientationRefs = emptyList(),
        workspaces = emptyList(), workspaceBindings = emptyList(),
        workspaceCapabilityInstances = emptyList(), savedOrientationViews = emptyList(),
    )

    @Test fun `coverage dirty delta carries dependencies but ACK includes only sent dirty versions`() {
        val dirty = GroupScopeCoverageSnapshot(records = listOf(row("dirty", 2)))
        assertTrue(shouldPushCanonicalWifi(databaseIsEmpty = true, dirtyCanonicalSeries = emptyList(), dirtyGroupScopeCoverage = dirty))
        val plan = buildCanonicalWifiPushPlan(
            selection = LocalSyncSelection(), fullSnapshot = full(), dirtyCanonicalSeries = emptyList(),
            dirtyGroupScopeCoverage = dirty,
        )
        assertEquals(full().groupScopeCoverage, plan.snapshotDelta.groupScopeCoverage)
        assertNotNull(plan.snapshotDelta.hierarchyPlacements)
        assertNotNull(plan.snapshotDelta.hierarchyPlacementGroupScopes)
        assertEquals(listOf(GroupScopeCoverageVersion("dirty", 2)), plan.groupScopeCoverageAck)
    }

    @Test fun `unrelated delta excludes coverage`() {
        assertNull(buildCanonicalSnapshotDelta(SnapshotBundle(), full()).groupScopeCoverage)
    }

    @Test fun `missing or raced coverage cannot be acknowledged`() {
        val dirty = GroupScopeCoverageSnapshot(records = listOf(row("dirty", 2)))
        listOf(full().copy(groupScopeCoverage = null), full().copy(
            groupScopeCoverage = GroupScopeCoverageSnapshot(records = listOf(row("dirty", 3))),
        )).forEach { snapshot ->
            assertThrows(IllegalArgumentException::class.java) {
                buildCanonicalWifiPushPlan(LocalSyncSelection(), snapshot, dirtyCanonicalSeries = emptyList(), dirtyGroupScopeCoverage = dirty)
            }
        }
    }
}
