package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.DayPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.TacticalPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.StrategicPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.sync.datasource.CanonicalPlanningSyncPayload
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalPlanningWifiPushPlanTest {
    @Test
    fun `Planning only dirty state triggers wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(
                        scopes = listOf(scope(version = 3L)),
                    ),
            ),
        )
    }

    @Test
    fun `empty Planning state does not force wifi push`() {
        assertFalse(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning = CanonicalPlanningSyncPayload(),
            ),
        )
    }

    @Test
    fun `Planning delta is atomic and exact ack covers only dirty rows`() {
        val dependencyScope = scope(version = 4L)
        val dirtyCommitment = commitment(version = 7L)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    fullPlanningSnapshot(
                        scopes = listOf(dependencyScope),
                        commitments = listOf(dirtyCommitment),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(
                        commitments = listOf(dirtyCommitment),
                    ),
            )

        assertEquals(CURRENT_PLANNING_FORMAT_VERSION, plan.snapshotDelta.planningFormatVersion)
        assertNotNull(plan.snapshotDelta.planningScopes)
        assertNotNull(plan.snapshotDelta.planningCommitments)
        assertNotNull(plan.snapshotDelta.dayPlanningScopeExtensions)
        assertNotNull(plan.snapshotDelta.tacticalPlanningScopeExtensions)
        assertNotNull(plan.snapshotDelta.strategicPlanningScopeExtensions)
        assertEquals(listOf(dependencyScope), plan.snapshotDelta.planningScopes)
        assertEquals(listOf(dirtyCommitment), plan.snapshotDelta.planningCommitments)

        assertTrue(plan.planningAck.scopes.isEmpty())
        assertEquals(1, plan.planningAck.commitments.size)
        assertEquals(dirtyCommitment.id, plan.planningAck.commitments.single().id)
        assertEquals(dirtyCommitment.version, plan.planningAck.commitments.single().version)
    }

    @Test
    fun `dirty Day extension includes scope dependency and ACKs only extension version`() {
        val dependencyScope = scope(kind = "DAY")
        val extension = dayExtension(version = 9L)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    fullPlanningSnapshot(
                        scopes = listOf(dependencyScope),
                        commitments = emptyList(),
                        dayExtensions = listOf(extension),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(dayScopeExtensions = listOf(extension)),
            )

        assertEquals(listOf(dependencyScope), plan.snapshotDelta.planningScopes)
        assertEquals(listOf(extension), plan.snapshotDelta.dayPlanningScopeExtensions)
        assertTrue(plan.planningAck.scopes.isEmpty())
        assertEquals(9L, plan.planningAck.dayScopeExtensions.single().version)
    }

    @Test
    fun `dirty Tactical extension includes scope dependency and ACKs only extension version`() {
        val dependencyScope = scope()
        val extension = tacticalExtension(version = 11L)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    fullPlanningSnapshot(
                        scopes = listOf(dependencyScope),
                        commitments = emptyList(),
                        tacticalExtensions = listOf(extension),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(tacticalScopeExtensions = listOf(extension)),
            )

        assertEquals(listOf(dependencyScope), plan.snapshotDelta.planningScopes)
        assertEquals(listOf(extension), plan.snapshotDelta.tacticalPlanningScopeExtensions)
        assertTrue(plan.planningAck.scopes.isEmpty())
        assertEquals(11L, plan.planningAck.tacticalScopeExtensions.single().version)
    }

    @Test
    fun `dirty Strategic extension includes scope dependency and ACKs only extension version`() {
        val dependencyScope = scope(kind = "STRATEGIC_ARC")
        val extension = strategicExtension(version = 13L)

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    fullPlanningSnapshot(
                        scopes = listOf(dependencyScope),
                        commitments = emptyList(),
                        strategicExtensions = listOf(extension),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(strategicScopeExtensions = listOf(extension)),
            )

        assertEquals(listOf(dependencyScope), plan.snapshotDelta.planningScopes)
        assertEquals(listOf(extension), plan.snapshotDelta.strategicPlanningScopeExtensions)
        assertTrue(plan.planningAck.scopes.isEmpty())
        assertEquals(13L, plan.planningAck.strategicScopeExtensions.single().version)
    }

    @Test
    fun `Planning commitment carries Orientation dependency closure without acknowledging it`() {
        val dirtyCommitment = commitment()
        val subject = orientationSubject()
        val orientation = orientation()

        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    fullPlanningSnapshot(
                        scopes = listOf(scope()),
                        commitments = listOf(dirtyCommitment),
                        managedSubjects = listOf(subject),
                        orientations = listOf(orientation),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalPlanning =
                    CanonicalPlanningSyncPayload(
                        commitments = listOf(dirtyCommitment),
                    ),
            )

        assertEquals(listOf(subject), plan.snapshotDelta.managedSubjects)
        assertEquals(listOf(orientation), plan.snapshotDelta.orientations)
        assertTrue(plan.orientationsAck.managedSubjects.isEmpty())
        assertTrue(plan.orientationsAck.assessments.isEmpty())
        assertEquals(dirtyCommitment.id, plan.planningAck.commitments.single().id)
    }

    @Test
    fun `non Planning canonical delta keeps Planning transport absent`() {
        val delta =
            buildCanonicalSnapshotDelta(
                baseDelta = SnapshotBundle(version = 2),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertNull(delta.planningFormatVersion)
        assertNull(delta.planningScopes)
        assertNull(delta.planningCommitments)
        assertNull(delta.dayPlanningScopeExtensions)
        assertNull(delta.tacticalPlanningScopeExtensions)
        assertNull(delta.strategicPlanningScopeExtensions)
    }

    private fun fullPlanningSnapshot(
        scopes: List<PlanningScopeSnapshot>,
        commitments: List<PlanningCommitmentSnapshot>,
        managedSubjects: List<ManagedSubjectEntity> = emptyList(),
        orientations: List<OrientationEntity> = emptyList(),
        dayExtensions: List<DayPlanningScopeExtensionSnapshot> = emptyList(),
        tacticalExtensions: List<TacticalPlanningScopeExtensionSnapshot> = emptyList(),
        strategicExtensions: List<StrategicPlanningScopeExtensionSnapshot> = emptyList(),
    ) =
        SnapshotBundle(
            version = 2,
            planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
            planningScopes = scopes,
            planningCommitments = commitments,
            dayPlanningScopeExtensions = dayExtensions,
            tacticalPlanningScopeExtensions = tacticalExtensions,
            strategicPlanningScopeExtensions = strategicExtensions,
            managedSubjects = managedSubjects,
            orientations = orientations,
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

    private fun scope(
        version: Long = 1L,
        kind: String = "TACTICAL_CYCLE",
    ) =
        PlanningScopeSnapshot(
            id = SCOPE_ID,
            kind = kind,
            lifecycle = "ACTIVE",
            title = "Cycle",
            startsAt = 100L,
            endsAt = 200L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun dayExtension(version: Long = 1L) =
        DayPlanningScopeExtensionSnapshot(
            scopeId = SCOPE_ID,
            calendarDayStartMillis = 100L,
            outcome = null,
            plannedBudgetMinutes = 480L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun tacticalExtension(version: Long = 1L) =
        TacticalPlanningScopeExtensionSnapshot(
            scopeId = SCOPE_ID,
            iterationType = "TIMEBOXED",
            weekKey = "2026-W41",
            closedAt = null,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun strategicExtension(version: Long = 1L) =
        StrategicPlanningScopeExtensionSnapshot(
            scopeId = SCOPE_ID,
            legacyArcKey = "2026-10",
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun commitment(version: Long = 1L) =
        PlanningCommitmentSnapshot(
            id = COMMITMENT_ID,
            scopeId = SCOPE_ID,
            orientationId = ORIENTATION_ID,
            role = "DAY.THEME",
            order = 0L,
            priorityLevel = null,
            status = "PLANNED",
            provenanceKind = "IMPORTED",
            provenanceSourceType = "wifi-test",
            provenanceSourceId = "source-1",
            createdAt = 11L,
            updatedAt = 30L,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun orientationSubject() =
        ManagedSubjectEntity(
            id = ORIENTATION_ID,
            subjectType = "ORIENTATION",
            title = "Orientation",
            description = null,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 3L,
        )

    private fun orientation() =
        OrientationEntity(
            subjectId = ORIENTATION_ID,
            kind = "GOAL",
            lifecycle = null,
            lifecycleOrigin = "UNSET",
        )

    private companion object {
        const val SCOPE_ID = "scope-1"
        const val COMMITMENT_ID = "commitment-1"
        const val ORIENTATION_ID = "orientation-1"
    }
}
