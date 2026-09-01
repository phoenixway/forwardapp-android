package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceInboxRecordSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxCanonicalDeltaTest {
    @Test
    fun `INBOX dirty canonical rows trigger wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceInbox = listOf(record()),
            ),
        )
    }

    @Test
    fun `INBOX dirty canonical rows carry orientation closure and exact ack`() {
        val dirty = record()
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
                workspaces = listOf(workspace()),
                workspaceBindings = emptyList(),
                workspaceCapabilityInstances = listOf(capability()),
                savedOrientationViews = emptyList(),
                workspaceInboxRecords = listOf(dirty),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceInbox = listOf(dirty),
            )

        assertEquals(listOf(dirty), plan.snapshotDelta.workspaceInboxRecords)
        assertEquals(listOf(workspace()), plan.snapshotDelta.workspaces)
        assertEquals(listOf(capability()), plan.snapshotDelta.workspaceCapabilityInstances)
        assertEquals(1, plan.workspaceInboxAck.size)
        assertEquals(dirty.id, plan.workspaceInboxAck.single().id)
        assertEquals(dirty.version, plan.workspaceInboxAck.single().version)
    }

    @Test
    fun `legacy inbox rows are not projected into canonical delta after hard cutover`() {
        val tombstone =
            InboxRecordSnapshot(
                id = "legacy-inbox",
                contextId = "context-1",
                text = "deleted inbox record",
                createdAt = 100L,
                order = -100L,
                updatedAt = 500L,
                hideInOwnerInbox = false,
                isDeleted = true,
                version = 3L,
            )

        val delta =
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(version = 2, inbox = listOf(tombstone)),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertTrue(delta.inbox.isEmpty())
        assertEquals(null, delta.workspaceInboxRecords)
    }

    private fun record() =
        WorkspaceInboxRecordSnapshot(
            id = "inbox-1",
            workspaceId = "workspace-1",
            capabilityInstanceId = "inbox-capability",
            text = "Inbox item",
            order = 0L,
            createdAt = 100L,
            updatedAt = 120L,
            version = 4L,
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
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 15L,
            isDeleted = false,
            version = 3L,
            provenance = "CONTEXT_BACKED",
            sourceContextId = "workspace-1",
        )

    private fun capability() =
        WorkspaceCapabilityInstanceEntity(
            id = "inbox-capability",
            workspaceId = "workspace-1",
            capabilityType = "INBOX",
            instanceKey = "default",
            capabilityOrder = 3L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{\"ownerVisibility\":\"KEEP_VISIBLE\"}",
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 15L,
            isDeleted = false,
            version = 3L,
        )
}
