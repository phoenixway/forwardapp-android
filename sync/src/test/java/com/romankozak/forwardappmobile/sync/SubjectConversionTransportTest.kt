package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.sync.datasource.*
import org.junit.Assert.*
import org.junit.Test

class SubjectConversionTransportTest {
    private fun assertFails(block: () -> Unit) { assertTrue(runCatching(block).isFailure) }
    private inline fun <reified T : Throwable> assertFailsWith(noinline block: () -> Unit) {
        assertTrue(runCatching(block).exceptionOrNull() is T)
    }
    @Test fun `conversion wire generation roundtrips and old integer-version decoder rejects it`() {
        val codec = com.google.gson.GsonBuilder().registerTypeAdapterFactory(SubjectConversionSnapshotAdapter()).create()
        val snapshot = SnapshotBundle(version = 2, managedSubjectConversions = listOf(row))
        val json = codec.toJson(snapshot)
        assertTrue(json.contains(SubjectConversionSnapshotAdapter.GENERATION))
        assertFails { Gson().fromJson(json, SnapshotBundle::class.java) }
        val decoded = codec.fromJson(json, SnapshotBundle::class.java)
        assertEquals(2, decoded.version)
        assertEquals(listOf(row), decoded.managedSubjectConversions)
        assertFails { codec.fromJson(Gson().toJson(snapshot), SnapshotBundle::class.java) }
        val withoutProof = com.google.gson.JsonParser.parseString(json).asJsonObject.apply { remove("managedSubjectConversions") }
        assertFails { codec.fromJson(withoutProof, SnapshotBundle::class.java) }
        assertFails { codec.fromJson(json.replace("semantic-subject-conversion/1", "semantic-subject-conversion/99"), SnapshotBundle::class.java) }
        assertEquals(2, Gson().fromJson(codec.toJson(SnapshotBundle(version = 2)), SnapshotBundle::class.java).version)
    }
    private val owner = ManagedSubjectEntity("s", "ORIENTATION", "Meaning", null, 1, 20, null, false, 2)
    private val row = ManagedSubjectConversionEntity(managedSubjectConversionId("s", 1), "s", 1, "ASPECT", "ORIENTATION",
        "PROJECT", 1, 2, null, 0, false, 20, 20, 1, null)

    @Test fun `kind generation 2 roundtrips exact structured evidence and cannot be downgraded to generation 1`() {
        val kind = ManagedSubjectConversionEntity("s:ORIENTATION_KIND:2", "s", 2, "ORIENTATION", "ORIENTATION", "ONGOING_STANDARD",
            2, 3, null, 0, false, 30, 30, 1, null, "PROJECT", "[\"revision\"]")
        val codec = com.google.gson.GsonBuilder().registerTypeAdapterFactory(SubjectConversionSnapshotAdapter()).create()
        val snapshot = SnapshotBundle(managedSubjectConversions = listOf(row, kind))
        val raw = codec.toJson(snapshot)
        assertTrue(raw.contains(SubjectConversionSnapshotAdapter.KIND_GENERATION))
        assertEquals(snapshot.managedSubjectConversions, codec.fromJson(raw, SnapshotBundle::class.java).managedSubjectConversions)
        assertFails { Gson().fromJson(raw, SnapshotBundle::class.java) }
        assertFails { codec.fromJson(raw.replace(SubjectConversionSnapshotAdapter.KIND_GENERATION, SubjectConversionSnapshotAdapter.GENERATION), SnapshotBundle::class.java) }
    }

    @Test fun `JSON distinguishes absent history from an authoritative collection`() {
        val codec = Gson()
        val snapshot = SnapshotBundle(managedSubjectConversions = listOf(row))
        assertEquals(listOf(row), codec.fromJson(codec.toJson(snapshot), SnapshotBundle::class.java).managedSubjectConversions)
        assertNull(codec.fromJson("{}", SnapshotBundle::class.java).managedSubjectConversions)
        assertEquals(emptyList<ManagedSubjectConversionEntity>(), codec.fromJson("{\"managedSubjectConversions\":[]}", SnapshotBundle::class.java).managedSubjectConversions)
        assertFailsWith<IllegalArgumentException> { requireValidCanonicalOrientationPayload(snapshot) }
    }

