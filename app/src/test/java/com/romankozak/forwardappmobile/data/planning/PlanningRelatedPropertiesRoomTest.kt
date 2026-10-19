package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.features.planning.projectPlanningReferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningRelatedPropertiesRoomTest {
    @Test fun `every item owner links three canonical types without creating any identity participation binding or placement`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val orientation = f.base.orientation()
            val aspect = f.base.aspect()
            val mission = f.tactical.insertMission(TacticalMission(title = "Mission", description = null, projectId = null, deadline = 100, iterationId = "cycle", createdAt = 10))
            f.strategic.addQuest(ArcQuestEntity(id = "quest", arcKey = "2026-10", title = "Quest"))
            f.db.dayTaskDao().insert(DayTask(id = "task", dayPlanId = "day", title = "Task"))
            f.db.dayFocusItemDao().insert(DayFocusItem(id = "focus", dayPlanId = "day", title = "Focus"))
            val before = f.base.rows()
            val owners = listOf(PlanningReferenceOwner(PlanningReferenceOwnerKind.TACTICAL_MISSION, mission.toString()),
                PlanningReferenceOwner(PlanningReferenceOwnerKind.ARC_QUEST, "quest"),
                PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task"),
                PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_FOCUS, "focus"),
                PlanningReferenceOwner(PlanningReferenceOwnerKind.SCOPE, "day"))
            owners.forEach { owner ->
                f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientation))
                f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, aspect))
                f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "workspace"))
            }
            assertThat(f.base.rows().filter { (name, rows) -> before[name] != rows }.keys).containsExactly("planning_references")
            assertThat(f.db.planningReferenceDao().getAll()).hasSize(15)
        }
    }

    @Test fun `Room catalog projects typed collisions reactive titles removal and deleted endpoints without Context fallback`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            val id = f.base.orientation()
            val standalone = f.db.workspaceDao().getAll().single()
            f.db.workspaceDao().upsert(listOf(standalone.copy(id = id, nameOverride = "Operational", sourceContextId = "legacy-evidence")))
            val owner = PlanningReferenceOwner(PlanningReferenceOwnerKind.SCOPE, "day")
            val semanticRef = f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, id))
            f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, id))
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
            suspend fun projection() = projectPlanningReferences(f.references.observeAll().first(), catalog.observeCatalog().first(), owner)
            assertThat(projection().map { it.entity.title }).containsExactly("Meaning", "Operational")
            f.base.orientations.updateOrientationSemantics(id, "Renamed", null, now = 40)
            assertThat(projection().map { it.entity.title }).containsExactly("Renamed", "Operational")
            f.references.remove(semanticRef, 1, 50)
            assertThat(projection().single().entity.title).isEqualTo("Operational")
            f.db.workspaceDao().upsert(listOf(standalone.copy(id = id, isDeleted = true, version = 2)))
            assertThat(projection()).isEmpty()
            assertThat(f.base.dao.getManagedSubject(id)!!.isDeleted).isFalse()
            assertThat(f.db.contextDao().getAllRaw()).isEmpty()
        }
    }
}
