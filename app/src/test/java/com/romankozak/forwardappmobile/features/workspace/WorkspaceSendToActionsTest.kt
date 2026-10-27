package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class WorkspaceSendToActionsTest {
    private val rows = MutableStateFlow(listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("w"), "Not this source"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace")))
    private val catalog = mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns rows }
    private val days = mockk<DayManagementRepository>(relaxed = true)
    private val tasks = mockk<DayTaskDao> { coEvery { getMinOrderForDayPlan("today") } returns 5L }
    private val missions = mockk<MissionRepository> { coEvery { insertMissionWithAutoOrder(any(), any()) } returns 42L }
    private val cycles = mockk<TacticalIterationRepository> { coEvery { getCurrentIteration() } returns mockk { every { id } returns "cycle" } }
    private val streams = mockk<MissionStreamRepository>(relaxed = true)
    private val quests = mockk<ArcQuestRepository>(relaxed = true)
    private val actions = WorkspaceSendToActions(catalog, days, tasks, missions, cycles, streams, quests)
    private val now = Instant.parse("2026-10-26T12:00:00Z").toEpochMilli()
    private val source = PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "w")

    @Test fun `day command uses canonical Workspace SOURCE without legacy Context links`() = runTest {
        coEvery { days.createOrUpdateDayPlan(now, any()) } returns mockk { every { id } returns "today" }
        actions.send("w", WorkspaceSendTarget.DAY_TASK, now)
        val params = slot<NewTaskParameters>()
        coVerify(exactly = 1) { days.addTaskToDayPlan(capture(params)) }
        assertEquals("today", params.captured.dayPlanId)
        assertEquals("Workspace", params.captured.title)
        assertEquals(source, params.captured.planningSource)
        assertEquals(4L, params.captured.order)
        assertNull(params.captured.projectId)
        coVerify(exactly = 0) { missions.insertMissionWithAutoOrder(any(), any()) }
        coVerify(exactly = 0) { quests.addQuest(any(), any()) }
    }

    @Test fun `mission uses current cycle General and exact typed source`() = runTest {
        actions.send("w", WorkspaceSendTarget.TACTICAL_MISSION, now)
        val mission = slot<TacticalMission>()
        coVerify(exactly = 1) { missions.insertMissionWithAutoOrder(capture(mission), source) }
        assertEquals("Workspace", mission.captured.title)
        assertEquals("cycle", mission.captured.iterationId)
        assertEquals(GENERAL_MISSION_STREAM_ID, mission.captured.missionStreamId)
        assertNull(mission.captured.projectId)
        coVerify(exactly = 1) { streams.ensureDefaultStream() }
        coVerify(exactly = 0) { days.addTaskToDayPlan(any()) }
    }

    @Test fun `quest uses current month rather than another open historical arc`() = runTest {
        actions.send("w", WorkspaceSendTarget.ARC_QUEST, now)
        val quest = slot<ArcQuestEntity>()
        coVerify(exactly = 1) { quests.addQuest(capture(quest), source) }
        assertEquals("2026-10", quest.captured.arcKey)
        assertEquals("Workspace", quest.captured.title)
        assertNull(quest.captured.linkedContextId)
        coVerify(exactly = 0) { days.addTaskToDayPlan(any()) }
    }

    @Test fun `missing deleted or wrong typed source never starts a creation command`() = runTest {
        rows.value = rows.value.filterNot { it.ref is CanonicalEntityRef.Workspace }
        WorkspaceSendTarget.entries.forEach {
            try { actions.send("w", it, now); fail("must fail closed") } catch (_: IllegalArgumentException) { }
        }
        coVerify(exactly = 0) { days.createOrUpdateDayPlan(any(), any()) }
        coVerify(exactly = 0) { missions.insertMissionWithAutoOrder(any(), any()) }
        coVerify(exactly = 0) { quests.addQuest(any(), any()) }
    }
}
