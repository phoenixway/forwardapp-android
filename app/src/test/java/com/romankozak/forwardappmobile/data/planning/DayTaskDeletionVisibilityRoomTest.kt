package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.observeDayTaskList
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayTaskDeletionVisibilityRoomTest {
    @Test fun `real repository deletion is visible before blocked metrics and metadata finish`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            f.day.persist(PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, f.base.orientation())) {
                DayTask(id = "task", dayPlanId = "day", title = "Task").also { f.db.dayTaskDao().insert(it) }
            }
            val metricsReached = CompletableDeferred<Unit>()
            val releaseMetrics = CompletableDeferred<Unit>()
            val adapter = mockk<CanonicalDayPlanningAdapter>()
            coEvery { adapter.get("day") } coAnswers {
                metricsReached.complete(Unit)
                releaseMetrics.await()
                DayPlan(id = "day", date = 0)
            }
            val repository = DayManagementRepository(
                dayPlanDao = f.db.dayPlanDao(), dayFocusItemDao = f.db.dayFocusItemDao(), dayTaskDao = f.db.dayTaskDao(),
                dailyMetricDao = mockk(relaxed = true), canonicalDayPlanningAdapter = adapter,
                goalDao = f.db.goalDao(), contextDao = f.db.contextDao(), canonicalRecurrenceMaterializationAdapter = mockk(relaxed = true),
                backlogPlacementCommands = mockk(relaxed = true), activityRepository = mockk(relaxed = true),
                taskExecutionTimingCalculator = mockk(relaxed = true), taskExecutionAlarmCoordinator = mockk(relaxed = true),
                aiEventRepository = mockk(relaxed = true), systemWorkspacePresentationContextProjector = mockk(relaxed = true),
                canonicalOrientationRepository = f.base.orientations, canonicalGoalSemanticReadRepository = mockk(relaxed = true),
                ioDispatcher = Dispatchers.IO, planningSourceWriter = f.day,
            )
            val read = observeDayTaskList(f.db.dayTaskDao().getTasksForDay("day"),
                emptyFlow(), emptyFlow(), emptyFlow())
            val initial = CompletableDeferred<Unit>()
            val disappeared = async(Dispatchers.Default) {
                withTimeout(10_000) { read.onEach { if (it.isNotEmpty()) initial.complete(Unit) }.first { it.isEmpty() } }
            }
            withTimeout(10_000) { initial.await() }
            val deletion = async(Dispatchers.Default) { repository.deleteTask("task") }
            try {
                withTimeout(10_000) { metricsReached.await() }
                assertThat(disappeared.await()).isEmpty()
                assertThat(deletion.isActive).isTrue()
                assertThat(f.db.planningReferenceDao().getAll().single().isDeleted).isTrue()
                assertThat(f.db.planningDao().getAllCommitments().single().isDeleted).isTrue()
            } finally { releaseMetrics.complete(Unit); deletion.await() }
        }
    }
}
