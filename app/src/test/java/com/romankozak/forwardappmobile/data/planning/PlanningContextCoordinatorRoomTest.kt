package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.TacticalIterationRepository
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import io.mockk.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningContextCoordinatorRoomTest {
    private val now = LocalDate.of(2026, 10, 28).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private class Runtime(val f: PlanningSourceRoomFixture) {
        val days = mockk<DayManagementRepository>()
        val relations = CanonicalPlanningCommitmentRelationRepository(f.db)
        val commands = PlanningContextCoordinator(f.db, f.planning, relations, f.base.orientations, f.strategic, f.tactical,
            TacticalIterationRepository(CanonicalTacticalPlanningAdapter(f.db, f.tacticalAuthority)), days)
        init {
            coEvery { days.createOrUpdateDayPlan(any(), any()) } coAnswers { requireNotNull(f.db.dayPlanDao().getPlanById("day")) }
            coEvery { days.addTaskToDayPlan(any()) } coAnswers {
                val p = firstArg<NewTaskParameters>()
                f.day.persist(p.planningSource) {
                    DayTask(id = UUID.randomUUID().toString(), dayPlanId = p.dayPlanId, title = p.title, description = p.description)
                        .also { f.db.dayTaskDao().insert(it) }
                }
            }
            coEvery { days.deleteTask(any()) } coAnswers {
                val id = firstArg<String>()
                f.day.deleteTask(id, System.currentTimeMillis()) { f.db.dayTaskDao().softDelete(id, System.currentTimeMillis()) }
            }
        }
    }
    private fun test(block: suspend Runtime.(String) -> Unit) = runBlocking {
        PlanningSourceRoomFixture().use { fixture ->
            fixture.seed()
            Runtime(fixture).block(fixture.base.orientation())
        }
    }
    @Test fun `Strategic add creates FOCUS without semantic duplicate`() = test { id ->
        val before = f.base.dao.getAllManagedSubjects()
        val item = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        assertEquals(id, item.orientationId); assertEquals(PlanningRoleCode.FOCUS, item.role)
        assertEquals(before, f.base.dao.getAllManagedSubjects())
    }
    @Test fun `Tactical add creates MISSION in exact current cycle`() = test { id ->
        val item = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        assertEquals("cycle", item.scopeId.value); assertEquals(PlanningRoleCode.MISSION, item.role)
    }
    @Test fun `Day add uses accepted DAY TASK role and canonical source`() = test { id ->
        val item = commands.add(id, PlanningScopeKind.DAY, now)
        assertEquals(PlanningRoleCode.DAY_TASK, item.role); assertEquals(id, item.orientationId)
        assertEquals("DAY_TASK", item.provenance.sourceType)
        assertEquals(id, f.db.planningReferenceDao().getAll().single().targetId)
    }
    @Test fun `same Orientation participates in three horizons`() = test { id ->
        val items = PlanningScopeKind.entries.map { commands.add(id, it, now) }
        assertEquals(3, items.map { it.id }.distinct().size)
        assertEquals(setOf(id), items.map { it.orientationId }.toSet())
    }
    @Test fun `Strategic to Tactical promotion adds COMPONENT OF`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val target = commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now)
        val edge = f.db.planningCommitmentRelationDao().getAll().single()
        assertEquals(target.id.value, edge.sourceCommitmentId); assertEquals(source.id.value, edge.targetCommitmentId)
        assertEquals(source, f.planning.getCommitment(source.id))
    }
    @Test fun `Tactical to Day promotion preserves source and identity`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        val target = commands.promote(source.id, PlanningScopeKind.DAY, now)
        assertEquals(source.orientationId, target.orientationId)
        assertEquals(source, f.planning.getCommitment(source.id))
        assertEquals(target.id.value, f.db.planningCommitmentRelationDao().getAll().single().sourceCommitmentId)
    }
    @Test fun `repeated promotion reuses participation and relation`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val first = commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now)
        assertEquals(first, commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now))
        assertEquals(2, f.db.planningDao().getAllCommitments().size)
        assertEquals(1, f.db.planningCommitmentRelationDao().getAll().size)
    }
    @Test fun `repeated Day action is idempotent`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        val first = commands.promote(source.id, PlanningScopeKind.DAY, now)
        assertEquals(first, commands.promote(source.id, PlanningScopeKind.DAY, now))
        coVerify(exactly = 1) { days.addTaskToDayPlan(any()) }
    }
    @Test fun `existing participation gains missing relation without new carrier`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val mission = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        assertEquals(mission, commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now))
        assertEquals(1, f.db.tacticalMissionDao().getAllMissionsSync().size)
    }
    @Test fun `ambiguous Tactical scope fails without creating participation`() = test { id ->
        f.planning.saveScope(PlanningScope(PlanningScopeId("other-cycle"), PlanningScopeKind.TACTICAL_CYCLE,
            PlanningScopeLifecycle.ACTIVE, "Other", null, null, 1, 1, null, 1, false))
        assertTrue(runCatching { commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now) }.isFailure)
        assertTrue(f.db.planningDao().getAllCommitments().isEmpty())
    }
    @Test fun `Aspect cannot become a Planning target`() = test { _ ->
        val aspect = f.base.aspect()
        assertTrue(runCatching { commands.add(aspect, PlanningScopeKind.DAY, now) }.isFailure)
        coVerify(exactly = 0) { days.addTaskToDayPlan(any()) }
    }
    @Test fun `late relation failure rolls back promoted carrier and commitment`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_composition BEFORE INSERT ON planning_commitment_relations BEGIN SELECT RAISE(ABORT, 'injected'); END")
        assertTrue(runCatching { commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now) }.isFailure)
        assertEquals(listOf(source.id.value), f.db.planningDao().getAllCommitments().map { it.id })
        assertTrue(f.db.tacticalMissionDao().getAllMissionsSync().isEmpty())
    }
    @Test fun `remove participation retires edges not semantic identity`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val target = commands.promote(source.id, PlanningScopeKind.TACTICAL_CYCLE, now)
        commands.remove(target.id, target.version)
        assertTrue(f.planning.getCommitment(target.id)!!.isDeleted)
        assertTrue(f.db.planningCommitmentRelationDao().getAll().single().isDeleted)
        assertFalse(f.base.dao.getManagedSubject(id)!!.isDeleted)
        assertFalse(f.planning.getCommitment(source.id)!!.isDeleted)
    }
    @Test fun `status changes participation without completing Orientation`() = test { id ->
        val item = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val semantic = f.base.dao.getManagedSubject(id)
        commands.status(item.id, item.version, PlanningCommitmentStatus.COMPLETED)
        assertEquals(PlanningCommitmentStatus.COMPLETED, f.planning.getCommitment(item.id)!!.status)
        assertEquals(semantic, f.base.dao.getManagedSubject(id))
    }
    @Test fun `stale version status fails without mutation`() = test { id ->
        val item = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        assertTrue(runCatching { commands.status(item.id, item.version + 1, PlanningCommitmentStatus.COMPLETED) }.isFailure)
        assertEquals(item, f.planning.getCommitment(item.id))
    }
    @Test fun `planning does not create WorkspaceBinding or H1`() = test { id ->
        val before = f.base.snapshot()
        commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        commands.add(id, PlanningScopeKind.DAY, now)
        assertTrue(f.base.dao.getAllWorkspaceBindings().isEmpty())
        assertTrue(f.base.dao.getAllOrientationRelations().isEmpty())
        assertEquals(1, f.db.workspaceDao().getAll().size)
        assertEquals(before.hierarchyPlacements, f.base.snapshot().hierarchyPlacements)
    }
    @Test fun `legacy Quest action resolves exact source commitment without source copy fields`() = test { id ->
        val source = commands.add(id, PlanningScopeKind.STRATEGIC_ARC, now)
        val questId = requireNotNull(source.provenance.sourceId)
        val before = f.strategic.getById(questId)
        val mission = commands.questToTactical(questId)
        assertEquals(id, mission.orientationId)
        assertEquals(before, f.strategic.getById(questId))
        assertNull(f.db.tacticalMissionDao().getAllMissionsSync().single().sourceArcQuestId)
    }
    @Test fun `legacy Mission action resolves exact source into canonical Day participation`() = test { id ->
        val mission = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        val task = commands.missionToToday(requireNotNull(mission.provenance.sourceId).toLong())
        assertEquals(id, task.orientationId); assertEquals(PlanningRoleCode.DAY_TASK, task.role)
    }
    @Test fun `Mission current Arc action reuses identity and correct component direction`() = test { id ->
        val mission = commands.add(id, PlanningScopeKind.TACTICAL_CYCLE, now)
        val focus = commands.missionToArc(requireNotNull(mission.provenance.sourceId).toLong())
        val relation = f.db.planningCommitmentRelationDao().getAll().single()
        assertEquals(mission.id.value, relation.sourceCommitmentId)
        assertEquals(focus.id.value, relation.targetCommitmentId)
        assertEquals(id, focus.orientationId)
    }
}
