package com.romankozak.forwardappmobile.features.planning

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningReferencesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val owner = PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task")
    private val refs = MutableStateFlow(emptyList<PlanningReferenceEntity>())
    private val entities = MutableStateFlow(emptyList<CanonicalEntitySummary>())
    private val repository = mockk<CanonicalPlanningReferenceRepository> { every { observeAll() } returns refs }
    private val catalog = mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns entities }
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()
    private fun model() = PlanningReferencesViewModel(repository, catalog, navigation, dispatcher).also { store.put("model", it) }
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun `same raw ID retains canonical class exact editor and Workspace routes`() {
        val semantic = CanonicalEntityRef.Orientation("same/id:? ü")
        val workspace = CanonicalEntityRef.Workspace(semantic.id)
        assertThat(semantic.toPlanningTarget()).isNotEqualTo(workspace.toPlanningTarget())
        assertThat(semantic.toPlanningTarget().toCanonicalRef()).isEqualTo(semantic)
        assertThat(semantic.planningNavigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor(semantic.id))
        assertThat(CanonicalEntityRef.Aspect("aspect").planningNavigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor("aspect"))
        assertThat(workspace.planningNavigationTarget()).isEqualTo(NavTarget.Workspace(workspace.id))
        val projected = projectPlanningReferences(listOf(row("a", "ORIENTATION", semantic.id), row("b", "WORKSPACE", workspace.id)),
            listOf(CanonicalEntitySummary(semantic, "Semantic"), CanonicalEntitySummary(workspace, "Operational")), owner)
        assertThat(projected.map { it.entity.title }).containsExactly("Semantic", "Operational")
    }

    @Test fun `Properties references react to add remove title and endpoint disappearance`() = runTest {
        val model = model()
        model.selectOwner(owner)
        val collect = backgroundScope.launch(dispatcher) { model.links.collect() }
        val entity = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Old")
        entities.value = listOf(entity)
        assertThat(model.links.value).isEmpty()
        refs.value = listOf(row("r", "ORIENTATION", "o"))
        assertThat(model.links.value.single().entity.title).isEqualTo("Old")
        entities.value = listOf(entity.copy(title = "New"))
        assertThat(model.links.value.single().entity.title).isEqualTo("New")
        refs.value = refs.value.map { it.copy(isDeleted = true, version = 2) }
        assertThat(model.links.value).isEmpty()
        refs.value = listOf(row("r", "ORIENTATION", "o"))
        entities.value = emptyList()
        assertThat(model.links.value).isEmpty()
        collect.cancel()
    }

    @Test fun `RELATED writes only reference authority and guards double submission`() = runTest {
        val model = model()
        model.selectOwner(owner)
        val gate = CompletableDeferred<Unit>()
        val ref = CanonicalEntityRef.Aspect("a")
        coEvery { repository.attach(owner, ref.toPlanningTarget(), PlanningReferenceRole.RELATED, any()) } coAnswers { gate.await(); "reference" }
        model.attach(ref)
        model.attach(ref)
        assertThat(model.busy.value).isTrue()
        coVerify(exactly = 1) { repository.attach(owner, ref.toPlanningTarget(), PlanningReferenceRole.RELATED, any()) }
        gate.complete(Unit)
        assertThat(model.busy.value).isFalse()
    }

    @Test fun `failure is visible and recreation never replays a reference command`() = runTest {
        val model = model()
        model.selectOwner(owner)
        coEvery { repository.attach(any(), any(), any(), any()) } throws IllegalStateException("Deleted target")
        model.attach(CanonicalEntityRef.Workspace("w"))
        assertThat(model.error.value).isEqualTo("Deleted target")
        assertThat(model.busy.value).isFalse()
        model()
        coVerify(exactly = 1) { repository.attach(any(), any(), any(), any()) }
    }

    @Test fun `reused picker admits three canonical classes single selection and creates nothing on cancellation`() {
        val model = model()
        val picker = model.newPicker()
        store.put("picker", picker)
        assertThat(picker.request.allowedClasses).containsExactlyElementsIn(CanonicalEntityClass.entries)
        assertThat(picker.request.selectionMode).isEqualTo(CanonicalEntitySelectionMode.SINGLE)
        assertThat(picker.request.excludedRefs).isEmpty()
        coVerify(exactly = 0) { repository.attach(any(), any(), any(), any()) }
    }

    private fun row(id: String, kind: String, target: String) =
        PlanningReferenceEntity(id, owner.kind.name, owner.id, kind, target, "RELATED", 1, 1, null, 1, false)
}
