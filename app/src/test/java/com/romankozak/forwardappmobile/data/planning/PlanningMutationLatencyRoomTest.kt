package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningMutationLatencyRoomTest {
    @Test fun `Mission auto order uses canonical commitment instead of stale carrier order`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            fun mission() = TacticalMission(title = "Mission", description = null, projectId = null,
                iterationId = "cycle", deadline = 100, createdAt = 10, version = 1)
            val first = f.tactical.insertMissionWithAutoOrder(mission())
            f.db.tacticalMissionDao().updateMissionOrder(first, 900)
            val second = f.tactical.insertMissionWithAutoOrder(mission())
            assertThat(f.tactical.getMissionById(first)!!.orderInWeek).isEqualTo(-1L)
            assertThat(f.tactical.getMissionById(second)!!.orderInWeek).isEqualTo(-2L)
        }
    }

    @Test fun `Quest auto order uses canonical FOCUS instead of stale carrier order`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            f.strategic.addQuest(ArcQuestEntity(id = "one", arcKey = "2026-10", title = "One"))
            f.db.arcQuestDao().update(f.db.arcQuestDao().getById("one")!!.copy(order = 900))
            f.strategic.addQuest(ArcQuestEntity(id = "two", arcKey = "2026-10", title = "Two"))
            assertThat(f.strategic.getById("one")!!.order).isEqualTo(-1L)
            assertThat(f.strategic.getById("two")!!.order).isEqualTo(-2L)
        }
    }

    @Test fun `Day delete failure rolls back task participation and references together`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = f.base.orientation()
            f.day.persist(PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source)) {
                DayTask(id = "task", dayPlanId = "day", title = "Task").also { f.db.dayTaskDao().insert(it) }
            }
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_reference_delete BEFORE UPDATE ON planning_references BEGIN SELECT RAISE(ABORT, 'late failure'); END")
            val before = f.base.rows()
            assertThat(runCatching { f.day.deleteTask("task", 40) { f.db.dayTaskDao().softDelete("task", 40) } }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
            assertThat(f.base.dao.getManagedSubject(source)!!.isDeleted).isFalse()
        }
    }
}
