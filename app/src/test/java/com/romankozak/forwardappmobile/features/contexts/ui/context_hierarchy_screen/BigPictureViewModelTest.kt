package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_BEACON_SCOPE_ID
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BigPictureViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<BigPictureStructureRepository>()
    private val catalog = mockk<CanonicalEntityCatalogRepository>()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `waits for placement and catalog readiness then renders true empty state`() = runTest(dispatcher) {
        val placements = MutableSharedFlow<List<HierarchyPlacement>>(replay = 1)
        val entities = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
        every { repository.observe() } returns placements.map { BigPictureStructure(it) }
        every { catalog.observeCatalog() } returns entities
        val vm = BigPictureViewModel(repository, catalog, SavedStateHandle(), dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        placements.emit(emptyList())
        assertThat(vm.uiState.value).isEqualTo(BigPictureUiState.Loading)
        entities.emit(emptyList())
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).isEmpty()
    }

    @Test fun `focus restores navigates breadcrumbs and never invokes mutation authority`() = runTest(dispatcher) {
        val placements = MutableStateFlow(listOf(p("root", "w"), p("child", "o", "root")))
        every { repository.observe() } returns placements.map { BigPictureStructure(it) }
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Orientation"),
        ))
        val state = SavedStateHandle(mapOf("bigPictureFocus" to "root"))
        val vm = BigPictureViewModel(repository, catalog, state, dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).hasSize(1)
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode?.placement?.id?.value).isEqualTo("root")
        vm.focusPlacement("child")
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).isEmpty()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).breadcrumbs.map { it.placement.id.value })
            .containsExactly("root", "child").inOrder()
        assertThat(state.get<String>("bigPictureFocus")).isEqualTo("child")
        vm.goUp()
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode?.placement?.id?.value).isEqualTo("root")
        vm.focusPlacement(null)
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode).isNull()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).scopes).isEmpty()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes.map { it.placement.id.value }).containsExactly("root")
        vm.focusPlacement("child") // A descendant cannot bypass the visible focus level.
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode).isNull()
        placements.value = placements.value.take(1)
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).hasSize(1)
        verify(exactly = 1) { repository.observe() }
        confirmVerified(repository)
    }

    @Test fun `read failure becomes Error without fallback or false emptiness`() = runTest(dispatcher) {
        every { repository.observe() } returns flow { error("Malformed placements") }
        every { catalog.observeCatalog() } returns flowOf(emptyList())
        val vm = BigPictureViewModel(repository, catalog, SavedStateHandle(), dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Error).message).contains("Malformed placements")
    }

    @Test fun `unscoped Aspect root is visible alongside compatibility containers and can focus mixed descendants`() = runTest(dispatcher) {
        val root = p("aspect-root", "a").copy(target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "a"))
        val workspace = p("workspace", "w", "aspect-root").copy(target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"))
        val orientation = p("orientation", "o", "workspace")
        every { repository.observe() } returns flowOf(BigPictureStructure(
            listOf(root, workspace, orientation),
        ))
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Aspect"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Orientation"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("group"), "Historical group"),
        ))
        val vm = BigPictureViewModel(repository, catalog, SavedStateHandle(), dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        fun ready() = vm.uiState.value as BigPictureUiState.Ready
        assertThat(ready().nodes.single().entity?.ref).isEqualTo(CanonicalEntityRef.Aspect("a"))
        assertThat(ready().scopes).isEmpty() // Semantic/compatibility metadata alone cannot create a parent.
        vm.focusPlacement("aspect-root")
        advanceUntilIdle()
        assertThat(ready().activeScope).isNull()
        assertThat(ready().nodes.single().entity?.ref).isEqualTo(CanonicalEntityRef.Workspace("w"))
        vm.focusPlacement("workspace")
        advanceUntilIdle()
        assertThat(ready().nodes.single().entity?.ref).isEqualTo(CanonicalEntityRef.Orientation("o"))
        vm.focusPlacement(null)
        advanceUntilIdle()
        assertThat(ready().nodes.single().placement.id.value).isEqualTo("aspect-root")
        verify(exactly = 1) { repository.observe() }
        confirmVerified(repository)
    }

    @Test fun `groups are root navigation units and Back from beacon returns to its group`() = runTest(dispatcher) {
        val beacon = p("b", "beacon").copy(target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "beacon"))
        every { repository.observe() } returns flowOf(BigPictureStructure(
            listOf(beacon, p("w", "workspace", "b").copy(target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"))),
            presentation = com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot(
                containers = listOf(com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot(
                    "group-container", "GENERAL", "Group", null, null, 0, 1, 1, null, false, 1)),
                memberships = listOf(com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureMembershipSnapshot(
                    "b", "GENERAL", "group-container", 1, 1, null, false, 1)),
            ),
        ))
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("group"), "Group"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("beacon"), "Beacon"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace"), "Workspace"),
        ))
        val vm = BigPictureViewModel(repository, catalog, SavedStateHandle(), dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        fun ready() = vm.uiState.value as BigPictureUiState.Ready
        assertThat(ready().nodes).isEmpty()
        assertThat(ready().scopes.single().title).isEqualTo("Group")
        vm.focusScope(ready().scopes.single().id)
        advanceUntilIdle()
        assertThat(ready().nodes.single().placement.id.value).isEqualTo("b")
        vm.focusPlacement("b")
        advanceUntilIdle()
        assertThat(ready().activeScope?.title).isEqualTo("Group")
        assertThat(ready().nodes.single().entity?.ref).isEqualTo(CanonicalEntityRef.Workspace("workspace"))
        vm.goUp()
        advanceUntilIdle()
        assertThat(ready().activeNode).isNull()
        assertThat(ready().activeScope?.title).isEqualTo("Group")
        vm.goUp()
        advanceUntilIdle()
        assertThat(ready().activeScope).isNull()
        assertThat(ready().nodes).isEmpty()
    }

    @Test fun `Workspace reveal focuses its occurrence and root breadcrumb exits focus`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(BigPictureStructure(listOf(
            p("root", "w"), p("child", "w2", "root").copy(target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w2")),
        )))
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Root"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w2"), "Child"),
        ))
        val handle = SavedStateHandle(mapOf("projectIdToReveal" to "w2"))
        val vm = BigPictureViewModel(repository, catalog, handle, dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).activeNode?.placement?.id?.value).isEqualTo("child")
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).isEmpty()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).breadcrumbs).hasSize(2)
        vm.focusPlacement(null)
        advanceUntilIdle()
        assertThat((vm.uiState.value as BigPictureUiState.Ready).nodes).hasSize(1)
        assertThat((vm.uiState.value as BigPictureUiState.Ready).scopes).isEmpty()
    }

    private fun p(id: String, target: String, parent: String? = null) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(if (parent == null) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, target),
        parent?.let(::PlacementId), PlacementKind.PRIMARY, 0, 1, 1, null, false, 1,
    )
}
