package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayFocusOrientationSourcesTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation"), "Health")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace"), "Health work")
    private val rows = MutableStateFlow(listOf(
        PlanningReferenceEntity("reference", "DAY_FOCUS", "focus", "ORIENTATION", "orientation", "SOURCE", 1, 1, null, 1, false),
    ))
    private val entities = MutableStateFlow(listOf(orientation, workspace))
    private val bindings = MutableStateFlow(emptyList<WorkspaceBindingEntity>())
    private val references = mockk<CanonicalPlanningReferenceRepository> { every { observeAll() } returns rows }
    private val catalog = mockk<CanonicalEntityCatalogRepository> { every { observeCatalog() } returns entities }
    private val dao = mockk<OrientationDao> { every { observeWorkspaceBindings() } returns bindings }
    private val navigation = mockk<DefaultNavigationDispatcher>(relaxed = true)
    private val store = ViewModelStore()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model() = DayFocusOrientationSourcesViewModel(references, catalog, dao, navigation, dispatcher)
        .also { store.put("model", it) }

    @Test fun `priority source opens its live embodying workspace then falls back to orientation`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.snapshot.collect {} }
        assertEquals(orientation, model.snapshot.value.sources["focus"])

        bindings.value = listOf(WorkspaceBindingEntity("binding", "workspace", "orientation", "EMBODIES", true, 0, 1, 1, null, false, 1))
        model.open("focus")
        verify(exactly = 1) { navigation.navigate(NavTarget.Workspace("workspace")) }

        entities.value = listOf(orientation)
        model.open("focus")
        verify(exactly = 1) { navigation.navigate(NavTarget.SemanticEntityEditor("orientation")) }
    }

    @Test fun `removed or non source reference cannot open from priority card`() = runTest {
        val model = model()
        backgroundScope.launch(dispatcher) { model.snapshot.collect {} }

        model.open("another-focus")
        rows.value = rows.value.map { it.copy(role = "RELATED") }
        assertTrue(model.snapshot.value.sources.isEmpty())
        model.open("focus")
        rows.value = emptyList()
        model.open("focus")

        verify(exactly = 0) { navigation.navigate(any<NavTarget>()) }
    }
}
