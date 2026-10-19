package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.datasource.CanonicalPlanningSyncPayload
import org.junit.Assert.*
import org.junit.Test

class PlanningReferenceTransportTest {
    private fun reference(version: Long = 3) = PlanningReferenceEntity("reference", "DAY_TASK", "task", "ORIENTATION",
        "orientation", "SOURCE", 1, 2, null, version, false)

    @Test fun `reference-only changes trigger push and acknowledge only exact dirty reference version`() {
        val row = reference()
        val dirty = CanonicalPlanningSyncPayload(references = listOf(row))
        assertTrue(shouldPushCanonicalWifi(true, emptyList(), dirty))
        val full = SnapshotBundle(version = 2, planningReferences = listOf(row),
            planningScopes = emptyList(), planningCommitments = emptyList(), managedSubjects = emptyList(), orientations = emptyList())
        val plan = buildCanonicalWifiPushPlan(selection = LocalSyncSelection(), fullSnapshot = full,
            dirtyCanonicalSeries = emptyList(), dirtyCanonicalPlanning = dirty)
        assertEquals(listOf(row), plan.snapshotDelta.planningReferences)
        assertEquals(full.planningScopes, plan.snapshotDelta.planningScopes)
        assertEquals(full.managedSubjects, plan.snapshotDelta.managedSubjects)
        assertEquals("reference", plan.planningAck.references.single().id)
        assertEquals(3L, plan.planningAck.references.single().version)
        assertTrue(plan.planningAck.scopes.isEmpty())
        assertTrue(plan.planningAck.commitments.isEmpty())
    }

    @Test fun `selecting an entity does not implicitly import Planning ownership or references`() {
        val source = SnapshotBundle(version = 2, planningReferences = listOf(reference()))
        val filtered = SnapshotBundleSelectiveImportFilter().filter(source, WorkspaceSelectiveImportSelection())
        assertNull(filtered.planningReferences)
        assertNull(filtered.planningScopes)
        assertNull(filtered.planningCommitments)
    }

    @Test fun `reference closure carries tombstone but never synthetic legacy parent links`() {
        val row = reference().copy(isDeleted = true, version = 4)
        val full = SnapshotBundle(version = 2, planningReferences = listOf(row))
        val closure = planningReferenceTransportClosure(full, listOf(row))
        assertEquals(listOf(row), closure.planningReferences)
        assertTrue(closure.contextParentLinks.isEmpty())
        assertTrue(closure.mainBeaconParentLinks.isEmpty())
        assertTrue(closure.backlogItems.isEmpty())
        assertTrue(closure.backlogOrders.isEmpty())
    }
}