    @Test fun `dirty kind change sends its full owner history while ACK names only the submitted dirty fact`() {
        val kind = ManagedSubjectConversionEntity("s:ORIENTATION_KIND:2", "s", 2, "ORIENTATION", "ORIENTATION", "ONGOING_STANDARD",
            2, 3, null, 0, false, 30, 30, 1, null, "PROJECT", "[\"revision\"]")
        val selected = owner.copy(version = 3)
        val dirty = CanonicalOrientationSyncPayload(managedSubjects = listOf(selected), subjectConversions = listOf(kind))
        val full = SnapshotBundle(version = 2, managedSubjects = listOf(selected), managedSubjectConversions = listOf(row.copy(syncedAt = 21)))
        val plan = buildCanonicalWifiPushPlan(LocalSyncSelection(), full, emptyList(), dirtyCanonicalOrientations = dirty)
        assertEquals(listOf(row.copy(syncedAt = 21), kind), plan.snapshotDelta.managedSubjectConversions)
        assertEquals(listOf(CanonicalOrientationSyncVersion(kind.id, 1)), plan.orientationsAck.subjectConversions)
    }

    @Test fun `dirty-only conversion is sent with its exact ACK even if full frame predates it`() {
        val dirty = CanonicalOrientationSyncPayload(managedSubjects = listOf(owner), subjectConversions = listOf(row))
        val full = SnapshotBundle(version = 2, managedSubjects = listOf(owner), managedSubjectConversions = emptyList())
        val plan = buildCanonicalWifiPushPlan(LocalSyncSelection(), full, emptyList(), dirtyCanonicalOrientations = dirty)
        assertEquals(listOf(row), plan.snapshotDelta.managedSubjectConversions)
        assertEquals(listOf(CanonicalOrientationSyncVersion(row.id, 1)), plan.orientationsAck.subjectConversions)
    }

    @Test fun `conflicting frames and wrong typed owner fail before a delta can be acknowledged`() {
        assertFailsWith<IllegalArgumentException> { subjectConversionsForDelta(listOf(row.copy(previousAspectOrder = 7)), listOf(row), listOf(owner)) }
        assertFailsWith<IllegalArgumentException> { subjectConversionsForDelta(emptyList(), listOf(row), listOf(owner.copy(subjectType = "ASPECT"))) }
        assertFailsWith<IllegalArgumentException> { subjectConversionsForDelta(emptyList(), listOf(row), listOf(owner.copy(version = 1))) }
        assertFailsWith<IllegalArgumentException> { subjectConversionsForDelta(null, listOf(row), null) }
    }

    @Test fun `selective metadata follows only proven current typed owner closure`() {
        val source = SnapshotBundle(managedSubjectConversions = listOf(row))
        assertEquals(listOf(row), SnapshotBundle(managedSubjects = listOf(owner)).withSemanticTagsFrom(source).managedSubjectConversions)
        assertTrue(SnapshotBundle(managedSubjects = listOf(owner.copy(id = "other"))).withSemanticTagsFrom(source).managedSubjectConversions!!.isEmpty())
        assertTrue(SnapshotBundle(managedSubjects = listOf(owner.copy(subjectType = "ASPECT"))).withSemanticTagsFrom(source).managedSubjectConversions!!.isEmpty())
        assertNull(SnapshotBundle(managedSubjects = listOf(owner)).withSemanticTagsFrom(SnapshotBundle()).managedSubjectConversions)
    }

    @Test fun `timestamp export notices a conversion without a tag edit and sends common closure`() {
        val full = SnapshotBundle(managedSubjects = listOf(owner), orientations = emptyList(), aspects = emptyList(),
            orientationAssessments = emptyList(), orientationAssessmentRevisions = emptyList(), legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(), aspectOrientationRefs = emptyList(), workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(), savedOrientationViews = emptyList(), managedSubjectConversions = listOf(row))
        assertEquals(listOf(row), full.semanticTagChangesSince(19).subjectConversions)
        assertEquals(listOf(owner), full.semanticTagChangesSince(19).managedSubjects)
        assertTrue(full.semanticTagChangesSince(20).subjectConversions.isEmpty())
    }
}
