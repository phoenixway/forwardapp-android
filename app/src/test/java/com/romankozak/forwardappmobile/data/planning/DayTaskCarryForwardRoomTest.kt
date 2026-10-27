package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayTaskCarryForwardRoomTest {
    private suspend fun tomorrow(f: PlanningSourceRoomFixture): DayPlan {
        val plan = DayPlan(id = "tomorrow", date = 86_400_000)
        f.db.dayPlanDao().insert(plan)
        f.db.planningDao().upsertScope(PlanningScopeEntity("tomorrow", "DAY", "DRAFT", null, null, null, 1, 1, null, 1, false))
        f.db.dayPlanningScopeExtensionDao().upsert(DayPlanningScopeExtensionEntity("tomorrow", plan.date, null, null, 1, 1, null, 1, false))
        return plan
    }

    private suspend fun source(f: PlanningSourceRoomFixture): DayTask {
        val id = f.base.orientation()
        return f.day.persist(PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, id)) {
            DayTask(id = "source", dayPlanId = "day", title = "Task", completed = true,
                status = TaskStatus.COMPLETED, completedAt = 10, actualDurationMinutes = 5,
                scheduledTime = 20, version = 1, realizedDayFocusItemIds = listOf("today-focus")).also { f.db.dayTaskDao().insert(it) }
        }
    }

    @Test fun `carry creates new task and participation preserves exact source history and semantic identity`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); val old = source(f)
            val before = f.db.planningDao().getCommitment(dayTaskParticipationId(old.id).value)!!
            val semantics = f.base.dao.getAllManagedSubjects()
            val carried = f.day.carryForward(old, 100) { target }
            val after = f.db.planningDao().getCommitment(dayTaskParticipationId(carried.id).value)!!
            assertThat(carried.id).isNotEqualTo(old.id)
            assertThat(carried.dayPlanId).isEqualTo(target.id)
            assertThat(carried.completed).isFalse()
            assertThat(carried.completedAt).isNull()
            assertThat(carried.scheduledTime).isNull()
            assertThat(carried.actualDurationMinutes).isNull()
            assertThat(carried.version).isEqualTo(1)
            assertThat(carried.syncedAt).isNull()
            assertThat(carried.realizedDayFocusItemIds).isEmpty()
            assertThat(f.db.dayTaskDao().getTaskById(old.id)).isEqualTo(old)
            assertThat(f.db.planningDao().getCommitment(before.id)).isEqualTo(before)
            assertThat(after.orientationId).isEqualTo(before.orientationId)
            assertThat(after.role).isEqualTo(before.role)
            assertThat(after.scopeId).isEqualTo(target.id)
            assertThat(after.provenanceKind).isEqualTo("CARRIED_FORWARD")
            assertThat(after.provenanceSourceType).isEqualTo("PLANNING_COMMITMENT")
            assertThat(after.provenanceSourceId).isEqualTo(before.id)
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(semantics)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
        }
    }

    @Test fun `replay is idempotent and tombstoned carried occurrence never resurrects`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); val old = source(f)
            val first = f.day.carryForward(old, 100) { target }
            val snapshot = f.base.rows()
            assertThat(f.day.carryForward(old, 200) { target }).isEqualTo(first)
            assertThat(f.base.rows()).isEqualTo(snapshot)
            f.day.deleteTask(first.id, 300) { f.db.dayTaskDao().softDelete(first.id, 300) }
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(first.id).value)!!.isDeleted).isTrue()
            val tombstoned = f.base.rows()
            assertThat(runCatching { f.day.carryForward(old, 400) { target } }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(tombstoned)
            assertThat(f.db.dayTaskDao().getTaskById(old.id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `Aspect and Workspace sources retain typed references without manufacturing Orientation`() = runBlocking<Unit> {
        for (kind in listOf(PlanningReferenceTargetKind.ASPECT, PlanningReferenceTargetKind.WORKSPACE)) {
            PlanningSourceRoomFixture().use { f ->
                f.seed(); val target = tomorrow(f)
                val ref = PlanningReferenceTarget(kind, if (kind == PlanningReferenceTargetKind.ASPECT) f.base.aspect() else "workspace")
                val old = f.day.persist(ref) { DayTask(id = "source", dayPlanId = "day", title = "Task").also { f.db.dayTaskDao().insert(it) } }
                val subjects = f.base.dao.getAllManagedSubjects()
                val carried = f.day.carryForward(old, 100) { target }
                val saved = f.db.planningReferenceDao().getForOwner("DAY_TASK", carried.id).single()
                assertThat(saved.targetKind).isEqualTo(kind.name)
                assertThat(saved.targetId).isEqualTo(ref.id)
                assertThat(saved.role).isEqualTo("SOURCE")
                assertThat(f.db.planningDao().getAllCommitments()).isEmpty()
                assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(subjects)
            }
        }
    }

    @Test fun `late participation write failure rolls back destination task and references`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); val old = source(f)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_carry BEFORE INSERT ON planning_commitments WHEN NEW.scopeId='tomorrow' BEGIN SELECT RAISE(ABORT, 'late failure'); END")
            val before = f.base.rows()
            assertThat(runCatching { f.day.carryForward(old, 100) { target } }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `already participating target day rejects carry atomically instead of duplicate live participation`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); val old = source(f)
            val commitment = f.db.planningDao().getCommitment(dayTaskParticipationId(old.id).value)!!.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
            f.planning.saveCommitment(commitment.copy(id = PlanningCommitmentId("other"), scopeId = PlanningScopeId(target.id)))
            val before = f.base.rows()
            assertThat(runCatching { f.day.carryForward(old, 100) { target } }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `carried provenance survives exact restore and peer merge in existing Planning transport`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); f.day.carryForward(source(f), 100) { target }
            val store = CanonicalPlanningSyncStore(f.db)
            val scopes = store.loadAllScopes(); val commitments = store.loadAllCommitments()
            val decoded = store.decodeAndValidateForRestore(scopes, commitments, commitments.mapTo(hashSetOf()) { it.orientationId })
            store.restoreExactDecoded(decoded)
            store.mergeIncoming(scopes, commitments)
            assertThat(store.loadAllCommitments()).isEqualTo(commitments)
        }
    }

    @Test fun `canonical writer rejects moving an existing commitment identity to another day`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val target = tomorrow(f); val old = source(f)
            val original = f.db.planningDao().getCommitment(dayTaskParticipationId(old.id).value)!!
                .toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
            val before = f.base.rows()
            assertThat(runCatching { f.planning.saveCommitment(original.copy(
                scopeId = PlanningScopeId(target.id), version = original.version + 1, updatedAt = 100, syncedAt = null,
            )) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }
}
