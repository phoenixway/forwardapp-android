package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalPlanningReferenceRoomTest {
    private val owner = PlanningReferenceOwner(PlanningReferenceOwnerKind.SCOPE, "day")
    private suspend fun scope(f: SemanticDeletionRoomFixture) {
        f.db.planningDao().upsertScope(PlanningScopeEntity("day", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
    }

    @Test fun `three target classes attach without identity participation or structural mutation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            scope(f)
            val orientation = f.orientation()
            val aspect = f.aspect()
            f.workspace()
            val before = f.rows()
            val repository = CanonicalPlanningReferenceRepository(f.db)
            listOf(PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientation),
                PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, aspect),
                PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "workspace")).forEach { repository.attach(owner, it, now = 20) }
            assertThat(f.rows().filter { (table, rows) -> before[table] != rows }.keys).containsExactly("planning_references")
            assertThat(f.db.planningReferenceDao().getAll()).hasSize(3)
        }
    }

    @Test fun `SOURCE and RELATED remain distinct and repeated attach is idempotent`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            scope(f)
            val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, f.orientation())
            val repo = CanonicalPlanningReferenceRepository(f.db)
            val id = repo.attach(owner, source, PlanningReferenceRole.SOURCE, 10)
            assertThat(repo.attach(owner, source, PlanningReferenceRole.SOURCE, 30)).isEqualTo(id)
            repo.attach(owner, source, PlanningReferenceRole.RELATED, 30)
            assertThat(f.db.planningReferenceDao().getAll()).hasSize(2)
            val other = PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, f.aspect())
            assertThat(runCatching { repo.attach(owner, other, PlanningReferenceRole.SOURCE, 40) }.isFailure).isTrue()
            assertThat(f.db.planningReferenceDao().getAll()).hasSize(2)
        }
    }

    @Test fun `removal preserves canonical target and stale ACK cannot acknowledge a newer tombstone`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            scope(f)
            val subject = f.orientation()
            val repo = CanonicalPlanningReferenceRepository(f.db)
            val id = repo.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, subject), now = 20)
            repo.remove(id, 1, 30)
            f.db.planningReferenceDao().acknowledge(id, 1, 40)
            val row = f.db.planningReferenceDao().get(id)!!
            assertThat(row.isDeleted).isTrue()
            assertThat(row.version).isEqualTo(2)
            assertThat(row.syncedAt).isNull()
            assertThat(f.dao.getManagedSubject(subject)!!.isDeleted).isFalse()
            assertThat(f.db.planningReferenceDao().getChangedSince(25)).containsExactly(row)
        }
    }

    @Test fun `JSON restore merge equal divergence and stale tombstone replay are safe`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            scope(f)
            val repo = CanonicalPlanningReferenceRepository(f.db)
            val id = repo.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, f.orientation()), now = 20)
            val old = f.db.planningReferenceDao().getAll()
            val bundle = Gson().fromJson(Gson().toJson(SnapshotBundle(planningReferences = old)), SnapshotBundle::class.java)
            val sync = CanonicalPlanningReferenceSyncStore(f.db)
            sync.restore(bundle.planningReferences)
            sync.merge(bundle.planningReferences)
            assertThat(runCatching { sync.merge(old.map { it.copy(updatedAt = 99) }) }.isFailure).isTrue()
            repo.remove(id, 1, 30)
            sync.merge(old)
            assertThat(f.db.planningReferenceDao().get(id)!!.isDeleted).isTrue()
            val tombstones = f.db.planningReferenceDao().getAll()
            sync.restore(tombstones)
            assertThat(f.db.planningReferenceDao().getAll()).isEqualTo(tombstones)
        }
    }

    @Test fun `missing deleted mismatched typed targets and missing owners reject without writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            scope(f)
            val subject = f.orientation()
            val repo = CanonicalPlanningReferenceRepository(f.db)
            val before = f.rows()
            assertThat(runCatching { repo.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, subject)) }.isFailure).isTrue()
            assertThat(runCatching { repo.attach(owner.copy(id = "missing"), PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, subject)) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            f.delete(subject)
            assertThat(runCatching { repo.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, subject)) }.isFailure).isTrue()
        }
    }
}
