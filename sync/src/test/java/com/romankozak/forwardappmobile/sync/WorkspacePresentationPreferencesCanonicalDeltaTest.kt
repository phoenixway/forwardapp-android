package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspacePresentationPreferencesSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePresentationPreferencesCanonicalDeltaTest {
    @Test
    fun `presentation preferences dirty canonical row triggers wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspacePresentationPreferences = listOf(preferences()),
            ),
        )
    }

    @Test
    fun `presentation preferences delta carries Workspace closure and exact ack`() {
        val dirty = preferences()
        val workspace = workspace()
        val fullSnapshot =
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
                workspaces = listOf(workspace),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = emptyList(),
                savedOrientationViews = emptyList(),
                workspacePresentationPreferences = listOf(dirty),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspacePresentationPreferences = listOf(dirty),
            )

        assertEquals(listOf(dirty), plan.snapshotDelta.workspacePresentationPreferences)
        assertEquals(listOf(workspace), plan.snapshotDelta.workspaces)
        assertEquals(1, plan.workspacePresentationPreferencesAck.size)
        assertEquals(dirty.workspaceId, plan.workspacePresentationPreferencesAck.single().workspaceId)
        assertEquals(dirty.version, plan.workspacePresentationPreferencesAck.single().version)
    }

    @Test
    fun `presentation preferences remain absent from delta when no canonical row is dirty`() {
        val delta =
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(version = 2),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertNull(delta.workspacePresentationPreferences)
    }

    private fun preferences() =
        WorkspacePresentationPreferencesSnapshot(
            workspaceId = "workspace-1",
            defaultViewModeName = "BACKLOG",
            isAttachmentsExpanded = true,
            showCheckboxes = true,
            createdAt = 100L,
            updatedAt = 120L,
            version = 4L,
        )

    private fun workspace() =
        WorkspaceSnapshot(
            id = "workspace-1",
            nameOverride = "Workspace",
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 15L,
            isDeleted = false,
            version = 3L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )
}
