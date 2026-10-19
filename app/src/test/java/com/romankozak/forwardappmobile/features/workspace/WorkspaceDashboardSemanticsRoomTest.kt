package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.workspaceDeletionRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.WorkspacePropertiesUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.WorkspacePropertiesViewModel
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkspaceDashboardSemanticsRoomTest {
    @Before fun setup() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `Properties add and remove update dashboard for standalone and Context-backed Workspace without side effects`() = runBlocking<Unit> {
        for (backed in listOf(false, true)) fixture { f, models ->
            f.workspace()
            if (backed) {
                f.db.contextDao().insert(Context(id = "workspace", name = "Legacy meaning is not authority", description = null,
                    parentId = null, createdAt = 1, updatedAt = 1))
                val current = f.db.workspaceDao().getById("workspace")!!
                f.db.workspaceDao().upsert(listOf(current.copy(provenance = "CONTEXT_BACKED", sourceContextId = "workspace")))
            }
            val orientation = f.orientation()
            val aspect = f.aspect()
            f.orientation() // Canonical but unbound: must never be inferred from anything else.
            val before = f.rows()
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
            val dashboard = models.keep("dashboard", WorkspaceLinkedSemanticsViewModel(
                SavedStateHandle(mapOf("workspaceId" to "workspace", "listId" to "wrong-context")), f.bindings, catalog))
            val properties = models.keep("properties", WorkspacePropertiesViewModel(SavedStateHandle(mapOf("workspaceId" to "workspace")),
                workspaceDeletionRepository(f.db), f.bindings, catalog))
            assertThat(dashboard.ready { true }.links).isEmpty()
            suspend fun propertiesReady(count: Int) = withTimeout(10_000) {
                properties.uiState.first { it is WorkspacePropertiesUiState.Ready &&
                    it.model.bindings.size == count && !it.model.isRelationMutationInFlight } as WorkspacePropertiesUiState.Ready
            }
            propertiesReady(0)
            properties.acceptPickerResult(setOf(CanonicalEntityRef.Orientation(orientation)))
            properties.addBinding(WorkspaceBindingType.SUPPORTS)
            val first = dashboard.ready { it.links.size == 1 }.links.single()
            assertThat(first.title).isEqualTo("Meaning")
            assertThat(first.bindingTypes).containsExactly("SUPPORTS")
            assertThat(first.navigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor(orientation))
            propertiesReady(1)
            properties.acceptPickerResult(setOf(CanonicalEntityRef.Aspect(aspect)))
            properties.addBinding(WorkspaceBindingType.REALIZES)
            val both = dashboard.ready { it.links.size == 2 }.links
            assertThat(both.single { it.ref is CanonicalEntityRef.Aspect }.title).isEqualTo("Health")
            assertThat(both.single { it.ref is CanonicalEntityRef.Aspect }.navigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor(aspect))
            val bound = propertiesReady(2)
            properties.removeBinding(bound.model.bindings.single { it.subjectId == orientation }.bindingId)
            assertThat(dashboard.ready { it.links.size == 1 }.links.single().ref).isEqualTo(CanonicalEntityRef.Aspect(aspect))
            val remaining = propertiesReady(1)
            properties.removeBinding(remaining.model.bindings.single().bindingId)
            assertThat(dashboard.ready { it.links.isEmpty() }.links).isEmpty()
            assertThat(f.rows().filterKeys { it != "workspace_bindings" })
                .isEqualTo(before.filterKeys { it != "workspace_bindings" })
            assertThat(f.db.contextDao().getAllRaw()).hasSize(if (backed) 1 else 0)
        }
    }

    @Test fun `canonical title changes and target deletion propagate without refresh and cold recreation stays correct`() = runBlocking<Unit> {
        fixture { f, models ->
            f.workspace()
            val orientation = f.orientation()
            val aspect = f.aspect()
            f.bindings.bind(orientation, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.bindings.bind(orientation, "workspace", WorkspaceBindingType.MONITORS, 21)
            f.bindings.bind(aspect, "workspace", WorkspaceBindingType.REALIZES, 22)
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
            fun model(key: String) = models.keep(key, WorkspaceLinkedSemanticsViewModel(SavedStateHandle(mapOf("workspaceId" to "workspace")), f.bindings, catalog))
            val dashboard = model("dashboard")
            val initial = dashboard.ready { it.links.size == 2 }
            assertThat(initial.links.single { it.ref!!.id == orientation }.bindingTypes).containsExactly("EMBODIES", "MONITORS").inOrder()
            f.orientations.updateOrientationSemantics(orientation, "Renamed Orientation", null, 1, 30)
            f.aspects.updateDetails(aspect, "Renamed Aspect", null, 1, 31)
            dashboard.ready { state -> state.links.map { it.title }.toSet() == setOf("Renamed Orientation", "Renamed Aspect") }
            f.delete(orientation, version = 2)
            assertThat(dashboard.ready { it.links.size == 1 && it.links.single().ref?.id == aspect }.links.single().navigationTarget())
                .isEqualTo(NavTarget.SemanticEntityEditor(aspect))
            f.delete(aspect, ManagedSubjectType.ASPECT, 2)
            dashboard.ready { it.links.isEmpty() }
            val beforeRead = f.rows()
            assertThat(model("recreated").ready { true }.links).isEmpty()
            assertThat(f.rows()).isEqualTo(beforeRead)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
            assertThat(f.db.contextDao().getAllRaw()).isEmpty()
        }
    }

    @Test fun `unavailable canonical target has disabled navigation and dashboard does not repair or resurrect it`() = runBlocking<Unit> {
        fixture { f, models ->
            f.workspace()
            val id = f.orientation()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            // Persisted legacy inconsistency: read projection must fail closed without taking lifecycle ownership.
            f.dao.upsertManagedSubjects(listOf(f.dao.getManagedSubject(id)!!.copy(isDeleted = true)))
            val before = f.rows()
            val dashboard = models.keep("dashboard", WorkspaceLinkedSemanticsViewModel(SavedStateHandle(mapOf("workspaceId" to "workspace")),
                f.bindings, CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())))
            val unavailable = dashboard.ready { it.links.size == 1 }.links.single()
            assertThat(unavailable.ref).isNull()
            assertThat(unavailable.navigationTarget()).isNull()
            assertThat(unavailable.dashboardDescription()).contains("Недоступна сутність")
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    private suspend fun WorkspaceLinkedSemanticsViewModel.ready(predicate: (WorkspaceLinkedSemanticsUiState.Ready) -> Boolean) =
        withTimeout(10_000) { uiState.first { it is WorkspaceLinkedSemanticsUiState.Ready && predicate(it) } as WorkspaceLinkedSemanticsUiState.Ready }

    private class Models {
        val store = ViewModelStore()
        val jobs = mutableListOf<Job>()
        fun <T : ViewModel> keep(key: String, model: T): T = model.also {
            store.put(key, it); jobs += it.viewModelScope.coroutineContext[Job]!!
        }
        suspend fun close() { store.clear(); jobs.forEach { it.join() } }
    }

    private suspend fun fixture(block: suspend (SemanticDeletionRoomFixture, Models) -> Unit) {
        SemanticDeletionRoomFixture().use { f ->
            val models = Models()
            try { block(f, models) } finally { models.close() }
        }
    }
}
