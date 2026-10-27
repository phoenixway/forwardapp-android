package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.workspace.capability.*
import com.romankozak.forwardappmobile.features.workspace.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceRelationsRoomTest {
    private fun repository(f: SemanticDeletionRoomFixture) = WorkspaceRelationsRepository(f.bindings,
        CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()), CanonicalPlanningReferenceRepository(f.db),
        EmbodiedAspectRelationsRepository(f.aspects, f.links, f.bindings, CanonicalPlanningReferenceRepository(f.db)),
        EmbodiedOrientationRelationsRepository(f.links, f.graph))
    private fun capability(f: SemanticDeletionRoomFixture) = CanonicalRelationsCapabilityRepository(
        CanonicalCapabilityInstanceStore(f.db, f.db.workspaceDao(), f.dao))
    private suspend fun WorkspaceRelationsRepository.current(predicate: (WorkspaceRelationsProjection) -> Boolean = { true }) =
        withTimeout(10_000) { observe("workspace").first(predicate) }
    private suspend fun scope(f: SemanticDeletionRoomFixture, id: String = "scope") {
        f.db.planningDao().upsertScope(PlanningScopeEntity(id, "DAY", "ACTIVE", "Day", null, null, 1, 1, null, 1, false))
    }
    private suspend fun attach(f: SemanticDeletionRoomFixture, role: PlanningReferenceRole = PlanningReferenceRole.SOURCE,
        kind: PlanningReferenceOwnerKind = PlanningReferenceOwnerKind.SCOPE, id: String = "scope") =
        CanonicalPlanningReferenceRepository(f.db).attach(PlanningReferenceOwner(kind, id),
            PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "workspace"), role, 20)

    @Test fun `empty standalone Workspace valid and H1 or tags do not infer relations`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val subject = f.orientation(); f.appearance(subject)
            CanonicalWorkspaceTagRepository(f.db).replaceTags("workspace", listOf("Meaning"), 30)
            val before = f.rows()
            assertThat(repository(f).current().relations).isEmpty()
            assertThat(f.rows()).isEqualTo(before)
            assertThat(f.db.planningReferenceDao().getAll()).isEmpty()
        }
    }
    @Test fun `all binding kinds direct typed semantic endpoints and merged rows`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val o = f.orientation(); val a = f.aspect()
            f.bindings.bind(o, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.bindings.bind(o, "workspace", WorkspaceBindingType.REALIZES, 21)
            f.bindings.bind(a, "workspace", WorkspaceBindingType.SUPPORTS, 22)
            f.bindings.bind(a, "workspace", WorkspaceBindingType.MONITORS, 23)
            val before = f.rows()
            val rows = repository(f).current { it.relations.size == 2 }.relations
            assertThat(rows.single { it.targetId == o }.kinds).containsExactly("EMBODIES", "REALIZES")
            assertThat(rows.single { it.targetId == a }.kinds).containsExactly("SUPPORTS", "MONITORS")
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `binding removal rename and target deletion update one subscription`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val id = f.orientation()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repository(f).observe("workspace").stateIn(scope)
                withTimeout(10_000) { state.first { it.relations.size == 1 } }
                f.dao.upsertManagedSubjects(listOf(f.dao.getManagedSubject(id)!!.copy(title = "Renamed")))
                withTimeout(10_000) { state.first { it.relations.singleOrNull()?.title == "Renamed" } }
                val binding = f.dao.getAllWorkspaceBindings().single()
                f.dao.upsertWorkspaceBindings(listOf(binding.copy(isDeleted = true, version = 2)))
                withTimeout(10_000) { state.first { it.relations.isEmpty() } }
                f.dao.upsertWorkspaceBindings(listOf(binding.copy(version = 3)))
                withTimeout(10_000) { state.first { it.relations.size == 1 } }
                f.delete(id)
                withTimeout(10_000) { state.first { it.relations.isEmpty() } }
            } finally { scope.cancel() }
        }
    }
    @Test fun `Planning SOURCE RELATED inverse read is reactive to rename tombstone and owner deletion`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); scope(f)
            val source = attach(f); val related = attach(f, PlanningReferenceRole.RELATED)
            val subscription = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repository(f).observe("workspace").stateIn(subscription)
                assertThat(state.value.relations.single().kinds).containsExactly("RELATED", "SOURCE")
                f.db.planningDao().upsertScope(f.db.planningDao().getScope("scope")!!.copy(title = "Renamed scope"))
                withTimeout(10_000) { state.first { it.relations.singleOrNull()?.title == "Renamed scope" } }
                CanonicalPlanningReferenceRepository(f.db).remove(source, 1, 30)
                withTimeout(10_000) { state.first { it.relations.singleOrNull()?.kinds == listOf("RELATED") } }
                f.db.planningDao().upsertScope(f.db.planningDao().getScope("scope")!!.copy(isDeleted = true))
                withTimeout(10_000) { state.first { it.relations.isEmpty() } }
                assertThat(f.db.planningReferenceDao().get(related)!!.isDeleted).isFalse() // Projection does not invent cleanup writes.
            } finally { subscription.cancel() }
        }
    }
    @Test fun `all five typed Planning owner families admitted without Context and exact task navigation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); scope(f)
            val mission = f.db.tacticalMissionDao().insertMission(TacticalMission(title = "Mission", description = null, deadline = 0, projectId = null))
            f.db.arcQuestDao().insertAll(listOf(ArcQuestEntity(id = "quest", arcKey = "arc", title = "Quest")))
            f.db.dayPlanDao().insert(DayPlan(id = "day", date = 1))
            f.db.dayTaskDao().insert(DayTask(id = "task", dayPlanId = "day", title = "Task"))
            f.db.dayFocusItemDao().insert(DayFocusItem(id = "focus", dayPlanId = "day", title = "Focus"))
            listOf(PlanningReferenceOwnerKind.SCOPE to "scope", PlanningReferenceOwnerKind.TACTICAL_MISSION to mission.toString(),
                PlanningReferenceOwnerKind.ARC_QUEST to "quest", PlanningReferenceOwnerKind.DAY_TASK to "task", PlanningReferenceOwnerKind.DAY_FOCUS to "focus")
                .forEach { (kind, id) -> attach(f, kind = kind, id = id) }
            val rows = repository(f).current { it.relations.size == 5 }.relations
            assertThat(rows.single { it.targetType == "DAY_TASK" }.navigation)
                .isEqualTo(com.romankozak.forwardappmobile.core.navigation.NavTarget.EditTask("task"))
            assertThat(rows.filter { it.targetType != "DAY_TASK" }.all { it.navigation == null }).isTrue()
        }
    }
    @Test fun `deleted Workspace excludes retained relations and raw same ID does not infer target type`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); scope(f); attach(f)
            val row = f.db.planningReferenceDao().getAll().single()
            f.db.planningReferenceDao().upsert(row.copy(id = "other", targetKind = "ORIENTATION"))
            assertThat(repository(f).current().relations).hasSize(1)
            val workspace = f.db.workspaceDao().getById("workspace")!!
            f.db.workspaceDao().upsert(listOf(workspace.copy(isDeleted = true)))
            assertThat(repository(f).current().relations).isEmpty()
        }
    }
    @Test fun `capability lifecycle dirty metadata restore and exact-version ACK use existing kernel`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val repo = capability(f)
            assertThat(repo.observeEnabled("workspace").first()).isFalse()
            repo.setEnabled("workspace", true, 20)
            val initial = f.dao.getAllWorkspaceCapabilities().single()
            assertThat(initial.capabilityType).isEqualTo("RELATIONS")
            assertThat(initial.configuration).isEqualTo("{}")
            assertThat(initial.syncedAt).isNull()
            assertThat(repo.observeEnabled("workspace").first()).isTrue()
            repo.setEnabled("workspace", false, 30)
            assertThat(repo.observeEnabled("workspace").first()).isFalse()
            val disabled = f.dao.getAllWorkspaceCapabilities().single()
            assertThat(disabled.version).isEqualTo(initial.version + 1)
            repo.archive("workspace", 35)
            repo.restore("workspace", 40)
            assertThat(repo.isEnabled("workspace")).isFalse() // Restore does not mean enable.
            repo.setEnabled("workspace", true, 50)
            val enabled = f.dao.getAllWorkspaceCapabilities().single()
            assertThat(enabled.id).isEqualTo(initial.id)
            assertThat(enabled.capabilityOrder).isEqualTo(initial.capabilityOrder)
            assertThat(f.dao.markWorkspaceCapabilitySynced(initial.id, initial.version, 60)).isEqualTo(0)
            assertThat(f.dao.getAllWorkspaceCapabilities().single().syncedAt).isNull()
            val snapshot = f.snapshot()
            f.admit(snapshot, merge = false); f.admit(snapshot, merge = true)
            assertThat(f.dao.getAllWorkspaceCapabilities().single()).isEqualTo(enabled)
            repo.delete("workspace", 70)
            assertThat(repo.observeEnabled("workspace").first()).isFalse()
        }
    }
    @Test fun `Context backed Workspace capability needs no Context shell or inferred relation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val w = f.db.workspaceDao().getById("workspace")!!
            f.db.workspaceDao().upsert(listOf(w.copy(provenance = "CONTEXT_BACKED", sourceContextId = "old-context")))
            capability(f).setEnabled("workspace", true, 20)
            assertThat(capability(f).observeEnabled("workspace").first()).isTrue()
            assertThat(repository(f).current().relations).isEmpty()
        }
    }
}
