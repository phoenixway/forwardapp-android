package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayTaskNotesRoomTest {
    private fun repository(f: SemanticDeletionRoomFixture) = DayManagementRepository(
        dayPlanDao = f.db.dayPlanDao(), dayFocusItemDao = f.db.dayFocusItemDao(), dayTaskDao = f.db.dayTaskDao(),
        dailyMetricDao = mockk(), canonicalDayPlanningAdapter = mockk(), goalDao = f.db.goalDao(), contextDao = f.db.contextDao(),
        canonicalRecurrenceMaterializationAdapter = mockk(), backlogPlacementCommands = mockk(), activityRepository = mockk(),
        taskExecutionTimingCalculator = mockk(), taskExecutionAlarmCoordinator = mockk(), aiEventRepository = mockk(),
        systemWorkspacePresentationContextProjector = mockk(), canonicalOrientationRepository = f.orientations,
        canonicalGoalSemanticReadRepository = mockk(), ioDispatcher = Dispatchers.IO,
    )
    @Test fun `journal persists reactively only on exact occurrence with normal dirty lifecycle and snapshot mapping`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.db.dayPlanDao().insert(DayPlan(id = "d", date = 1))
            val first = DayTask(id = "t", dayPlanId = "d", title = "Task", description = "Shared instruction",
                recurrenceSeriesId = "series", recurrenceOccurrenceDayKey = "2026-10-20", recurrenceSourceSeriesVersion = 1,
                notes = "Before", version = 4, syncedAt = 1)
            val second = first.copy(id = "other", notes = null, recurrenceOccurrenceDayKey = "2026-10-21")
            f.db.dayTaskDao().insertAll(listOf(first, second))
            val before = f.rows().filterKeys { it != "day_tasks" }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val tasks = f.db.dayTaskDao().getTasksForDay("d").stateIn(scope)
                repository(f).updateTaskNotes("t", "## Plan\n- [x] First step")
                val saved = withTimeout(5_000) { tasks.first { rows -> rows.any { it.id == "t" && it.notes?.startsWith("## Plan") == true } } }.single { it.id == "t" }
                assertThat(saved.description).isEqualTo(first.description)
                assertThat(saved.version).isEqualTo(5)
                assertThat(saved.syncedAt).isNull()
                assertThat(f.db.dayTaskDao().getTaskById("other")).isEqualTo(second)
                assertThat(saved.recurrenceSeriesId).isEqualTo(first.recurrenceSeriesId)
                assertThat(saved.recurrenceOccurrenceDayKey).isEqualTo(first.recurrenceOccurrenceDayKey)
                assertThat(saved.toSnapshot().toEntity().notes).isEqualTo(saved.notes)
                assertThat(saved.toSnapshot().toEntity().description).isEqualTo(saved.description)
                assertThat(f.rows().filterKeys { it != "day_tasks" }).isEqualTo(before)
                repository(f).updateTaskNotes("t", "")
                assertThat(f.db.dayTaskDao().getTaskById("t")!!.notes).isEmpty()
            } finally { scope.cancel() }
        }
    }
    @Test fun `deleted or missing task refuses notes without resurrection`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.db.dayPlanDao().insert(DayPlan(id = "d", date = 1))
            val task = DayTask(id = "t", dayPlanId = "d", title = "Deleted", isDeleted = true)
            f.db.dayTaskDao().insert(task)
            val repo = repository(f)
            assertThat(runCatching { repo.updateTaskNotes("t", "New") }.isFailure).isTrue()
            assertThat(runCatching { repo.updateTaskNotes("missing", "New") }.isFailure).isTrue()
            assertThat(f.db.dayTaskDao().getTaskById("t")).isEqualTo(task)
        }
    }
}
