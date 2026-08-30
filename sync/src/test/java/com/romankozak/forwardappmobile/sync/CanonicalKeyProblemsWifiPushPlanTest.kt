package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.AttachmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSnapshot
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalKeyProblemsWifiPushPlanTest {
    @Test
    fun `KEY_PROBLEMS only dirty state triggers wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceProblems =
                    CanonicalWorkspaceProblemSyncPayload(
                        problems = listOf(problem()),
                    ),
            ),
        )
    }

    @Test
    fun `KEY_PROBLEMS dirty state carries full closure dependencies and exact ack`() {
        val dirtyProblem = problem()
        val attachment = attachment()
        val attachmentRef = attachmentRef()

        val fullSnapshot =
            SnapshotBundle(
                version = 2,
                attachments = listOf(attachment),
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
                workspaceProblems = listOf(dirtyProblem),
                workspaceProblemWorkspaceRefs = emptyList(),
                workspaceProblemAttachmentRefs = listOf(attachmentRef),
            )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalWorkspaceProblems =
                    CanonicalWorkspaceProblemSyncPayload(
                        problems = listOf(dirtyProblem),
                    ),
            )

        assertEquals(listOf(dirtyProblem), plan.snapshotDelta.workspaceProblems)
        assertTrue(plan.snapshotDelta.workspaceProblemWorkspaceRefs!!.isEmpty())
        assertEquals(
            listOf(attachmentRef),
            plan.snapshotDelta.workspaceProblemAttachmentRefs,
        )

        assertEquals(listOf(workspace()), plan.snapshotDelta.workspaces)
        assertEquals(
            listOf(capability()),
            plan.snapshotDelta.workspaceCapabilityInstances,
        )
        assertEquals(listOf(attachment), plan.snapshotDelta.attachments)

        assertEquals(1, plan.workspaceProblemsAck.problems.size)
        assertEquals(dirtyProblem.id, plan.workspaceProblemsAck.problems.single().id)
        assertEquals(dirtyProblem.version, plan.workspaceProblemsAck.problems.single().version)
        assertTrue(plan.workspaceProblemsAck.workspaceRefs.isEmpty())
        assertTrue(plan.workspaceProblemsAck.attachmentRefs.isEmpty())
    }

    private fun problem() =
        WorkspaceProblemSnapshot(
            id = "problem-1",
            workspaceId = "workspace-1",
            capabilityInstanceId = "key-problems-capability",
            title = "Blocked deployment",
            description = "Investigate canonical sync",
            status = "OPEN",
            order = 0L,
            createdAt = 100L,
            updatedAt = 120L,
            version = 4L,
            isDeleted = false,
        )

    private fun attachmentRef() =
        WorkspaceProblemAttachmentRefSnapshot(
            id = "problem-attachment-ref",
            problemId = "problem-1",
            attachmentId = "attachment-1",
            createdAt = 100L,
            updatedAt = 120L,
            version = 2L,
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
            id = "key-problems-capability",
            workspaceId = "workspace-1",
            capabilityType = "KEY_PROBLEMS",
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

    private fun attachment() =
        AttachmentSnapshot(
            id = "attachment-1",
            entityId = "workspace-1",
            attachmentType = "FILE",
            ownerContextId = "workspace-1",
            createdAt = 100L,
            updatedAt = 120L,
            isDeleted = false,
            version = 1L,
        )
}
