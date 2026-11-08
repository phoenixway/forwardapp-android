package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureMutationViewModel
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationClipboardTest {
    private val dispatcher = StandardTestDispatcher()
    private val clipboard = NavigationClipboard()
    private val writer = mockk<H1StructuralMutationCoordinator>()
    private val models = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun navigation() = BigPictureMutationViewModel(writer, SavedStateHandle(), clipboard)
        .also { models.put("navigation", it) }
    private val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w")
    private fun properties(graph: MutableStateFlow<BigPictureStructure>, catalog: MutableStateFlow<List<CanonicalEntitySummary>>): WorkspaceOccurrencesViewModel {
        val structure = mockk<BigPictureStructureRepository>()
        val entities = mockk<CanonicalEntityCatalogRepository>()
        every { structure.observe() } returns graph
        every { entities.observeCatalog() } returns catalog
        return WorkspaceOccurrencesViewModel(SavedStateHandle(mapOf("workspaceId" to "w")), structure, entities,
            dispatcher, clipboard).also { models.put("properties", it) }
    }
    private fun placement(id: String, kind: PlacementKind = PlacementKind.PRIMARY) =
        HierarchyPlacement(PlacementId(id), HierarchyId.GENERAL, target, null, kind, 0, 1, 1, null, false, 1)

    @Test fun `unplaced Workspace copy from properties pastes exact typed source into Navigation and is reusable`() = runTest(dispatcher) {
        val props = properties(MutableStateFlow(BigPictureStructure(emptyList())),
            MutableStateFlow(listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { props.state.collect {} }
        val nav = navigation()
        advanceUntilIdle()
        assertThat(props.copyWorkspace()).isTrue()
        assertThat(props.cutOccurrence("missing")).isFalse()
        coEvery { writer.addAppearance(any(), any(), any(), any()) } returns PlacementId("new")
        val destination = H1StructuralParent.Node(H1TypedNode.Occurrence("leaf"))
        nav.paste(destination); advanceUntilIdle()
        nav.paste(H1StructuralParent.Root); advanceUntilIdle()
        coVerify(exactly = 1) { writer.addAppearance(target, destination, any(), any()) }
        coVerify(exactly = 1) { writer.addAppearance(target, H1StructuralParent.Root, any(), any()) }
        coVerify(exactly = 0) { writer.moveNode(any(), any(), any(), any()) }
        assertThat(clipboard.entry.value).isInstanceOf(NavigationClipboardEntry.Copy::class.java)
    }

    @Test fun `selected LINK from properties moves exact appearance and clears shared clipboard only after success`() = runTest(dispatcher) {
        val props = properties(MutableStateFlow(BigPictureStructure(listOf(placement("primary"), placement("link", PlacementKind.LINK)))),
            MutableStateFlow(listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { props.state.collect {} }
        advanceUntilIdle()
        assertThat(props.cutOccurrence("link")).isTrue()
        val nav = navigation()
        coEvery { writer.moveNode(any(), any(), any(), any()) } just Runs
        nav.paste(H1StructuralParent.Node(H1TypedNode.Container("group"))); advanceUntilIdle()
        coVerify(exactly = 1) { writer.moveNode(H1TypedNode.Occurrence("link"),
            H1StructuralParent.Node(H1TypedNode.Container("group")), null, any()) }
        assertThat(clipboard.entry.value).isNull()
        coVerify(exactly = 0) { writer.addAppearance(any(), any(), any(), any()) }
    }

    @Test fun `deleted target and stale selected placement fail closed without replacing clipboard`() = runTest(dispatcher) {
        val graph = MutableStateFlow(BigPictureStructure(listOf(placement("p"))))
        val catalog = MutableStateFlow(listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace")))
        val props = properties(graph, catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { props.state.collect {} }
        advanceUntilIdle()
        clipboard.copy(CanonicalEntityRef.Aspect("w"))
        val original = clipboard.entry.value
        graph.value = BigPictureStructure(emptyList()); advanceUntilIdle()
        assertThat(props.cutOccurrence("p")).isFalse()
        catalog.value = emptyList(); advanceUntilIdle()
        assertThat(props.copyWorkspace()).isFalse()
        assertThat(clipboard.entry.value).isEqualTo(original)
        verify { writer wasNot Called }
    }

    @Test fun `failed paste retains payload and never converts copy into move`() = runTest(dispatcher) {
        val nav = navigation()
        nav.copy(CanonicalEntityRef.Orientation("w"))
        val copied = clipboard.entry.value
        coEvery { writer.addAppearance(any(), any(), any(), any()) } throws IllegalArgumentException("Unavailable")
        nav.paste(H1StructuralParent.Root); advanceUntilIdle()
        assertThat(clipboard.entry.value).isEqualTo(copied)
        coVerify { writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "w"), any(), any(), any()) }
        coVerify(exactly = 0) { writer.moveNode(any(), any(), any(), any()) }
    }

    @Test fun `late cut completion cannot clear a newer clipboard command and double tap dispatches once`() = runTest(dispatcher) {
        val nav = navigation()
        val gate = CompletableDeferred<Unit>()
        coEvery { writer.moveNode(any(), any(), any(), any()) } coAnswers { gate.await() }
        clipboard.cut(H1TypedNode.Occurrence("p"))
        nav.paste(H1StructuralParent.Root); nav.paste(H1StructuralParent.Root); advanceUntilIdle()
        clipboard.copy(CanonicalEntityRef.Workspace("other"))
        val newer = clipboard.entry.value
        gate.complete(Unit); advanceUntilIdle()
        assertThat(clipboard.entry.value).isEqualTo(newer)
        coVerify(exactly = 1) { writer.moveNode(any(), any(), any(), any()) }
    }

    @Test fun `missing placement reconciliation does not discard unplaced typed copy`() = runTest(dispatcher) {
        val nav = navigation()
        nav.copy(CanonicalEntityRef.Workspace("w"))
        nav.discardMissingClipboard(emptySet())
        assertThat(clipboard.entry.value).isInstanceOf(NavigationClipboardEntry.Copy::class.java)
        nav.cancelCut()
        assertThat(clipboard.entry.value).isNull()
    }
}
