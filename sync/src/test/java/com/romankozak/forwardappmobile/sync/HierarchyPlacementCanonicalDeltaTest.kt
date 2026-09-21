package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyPlacementCanonicalDeltaTest {
    @Test
    fun `dirty H1 row triggers wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacements = listOf(child()),
            ),
        )
    }

    @Test
    fun `dirty child sends complete H1 graph target closure and exact dirty ACK`() {
        val parent = parent()
        val child = child()
        val full =
            SnapshotBundle(
                version = 2,
                managedSubjects = emptyList(),
                orientations = emptyList(),
                aspects = emptyList(),
                orientationAssessments = emptyList(),
                orientationAssessmentRevisions = emptyList(),
                legacySubjectMappings = emptyList(),
                orientationRelations = emptyList(),
                aspectOrientationRefs = emptyList(),
                workspaces =
                    listOf(
                        workspace("workspace-parent"),
                        workspace("workspace-child"),
                    ),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                hierarchyPlacements = listOf(parent, child),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacements = listOf(child),
            )

        assertEquals(listOf(parent, child), plan.snapshotDelta.hierarchyPlacements)
        assertEquals(full.workspaces, plan.snapshotDelta.workspaces)
        assertEquals(1, plan.hierarchyPlacementsAck.size)
        assertEquals(child.id, plan.hierarchyPlacementsAck.single().id)
        assertEquals(child.version, plan.hierarchyPlacementsAck.single().version)
    }

    @Test
    fun `dirty H1 sends complete coupled Group scope stream`() {
        val parent = parent()
        val child = child()
        val scope = groupScope("parent")

        val full =
            SnapshotBundle(
                version = 2,
                managedSubjects = emptyList(),
                orientations = emptyList(),
                aspects = emptyList(),
                orientationAssessments = emptyList(),
                orientationAssessmentRevisions = emptyList(),
                legacySubjectMappings = emptyList(),
                orientationRelations = emptyList(),
                aspectOrientationRefs = emptyList(),
                workspaces =
                    listOf(
                        workspace("workspace-parent"),
                        workspace("workspace-child"),
                    ),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                hierarchyPlacements = listOf(parent, child),
                hierarchyPlacementGroupScopes = listOf(scope),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacements = listOf(child),
            )

        assertEquals(listOf(parent, child), plan.snapshotDelta.hierarchyPlacements)
        assertEquals(listOf(scope), plan.snapshotDelta.hierarchyPlacementGroupScopes)
        assertEquals(1, plan.hierarchyPlacementsAck.size)
        assertTrue(plan.hierarchyPlacementGroupScopesAck.isEmpty())
    }

    @Test
    fun `dirty Group scope sends complete H1 and scopes but ACKs only dirty scope`() {
        val parent = parent()
        val child = child()
        val scope = groupScope("parent")

        val full =
            SnapshotBundle(
                version = 2,
                managedSubjects = emptyList(),
                orientations = emptyList(),
                aspects = emptyList(),
                orientationAssessments = emptyList(),
                orientationAssessmentRevisions = emptyList(),
                legacySubjectMappings = emptyList(),
                orientationRelations = emptyList(),
                aspectOrientationRefs = emptyList(),
                workspaces =
                    listOf(
                        workspace("workspace-parent"),
                        workspace("workspace-child"),
                    ),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                hierarchyPlacements = listOf(parent, child),
                hierarchyPlacementGroupScopes = listOf(scope),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacementGroupScopes = listOf(scope),
            ),
        )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacementGroupScopes = listOf(scope),
            )

        assertEquals(listOf(parent, child), plan.snapshotDelta.hierarchyPlacements)
        assertEquals(listOf(scope), plan.snapshotDelta.hierarchyPlacementGroupScopes)
        assertTrue(plan.hierarchyPlacementsAck.isEmpty())
        assertEquals(1, plan.hierarchyPlacementGroupScopesAck.size)
        assertEquals(
            scope.placementId,
            plan.hierarchyPlacementGroupScopesAck.single().placementId,
        )
        assertEquals(
            scope.version,
            plan.hierarchyPlacementGroupScopesAck.single().version,
        )
    }

    @Test
    fun `dirty linked appearance triggers wifi push and sends complete hierarchy trio`() {
        val parent = parent()
        val child = child()
        val linked = linkedAppearance("child")

        val full =
            SnapshotBundle(
                version = 2,
                managedSubjects = emptyList(),
                orientations = emptyList(),
                aspects = emptyList(),
                orientationAssessments = emptyList(),
                orientationAssessmentRevisions = emptyList(),
                legacySubjectMappings = emptyList(),
                orientationRelations = emptyList(),
                aspectOrientationRefs = emptyList(),
                workspaces =
                    listOf(
                        workspace("workspace-parent"),
                        workspace("workspace-child"),
                    ),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                hierarchyPlacements = listOf(parent, child),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = listOf(linked),
            )

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacementLinkedAppearances = listOf(linked),
            ),
        )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacementLinkedAppearances = listOf(linked),
            )

        assertEquals(listOf(parent, child), plan.snapshotDelta.hierarchyPlacements)
        assertEquals(
            emptyList<HierarchyPlacementGroupScopeSnapshot>(),
            plan.snapshotDelta.hierarchyPlacementGroupScopes,
        )
        assertEquals(
            listOf(linked),
            plan.snapshotDelta.hierarchyPlacementLinkedAppearances,
        )
        assertTrue(plan.hierarchyPlacementsAck.isEmpty())
        assertTrue(plan.hierarchyPlacementGroupScopesAck.isEmpty())
        assertEquals(1, plan.hierarchyPlacementLinkedAppearancesAck.size)
        assertEquals(
            linked.placementId,
            plan.hierarchyPlacementLinkedAppearancesAck.single().placementId,
        )
        assertEquals(
            linked.version,
            plan.hierarchyPlacementLinkedAppearancesAck.single().version,
        )
    }

    @Test
    fun `dirty H1 carries authoritative empty linked appearance stream`() {
        val parent = parent()
        val child = child()
        val full =
            SnapshotBundle(
                version = 2,
                managedSubjects = emptyList(),
                orientations = emptyList(),
                aspects = emptyList(),
                orientationAssessments = emptyList(),
                orientationAssessmentRevisions = emptyList(),
                legacySubjectMappings = emptyList(),
                orientationRelations = emptyList(),
                aspectOrientationRefs = emptyList(),
                workspaces =
                    listOf(
                        workspace("workspace-parent"),
                        workspace("workspace-child"),
                    ),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                hierarchyPlacements = listOf(parent, child),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalHierarchyPlacements = listOf(child),
            )

        assertEquals(
            emptyList<HierarchyPlacementLinkedAppearanceSnapshot>(),
            plan.snapshotDelta.hierarchyPlacementLinkedAppearances,
        )
    }

    private fun linkedAppearance(placementId: String) =
        HierarchyPlacementLinkedAppearanceSnapshot(
            placementId = placementId,
            hierarchyId = "GENERAL",
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            isDeleted = false,
            version = 2L,
        )

    private fun groupScope(placementId: String) =
        HierarchyPlacementGroupScopeSnapshot(
            placementId = placementId,
            hierarchyId = "GENERAL",
            groupSubjectId = null,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            isDeleted = false,
            version = 2L,
        )

    private fun parent() =
        HierarchyPlacementSnapshot(
            id = "parent",
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = "workspace-parent",
            parentPlacementId = null,
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 10L,
            updatedAt = 10L,
            syncedAt = 9L,
            isDeleted = false,
            version = 1L,
        )

    private fun child() =
        HierarchyPlacementSnapshot(
            id = "child",
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = "workspace-child",
            parentPlacementId = "parent",
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 11L,
            updatedAt = 20L,
            syncedAt = null,
            isDeleted = false,
            version = 2L,
        )

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )
}
