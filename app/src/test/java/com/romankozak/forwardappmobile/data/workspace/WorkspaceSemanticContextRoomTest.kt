package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.workspace.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceSemanticContextRoomTest {
    private fun repository(f: SemanticDeletionRoomFixture): WorkspaceRelationsRepository {
        val planning = CanonicalPlanningReferenceRepository(f.db)
        return WorkspaceRelationsRepository(f.bindings, CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()), planning,
            EmbodiedAspectRelationsRepository(f.aspects, f.links, f.bindings, planning),
            EmbodiedOrientationRelationsRepository(f.links, f.graph))
    }
    private suspend fun WorkspaceRelationsRepository.current() = withTimeout(10_000) { observe("workspace").first() }

    @Test fun `Orientation context uses canonical membership graph and exact edit route without any writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val source = f.orientation(); val other = f.orientation(); val aspect = f.aspect()
            f.bindings.bind(source, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.links.linkOrientation(aspect, source, AspectOrientationRelationType.BELONGS_TO, now = 21)
            f.graph.addRelation(other, source, OrientationRelationType.SUPPORTS, 22)
            f.appearance(source)
            val before = f.rows()
            val projection = repository(f).current()
            assertThat(projection.embodiedAspect).isNull()
            assertThat(projection.embodiedOrientation!!.relations.map { it.targetId }).containsExactly(aspect, other)
            assertThat(projection.semanticContextEditAction()!!.destination).isEqualTo(NavTarget.SemanticEntityEditor(source))
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `same subscription reacts to editor membership graph rename and source binding removal`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val source = f.orientation(); val other = f.orientation(); val aspect = f.aspect()
            val binding = f.bindings.bind(source, "workspace", WorkspaceBindingType.EMBODIES, 20)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repository(f).observe("workspace").stateIn(scope)
                assertThat(state.value.embodiedOrientation!!.relations).isEmpty()
                val membership = f.links.linkOrientation(aspect, source, AspectOrientationRelationType.BELONGS_TO, now = 21)
                withTimeout(10_000) { state.first { it.embodiedOrientation?.relations?.size == 1 } }
                val graph = f.graph.addRelation(source, other, OrientationRelationType.DEPENDS_ON, 22)
                withTimeout(10_000) { state.first { it.embodiedOrientation?.relations?.size == 2 } }
                f.dao.upsertManagedSubjects(listOf(f.dao.getManagedSubject(other)!!.copy(title = "Renamed target")))
                withTimeout(10_000) { state.first { it.embodiedOrientation?.relations?.any { row -> row.title == "Renamed target" } == true } }
                f.graph.removeRelation(graph, 1, 23)
                f.links.unlinkOrientation(membership, 24)
                withTimeout(10_000) { state.first { it.embodiedOrientation?.relations?.isEmpty() == true } }
                f.bindings.unbind(binding, 25)
                withTimeout(10_000) { state.first { it.embodiedOrientation == null && it.semanticContextEditAction() == null } }
            } finally { scope.cancel() }
        }
    }

    @Test fun `delegated Aspect move updates Workspace context but never Navigation placement`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val source = f.aspect(); val parent = f.aspects.create("New parent", now = 11)
            f.bindings.bind(source, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.appearance(source)
            val repo = repository(f)
            assertThat(repo.current().semanticContextEditAction()!!.destination).isEqualTo(NavTarget.SemanticEntityEditor(source))
            val before = f.rows()
            f.aspects.move(source, parent, now = 30)
            val after = f.rows()
            assertThat(after.keys.filter { before[it] != after[it] }).containsExactly("aspects", "managed_subjects")
            val context = repo.current().embodiedAspect!!
            assertThat(context.relations.single { it.targetId == parent }.kinds).containsExactly("PARENT_ASPECT")
        }
    }

    @Test fun `deleted target source and Workspace never resurrect semantic context`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val source = f.orientation(); val other = f.orientation()
            f.bindings.bind(source, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.graph.addRelation(source, other, OrientationRelationType.SUPPORTS, 21)
            f.delete(other)
            assertThat(repository(f).current().embodiedOrientation!!.relations).isEmpty()
            f.delete(source)
            assertThat(repository(f).current().semanticContextEditAction()).isNull()
            f.db.workspaceDao().upsert(listOf(f.db.workspaceDao().getById("workspace")!!.copy(isDeleted = true)))
            assertThat(repository(f).current().relations).isEmpty()
        }
    }

    @Test fun `no source other binding kinds or ambiguous EMBODIES have no fake hierarchy or edit action`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val orientation = f.orientation(); val aspect = f.aspect()
            f.appearance(orientation)
            val repo = repository(f)
            assertThat(repo.current().semanticContextEditAction()).isNull()
            f.bindings.bind(orientation, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            assertThat(repo.current().embodiedOrientation).isNull()
            f.bindings.bind(orientation, "workspace", WorkspaceBindingType.EMBODIES, 21)
            val existing = f.dao.getAllWorkspaceBindings().single { it.bindingType == "EMBODIES" }
            f.dao.upsertWorkspaceBindings(listOf(existing.copy(id = "ambiguous", subjectId = aspect)))
            val ambiguous = repo.current()
            assertThat(ambiguous.embodiedAspect).isNull()
            assertThat(ambiguous.embodiedOrientation).isNull()
            assertThat(ambiguous.semanticContextEditAction()).isNull()
        }
    }
}
