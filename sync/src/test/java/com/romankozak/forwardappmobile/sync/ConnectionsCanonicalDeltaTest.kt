package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ContextAttachmentCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceConnectionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionsCanonicalDeltaTest {
    @Test
    fun `CONNECTIONS dirty canonical rows trigger wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceConnections = listOf(connection()),
            ),
        )
    }

    @Test
    fun `CONNECTIONS dirty rows carry orientation and Attachment closure with exact ack`() {
        val dirty = connection()
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
                attachments = listOf(attachment()),
                workspaceConnections = listOf(dirty),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceConnections = listOf(dirty),
            )

        assertEquals(listOf(dirty), plan.snapshotDelta.workspaceConnections)
        assertEquals(listOf(attachment()), plan.snapshotDelta.attachments)
        assertEquals(listOf(workspace()), plan.snapshotDelta.workspaces)
        assertEquals(listOf(capability()), plan.snapshotDelta.workspaceCapabilityInstances)
        assertEquals(1, plan.workspaceConnectionsAck.size)
        assertEquals(dirty.id, plan.workspaceConnectionsAck.single().id)
        assertEquals(dirty.version, plan.workspaceConnectionsAck.single().version)
    }

    @Test
    fun `legacy Context attachment links are not projected after Connections hard cutover`() {
        val tombstone =
            ContextAttachmentCrossRefSnapshot(
                contextId = "context-1",
                attachmentId = "attachment-1",
                attachmentOrder = 0L,
                updatedAt = 500L,
                isDeleted = true,
                version = 3L,
            )

        val delta =
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(version = 2, crossRefs = listOf(tombstone)),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertTrue(delta.crossRefs.isEmpty())
        assertEquals(null, delta.workspaceConnections)
    }

    private fun connection() =
        WorkspaceConnectionSnapshot(
            id = "connection-1",
            workspaceId = "workspace-1",
            capabilityInstanceId = "connections-capability",
            attachmentId = "attachment-1",
            order = 0L,
            createdAt = 100L,
            updatedAt = 120L,
            version = 4L,
            isDeleted = false,
        )

    private fun attachment() =
        AttachmentSnapshot(
            id = "attachment-1",
            entityId = "entity-1",
            attachmentType = "LINK_ITEM",
            ownerContextId = null,
            createdAt = 10L,
            updatedAt = 20L,
            isDeleted = false,
            version = 3L,
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
            id = "connections-capability",
            workspaceId = "workspace-1",
            capabilityType = "CONNECTIONS",
            instanceKey = "default",
            capabilityOrder = 3L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 15L,
            isDeleted = false,
            version = 3L,
        )
}
