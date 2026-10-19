package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.CANONICAL_V2_NO_BEACON_SCOPE_ID
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BigPictureContainerOrigin
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BigPictureGenericNavigationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<BigPictureStructureRepository>()
    private val catalog = mockk<CanonicalEntityCatalogRepository>()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `nested generic sections navigate to occurrence and back through section then H1 anchor`() =
        runTest(dispatcher) {
            every { catalog.observeCatalog() } returns flowOf(emptyList())
            every { repository.observe() } returns flowOf(
                BigPictureStructure(
                    placements = listOf(
                        p("root"),
                        p("child", "root", 3),
                        p("free", "root", 2),
                    ),
                    presentation = BigPicturePresentationSnapshot(
                        containers = listOf(
                            c("outer", "root", order = 1),
                            c("inner", "root", "outer", 2),
                        ),
                        memberships = listOf(m("child", "inner")),
                    ),
                ),
            )

            val vm = BigPictureViewModel(
                repository, catalog, SavedStateHandle(), dispatcher,
            )
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            advanceUntilIdle()
            fun ready() = vm.uiState.value as BigPictureUiState.Ready

            // Workspace roots are ordinary H1 occurrences; there is no NoBeacon wrapper.
            assertThat(ready().scopes).isEmpty()
            assertThat(ready().nodes.map { it.placement.id.value })
                .containsExactly("root")

            vm.focusPlacement("root")
            advanceUntilIdle()
            assertThat(ready().entries.map { it.stableId }).containsExactly(
                "container:presentation:outer",
                "occurrence:free",
            ).inOrder()

            vm.focusScope("presentation:outer")
            advanceUntilIdle()
            assertThat(ready().entries.map { it.stableId })
                .containsExactly("container:presentation:inner")

            vm.focusScope("presentation:inner")
            advanceUntilIdle()
            assertThat(ready().nodes.map { it.placement.id.value })
                .containsExactly("child")
            assertThat(ready().scopeBreadcrumbs.map { it.id })
                .containsExactly("presentation:outer", "presentation:inner").inOrder()

            vm.focusPlacement("child")
            advanceUntilIdle()
            assertThat(ready().activeNode?.placement?.id?.value)
                .isEqualTo("child")
            assertThat(ready().directSectionId).isEqualTo("presentation:inner")

            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeScope?.id).isEqualTo("presentation:inner")
            assertThat(ready().activeNode).isNull()

            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeScope?.id).isEqualTo("presentation:outer")

            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeNode?.placement?.id?.value)
                .isEqualTo("root")

            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeNode).isNull()
        }

    @Test
    fun `breadcrumbs preserve the full alternating section occurrence path and navigate to ancestors`() =
        runTest(dispatcher) {
            every { repository.observe() } returns flowOf(mixedPath())
            every { catalog.observeCatalog() } returns flowOf(emptyList())
            val vm = BigPictureViewModel(repository, catalog, SavedStateHandle(), dispatcher)
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            advanceUntilIdle()
            fun ready() = vm.uiState.value as BigPictureUiState.Ready
            fun path() = ready().navigationBreadcrumbs.map { it.id }

            assertThat(path()).isEmpty() // Root is supplied by the breadcrumb bar itself.
            vm.focusScope("presentation:outer")
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer")
            vm.focusPlacement("a")
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer", "a").inOrder()
            vm.focusScope("presentation:middle")
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer", "a", "presentation:middle").inOrder()
            vm.focusPlacement("b")
            advanceUntilIdle()
            vm.focusScope("presentation:inner")
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer", "a", "presentation:middle", "b", "presentation:inner").inOrder()
            vm.focusPlacement("c")
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer", "a", "presentation:middle", "b", "presentation:inner", "c").inOrder()
            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeScope?.id).isEqualTo("presentation:inner")
            // Tapping an occurrence crumb works while the active location is a section.
            vm.focusPlacement("a")
            advanceUntilIdle()
            assertThat(ready().activeNode?.placement?.id?.value).isEqualTo("a")
            vm.goUp()
            advanceUntilIdle()
            assertThat(path()).containsExactly("presentation:outer")
            vm.focusPlacement(null)
            advanceUntilIdle()
            assertThat(path()).isEmpty()
        }

    @Test
    fun `restored section focus reconstructs occurrence and container breadcrumbs from H1`() =
        runTest(dispatcher) {
            every { repository.observe() } returns flowOf(mixedPath())
            every { catalog.observeCatalog() } returns flowOf(emptyList())
            val vm = BigPictureViewModel(repository, catalog,
                SavedStateHandle(mapOf("bigPictureScope" to "presentation:inner")), dispatcher)
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            advanceUntilIdle()
            val ready = vm.uiState.value as BigPictureUiState.Ready
            assertThat(ready.navigationBreadcrumbs.map { it.id })
                .containsExactly("presentation:outer", "a", "presentation:middle", "b", "presentation:inner").inOrder()
            assertThat(ready.navigationBreadcrumbs.map { it.placementId })
                .containsExactly(null, "a", null, "b", null).inOrder()
        }

    private fun mixedPath() = BigPictureStructure(
        placements = listOf(p("a"), p("b", "a"), p("c", "b")),
        presentation = BigPicturePresentationSnapshot(
            containers = listOf(c("outer"), c("middle", "a"), c("inner", "b")),
            memberships = listOf(m("a", "outer"), m("b", "middle"), m("c", "inner")),
        ),
    )

    @Test
    fun `section breadcrumbs show persisted display titles and react to title changes`() = runTest(dispatcher) {
        val names = mapOf("outer" to "Робота", "middle" to "WIP", "inner" to "Моя секція")
        val source = MutableStateFlow(mixedPath().let { it.copy(presentation = it.presentation.copy(
            containers = it.presentation.containers.map { c -> c.copy(title = names.getValue(c.id)) },
        )) })
        every { repository.observe() } returns source
        every { catalog.observeCatalog() } returns flowOf(listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("a"), "Простір A"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("b"), "Простір B"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("c"), "Простір C"),
        ))
        val vm = BigPictureViewModel(repository, catalog,
            SavedStateHandle(mapOf("bigPictureScope" to "presentation:inner")), dispatcher)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        fun ready() = vm.uiState.value as BigPictureUiState.Ready
        assertThat(ready().navigationBreadcrumbs.map { it.name })
            .containsExactly("Робота", "Простір A", "WIP", "Простір B", "Моя секція").inOrder()
        assertThat(ready().activeScope?.title).isEqualTo("Моя секція")
        source.value = source.value.let { it.copy(presentation = it.presentation.copy(
            containers = it.presentation.containers.map { c -> if (c.id == "inner") c.copy(title = "Оновлена секція") else c },
        )) }
        advanceUntilIdle()
        assertThat(ready().navigationBreadcrumbs.last().name).isEqualTo("Оновлена секція")
        assertThat(ready().activeScope?.title).isEqualTo("Оновлена секція")
    }

    @Test
    fun `reactive generic root membership takes display precedence without altering historical scope`() =
        runTest(dispatcher) {
            val source = MutableStateFlow(
                BigPictureStructure(placements = listOf(p("workspace"))),
            )
            every { repository.observe() } returns source
            every { catalog.observeCatalog() } returns flowOf(emptyList())

            val vm = BigPictureViewModel(
                repository, catalog, SavedStateHandle(), dispatcher,
            )
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            advanceUntilIdle()
            fun ready() = vm.uiState.value as BigPictureUiState.Ready

            assertThat(ready().scopes).isEmpty()

            source.value = source.value.copy(
                presentation = BigPicturePresentationSnapshot(
                    containers = listOf(c("generic")),
                    memberships = listOf(m("workspace", "generic")),
                ),
            )
            advanceUntilIdle()

            assertThat(ready().entries.map { it.stableId }).containsExactly(
                "container:presentation:generic",
            ).inOrder()
            assertThat(ready().scopes.single().origin).isEqualTo(BigPictureContainerOrigin.PRESENTATION)

            vm.focusScope("presentation:generic")
            advanceUntilIdle()
            assertThat(ready().nodes.map { it.placement.id.value })
                .containsExactly("workspace")

            vm.focusPlacement("workspace")
            advanceUntilIdle()
            assertThat(ready().activeScope?.id)
                .isEqualTo("presentation:generic")

            vm.goUp()
            advanceUntilIdle()
            assertThat(ready().activeScope?.id)
                .isEqualTo("presentation:generic")
            assertThat(ready().activeNode).isNull()
        }

    private fun c(
        id: String,
        anchor: String? = null,
        parent: String? = null,
        order: Long = 0,
    ) = BigPictureContainerSnapshot(
        id, "GENERAL", id, anchor, parent, order,
        1, 1, null, false, 1,
    )

    private fun m(id: String, container: String) =
        BigPictureMembershipSnapshot(
            id, "GENERAL", container, 1, 1, null, false, 1,
        )

    private fun p(id: String, parent: String? = null, order: Long = 0) =
        HierarchyPlacement(
            PlacementId(id), HierarchyId.GENERAL,
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
            parent?.let(::PlacementId),
            PlacementKind.PRIMARY, order, 1, 1, null, false, 1,
        )
}
