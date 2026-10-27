package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.planning.strategic.canonicalStrategicArcScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningSourceCreationRoomTest {
    @Test fun `same Project identity participates in Tactical Strategic and Day without semantic duplication`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val id = f.base.orientation(com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.PROJECT)
            val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, id)
            val before = f.base.dao.getAllManagedSubjects()
            f.tactical.insertMission(mission(), source)
            f.strategic.addQuest(ArcQuestEntity(id = "project-quest", arcKey = "2026-10", title = "Planning"), source)
            f.day.persist(source) {
                DayTask(id = "project-day", dayPlanId = "day", title = "Day work").also { f.db.dayTaskDao().insert(it) }
            }
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(before)
            assertThat(f.db.planningDao().getAllCommitments()).hasSize(3)
            assertThat(f.db.planningDao().getAllCommitments().map { it.orientationId }.toSet()).containsExactly(id)
            assertThat(f.base.dao.getAllOrientations().single().kind).isEqualTo("PROJECT")
            assertIsolation(f)
        }
    }

    private fun mission() = TacticalMission(title = "Planning item", description = null, projectId = null, deadline = 100,
        iterationId = "cycle", weekKey = "2026-W43", createdAt = 10, updatedAt = 10, version = 1)

    @Test fun `Tactical Orientation source reuses exact identity without another INTENT`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = f.base.orientation()
            val before = f.base.dao.getAllManagedSubjects()
            val id = f.tactical.insertMission(mission(), PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source))
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(before)
            assertThat(f.db.planningDao().getAllCommitments().single().orientationId).isEqualTo(source)
            assertThat(f.db.planningReferenceDao().getAll().single().ownerId).isEqualTo(id.toString())
            assertThat(f.tactical.getMissionById(id)!!.title).isEqualTo("Meaning")
            assertThat(f.base.dao.getAllLegacyMappings()).isEmpty()
            f.tacticalAuthority.admitCanonicalIngress()
            f.tactical.deleteMissionById(id)
            assertThat(f.base.dao.getManagedSubject(source)!!.isDeleted).isFalse()
            assertThat(f.db.planningReferenceDao().getAll().single().isDeleted).isTrue()
        }
    }

    @Test fun `Tactical Aspect Workspace sources retain normal planning identity with independent SOURCE`() = runBlocking<Unit> {
        for (kind in listOf(PlanningReferenceTargetKind.ASPECT, PlanningReferenceTargetKind.WORKSPACE)) PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = if (kind == PlanningReferenceTargetKind.ASPECT) f.base.aspect() else "workspace"
            val before = f.base.dao.getAllOrientations().size
            val id = f.tactical.insertMission(mission(), PlanningReferenceTarget(kind, source))
            assertThat(f.base.dao.getAllOrientations()).hasSize(before + 1)
            val row = f.db.planningReferenceDao().getAll().single()
            assertThat(row.targetKind).isEqualTo(kind.name)
            assertThat(row.targetId).isEqualTo(source)
            assertThat(row.role).isEqualTo("SOURCE")
            assertThat(f.tactical.getMissionById(id)).isNotNull()
            assertIsolation(f)
        }
    }

    @Test fun `Strategic Orientation source reuses identity and survives canonical ingress`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = f.base.orientation()
            val before = f.base.dao.getAllManagedSubjects()
            val quest = ArcQuestEntity(id = "quest", arcKey = "2026-10", title = "Planning")
            f.strategic.addQuest(quest, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source))
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(before)
            assertThat(f.strategic.getById("quest")!!.sourceType).isEqualTo(ArcQuestSourceType.ORIENTATION.name)
            assertThat(f.db.planningDao().getAllCommitments().single().orientationId).isEqualTo(source)
            f.strategicAuthority.admitCanonicalIngress()
            f.strategic.deleteQuest(f.strategic.getById("quest")!!)
            assertThat(f.base.dao.getManagedSubject(source)!!.isDeleted).isFalse()
            assertIsolation(f)
        }
    }

    @Test fun `Strategic Aspect Workspace sources create ordinary QUEST plus typed source`() = runBlocking<Unit> {
        for (kind in listOf(PlanningReferenceTargetKind.ASPECT, PlanningReferenceTargetKind.WORKSPACE)) PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = if (kind == PlanningReferenceTargetKind.ASPECT) f.base.aspect() else "workspace"
            f.strategic.addQuest(ArcQuestEntity(id = "quest", arcKey = "2026-10", title = "Quest"), PlanningReferenceTarget(kind, source))
            assertThat(f.db.planningReferenceDao().getAll().single().targetKind).isEqualTo(kind.name)
            assertThat(f.base.dao.getAllOrientations().single().kind).isEqualTo("QUEST")
            f.strategicAuthority.admitCanonicalIngress()
            assertIsolation(f)
        }
    }

    @Test fun `Day Orientation participates while Aspect Workspace only supply source references`() = runBlocking<Unit> {
        for (kind in PlanningReferenceTargetKind.entries) PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = when (kind) {
                PlanningReferenceTargetKind.ORIENTATION -> f.base.orientation()
                PlanningReferenceTargetKind.ASPECT -> f.base.aspect()
                PlanningReferenceTargetKind.WORKSPACE -> "workspace"
            }
            val before = f.base.dao.getAllManagedSubjects()
            f.day.persist(PlanningReferenceTarget(kind, source)) {
                DayTask(id = "task", dayPlanId = "day", title = "Task").also { f.db.dayTaskDao().insert(it) }
            }
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(before)
            assertThat(f.db.planningDao().getAllCommitments()).hasSize(if (kind == PlanningReferenceTargetKind.ORIENTATION) 1 else 0)
            assertThat(f.db.planningReferenceDao().getAll().single().targetId).isEqualTo(source)
            f.day.deleteTask("task", 40) { f.db.dayTaskDao().softDelete("task", 40) }
            assertThat(f.db.planningDao().getAllCommitments().all { it.isDeleted }).isTrue()
            assertThat(f.db.planningReferenceDao().getAll().single().isDeleted).isTrue()
            assertIsolation(f)
        }
    }

    @Test fun `late reference failure rolls back Tactical semantic and planning creation`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val before = f.base.rows()
            f.db.openHelper.writableDatabase.execSQL("CREATE TEMP TRIGGER reject_reference BEFORE INSERT ON planning_references BEGIN SELECT RAISE(ABORT, 'reject'); END")
            assertThat(runCatching { f.tactical.insertMission(mission(), PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "workspace")) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `duplicate Orientation participation rejects without another item or identity`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, f.base.orientation())
            f.tactical.insertMission(mission(), source)
            val before = f.base.rows()
            assertThat(runCatching { f.tactical.insertMission(mission(), source) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    private suspend fun assertIsolation(f: PlanningSourceRoomFixture) {
        assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
        assertThat(f.base.dao.getAllWorkspaceBindings()).isEmpty()
        assertThat(f.base.dao.getAllOrientationRelations()).isEmpty()
        assertThat(f.base.dao.getAllAspectOrientationRefs()).isEmpty()
    }
}
