package com.romankozak.forwardappmobile.data.planning

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningCommitmentCompositionRoomTest {
    private fun id(value: String) = PlanningCommitmentId(value)
    private class Fixture(val db: AppDatabase) {
        val planning = CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
        val relations = CanonicalPlanningCommitmentRelationRepository(db)
        val sync = CanonicalPlanningCommitmentRelationSyncStore(db)
    }
    private fun test(block: suspend Fixture.() -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val f = Fixture(db)
            listOf("a", "b", "c", "d").forEachIndexed { n, key ->
                db.orientationDao().upsertManagedSubjects(listOf(ManagedSubjectEntity(key, "ORIENTATION", key, null, 1, 1, null, false, 1)))
                db.orientationDao().upsertOrientations(listOf(OrientationEntity(key, listOf("GOAL", "PROJECT", "ONGOING_STANDARD", "INTENT")[n], null, "UNSET")))
                val scope = PlanningScope(PlanningScopeId("scope-$key"), PlanningScopeKind.entries[n % 3], PlanningScopeLifecycle.ACTIVE,
                    key, null, null, 1, 1, null, 1, false)
                f.planning.saveScope(scope)
                f.planning.saveCommitment(PlanningCommitment(id(key), scope.id, key, PlanningRoleCode.FOCUS, 0, null,
                    PlanningCommitmentStatus.ACTIVE, PlanningProvenance(PlanningProvenanceKind.MANUAL), 1, 1, null, 1, false))
            }
            f.block()
        } finally { db.close() }
    }
    private suspend fun Fixture.link(a: String, b: String) = relations.addRelation(id(a), id(b), now = 10)
    private suspend fun reject(block: suspend () -> Unit) { assertTrue(runCatching { block() }.isFailure) }

    @Test fun `cross scope COMPONENT_OF stores typed relation and not semantic state`() = test {
        val relation = link("a", "b")
        assertEquals(PlanningCommitmentRelationType.COMPONENT_OF, relation.relationType)
        assertEquals(listOf(relation), relations.observeIncoming(id("b")).first())
        assertEquals(4, db.planningDao().getAllCommitments().size)
        assertTrue(db.orientationDao().getAllWorkspaceBindings().isEmpty())
        assertTrue(db.orientationDao().getAllOrientationRelations().isEmpty())
    }
    @Test fun `duplicate add is idempotent`() = test { assertEquals(link("a", "b"), link("a", "b")) }
    @Test fun `self link rejected`() = test { reject { link("a", "a") }; assertTrue(db.planningCommitmentRelationDao().getAll().isEmpty()) }
    @Test fun `direct cycle rejected`() = test { link("a", "b"); reject { link("b", "a") } }
    @Test fun `transitive cross scope cycle rejected`() = test { link("a", "b"); link("b", "c"); reject { link("c", "a") } }
    @Test fun `same scope composition admitted`() = test {
        val original = planning.getCommitment(id("d"))!!
        planning.saveCommitment(original.copy(id = id("same-scope"), scopeId = PlanningScopeId("scope-a")))
        link("same-scope", "a")
    }
    @Test fun `many to many does not duplicate participation`() = test {
        link("a", "b"); link("a", "c"); link("d", "b")
        assertEquals(2, relations.observeOutgoing(id("a")).first().size)
        assertEquals(2, relations.observeIncoming(id("b")).first().size)
        assertEquals(4, db.planningDao().getAllCommitments().size)
    }
    @Test fun `remove tombstones relation only and readd reuses identity`() = test {
        val before = link("a", "b")
        relations.removeRelation(before.id, before.version, 20)
        assertTrue(db.planningCommitmentRelationDao().get(before.id.value)!!.isDeleted)
        assertFalse(planning.getCommitment(id("a"))!!.isDeleted)
        val next = link("a", "b")
        assertEquals(before.id, next.id); assertEquals(3, next.version)
    }
    @Test fun `relation order independent under each parent`() = test {
        val ab = link("a", "b"); val cb = link("c", "b")
        val ac = link("a", "c"); val dc = link("d", "c")
        relations.reorderComponents(id("b"), listOf(cb.id, ab.id), 20)
        assertEquals(listOf("c", "a"), relations.observeIncoming(id("b")).first().map { it.sourceCommitmentId.value })
        assertEquals(listOf(ac.id, dc.id), relations.observeIncoming(id("c")).first().map { it.id })
    }
    @Test fun `invalid reorder writes nothing`() = test {
        val ab = link("a", "b")
        reject { relations.reorderComponents(id("b"), emptyList(), 20) }
        assertEquals(ab, relations.observeIncoming(id("b")).first().single())
    }
    @Test fun `source tombstone retires links atomically`() = test {
        val ab = link("a", "b"); planning.tombstoneCommitment(id("a"), 20)
        assertTrue(db.planningCommitmentRelationDao().get(ab.id.value)!!.isDeleted)
        assertFalse(planning.getCommitment(id("b"))!!.isDeleted)
    }
    @Test fun `target scope tombstone retires all incident links`() = test {
        link("a", "b"); link("b", "c")
        planning.tombstoneScope(PlanningScopeId("scope-b"), 20)
        assertTrue(db.planningCommitmentRelationDao().getAll().all { it.isDeleted })
    }
    @Test fun `nonlive endpoint rejected`() = test { planning.tombstoneCommitment(id("a"), 20); reject { link("a", "b") } }
    @Test fun `all admitted semantic kinds share same relation contract`() = test { link("a", "b"); link("b", "c"); link("c", "d") }
    @Test fun `merge rejects equal version divergence and stale resurrection`() = test {
        val row = link("a", "b").toEntity().toRecord()
        reject { sync.merge(listOf(row.copy(order = 5))) }
        relations.removeRelation(PlanningCommitmentRelationId(row.id), row.version, 20)
        sync.merge(listOf(row))
        assertTrue(db.planningCommitmentRelationDao().get(row.id)!!.isDeleted)
    }
    @Test fun `restore exact metadata and dependent graph`() = test {
        val row = link("a", "b").toEntity().toRecord().copy(syncedAt = 99)
        sync.restore(listOf(row)); assertEquals(row, db.planningCommitmentRelationDao().get(row.id)!!.toRecord())
    }
    @Test fun `invalid multirow merge rolls back all rows`() = test {
        val ab = link("a", "b").toEntity().toRecord()
        val bc = ab.copy(id = "bc", sourceCommitmentId = "b", targetCommitmentId = "c")
        val ca = ab.copy(id = "ca", sourceCommitmentId = "c", targetCommitmentId = "a")
        reject { sync.merge(listOf(bc, ca)) }
        assertEquals(listOf(ab), db.planningCommitmentRelationDao().getAll().map { it.toRecord() })
    }
    @Test fun `exact ACK does not clear later reorder`() = test {
        val ab = link("a", "b"); val cb = link("c", "b")
        relations.reorderComponents(id("b"), listOf(cb.id, ab.id), 20)
        db.planningCommitmentRelationDao().acknowledge(ab.id.value, ab.version, 30)
        assertNull(db.planningCommitmentRelationDao().get(ab.id.value)!!.syncedAt)
        assertEquals(2, db.planningCommitmentRelationDao().changedSince(10).size)
    }
    @Test fun `carried source remaps once and preserves history`() = test {
        val ab = link("a", "b")
        relations.carryRelations(mapOf(id("a") to id("c")), 20)
        relations.carryRelations(mapOf(id("a") to id("c")), 20)
        assertEquals(ab, db.planningCommitmentRelationDao().get(ab.id.value)!!.toDomain())
        assertEquals(2, db.planningCommitmentRelationDao().getAll().size)
        assertEquals("b", relations.observeOutgoing(id("c")).first().single().targetCommitmentId.value)
    }
    @Test fun `both carried endpoints map to replacements`() = test {
        link("a", "b"); relations.carryRelations(mapOf(id("a") to id("c"), id("b") to id("d")), 20)
        assertEquals("d", relations.observeOutgoing(id("c")).first().single().targetCommitmentId.value)
    }
    @Test fun `target only carry fabricates nothing`() = test {
        link("a", "b"); relations.carryRelations(mapOf(id("b") to id("d")), 20)
        assertEquals(1, db.planningCommitmentRelationDao().getAll().size)
    }
    @Test fun `v5 incomplete endpoint deletion merge rolls back commitment and relation together`() = test {
        val edge = link("a", "b")
        val before = planning.getCommitment(id("a"))!!
        val deleted = before.copy(isDeleted = true, status = PlanningCommitmentStatus.DROPPED, version = 2, updatedAt = 20).toTransportSnapshot()
        reject { CanonicalPlanningSyncStore(db).mergeIncoming(emptyList(), listOf(deleted), emptyList()) }
        assertEquals(before, planning.getCommitment(id("a")))
        assertEquals(edge, db.planningCommitmentRelationDao().get(edge.id.value)!!.toDomain())
    }
    @Test fun `older Planning merge retires local incident links in same transaction`() = test {
        val edge = link("a", "b")
        val before = planning.getCommitment(id("a"))!!
        val deleted = before.copy(isDeleted = true, status = PlanningCommitmentStatus.DROPPED, version = 2, updatedAt = 20).toTransportSnapshot()
        CanonicalPlanningSyncStore(db).mergeIncoming(emptyList(), listOf(deleted))
        assertTrue(planning.getCommitment(id("a"))!!.isDeleted)
        assertTrue(db.planningCommitmentRelationDao().get(edge.id.value)!!.isDeleted)
    }
    @Test fun `invalid dependency and unknown relation type fail before any merge write`() = test {
        val row = link("a", "b").toEntity().toRecord()
        reject { sync.merge(listOf(row.copy(id = "missing", targetCommitmentId = "missing"))) }
        reject { sync.merge(listOf(row.copy(relationType = "UNKNOWN", version = 0))) }
        assertEquals(listOf(row), db.planningCommitmentRelationDao().getAll().map { it.toRecord() })
    }
    @Test fun `matching ACK clears only exact relation version`() = test {
        val row = link("a", "b")
        db.planningCommitmentRelationDao().acknowledge(row.id.value, row.version, 99)
        assertEquals(99L, db.planningCommitmentRelationDao().get(row.id.value)!!.syncedAt)
        assertTrue(db.planningCommitmentRelationDao().getUnsynced().isEmpty())
    }
    @Test fun `late relation retirement failure rolls back source deletion`() = test {
        val row = link("a", "b")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_retirement BEFORE UPDATE ON planning_commitment_relations BEGIN SELECT RAISE(ABORT,'test'); END")
        reject { planning.tombstoneCommitment(id("a"), 20) }
        assertFalse(planning.getCommitment(id("a"))!!.isDeleted)
        assertEquals(row, db.planningCommitmentRelationDao().get(row.id.value)!!.toDomain())
    }
    @Test fun `late commitment write failure rolls back already retired incident links`() = test {
        val row = link("a", "b")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_commitment_delete BEFORE UPDATE ON planning_commitments WHEN NEW.isDeleted=1 BEGIN SELECT RAISE(ABORT,'test'); END")
        reject { planning.tombstoneCommitment(id("a"), 20) }
        assertFalse(planning.getCommitment(id("a"))!!.isDeleted)
        assertEquals(row, db.planningCommitmentRelationDao().get(row.id.value)!!.toDomain())
    }
    @Test fun `target commitment tombstone retires relation without deleting source`() = test {
        link("a", "b"); planning.tombstoneCommitment(id("b"), 20)
        assertTrue(db.planningCommitmentRelationDao().getAll().single().isDeleted)
        assertFalse(planning.getCommitment(id("a"))!!.isDeleted)
    }
    @Test fun `bulk projection reacts to relation title and lifecycle changes`() = test {
        val reads = PlanningCompositionReadRepository(db,
            com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()))
        val frames = kotlinx.coroutines.channels.Channel<PlanningCommitmentCompositionProjection>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val job = scope.launch { reads.projection.collect { frames.send(it) } }
        suspend fun until(predicate: (PlanningCommitmentCompositionProjection) -> Boolean): PlanningCommitmentCompositionProjection =
            kotlinx.coroutines.withTimeout(10_000) { var frame = frames.receive(); while (!predicate(frame)) frame = frames.receive(); frame }
        try {
            until { it.participants.size == 4 }
            val edge = link("a", "b")
            until { it.incoming(id("b")).size == 1 }
            val old = db.orientationDao().getManagedSubject("a")!!
            db.orientationDao().upsertManagedSubjects(listOf(old.copy(title = "Renamed", version = 2)))
            until { it.incoming(id("b")).singleOrNull()?.participant?.entity?.title == "Renamed" }
            val participant = planning.getCommitment(id("a"))!!
            planning.saveCommitment(participant.copy(status = PlanningCommitmentStatus.COMPLETED, version = participant.version + 1, updatedAt = 20))
            until { it.incoming(id("b")).singleOrNull()?.participant?.commitment?.status == PlanningCommitmentStatus.COMPLETED }
            relations.removeRelation(edge.id, edge.version, 20)
            until { it.incoming(id("b")).isEmpty() }
        } finally { job.cancel(); frames.close() }
    }
}
