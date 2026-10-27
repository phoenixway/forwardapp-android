package com.romankozak.forwardappmobile.features.globalsearch

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.*
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class MagicBoxCanonicalQueryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val catalog = mockk<CanonicalEntityQueryRepository>()
    private val saved = mockk<SavedEntityQueryRepository>()
    private val presence = mockk<CanonicalHierarchyPlacementPresenceRepository>()
    private val item = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Health")
    private val items = MutableStateFlow(listOf(item))
    private val queries = MutableStateFlow(SavedEntityQueries())
    private val placed = MutableStateFlow(emptySet<HierarchyTargetRef>())
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { catalog.observeCatalog() } returns items
        every { saved.observe() } returns queries
        every { presence.observeVisiblePlacedTargets(any()) } returns placed
    }
    @After fun reset() { Dispatchers.resetMain() }
    private suspend fun MagicBoxCanonicalQueryViewModel.await(predicate: (MagicBoxCanonicalState) -> Boolean) =
        withTimeout(5_000) { state.first(predicate) }

    @Test fun `tag and score changes update results without manual refresh`() = runBlocking<Unit> {
        val vm = MagicBoxCanonicalQueryViewModel(catalog, saved, presence)
        vm.setInput("#health score:importance>=7")
        vm.await { it.input == "#health score:importance>=7" }
        assertThat(vm.state.value.results).isEmpty()
        items.value = listOf(item.copy(tags = setOf("health"), scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 8.0)))
        vm.await { it.results.size == 1 }
        items.value = listOf(item.copy(tags = setOf("health"), scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 6.0)))
        vm.await { it.results.isEmpty() }
        items.value = listOf(item.copy(scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 8.0)))
        vm.await { it.results.isEmpty() }
        verify(exactly = 1) { catalog.observeCatalog() }
        verify(exactly = 1) { saved.observe() }
        verify(exactly = 1) { presence.observeVisiblePlacedTargets(any()) }
        confirmVerified(catalog, saved, presence)
    }

    @Test fun `rename and live catalog tombstone removal react`() = runBlocking<Unit> {
        val vm = MagicBoxCanonicalQueryViewModel(catalog, saved, presence)
        vm.setInput("Health")
        vm.await { it.results.size == 1 }
        items.value = listOf(item.copy(title = "Finance"))
        vm.await { it.results.isEmpty() }
        items.value = listOf(item)
        vm.await { it.results.size == 1 }
        items.value = emptyList() // Canonical catalog owns live admission, adapter never resurrects rows.
        vm.await { it.results.isEmpty() }
    }

    @Test fun `saved query edits and deletion are reactive`() = runBlocking<Unit> {
        val record = SavedEntityQuery("q", "Test", CanonicalEntityQueryCodec.encode(CanonicalEntityQuery(text = "Health")), 1, 1)
        queries.value = SavedEntityQueries(listOf(record))
        val vm = MagicBoxCanonicalQueryViewModel(catalog, saved, presence)
        vm.setInput("saved:Test")
        vm.await { it.results.size == 1 }
        queries.value = SavedEntityQueries(listOf(record.copy(specification = CanonicalEntityQueryCodec.encode(CanonicalEntityQuery(text = "Finance")))))
        vm.await { it.input == "saved:Test" && it.results.isEmpty() }
        queries.value = SavedEntityQueries()
        vm.await { it.resolution.issue == MagicBoxQueryIssue.SAVED_MISSING }
    }

    @Test fun `saved Unplaced uses same reactive H1 presence predicate as Library`() = runBlocking<Unit> {
        queries.value = SavedEntityQueries(listOf(SavedEntityQuery("q", "Unplaced",
            CanonicalEntityQueryCodec.encode(CanonicalEntityQuery(unplaced = true)), 1, 1)))
        val vm = MagicBoxCanonicalQueryViewModel(catalog, saved, presence)
        vm.setInput("saved:Unplaced")
        vm.await { it.results.size == 1 }
        placed.value = setOf(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "o"))
        vm.await { it.input == "saved:Unplaced" && it.results.isEmpty() }
        placed.value = emptySet()
        vm.await { it.results.size == 1 }
    }
}
