package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StrategicQuestReferenceNavigationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Meaning")
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Facet")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Work")
    private val entities = MutableStateFlow(listOf(orientation, aspect, workspace))
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()

    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { store.clear(); Dispatchers.resetMain() }

    private fun model() = StrategicQuestReferencesViewModel(
        mockk<CanonicalPlanningReferenceRepository> {
            every { observeAll() } returns MutableStateFlow(emptyList<PlanningReferenceEntity>())
        },
        mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns entities },
        mockk<OrientationDao> { every { observeWorkspaceBindings() } returns bindings },
        navigation,
    ).also { store.put("model", it) }

    private fun binding(subject: String = "o", kind: String = "EMBODIES") =
        WorkspaceBindingEntity("binding", "w", subject, kind, true, 0, 1, 1, null, false, 1)

    @Test fun `Orientation and Aspect badges open exact embodying Workspace`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        bindings.value = listOf(binding())
        model.open(orientation.ref)
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("w")) }
        bindings.value = listOf(binding("a"))
        model.open(aspect.ref)
        verify(exactly = 2) { navigation.navigate(NavTarget.Workspace("w")) }
        model.open(workspace.ref)
        verify(exactly = 3) { navigation.navigate(NavTarget.Workspace("w")) }
    }

    @Test fun `binding removal and Workspace deletion restore semantic editor navigation reactively`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        model.open(orientation.ref)
        bindings.value = listOf(binding())
        model.open(orientation.ref)
        bindings.value = emptyList()
        model.open(orientation.ref)
        bindings.value = listOf(binding())
        entities.value = listOf(orientation, aspect)
        model.open(orientation.ref)
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("w")) }
        verify(exactly = 3) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
    }

    @Test fun `non embodying tombstoned or ambiguous bindings do not redirect`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.links.collect {} }
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach {
            bindings.value = listOf(binding(kind = it))
            model.open(orientation.ref)
        }
        bindings.value = listOf(binding().copy(isDeleted = true))
        model.open(orientation.ref)
        bindings.value = listOf(binding(), binding().copy(id = "duplicate"))
        model.open(orientation.ref)
        verify(exactly = 5) { navigation.navigate(NavTarget.SemanticEntityEditor("o")) }
        verify(exactly = 0) { navigation.navigate(match<NavTarget> { it is NavTarget.Workspace }) }
    }
}
