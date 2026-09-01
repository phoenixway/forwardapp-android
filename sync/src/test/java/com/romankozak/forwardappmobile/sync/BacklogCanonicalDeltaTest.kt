package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogCanonicalDeltaTest {
    @Test
    fun `BACKLOG dirty canonical rows trigger wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceBacklog = listOf(entry()),
            ),
        )
    }

    @Test
    fun `BACKLOG delta carries owner target closure and exact ack without legacy rows`() {
        val dirty = entry()
        val checklist = checklist()
        val checklistItem = checklistItem()
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
                workspaces = listOf(workspace()),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = listOf(capability()),
                savedOrientationViews = emptyList(),
                checklists = listOf(checklist),
                checklistItems = listOf(checklistItem),
                workspaceBacklogEntries = listOf(dirty),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = full,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceBacklog = listOf(dirty),
            )

        assertEquals(listOf(dirty), plan.snapshotDelta.workspaceBacklogEntries)
        assertEquals(listOf(checklist), plan.snapshotDelta.checklists)
        assertEquals(listOf(checklistItem), plan.snapshotDelta.checklistItems)
        assertEquals(listOf(workspace()), plan.snapshotDelta.workspaces)
        assertEquals(listOf(capability()), plan.snapshotDelta.workspaceCapabilityInstances)
        assertTrue(plan.snapshotDelta.backlogItems.isEmpty())
        assertTrue(plan.snapshotDelta.backlogOrders.isEmpty())
        assertEquals(dirty.id, plan.workspaceBacklogAck.single().id)
        assertEquals(dirty.version, plan.workspaceBacklogAck.single().version)
    }

    @Test
    fun `legacy Backlog delta is suppressed after canonical cutover`() {
        val delta =
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(version = 2),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertTrue(delta.backlogItems.isEmpty())
        assertTrue(delta.backlogOrders.isEmpty())
        assertEquals(null, delta.workspaceBacklogEntries)
    }

    private fun entry() =
        WorkspaceBacklogEntrySnapshot(
            id = "placement-1",
            workspaceId = "workspace-1",
            capabilityInstanceId = "backlog-capability",
            targetKind = "CHECKLIST",
            targetId = "checklist-1",
            order = 0L,
            createdAt = 10L,
            updatedAt = 20L,
            version = 3L,
            isDeleted = false,
        )

    private fun checklist() =
        ChecklistSnapshot(
            id = "checklist-1",
            name = "Checklist",
            contextId = null,
            createdAt = 1L,
            updatedAt = 2L,
            version = 1L,
            isDeleted = false,
        )

    private fun checklistItem() =
        ChecklistItemSnapshot(
            id = "item-1",
            checklistId = "checklist-1",
            text = "Item",
            isChecked = false,
            order = 0,
            updatedAt = 2L,
            version = 1L,
            isDeleted = false,
        )

    private fun workspace() =
        WorkspaceEntity(
            id = "workspace-1",
            nameOverride = "Workspace",
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

    private fun capability() =
        WorkspaceCapabilityInstanceEntity(
            id = "backlog-capability",
            workspaceId = "workspace-1",
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
