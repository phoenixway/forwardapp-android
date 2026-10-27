package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncPayload
import org.junit.Assert.*
import org.junit.Test

class SemanticTagTransportTest {
    private fun tag(type: ManagedSubjectType = ManagedSubjectType.ORIENTATION, id: String = "s") =
        ManagedSubjectTagRefEntity(managedSubjectTagId(ManagedSubjectRef(type, id), "health"), type.name, id, "health", 1, 20, null, false, 3)

    @Test fun `semantic tag only dirty payload triggers push and exact tag ACK`() {
        val row = tag()
        val dirty = CanonicalOrientationSyncPayload(semanticTagRefs = listOf(row),
            managedSubjects = listOf(ManagedSubjectEntity("s", "ORIENTATION", "Meaning", null, 1, 1, null, false, 1)))
        assertTrue(shouldPushCanonicalWifi(true, emptyList(), dirtyCanonicalOrientations = dirty))
        val plan = buildCanonicalWifiPushPlan(selection = LocalSyncSelection(), fullSnapshot = SnapshotBundle(version = 2),
            dirtyCanonicalSeries = emptyList(), dirtyCanonicalOrientations = dirty)
        assertEquals(listOf(row), plan.snapshotDelta.managedSubjectTagRefs)
        assertEquals(row.id, plan.orientationsAck.semanticTagRefs.single().id)
        assertEquals(3L, plan.orientationsAck.semanticTagRefs.single().version)
        assertTrue(plan.orientationsAck.workspaceTagRefs.isEmpty())
    }
    @Test fun `changedSince retains acknowledged tag changes and owner graph closure`() {
        val row = tag().copy(syncedAt = 25)
        val owner = ManagedSubjectEntity("s", "ORIENTATION", "Meaning", null, 1, 1, null, false, 1)
        val full = SnapshotBundle(version = 2, managedSubjectTagRefs = listOf(row), managedSubjects = listOf(owner),
            orientations = listOf(OrientationEntity("s", "INTENT", null, "UNSET")), aspects = emptyList(), orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(), legacySubjectMappings = emptyList(), orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(), workspaceBindings = emptyList(), workspaceCapabilityInstances = emptyList(), savedOrientationViews = emptyList())
        val changed = full.semanticTagChangesSince(10)
        assertEquals(listOf(row), changed.semanticTagRefs)
        assertEquals(full.managedSubjects, changed.managedSubjects)
        assertEquals("s", changed.managedSubjects.single().id)
        assertTrue(full.semanticTagChangesSince(20).semanticTagRefs.isEmpty())
        val delta = buildCanonicalSnapshotDelta(SnapshotBundle(version = 2), full, explicitCanonicalOrientations = changed)
        assertEquals(listOf(row), delta.managedSubjectTagRefs)
    }
    @Test fun `selective closure keeps only exact typed selected subjects metadata`() {
        val orientation = ManagedSubjectEntity("s", "ORIENTATION", "Subject", null, 1, 1, null, false, 1)
        val selected = SnapshotBundle(version = 2, managedSubjects = listOf(orientation))
        val row = tag()
        val source = SnapshotBundle(version = 2, managedSubjectTagRefs = listOf(row, tag(ManagedSubjectType.ASPECT), tag(id = "other")))
        assertEquals(listOf(row), selected.withSemanticTagsFrom(source).managedSubjectTagRefs)
        assertTrue(SnapshotBundle(version = 2).withSemanticTagsFrom(source).managedSubjectTagRefs!!.isEmpty())
        assertNull(selected.withSemanticTagsFrom(SnapshotBundle(version = 2)).managedSubjectTagRefs)
    }
    @Test fun `JSON carries live and tombstoned rows without borrowing Workspace authority`() {
        val snapshot = SnapshotBundle(version = 2, managedSubjectTagRefs = listOf(tag(), tag(id = "deleted").copy(isDeleted = true)), workspaceTagRefs = emptyList())
        val decoded = Gson().fromJson(Gson().toJson(snapshot), SnapshotBundle::class.java)
        assertEquals(snapshot.managedSubjectTagRefs, decoded.managedSubjectTagRefs)
        assertTrue(decoded.workspaceTagRefs!!.isEmpty())
    }
    @Test fun `later full frame cannot add a tag outside the selected owner closure`() {
        val row = tag()
        val owner = ManagedSubjectEntity("s", "ORIENTATION", "Meaning", null, 1, 1, null, false, 1)
        val dirty = CanonicalOrientationSyncPayload(managedSubjects = listOf(owner), semanticTagRefs = listOf(row))
        val full = SnapshotBundle(version = 2, managedSubjects = listOf(owner, owner.copy(id = "new")),
            managedSubjectTagRefs = listOf(row, tag(id = "new")))
        val plan = buildCanonicalWifiPushPlan(LocalSyncSelection(), full, emptyList(), dirtyCanonicalOrientations = dirty)
        assertEquals(listOf(owner), plan.snapshotDelta.managedSubjects)
        assertEquals(listOf(row), plan.snapshotDelta.managedSubjectTagRefs)
    }
    @Test fun `older full frame cannot replace dirty newer version while acknowledging it`() {
        val row = tag()
        val owner = ManagedSubjectEntity("s", "ORIENTATION", "Meaning", null, 1, 1, null, false, 1)
        val dirty = CanonicalOrientationSyncPayload(managedSubjects = listOf(owner), semanticTagRefs = listOf(row))
        val full = SnapshotBundle(version = 2, managedSubjectTagRefs = listOf(row.copy(version = 2)))
        val plan = buildCanonicalWifiPushPlan(LocalSyncSelection(), full, emptyList(), dirtyCanonicalOrientations = dirty)
        assertEquals(3L, plan.snapshotDelta.managedSubjectTagRefs!!.single().version)
        assertEquals(3L, plan.orientationsAck.semanticTagRefs.single().version)
    }
}
