package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceOccurrencesTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("parent"), "Parent"),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("w"), "Semantic, same raw ID"),
    )

    @Test fun `all exact Workspace appearances include root occurrence and nested container paths`() {
        val source = BigPictureStructure(
            listOf(p("primary"), p("parent", "parent"), p("link", parent = "parent", kind = PlacementKind.LINK)),
            BigPicturePresentationSnapshot(
                containers = listOf(c("outer", anchor = "parent"), c("inner", anchor = "parent", parent = "outer")),
                memberships = listOf(BigPictureMembershipSnapshot("link", "GENERAL", "inner", 1, 1, null, false, 1)),
            ),
        )
        val rows = projectWorkspaceOccurrences("w", source, catalog)
        assertThat(rows.map { it.placementId }).containsExactly("primary", "link")
        assertThat(rows.single { it.placementId == "primary" }.path).containsExactly("Корінь", "Workspace").inOrder()
        val link = rows.single { it.placementId == "link" }
        assertThat(link.path).containsExactly("Корінь", "Parent", "outer", "inner", "Workspace").inOrder()
        assertThat(link.kind).isEqualTo(PlacementKind.LINK)
        assertThat(link.destination.projectIdToReveal).isEqualTo("w")
        assertThat(link.destination.placementIdToReveal).isEqualTo("link")
        assertThat(source.placements.last().parentPlacementId).isEqualTo(PlacementId("parent"))
    }

    @Test fun `semantic same ID and deleted appearances never become Workspace appearances`() {
        val rows = listOf(p("primary"), p("deleted", kind = PlacementKind.LINK).copy(isDeleted = true),
            p("semantic").copy(target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "w")))
        assertThat(projectWorkspaceOccurrences("w", BigPictureStructure(rows), catalog).map { it.placementId })
            .containsExactly("primary")
    }

    @Test fun `unplaced unavailable and blank Workspace return empty without fabricating placement`() {
        assertThat(projectWorkspaceOccurrences("w", BigPictureStructure(emptyList()), catalog)).isEmpty()
        assertThat(projectWorkspaceOccurrences("w", BigPictureStructure(listOf(p("p"))), emptyList())).isEmpty()
        assertThat(projectWorkspaceOccurrences("", BigPictureStructure(listOf(p("p"))), catalog)).isEmpty()
    }

    @Test fun `rename and membership changes derive current exact path`() {
        val root = BigPictureStructure(listOf(p("p")))
        val grouped = root.copy(presentation = BigPicturePresentationSnapshot(containers = listOf(c("section")),
            memberships = listOf(BigPictureMembershipSnapshot("p", "GENERAL", "section", 1, 1, null, false, 1))))
        val renamed = listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Renamed"))
        assertThat(projectWorkspaceOccurrences("w", grouped, renamed).single().path)
            .containsExactly("Корінь", "section", "Renamed").inOrder()
        assertThat(projectWorkspaceOccurrences("w", root, catalog).single().path).hasSize(2)
    }

    @Test fun `equal labels keep distinct exact destinations and deterministic order`() {
        val rows = listOf(p("b", kind = PlacementKind.LINK), p("a"))
        val forward = projectWorkspaceOccurrences("w", BigPictureStructure(rows), catalog)
        val reverse = projectWorkspaceOccurrences("w", BigPictureStructure(rows.reversed()), catalog)
        assertThat(forward).isEqualTo(reverse)
        assertThat(forward.map { it.destination.placementIdToReveal }).containsExactly("a", "b").inOrder()
    }

    @Test fun `open picker reacts to renamed moved and removed appearances using bulk reads only`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val repository = mockk<BigPictureStructureRepository>()
            val entities = mockk<CanonicalEntityCatalogRepository>()
            val graph = MutableStateFlow(BigPictureStructure(listOf(p("primary"))))
            val titles = MutableStateFlow(catalog)
            every { repository.observe() } returns graph
            every { entities.observeCatalog() } returns titles
            val vm = WorkspaceOccurrencesViewModel(SavedStateHandle(mapOf("workspaceId" to "w", "sourceContextId" to "other")),
                repository, entities, dispatcher)
            store.put("picker", vm)
            backgroundScope.launch(dispatcher) { vm.state.collect {} }
            advanceUntilIdle()
            assertThat(vm.state.value.occurrences.single().placementId).isEqualTo("primary")
            titles.value = listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Renamed"))
            graph.value = graph.value.copy(presentation = BigPicturePresentationSnapshot(containers = listOf(c("section")),
                memberships = listOf(BigPictureMembershipSnapshot("primary", "GENERAL", "section", 1, 1, null, false, 1))))
            advanceUntilIdle()
            assertThat(vm.state.value.occurrences.single().path).containsExactly("Корінь", "section", "Renamed").inOrder()
            graph.value = BigPictureStructure(emptyList())
            advanceUntilIdle()
            assertThat(vm.state.value.loading).isFalse()
            assertThat(vm.state.value.occurrences).isEmpty()
            verify(exactly = 1) { repository.observe(); entities.observeCatalog() }
            confirmVerified(repository, entities)
        } finally { backgroundScope.cancel(); store.clear(); Dispatchers.resetMain() }
    }

    @Test fun `missing canonical Workspace ID never uses Context or same ID inference`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val repository = mockk<BigPictureStructureRepository>()
            val entities = mockk<CanonicalEntityCatalogRepository>()
            val vm = WorkspaceOccurrencesViewModel(SavedStateHandle(mapOf("sourceContextId" to "w")), repository, entities, dispatcher)
            store.put("picker", vm)
            backgroundScope.launch(dispatcher) { vm.state.collect {} }
            advanceUntilIdle()
            assertThat(vm.state.value.loading).isFalse()
            assertThat(vm.state.value.occurrences).isEmpty()
            verify { repository wasNot Called; entities wasNot Called }
        } finally { backgroundScope.cancel(); store.clear(); Dispatchers.resetMain() }
    }

    @Test fun `read failure is a safe error instead of navigating or changing structure`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val repository = mockk<BigPictureStructureRepository>()
            val entities = mockk<CanonicalEntityCatalogRepository>()
            every { repository.observe() } returns flow { error("unavailable") }
            every { entities.observeCatalog() } returns flowOf(catalog)
            val vm = WorkspaceOccurrencesViewModel(SavedStateHandle(mapOf("workspaceId" to "w")), repository, entities, dispatcher)
            store.put("picker", vm)
            backgroundScope.launch(dispatcher) { vm.state.collect {} }
            advanceUntilIdle()
            assertThat(vm.state.value.error).isNotNull()
            assertThat(vm.state.value.occurrences).isEmpty()
        } finally { backgroundScope.cancel(); store.clear(); Dispatchers.resetMain() }
    }

    private fun p(id: String, target: String = "w", parent: String? = null, kind: PlacementKind = PlacementKind.PRIMARY) =
        HierarchyPlacement(PlacementId(id), HierarchyId.GENERAL, HierarchyTargetRef(HierarchyTargetType.WORKSPACE, target),
            parent?.let(::PlacementId), kind, 0, 1, 1, null, false, 1)

    private fun c(id: String, anchor: String? = null, parent: String? = null) =
        BigPictureContainerSnapshot(id, "GENERAL", id, anchor, parent, 0, 1, 1, null, false, 1)
}
