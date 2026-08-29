package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalOrientationWifiPushPlanTest {
    @Test
    fun `dirty Workspace participates in atomic delta and exact acknowledgement`() {
        val workspace =
            WorkspaceEntity(
                id = "workspace",
                nameOverride = "Engineering",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = "aspect",
                workspaceOrder = 0L,
                createdAt = 10L,
                updatedAt = 20L,
                syncedAt = null,
                isDeleted = false,
                version = 4L,
            )
        val dirty = CanonicalOrientationSyncPayload(workspaces = listOf(workspace))

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = SnapshotBundle(),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalOrientations = dirty,
            )

        assertEquals(listOf(workspace), plan.snapshotDelta.workspaces)
        assertEquals("workspace", plan.orientationsAck.workspaces.single().id)
        assertEquals(4L, plan.orientationsAck.workspaces.single().version)
    }

    @Test
    fun `dirty Orientation triggers atomic delta and exact acknowledgement`() {
        val subject =
            ManagedSubjectEntity(
                id = "subject",
                subjectType = "ORIENTATION",
                title = "Goal",
                description = null,
                createdAt = 10,
                updatedAt = 20,
                syncedAt = null,
                isDeleted = false,
                version = 3,
            )
        val dirty =
            CanonicalOrientationSyncPayload(
                managedSubjects = listOf(subject),
                orientations =
                    listOf(
                        OrientationEntity(
                            subjectId = "subject",
                            kind = "GOAL",
                            lifecycle = null,
                            lifecycleOrigin = "UNSET",
                        ),
                    ),
            )

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalOrientations = dirty,
            ),
        )

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = SnapshotBundle(),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalOrientations = dirty,
            )

        assertEquals(listOf(subject), plan.snapshotDelta.managedSubjects)
        assertEquals(dirty.orientations, plan.snapshotDelta.orientations)
        assertEquals(emptyList<Any>(), plan.snapshotDelta.aspects)
        assertEquals("subject", plan.orientationsAck.managedSubjects.single().id)
        assertEquals(3L, plan.orientationsAck.managedSubjects.single().version)
    }
    @Test
    fun `canonical execution log alone triggers push`() {
        val log = canonicalExecutionLog()

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalExecutionLogs = listOf(log),
            ),
        )
    }

    @Test
    fun `canonical execution log carries full orientation dependency without acknowledging it`() {
        val workspace =
            WorkspaceEntity(
                id = "workspace",
                nameOverride = "Canonical workspace",
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
        val fullSnapshot =
            SnapshotBundle(
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
            )
        val log = canonicalExecutionLog()

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalExecutionLogs = listOf(log),
            )

        assertEquals(listOf(log), plan.snapshotDelta.canonicalExecutionLogs)
        assertEquals(listOf(workspace), plan.snapshotDelta.workspaces)
        assertTrue(plan.orientationsAck.workspaces.isEmpty())
        assertEquals(log.id, plan.executionLogsAck.single().id)
        assertEquals(log.version, plan.executionLogsAck.single().version)
    }

    @Test
    fun `canonical Direction entry alone triggers push`() {
        val entry = canonicalDirectionEntry()

        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDirectionEntries = listOf(entry),
            ),
        )
    }

    @Test
    fun `canonical Direction entry carries full orientation dependency and exact acknowledgement`() {
        val workspace =
            WorkspaceEntity(
                id = "workspace",
                nameOverride = "Canonical workspace",
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
        val fullSnapshot =
            SnapshotBundle(
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
            )
        val entry = canonicalDirectionEntry()

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot = fullSnapshot,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDirectionEntries = listOf(entry),
            )

        assertEquals(listOf(entry), plan.snapshotDelta.workspaceDirectionEntries)
        assertEquals(listOf(workspace), plan.snapshotDelta.workspaces)
        assertTrue(plan.orientationsAck.workspaces.isEmpty())
        assertEquals(entry.id, plan.directionEntriesAck.single().id)
        assertEquals(entry.version, plan.directionEntriesAck.single().version)
    }

    private fun canonicalDirectionEntry() =
        WorkspaceDirectionEntrySnapshot(
            id = "direction-entry",
            workspaceId = "workspace",
            capabilityInstanceId = "direction-capability",
            orientationId = null,
            targetWorkspaceId = "workspace",
            labelOverride = "Shortcut",
            entryOrder = 0L,
            provenance = "CANONICAL_ONLY",
            createdAt = 100L,
            updatedAt = 120L,
            version = 4L,
            isDeleted = false,
        )

    private fun canonicalExecutionLog() =
        CanonicalExecutionLogSnapshot(
            id = "log",
            workspaceId = "workspace",
            timestamp = 100L,
            type = "COMMENT",
            description = "Canonical event",
            details = null,
            updatedAt = 120L,
            version = 4L,
            isDeleted = false,
        )

}
