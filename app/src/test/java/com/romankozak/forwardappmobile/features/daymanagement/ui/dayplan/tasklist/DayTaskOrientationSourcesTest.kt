package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.projectPlanningOrientationSources
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DayTaskOrientationSourcesTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("source"), "Health")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("work"), "Operational")
    private fun row() = PlanningReferenceEntity("reference", "DAY_TASK", "exact-task", "ORIENTATION", "source", "SOURCE", 1, 1, null, 1, false)
    private fun binding(kind: String = "EMBODIES") = WorkspaceBindingEntity("binding", "work", "source", kind, true, 0, 1, 1, null, false, 1)
    private val rows = MutableStateFlow(listOf(row()))
    private val catalog = MutableStateFlow(listOf(orientation, workspace))
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val references = mockk<CanonicalPlanningReferenceRepository> { every { observeAll() } returns rows }
    private val entities = mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns catalog }
    private val dao = mockk<OrientationDao> { every { observeWorkspaceBindings() } returns bindings }
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun model() = DayTaskOrientationSourcesViewModel(references, entities, dao, navigation, dispatcher).also { store.put("model", it) }

    @Test fun `Properties RELATED badges react to edits deletion and exact EMBODIES navigation`() = runTest {
        rows.value = listOf(row().copy(role = "RELATED"))
        val model = model()
        backgroundScope.launch(dispatcher) { model.related.collect {} }
        assertEquals(listOf(orientation), model.related.value.getValue("exact-task"))
        bindings.value = listOf(binding()); model.open("source")
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("work")) }
        catalog.value = listOf(orientation.copy(title = "Renamed"), workspace)
        assertEquals("Renamed", model.related.value.getValue("exact-task").single().title)
        bindings.value = emptyList(); model.open("source")
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("source")) }
        catalog.value = listOf(workspace); assertTrue(model.related.value.isEmpty()); model.open("source")
        verify(exactly = 2) { navigation.navigate(any<NavTarget>()) }
        verify(exactly = 1) { references.observeAll() }
    }

    @Test fun `exact DAY_TASK SOURCE shows canonical Orientation title`() {
        assertEquals(mapOf("exact-task" to orientation), projectPlanningOrientationSources(rows.value, catalog.value, PlanningReferenceOwnerKind.DAY_TASK))
    }

    @Test fun `RELATED other owners targets and missing subjects do not infer an Orientation source`() {
        val ignored = listOf(row().copy(role = "RELATED"), row().copy(ownerKind = "TACTICAL_MISSION"),
            row().copy(targetKind = "WORKSPACE"), row().copy(targetKind = "ASPECT"), row().copy(isDeleted = true), row().copy(ownerId = ""))
        ignored.forEach { assertTrue(projectPlanningOrientationSources(listOf(it), catalog.value, PlanningReferenceOwnerKind.DAY_TASK).isEmpty()) }
        assertTrue(projectPlanningOrientationSources(rows.value, listOf(workspace.copy(ref = CanonicalEntityRef.Workspace("source"))), PlanningReferenceOwnerKind.DAY_TASK).isEmpty())
    }

    @Test fun `ambiguous SOURCE fails closed even across typed targets`() {
        val duplicate = row().copy(id = "second", targetKind = "WORKSPACE")
        assertTrue(projectPlanningOrientationSources(listOf(row(), duplicate), catalog.value, PlanningReferenceOwnerKind.DAY_TASK).isEmpty())
    }

    @Test fun `normal source opens exact semantic editor`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        model.open("source")
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("source")) }
        model.open("unknown")
        verify(exactly = 1) { navigation.navigate(any<NavTarget>()) }
    }

    @Test fun `binding changes and Workspace deletion update primary navigation reactively`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        bindings.value = listOf(binding()); model.open("source")
        bindings.value = emptyList(); model.open("source")
        bindings.value = listOf(binding()); catalog.value = listOf(orientation); model.open("source")
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("work")) }
        verify(exactly = 2) { navigation.navigate(NavTarget.SemanticEntityEditor("source")) }
    }

    @Test fun `non EMBODIES deleted and ambiguous bindings never redirect`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach { bindings.value = listOf(binding(it)); model.open("source") }
        bindings.value = listOf(binding().copy(isDeleted = true)); model.open("source")
        bindings.value = listOf(binding(), binding().copy(id = "second")); model.open("source")
        verify(exactly = 5) { navigation.navigate(NavTarget.SemanticEntityEditor("source")) }
        verify(exactly = 0) { navigation.navigate(match<NavTarget> { it is NavTarget.Workspace }) }
    }

    @Test fun `rename reference removal and source deletion update badges from bulk snapshots`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        catalog.value = listOf(orientation.copy(title = "Renamed"), workspace)
        assertEquals("Renamed", model.sources.value.getValue("exact-task").title)
        rows.value = emptyList(); assertTrue(model.sources.value.isEmpty())
        rows.value = listOf(row()); assertEquals(1, model.sources.value.size)
        catalog.value = listOf(workspace); assertTrue(model.sources.value.isEmpty())
        model.open("source"); verify(exactly = 0) { navigation.navigate(any<NavTarget>()) }
        verify(exactly = 1) { references.observeAll() }
        verify(exactly = 1) { entities.observeCatalog() }
        verify(exactly = 1) { dao.observeWorkspaceBindings() }
    }
}
