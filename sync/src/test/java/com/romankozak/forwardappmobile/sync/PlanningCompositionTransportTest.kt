package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.PlanningCommitmentRelationRecord
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.datasource.CanonicalPlanningSyncPayload
import org.junit.Assert.*
import org.junit.Test

class PlanningCompositionTransportTest {
    private val row = PlanningCommitmentRelationRecord("edge", "day", "mission", "COMPONENT_OF", 3, 1, 2, null, false, 7)
    private fun full() = SnapshotBundle(version = 2, planningFormatVersion = COMPOSITION_PLANNING_FORMAT_VERSION,
        planningScopes = listOf("day", "mission").map { PlanningScopeSnapshot("scope-$it", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false) },
        planningCommitments = listOf("day", "mission").map { PlanningCommitmentSnapshot(it, "scope-$it", "subject-$it", "FOCUS", 0,
            null, "ACTIVE", "MANUAL", null, null, 1, 1, null, 1, false) }, dayPlanningScopeExtensions = emptyList(),
        tacticalPlanningScopeExtensions = emptyList(), strategicPlanningScopeExtensions = emptyList(),
        planningCommitmentRelations = listOf(row),
        managedSubjects = listOf("day", "mission").map { ManagedSubjectEntity("subject-$it", "ORIENTATION", it, null, 1, 1, null, false, 1) },
        orientations = listOf("day", "mission").map { OrientationEntity("subject-$it", "INTENT", null, "UNSET") })

    @Test fun `composition only dirty state triggers push with full dependency frame and exact ACK`() {
        val dirty = CanonicalPlanningSyncPayload(relations = listOf(row))
        assertTrue(shouldPushCanonicalWifi(true, emptyList(), dirty))
        val snapshot = full()
        val plan = buildCanonicalWifiPushPlan(selection = LocalSyncSelection(), fullSnapshot = snapshot,
            dirtyCanonicalSeries = emptyList(), dirtyCanonicalPlanning = dirty)
        assertEquals(listOf(row), plan.snapshotDelta.planningCommitmentRelations)
        assertEquals(snapshot.planningCommitments, plan.snapshotDelta.planningCommitments)
        assertEquals(snapshot.managedSubjects, plan.snapshotDelta.managedSubjects)
        assertEquals("edge", plan.planningAck.relations.single().id)
        assertEquals(7L, plan.planningAck.relations.single().version)
        assertTrue(plan.planningAck.commitments.isEmpty())
    }
    @Test fun `v5 requires explicit relation collection and older marker cannot claim it`() {
        requireValidCanonicalPlanningPayload(full())
        assertTrue(runCatching { requireValidCanonicalPlanningPayload(full().copy(planningCommitmentRelations = null)) }.isFailure)
        assertTrue(runCatching { requireValidCanonicalPlanningPayload(full().copy(planningFormatVersion = 4)) }.isFailure)
        requireValidCanonicalPlanningPayload(full().copy(planningFormatVersion = 4, planningCommitmentRelations = null))
    }
    @Test fun `snapshot codec preserves exact typed record and tombstone`() {
        val source = full().copy(planningCommitmentRelations = listOf(row.copy(isDeleted = true, syncedAt = 9)))
        val decoded = Gson().fromJson(Gson().toJson(source), SnapshotBundle::class.java)
        assertEquals(source.planningCommitmentRelations, decoded.planningCommitmentRelations)
        requireValidCanonicalPlanningPayload(decoded)
    }
    @Test fun `selective import fails explicitly rather than losing composition closure`() {
        val failure = runCatching { SnapshotBundleSelectiveImportFilter().filter(full(), WorkspaceSelectiveImportSelection()) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("endpoint-selection closure"))
    }
    @Test fun `dirty record changed during snapshot assembly is rejected`() {
        assertTrue(runCatching { buildCanonicalWifiPushPlan(selection = LocalSyncSelection(), fullSnapshot = full(),
            dirtyCanonicalSeries = emptyList(), dirtyCanonicalPlanning = CanonicalPlanningSyncPayload(relations = listOf(row.copy(version = 8)))) }.isFailure)
    }
    @Test fun `composition cannot use an older dependency frame`() {
        assertTrue(runCatching { buildCanonicalWifiPushPlan(selection = LocalSyncSelection(),
            fullSnapshot = full().copy(planningFormatVersion = 4, planningCommitmentRelations = null),
            dirtyCanonicalSeries = emptyList(), dirtyCanonicalPlanning = CanonicalPlanningSyncPayload(relations = listOf(row))) }.isFailure)
    }
    @Test fun `partial endpoint closure cannot be sent as a valid composition delta`() {
        assertTrue(runCatching { buildCanonicalWifiPushPlan(selection = LocalSyncSelection(),
            fullSnapshot = full().copy(planningCommitments = emptyList()), dirtyCanonicalSeries = emptyList(),
            dirtyCanonicalPlanning = CanonicalPlanningSyncPayload(relations = listOf(row))) }.isFailure)
    }
}
