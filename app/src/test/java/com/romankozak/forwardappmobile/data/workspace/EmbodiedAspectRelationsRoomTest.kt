package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.workspace.WorkspaceRelationsProjection
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmbodiedAspectRelationsRoomTest {
    private fun repository(f: SemanticDeletionRoomFixture): WorkspaceRelationsRepository {
        val planning = CanonicalPlanningReferenceRepository(f.db)
        return WorkspaceRelationsRepository(f.bindings, CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()), planning,
            EmbodiedAspectRelationsRepository(f.aspects, f.links, f.bindings, planning),
            EmbodiedOrientationRelationsRepository(f.links, f.graph))
    }
    private suspend fun WorkspaceRelationsRepository.current() = withTimeout(10_000) { observe("workspace").first() }
    private suspend fun setup(f: SemanticDeletionRoomFixture): Pair<String, String> {
        f.workspace()
        val aspect = f.aspect()
        val orientation = f.orientation()
        f.bindings.bind(aspect, "workspace", WorkspaceBindingType.EMBODIES, 20)
        f.links.linkOrientation(aspect, orientation, AspectOrientationRelationType.BELONGS_TO, now = 21)
        return aspect to orientation
    }

    @Test fun `real Aspect relations extend direct Workspace projection without any writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val (aspect, orientation) = setup(f)
            val parent = f.aspects.create("Parent", now = 22)
            f.aspects.move(aspect, parent, now = 23)
            val child = f.aspects.create("Child", parentAspectId = aspect, now = 24)
            f.db.workspaceDao().upsert(listOf(f.db.workspaceDao().getById("workspace")!!.copy(id = "other", nameOverride = "Other")))
            f.bindings.bind(aspect, "other", WorkspaceBindingType.SUPPORTS, 25)
            val before = f.rows()
            val projection = repository(f).current()
            assertThat(projection.relations.map { it.targetId }).containsExactly(aspect)
            assertThat(projection.embodiedAspect!!.relations.map { it.targetId }).containsExactly(orientation, parent, child, "other")
            assertThat(projection.embodiedAspect!!.relations.single { it.targetId == orientation }.navigation)
                .isEqualTo(NavTarget.SemanticEntityEditor(orientation))
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `single subscription reacts to associations renames and removed embodiment`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val (aspect, orientation) = setup(f)
            val subscription = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repository(f).observe("workspace").stateIn(subscription)
                assertThat(state.value.embodiedAspect!!.relations).hasSize(1)
                f.dao.upsertManagedSubjects(listOf(f.dao.getManagedSubject(orientation)!!.copy(title = "Updated")))
                withTimeout(10_000) { state.first { it.embodiedAspect?.relations?.singleOrNull()?.title == "Updated" } }
                val ref = f.dao.getAllAspectOrientationRefs().single()
                f.links.unlinkOrientation(ref.id, now = 30)
                withTimeout(10_000) { state.first { it.embodiedAspect?.relations?.isEmpty() == true } }
                f.links.linkOrientation(aspect, orientation, AspectOrientationRelationType.RELEVANT_TO, now = 31)
                withTimeout(10_000) { state.first { it.embodiedAspect?.relations?.singleOrNull()?.kinds == listOf("RELEVANT_TO") } }
                val binding = f.dao.getAllWorkspaceBindings().single()
                f.dao.upsertWorkspaceBindings(listOf(binding.copy(isDeleted = true, version = 2)))
                withTimeout(10_000) { state.first { it.embodiedAspect == null && it.relations.isEmpty() } }
            } finally { subscription.cancel() }
        }
    }

    @Test fun `deleted Orientation and Aspect do not leave inherited live rows`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val (aspect, orientation) = setup(f)
            f.delete(orientation)
            assertThat(repository(f).current().embodiedAspect!!.relations).isEmpty()
            f.delete(aspect, ManagedSubjectType.ASPECT)
            assertThat(repository(f).current().embodiedAspect).isNull()
        }
    }

    @Test fun `Aspect Planning reference uses typed target owner lifecycle and exact Day navigation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val (aspect, _) = setup(f)
            f.db.dayPlanDao().insert(DayPlan(id = "day", date = 1))
            f.db.dayTaskDao().insert(DayTask(id = "task", dayPlanId = "day", title = "Task"))
            val planning = CanonicalPlanningReferenceRepository(f.db)
            val id = planning.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task"),
                PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, aspect), PlanningReferenceRole.SOURCE, 30)
            assertThat(repository(f).current().embodiedAspect!!.relations.single { it.targetId == "task" }.navigation)
                .isEqualTo(NavTarget.EditTask("task"))
            val row = f.db.planningReferenceDao().get(id)!!
            f.db.planningReferenceDao().upsert(row.copy(id = "wrong-type", targetKind = "WORKSPACE", role = "RELATED"))
            assertThat(planning.observeAspectRelations(aspect).first()).hasSize(1)
            f.db.dayTaskDao().insert(f.db.dayTaskDao().getTaskById("task")!!.copy(isDeleted = true))
            assertThat(planning.observeAspectRelations(aspect).first()).isEmpty()
            assertThat(f.db.planningReferenceDao().get(id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `SUPPORTS alone never expands Aspect relations`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val aspect = f.aspect(); val orientation = f.orientation()
            f.links.linkOrientation(aspect, orientation, AspectOrientationRelationType.BELONGS_TO, now = 21)
            f.bindings.bind(aspect, "workspace", WorkspaceBindingType.SUPPORTS, 22)
            val result = repository(f).current()
            assertThat(result.relations).hasSize(1)
            assertThat(result.embodiedAspect).isNull()
        }
    }
}
