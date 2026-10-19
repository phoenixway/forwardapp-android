package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BigPictureSourceDeleteFocusTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `deleted focused occurrence recovers live structural parent and persists it`() = runTest(dispatcher) {
        val rows = MutableStateFlow(listOf(p("parent"), p("child", "parent")))
        val saved = SavedStateHandle()
        val vm = model(rows, saved)
        backgroundScope.launch(dispatcher) { vm.uiState.collect() }
        vm.focusPlacement("parent")
        vm.focusPlacement("child")
        vm.rememberCurrentLocation()
        rows.value = rows.value.filter { it.id.value != "child" }
        vm.recoverMissingFocus()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode!!.placement.id.value).isEqualTo("parent")
        assertThat(saved.get<String>("bigPictureFocus")).isEqualTo("parent")
    }

    @Test fun `deleted focused root and every same-target ancestor fall back to root not another appearance`() = runTest(dispatcher) {
        val rows = MutableStateFlow(listOf(p("parent"), p("child", "parent")))
        val saved = SavedStateHandle()
        val vm = model(rows, saved)
        backgroundScope.launch(dispatcher) { vm.uiState.collect() }
        vm.focusPlacement("parent")
        vm.focusPlacement("child")
        vm.rememberCurrentLocation()
        rows.value = emptyList()
        vm.recoverMissingFocus()
        assertThat(saved.get<String>("bigPictureFocus")).isNull()
        assertThat(saved.get<String>("bigPictureScope")).isNull()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).navigationBreadcrumbs).isEmpty()
    }

    @Test fun `reconcile clipboard drops only missing exact structural identity`() {
        val writer = mockk<com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator>()
        val vm = BigPictureMutationViewModel(writer, SavedStateHandle())
        vm.cut(H1TypedNode.Occurrence("link"))
        vm.discardMissingClipboard(setOf(H1TypedNode.Occurrence("primary")))
        assertThat(vm.clipboard.value).isNull()
        vm.cut(H1TypedNode.Container("section"))
        vm.discardMissingClipboard(setOf(H1TypedNode.Container("section")))
        assertThat(vm.clipboard.value).isEqualTo("container:section")
        verify { writer wasNot Called }
    }

    private fun model(rows: MutableStateFlow<List<HierarchyPlacement>>, saved: SavedStateHandle): BigPictureViewModel {
        val repository = mockk<BigPictureStructureRepository>()
        every { repository.observe() } returns rows.map { BigPictureStructure(it) }
        val catalog = mockk<CanonicalEntityCatalogRepository>()
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("parent"), "Parent"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("child"), "Child")))
        return BigPictureViewModel(repository, catalog, saved, dispatcher)
    }
    private fun p(id: String, parent: String? = null) = HierarchyPlacement(PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id), parent?.let(::PlacementId), PlacementKind.PRIMARY, 0, 1, 1, null, false, 1)
}
