package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.sync.mergeDayTaskRealizations
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.planning.day.*
import com.romankozak.forwardappmobile.data.recurrence.toCanonicalDayTask
import com.romankozak.forwardappmobile.data.recurrence.toAndroidDayTask
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayTaskRealizationRoomTest {
    private suspend fun seed(f: SemanticDeletionRoomFixture): DayTask {
        f.db.dayPlanDao().insertAll(listOf(DayPlan(id = "d", date = 1), DayPlan(id = "other", date = 2)))
        f.db.dayFocusItemDao().insertAll(listOf(DayFocusItem("focus", "d", "Focus"),
            DayFocusItem("responsibility", "d", "Responsibility", type = DayFocusType.RESPONSIBILITY), DayFocusItem("wrong-day", "other", "Not today")))
        val task = DayTask(id = "t", dayPlanId = "d", title = "Task", notes = "Journal", description = "Instruction", syncedAt = 1, version = 1)
        f.db.dayTaskDao().insert(task)
        return task
    }
    @Test fun `multiple same-day typed priorities persist reactively with task lifecycle only`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val original = seed(f); val repo = DayTaskRealizationRepository(f.db)
            val before = f.rows().filterKeys { it != "day_tasks" }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repo.observeTask("t").stateIn(scope)
                repo.toggle("t", "focus", 20); repo.toggle("t", "responsibility", 21)
                val linked = withTimeout(5_000) { state.first { it?.realizedDayFocusItemIds?.size == 2 } }!!
                assertThat(linked.realizedDayFocusItemIds).containsExactly("focus", "responsibility")
                assertThat(linked.version).isEqualTo(3); assertThat(linked.syncedAt).isNull()
                assertThat(linked.copy(realizedDayFocusItemIds = emptyList(), updatedAt = original.updatedAt, syncedAt = original.syncedAt, version = original.version)).isEqualTo(original)
                assertThat(f.rows().filterKeys { it != "day_tasks" }).isEqualTo(before)
                assertThat(f.db.dayTaskDao().markSyncedIfVersionMatches("t", 1, 99)).isEqualTo(0)
                repo.toggle("t", "focus", 30)
                assertThat(f.db.dayTaskDao().getTaskById("t")!!.realizedDayFocusItemIds).containsExactly("responsibility")
                assertThat(f.db.dayFocusItemDao().getItemsForDayPlan("d").first()).hasSize(2)
            } finally { scope.cancel() }
        }
    }
    @Test fun `cross-day missing and deleted targets fail atomically while removal never deletes target`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            seed(f); val repo = DayTaskRealizationRepository(f.db)
            val before = f.rows()
            assertThat(runCatching { repo.toggle("t", "wrong-day") }.isFailure).isTrue()
            assertThat(runCatching { repo.toggle("t", "missing") }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            repo.toggle("t", "focus")
            f.db.dayFocusItemDao().softDelete("focus", 30)
            val task = f.db.dayTaskDao().getTaskById("t")!!
            assertThat(liveTaskPriorities(task, f.db.dayFocusItemDao().getItemsForDayPlanSync("d"))).isEmpty()
            repo.toggle("t", "focus") // Historical stale ref may be removed even when endpoint is deleted.
            assertThat(f.db.dayTaskDao().getTaskById("t")!!.realizedDayFocusItemIds).isEmpty()
            assertThat(f.db.dayFocusItemDao().getByIdForCanonicalRecurrenceSync("focus")!!.isDeleted).isTrue()
        }
    }
    @Test fun `deleted task cannot acquire refs and no title or raw ID inference occurs`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val original = seed(f); val repo = DayTaskRealizationRepository(f.db)
            assertThat(liveTaskPriorities(original, f.db.dayFocusItemDao().getItemsForDayPlan("d").first())).isEmpty()
            f.db.dayTaskDao().update(original.copy(isDeleted = true))
            assertThat(runCatching { repo.toggle("t", "focus") }.isFailure).isTrue()
            assertThat(repo.observeTask("t").first()).isNull()
        }
    }
    @Test fun `late task write failure rolls back realization and dirty lifecycle`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            seed(f)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_realization BEFORE UPDATE ON day_tasks BEGIN SELECT RAISE(ABORT, 'late failure'); END")
            val before = f.rows()
            assertThat(runCatching { DayTaskRealizationRepository(f.db).toggle("t", "focus", 20) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `wire and KMP roundtrips preserve exact references without template inheritance`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            seed(f); DayTaskRealizationRepository(f.db).toggle("t", "focus")
            val task = f.db.dayTaskDao().getTaskById("t")!!
            val snapshot = Gson().fromJson(Gson().toJson(task.toSnapshot()), DayTaskSnapshot::class.java)
            assertThat(snapshot.toEntity().realizedDayFocusItemIds).containsExactly("focus")
            assertThat(task.toCanonicalDayTask().toAndroidDayTask().realizedDayFocusItemIds).containsExactly("focus")
            f.db.dayTaskDao().insertTasks(listOf(snapshot.toEntity()))
            assertThat(f.db.dayTaskDao().getTaskById("t")!!.realizedDayFocusItemIds).containsExactly("focus")
        }
    }
    @Test fun `merge is stale-safe equal divergence closed and legacy missing field cannot clear refs`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            seed(f); DayTaskRealizationRepository(f.db).toggle("t", "focus")
            val local = f.db.dayTaskDao().getTaskById("t")!!
            val snapshot = local.toSnapshot()
            assertThat(mergeDayTaskRealizations(local, snapshot, snapshot.toEntity()).realizedDayFocusItemIds).containsExactly("focus")
            val divergent = snapshot.copy(realizedDayFocusItemIds = emptyList())
            assertThat(runCatching { mergeDayTaskRealizations(local, divergent, divergent.toEntity()) }.isFailure).isTrue()
            val stale = divergent.copy(version = 1)
            assertThat(mergeDayTaskRealizations(local, stale, stale.toEntity())).isEqualTo(local)
            val legacy = snapshot.copy(version = 3, realizedDayFocusItemIds = null)
            assertThat(mergeDayTaskRealizations(local, legacy, legacy.toEntity()).realizedDayFocusItemIds).containsExactly("focus")
            val removed = divergent.copy(version = 3)
            assertThat(mergeDayTaskRealizations(local, removed, removed.toEntity()).realizedDayFocusItemIds).isEmpty()
            assertThat(legacy.toEntity().realizedDayFocusItemIds).isEmpty() // Exact old Restore, no native guesses.
        }
    }
}
