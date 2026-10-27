package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityQueryRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntityLibraryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial loading is distinct from admitted empty catalog`() = runTest(dispatcher) {
        val source = MutableSharedFlow<List<CanonicalEntitySummary>>()
        val viewModel = viewModel(source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value is EntityLibraryUiState.Loading)
        source.emit(emptyList())

        val ready = viewModel.uiState.value as EntityLibraryUiState.Ready
        assertTrue(ready.isCatalogEmpty)
        assertThat(ready.items).isEmpty()
    }

    @Test
    fun `mixed catalog delegates class filters and search to canonical browser query`() = runTest(dispatcher) {
        val source = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
        source.emit(mixedCatalog())
        val viewModel = viewModel(source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(ready(viewModel).items.map { it.ref.entityClass })
            .containsExactly(
                CanonicalEntityClass.ORIENTATION,
                CanonicalEntityClass.ASPECT,
                CanonicalEntityClass.WORKSPACE,
            )

        viewModel.setFilter(EntityLibraryFilter.ASPECTS)
        assertThat(ready(viewModel).items.map { it.ref }).containsExactly(CanonicalEntityRef.Aspect("a1"))

        viewModel.setFilter(EntityLibraryFilter.ALL)
        viewModel.setSearchText("missing")
        val noMatches = ready(viewModel)
        assertTrue(noMatches.hasNoMatches)
        assertThat(noMatches.admittedCatalogSize).isEqualTo(3)

        viewModel.setSearchText("workspace")
        assertThat(ready(viewModel).items.map { it.ref }).containsExactly(CanonicalEntityRef.Workspace("w1"))
    }

    @Test
    fun `catalog failure is an explicit error state`() = runTest(dispatcher) {
        val repository = mockk<CanonicalEntityQueryRepository>()
        every { repository.observeCatalog() } returns flow { error("catalog failed") }
        val presence = mockk<CanonicalHierarchyPlacementPresenceRepository>()
        every { presence.observeVisiblePlacedTargets(any()) } returns flowOf(emptySet())
        val viewModel = EntityLibraryViewModel(repository, presence, SavedStateHandle(), dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as EntityLibraryUiState.Error).message).contains("catalog failed")
    }

    @Test
    fun `typed row opening preserves canonical identity for semantic entities and Workspace`() {
        assertThat(entityLibraryDestination(CanonicalEntityRef.Orientation("same")))
            .isEqualTo(EntityLibraryDestination.SemanticEditor("same"))
        assertThat(entityLibraryDestination(CanonicalEntityRef.Aspect("same")))
            .isEqualTo(EntityLibraryDestination.SemanticEditor("same"))
        assertThat(entityLibraryDestination(CanonicalEntityRef.Workspace("same")))
            .isEqualTo(EntityLibraryDestination.Workspace("same"))
    }

    @Test
    fun `UNPLACED waits for first hierarchy emission instead of flashing all entities`() =
        runTest(dispatcher) {
            val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
            catalog.emit(mixedCatalog())
            val presence = MutableSharedFlow<Set<HierarchyTargetRef>>()
            val state = SavedStateHandle(mapOf("entityLibraryFilter" to "UNPLACED"))
            val viewModel = viewModel(catalog, presence, state)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is EntityLibraryUiState.Loading)

            presence.emit(setOf(subject("o1")))
            advanceUntilIdle()

            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Aspect("a1"), CanonicalEntityRef.Workspace("w1"))
            assertThat(ready(viewModel).filter).isEqualTo(EntityLibraryFilter.UNPLACED)
        }

    @Test
    fun `UNPLACED mixes classes composes with search and reacts to presence changes`() =
        runTest(dispatcher) {
            val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
            catalog.emit(mixedCatalog())
            val presence = MutableStateFlow(setOf(subject("o1"), workspace("w1")))
            val viewModel = viewModel(catalog, presence)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            viewModel.setFilter(EntityLibraryFilter.UNPLACED)
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Aspect("a1"))

            viewModel.setSearchText("workspace")
            advanceUntilIdle()
            assertThat(ready(viewModel).items).isEmpty()
            assertTrue(ready(viewModel).hasNoMatches)

            presence.value = setOf(subject("o1"))
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Workspace("w1"))

            viewModel.setSearchText("")
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Aspect("a1"), CanonicalEntityRef.Workspace("w1"))

            viewModel.setFilter(EntityLibraryFilter.ALL)
            advanceUntilIdle()
            assertThat(ready(viewModel).items).hasSize(3)

            viewModel.setFilter(EntityLibraryFilter.ORIENTATIONS)
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Orientation("o1"))

            viewModel.setFilter(EntityLibraryFilter.ASPECTS)
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Aspect("a1"))

            viewModel.setFilter(EntityLibraryFilter.WORKSPACES)
            advanceUntilIdle()
            assertThat(ready(viewModel).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Workspace("w1"))
        }

    @Test
    fun `hierarchy presence failure is Error rather than false Unplaced`() = runTest(dispatcher) {
        val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
        catalog.emit(mixedCatalog())
        val failingPresence: Flow<Set<HierarchyTargetRef>> = flow {
            error("hierarchy read failed")
        }
        val state = SavedStateHandle(mapOf("entityLibraryFilter" to "UNPLACED"))
        val viewModel = viewModel(catalog, failingPresence, state)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as EntityLibraryUiState.Error).message)
            .contains("hierarchy read failed")
    }

    @Test
    fun `SavedState restores UNPLACED and stale filter safely falls back to ALL`() =
        runTest(dispatcher) {
            val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
            catalog.emit(mixedCatalog())

            val restored = viewModel(
                catalog,
                flowOf(setOf(subject("o1"))),
                SavedStateHandle(
                    mapOf(
                        "entityLibraryFilter" to "UNPLACED",
                        "entityLibrarySearch" to "Health",
                    ),
                ),
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                restored.uiState.collect {}
            }
            advanceUntilIdle()

            assertThat(ready(restored).filter).isEqualTo(EntityLibraryFilter.UNPLACED)
            assertThat(ready(restored).searchText).isEqualTo("Health")
            assertThat(ready(restored).items.map { it.ref })
                .containsExactly(CanonicalEntityRef.Aspect("a1"))

            val stale = viewModel(
                catalog,
                state = SavedStateHandle(mapOf("entityLibraryFilter" to "RETIRED_FILTER")),
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                stale.uiState.collect {}
            }
            advanceUntilIdle()

            assertThat(ready(stale).filter).isEqualTo(EntityLibraryFilter.ALL)
            assertThat(ready(stale).items).hasSize(3)
        }

    @Test
    fun `all entities placed produces no match rather than empty canonical Library`() =
        runTest(dispatcher) {
            val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
            catalog.emit(mixedCatalog())
            val viewModel = viewModel(
                catalog,
                flowOf(setOf(subject("o1"), subject("a1"), workspace("w1"))),
                SavedStateHandle(mapOf("entityLibraryFilter" to "UNPLACED")),
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            assertThat(ready(viewModel).items).isEmpty()
            assertThat(ready(viewModel).admittedCatalogSize).isEqualTo(3)
            assertThat(ready(viewModel).isCatalogEmpty).isFalse()
            assertThat(ready(viewModel).hasNoMatches).isTrue()
        }

    @Test
    fun `sort restores safely and composes with Unplaced search`() = runTest(dispatcher) {
        val catalog = MutableSharedFlow<List<CanonicalEntitySummary>>(replay = 1)
        catalog.emit(mixedCatalog())
        val handle = SavedStateHandle(mapOf("entityLibrarySort" to "NAME_DESC", "entityLibraryFilter" to "UNPLACED"))
        val vm = viewModel(catalog, flowOf(setOf(subject("o1"))), handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertThat(ready(vm).sort).isEqualTo(EntityLibrarySort.NAME_DESC)
        assertThat(ready(vm).items.map { it.ref }).containsExactly(CanonicalEntityRef.Workspace("w1"), CanonicalEntityRef.Aspect("a1")).inOrder()
        vm.setSearchText("workspce")
        assertThat(ready(vm).items.map { it.ref }).containsExactly(CanonicalEntityRef.Workspace("w1"))
        vm.setSort(EntityLibrarySort.UPDATED_ASC)
        assertThat(handle.get<String>("entityLibrarySort")).isEqualTo("UPDATED_ASC")
        val stale = viewModel(catalog, state = SavedStateHandle(mapOf("entityLibrarySort" to "RETIRED")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { stale.uiState.collect {} }
        advanceUntilIdle()
        assertThat(ready(stale).sort).isEqualTo(EntityLibrarySort.NAME_ASC)
    }

    @Test
    fun `search input persists immediately while catalog computation runs on its dispatcher`() = runTest(dispatcher) {
        val repository = mockk<CanonicalEntityQueryRepository>()
        every { repository.observeCatalog() } returns flowOf(mixedCatalog())
        val presence = mockk<CanonicalHierarchyPlacementPresenceRepository>()
        every { presence.observeVisiblePlacedTargets(any()) } returns flowOf(emptySet())
        val handle = SavedStateHandle()
        val vm = EntityLibraryViewModel(repository, presence, handle, StandardTestDispatcher(testScheduler))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertThat(vm.uiState.value).isEqualTo(EntityLibraryUiState.Loading)
        advanceUntilIdle()
        vm.setSearchText("wor")
        vm.setSearchText("workspce")
        assertThat(handle.get<String>("entityLibrarySearch")).isEqualTo("workspce")
        assertThat(ready(vm).searchText).isEmpty()
        advanceUntilIdle()
        assertThat(ready(vm).searchText).isEqualTo("workspce")
        assertThat(ready(vm).items.map { it.ref }).containsExactly(CanonicalEntityRef.Workspace("w1"))
    }

    private fun viewModel(
        source: MutableSharedFlow<List<CanonicalEntitySummary>>,
        placedTargets: Flow<Set<HierarchyTargetRef>> = flowOf(emptySet()),
        state: SavedStateHandle = SavedStateHandle(),
    ): EntityLibraryViewModel {
        val repository = mockk<CanonicalEntityQueryRepository>()
        every { repository.observeCatalog() } returns source
        val presence = mockk<CanonicalHierarchyPlacementPresenceRepository>()
        every { presence.observeVisiblePlacedTargets(any()) } returns placedTargets
        return EntityLibraryViewModel(repository, presence, state, dispatcher)
    }

    private fun subject(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun workspace(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun ready(viewModel: EntityLibraryViewModel) =
        viewModel.uiState.value as EntityLibraryUiState.Ready

    private fun mixedCatalog() =
        listOf(
            CanonicalEntitySummary(
                ref = CanonicalEntityRef.Orientation("o1"),
                title = "Purpose",
                orientationKind = OrientationKind.INTENT,
            ),
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("a1"), "Health"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w1"), "Workspace"),
        )
}
