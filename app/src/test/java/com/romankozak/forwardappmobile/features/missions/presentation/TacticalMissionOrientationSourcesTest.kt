package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TacticalMissionOrientationSourcesTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Health")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("o"), "Not the Orientation")
    private fun row(owner: String = "1", role: String = "SOURCE", kind: String = "ORIENTATION") =
        PlanningReferenceEntity("r:$owner:$role:$kind", "TACTICAL_MISSION", owner, kind, "o", role, 1, 1, null, 1, false)
    private val rows = MutableStateFlow(listOf(row()))
    private val catalog = MutableStateFlow(listOf(orientation, workspace))
    private val references = mockk<CanonicalPlanningReferenceRepository> { every { observeAll() } returns rows }
    private val entities = mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns catalog }
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val dao = mockk<OrientationDao> { every { observeWorkspaceBindings() } returns bindings }
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }
    private fun model() = TacticalMissionOrientationSourcesViewModel(references, entities, dao, navigation, dispatcher)
        .also { store.put("sources", it) }

    @Test fun `exact typed Orientation SOURCE is keyed by exact Mission identity`() {
        val result = projectTacticalOrientationSources(listOf(row("42")), catalog.value)
        assertEquals(mapOf(42L to orientation), result)
    }

    @Test fun `RELATED and other target or owner classes do not masquerade as source Orientation`() {
        val ignored = listOf(row(role = "RELATED"), row(kind = "WORKSPACE"), row(kind = "ASPECT"),
            row().copy(ownerKind = "ARC_QUEST"), row().copy(isDeleted = true))
        ignored.forEach { assertTrue(projectTacticalOrientationSources(listOf(it), catalog.value).isEmpty()) }
    }

    @Test fun `same raw ID across types never falls back to Workspace`() {
        assertTrue(projectTacticalOrientationSources(listOf(row()), listOf(workspace)).isEmpty())
        assertEquals(orientation, projectTacticalOrientationSources(listOf(row()), catalog.value)[1L])
    }

    @Test fun `ambiguous SOURCE ownership fails closed including contradictory target types`() {
        assertTrue(projectTacticalOrientationSources(listOf(row(), row(kind = "WORKSPACE")), catalog.value).isEmpty())
        assertTrue(projectTacticalOrientationSources(listOf(row(), row().copy(id = "duplicate")), catalog.value).isEmpty())
    }

    @Test fun `malformed noncanonical or missing Mission identity is not reinterpreted`() {
        listOf("", "0", "-1", "01", "+1", "other").forEach {
            assertTrue(projectTacticalOrientationSources(listOf(row(it)), catalog.value).isEmpty())
        }
    }

    @Test fun `reference removal rename and deleted source update reactively with one bulk read`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        assertEquals("Health", model.sources.value.getValue(1L).title)
        catalog.value = listOf(orientation.copy(title = "Renamed"), workspace)
        assertEquals("Renamed", model.sources.value.getValue(1L).title)
        rows.value = listOf(row().copy(isDeleted = true))
        assertTrue(model.sources.value.isEmpty())
        rows.value = listOf(row(), row("2"), row("3"))
        assertEquals(3, model.sources.value.size)
        catalog.value = listOf(workspace)
        assertTrue(model.sources.value.isEmpty())
        verify(exactly = 1) { references.observeAll() }
        verify(exactly = 1) { entities.observeCatalog() }
    }

    @Test fun `click opens exact semantic editor and ignores a stale missing source`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        model.open("o")
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
        catalog.value = emptyList()
        model.open("o")
        verify(exactly = 1) { navigation.navigate(any<NavTarget>()) }
    }

    @Test fun `Mission without explicit SOURCE remains without a badge`() {
        assertTrue(projectTacticalOrientationSources(emptyList(), catalog.value).isEmpty())
    }

    private fun binding(kind: String = "EMBODIES") = WorkspaceBindingEntity("binding", "o", "o", kind, true, 0, 1, 1, null, false, 1)

    @Test fun `Properties RELATED badges update reactively and open embodiment without becoming SOURCE`() = runTest {
        rows.value = listOf(row(role = "RELATED"))
        val model = model()
        backgroundScope.launch(dispatcher) { model.related.collect {} }
        assertEquals(listOf(orientation), model.related.value.getValue(1L))
        bindings.value = listOf(binding()); model.open("o")
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("o")) }
        catalog.value = listOf(orientation.copy(title = "Renamed"), workspace)
        assertEquals("Renamed", model.related.value.getValue(1L).single().title)
        bindings.value = emptyList(); model.open("o")
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
        rows.value = emptyList(); assertTrue(model.related.value.isEmpty()); model.open("o")
        verify(exactly = 2) { navigation.navigate(any<NavTarget>()) }
        verify(exactly = 1) { references.observeAll() }
    }

    @Test fun `source badge opens live EMBODIES Workspace and falls back after unbinding or deletion`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        bindings.value = listOf(binding())
        model.open("o")
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("o")) }
        bindings.value = emptyList()
        model.open("o")
        bindings.value = listOf(binding())
        catalog.value = listOf(orientation)
        model.open("o")
        verify(exactly = 2) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
        verify(exactly = 1) { dao.observeWorkspaceBindings() }
    }

    @Test fun `other relation kinds tombstones and ambiguous EMBODIES keep semantic navigation`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.sources.collect {} }
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach {
            bindings.value = listOf(binding(it)); model.open("o")
        }
        bindings.value = listOf(binding().copy(isDeleted = true)); model.open("o")
        bindings.value = listOf(binding(), binding().copy(id = "duplicate")); model.open("o")
        verify(exactly = 5) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
        verify(exactly = 0) { navigation.navigate(match<NavTarget> { it is NavTarget.Workspace }) }
    }
}
