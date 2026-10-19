package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TacticalReactiveCreationRoomTest {
    @Test fun `active Mission collector sees complete source creation and deletion frames`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val orientation = f.base.orientation()
            val aspect = f.base.aspect()
            for ((kind, id) in listOf(PlanningReferenceTargetKind.ORIENTATION to orientation,
                PlanningReferenceTargetKind.ASPECT to aspect, PlanningReferenceTargetKind.WORKSPACE to "workspace")) {
                val started = CompletableDeferred<Unit>()
                val created = async(Dispatchers.Default) {
                    withTimeout(10_000) { f.tactical.getAllMissions().onEach { started.complete(Unit) }
                        .first { it.isNotEmpty() } }
                }
                withTimeout(10_000) { started.await() }
                val mission = f.tactical.insertMission(TacticalMission(title = "Source", description = null,
                    projectId = null, iterationId = "cycle", createdAt = 10, deadline = 100, version = 1),
                    PlanningReferenceTarget(kind, id))
                assertThat(created.await().single().id).isEqualTo(mission)
                val collecting = CompletableDeferred<Unit>()
                val deleted = async(Dispatchers.Default) {
                    withTimeout(10_000) { f.tactical.getAllMissions().onEach { collecting.complete(Unit) }.first { it.isEmpty() } }
                }
                withTimeout(10_000) { collecting.await() }
                f.tactical.deleteMissionById(mission)
                assertThat(deleted.await()).isEmpty()
            }
        }
    }

    @Test fun `active cycle collector never observes scope without its transactionally written extension`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val adapter = CanonicalTacticalPlanningAdapter(f.db, f.tacticalAuthority)
            for (index in 1..4) {
                val started = CompletableDeferred<Unit>()
                val id = "new-cycle-$index"
                val collected = async(Dispatchers.Default) {
                    withTimeout(10_000) { adapter.observeIterations().onEach { started.complete(Unit) }
                        .first { rows -> rows.any { it.id == id } } }
                }
                withTimeout(10_000) { started.await() }
                adapter.saveIteration(TacticalIteration(id = id, title = id, startedAt = 30L + index,
                    plannedEndAt = 100, weekKey = "2026-W43", createdAt = 30, updatedAt = 30, version = 1))
                assertThat(collected.await().first { it.id == id }.title).isEqualTo(id)
            }
        }
    }
}
