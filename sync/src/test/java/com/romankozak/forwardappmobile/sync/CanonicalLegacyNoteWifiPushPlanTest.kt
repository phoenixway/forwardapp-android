package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalLegacyNoteWifiPushPlanTest {
    @Test
    fun `FK10B3c standalone Note push requires no Context or placement`() {
        val selected = note("standalone", "absent-historical-context")
        val unrelated = note("unrelated", selected.contextId)

        val plan =
            buildCanonicalWifiPushPlan(
                selection =
                    LocalSyncSelection(
                        notes = listOf(LocalSyncVersion(selected.id, selected.version)),
                    ),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        notes = listOf(selected, unrelated),
                    ),
                dirtyCanonicalSeries = emptyList(),
            )

        assertEquals(listOf(selected), plan.snapshotDelta.notes)
        assertTrue(plan.snapshotDelta.contexts.isEmpty())
        assertTrue(plan.snapshotDelta.workspaces.orEmpty().isEmpty())
        assertTrue(plan.snapshotDelta.workspaceBacklogEntries.orEmpty().isEmpty())
        assertTrue(plan.workspaceBacklogAck.isEmpty())
    }

    @Test
    fun `FK10B3c standalone Note tombstone is independently transported`() {
        val deleted = note("deleted", null, deleted = true)
        val unrelated = note("unrelated", null)

        val plan =
            buildCanonicalWifiPushPlan(
                selection =
                    LocalSyncSelection(
                        notes = listOf(LocalSyncVersion(deleted.id, deleted.version)),
                    ),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        notes = listOf(deleted, unrelated),
                    ),
                dirtyCanonicalSeries = emptyList(),
            )

        assertEquals(listOf(deleted), plan.snapshotDelta.notes)
        assertTrue(plan.snapshotDelta.notes.single().isDeleted)
        assertTrue(plan.snapshotDelta.contexts.isEmpty())
        assertTrue(plan.snapshotDelta.workspaceBacklogEntries.orEmpty().isEmpty())
    }

    @Test
    fun `FK10B3c dirty Note with two placements is emitted only once`() {
        val shared = note("shared", "retired-context")
        val first = placement("placement-a", "workspace-a", shared.id)
        val second = placement("placement-b", "workspace-b", shared.id)
        val dirty = listOf(first, second)

        val plan =
            buildCanonicalWifiPushPlan(
                selection =
                    LocalSyncSelection(
                        notes = listOf(LocalSyncVersion(shared.id, shared.version)),
                    ),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        // Required canonical capability-content collections.
                        managedSubjects = emptyList(),
                        orientations = emptyList(),
                        aspects = emptyList(),
                        orientationAssessments = emptyList(),
                        orientationAssessmentRevisions = emptyList(),
                        orientationScoringOverrides = emptyList(),
                        legacySubjectMappings = emptyList(),
                        orientationRelations = emptyList(),
                        aspectOrientationRefs = emptyList(),
                        workspaceBindings = emptyList(),
                        savedOrientationViews = emptyList(),
                        notes = listOf(shared),
                        workspaces = listOf(workspace("workspace-a"), workspace("workspace-b")),
                        workspaceCapabilityInstances =
                            listOf(capability("workspace-a"), capability("workspace-b")),
                        workspaceBacklogEntries = dirty,
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceBacklog = dirty,
            )

        assertEquals(listOf(shared), plan.snapshotDelta.notes)
        assertEquals(dirty.map { it.id }.toSet(), plan.snapshotDelta.workspaceBacklogEntries.orEmpty().map { it.id }.toSet())
        assertEquals(dirty.map { it.id }.toSet(), plan.workspaceBacklogAck.map { it.id }.toSet())
        assertTrue(plan.snapshotDelta.contexts.isEmpty())
    }

    @Test
    fun `FK10B3c placement dependency includes Note but not unrelated historical Notes`() {
        val selected = note("selected", "shared-historical-context")
        val unrelated = note("unplaced", "shared-historical-context")
        val first = placement("placement-a", "workspace-a", selected.id)
        val second = placement("placement-b", "workspace-b", selected.id)
        val dirty = listOf(first, second)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        // Required canonical capability-content collections.
                        managedSubjects = emptyList(),
                        orientations = emptyList(),
                        aspects = emptyList(),
                        orientationAssessments = emptyList(),
                        orientationAssessmentRevisions = emptyList(),
                        orientationScoringOverrides = emptyList(),
                        legacySubjectMappings = emptyList(),
                        orientationRelations = emptyList(),
                        aspectOrientationRefs = emptyList(),
                        workspaceBindings = emptyList(),
                        savedOrientationViews = emptyList(),
                        notes = listOf(selected, unrelated),
                        workspaces = listOf(workspace("workspace-a"), workspace("workspace-b")),
                        workspaceCapabilityInstances =
                            listOf(capability("workspace-a"), capability("workspace-b")),
                        workspaceBacklogEntries = dirty,
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceBacklog = dirty,
            )

        assertEquals(listOf(selected), plan.snapshotDelta.notes)
        assertEquals(2, plan.snapshotDelta.workspaceBacklogEntries.orEmpty().size)
        assertTrue(plan.snapshotDelta.contexts.isEmpty())
    }

    @Test
    fun `FK10B3c unchanged unselected Note does not enter WiFi push`() {
        val unchanged = note("unchanged", null)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        notes = listOf(unchanged),
                    ),
                dirtyCanonicalSeries = emptyList(),
            )

        assertTrue(plan.snapshotDelta.notes.isEmpty())
        assertTrue(plan.snapshotDelta.contexts.isEmpty())
        assertTrue(plan.workspaceBacklogAck.isEmpty())
    }

    private fun note(
        id: String,
        contextId: String?,
        deleted: Boolean = false,
    ) = LegacyNoteSnapshot(
        id = id,
        contextId = contextId,
        title = "Title-$id",
        content = "Body-$id",
        createdAt = 10L,
        updatedAt = 20L,
        isDeleted = deleted,
        version = 3L,
    )

    private fun placement(
        id: String,
        workspaceId: String,
        noteId: String,
    ) = WorkspaceBacklogEntrySnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = "backlog-$workspaceId",
        targetKind = "LEGACY_NOTE",
        targetId = noteId,
        order = 0L,
        createdAt = 10L,
        updatedAt = 20L,
        version = 3L,
        isDeleted = false,
    )

    private fun workspace(id: String) =
        WorkspaceSnapshot(
            id = id,
            nameOverride = "Workspace $id",
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = 1L,
            isDeleted = false,
            version = 1L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )

    private fun capability(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "backlog-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = "BACKLOG",
            instanceKey = "default",
            capabilityOrder = 0L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = 1L,
            isDeleted = false,
            version = 1L,
        )
}
