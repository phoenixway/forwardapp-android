package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityQueryRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class EntityLibraryQueryStateTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Health", tags = setOf("health")),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Meaning", scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 7.0)),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Facet"),
    )
    @Before fun main() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private fun model(handle: SavedStateHandle, source: Flow<List<CanonicalEntitySummary>> = flowOf(catalog)): EntityLibraryViewModel {
        val repo = mockk<CanonicalEntityQueryRepository>()
        every { repo.observeCatalog() } returns source
        val presence = mockk<CanonicalHierarchyPlacementPresenceRepository>()
        every { presence.observeVisiblePlacedTargets(any()) } returns flowOf(emptySet())
        return EntityLibraryViewModel(repo, presence, handle, dispatcher)
    }
    private fun ready(vm: EntityLibraryViewModel) = vm.uiState.value as EntityLibraryUiState.Ready

    @Test fun `query survives SavedState recreation as typed state without UI labels`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = model(handle)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        vm.setSearchText("Meaning")
        vm.applyFilters(setOf(CanonicalEntityClass.ORIENTATION), emptySet(), mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(7.0, 10.0)))
        vm.setUnplaced(true)
        vm.setScoreSort(CanonicalScoreDimension.IMPORTANCE, true)
        advanceUntilIdle()
        val saved = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = model(saved)
        backgroundScope.launch(dispatcher) { restored.uiState.collect {} }
        advanceUntilIdle()
        assertThat(ready(restored).query).isEqualTo(ready(vm).query)
        assertThat(ready(restored).items.map { it.ref }).containsExactly(catalog[1].ref)
        assertThat(handle.get<String>("entityLibraryQueryV1")).doesNotContain("Важливість")
    }
    @Test fun `tags restore and clear filters restores canonical inventory preserving sort`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = model(handle)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        vm.setTags(setOf("#HEALTH"))
        assertThat(ready(vm).items.map { it.ref }).containsExactly(catalog[0].ref)
        val restored = model(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        backgroundScope.launch(dispatcher) { restored.uiState.collect {} }
        assertThat(ready(restored).query.tags).containsExactly("health")
        restored.setSort(EntityLibrarySort.NAME_DESC)
        restored.clearFilters()
        assertThat(ready(restored).items).hasSize(3)
        assertThat(ready(restored).sort).isEqualTo(EntityLibrarySort.NAME_DESC)
    }
    @Test fun `stale invalid and future state falls back safely`() = runTest(dispatcher) {
        for (value in listOf("garbage", "{\"version\":99}", "{\"version\":1,\"types\":[\"CONTEXT\"]}", "{\"version\":1,\"scores\":{\"URGENCY\":{\"min\":7}}}")) {
            val vm = model(SavedStateHandle(mapOf("entityLibraryQueryV1" to value, "entityLibrarySort" to "RETIRED", "entityLibraryScoreDimension" to "RETIRED")))
            backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
            assertThat(ready(vm).items).hasSize(3)
            assertThat(ready(vm).sort).isEqualTo(EntityLibrarySort.NAME_ASC)
        }
    }
    @Test fun `metadata changes update filtered Library without manual refresh`() = runTest(dispatcher) {
        val source = MutableStateFlow(catalog)
        val vm = model(SavedStateHandle(), source)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        vm.setScoreFilter(CanonicalScoreDimension.IMPORTANCE, CanonicalScoreRange(min = 8.0))
        assertThat(ready(vm).items).isEmpty()
        source.value = catalog.map { if (it.ref == catalog[1].ref) it.copy(scores = mapOf(CanonicalScoreDimension.IMPORTANCE to 10.0)) else it }
        assertThat(ready(vm).items.map { it.ref }).containsExactly(catalog[1].ref)
    }
    @Test fun `numeric UI range validation accepts decimals and rejects malformed values`() {
        assertThat(parseLibraryScoreRange("7,5", "10")).isEqualTo(CanonicalScoreRange(7.5, 10.0))
        for ((min, max) in listOf("NaN" to "", "x" to "", "9" to "3", "" to "Infinity")) assertThat(parseLibraryScoreRange(min, max)).isNull()
    }

    @Test fun `applying saved specification restores every predicate and ordinary active chips state`() = runTest(dispatcher) {
        val vm = model(SavedStateHandle())
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        val spec = CanonicalEntityQuery("Meaning", setOf(CanonicalEntityClass.ORIENTATION), emptySet(),
            mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(7.0, 10.0)),
            CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.IMPORTANCE), true)
        vm.applyQuery(spec)
        assertThat(ready(vm).query).isEqualTo(spec)
        assertThat(ready(vm).items.map { it.ref }).containsExactly(catalog[1].ref)
        vm.setSearchText("changed manually")
        vm.setTags(setOf("another"))
        assertThat(spec.text).isEqualTo("Meaning")
        assertThat(spec.tags).isEmpty()
    }

    @Test fun `saved sort variations apply without lossy mapping`() = runTest(dispatcher) {
        val vm = model(SavedStateHandle())
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        for (sort in EntityLibrarySort.entries) {
            val spec = CanonicalEntityQuery(sort = sort.toQuerySort(CanonicalScoreDimension.IMPACT))
            vm.applyQuery(spec)
            assertThat(ready(vm).query).isEqualTo(spec)
        }
        val tagged = CanonicalEntityQuery(text = "Health", tags = setOf("health"), entityTypes = setOf(CanonicalEntityClass.WORKSPACE))
        vm.applyQuery(tagged)
        assertThat(ready(vm).query).isEqualTo(tagged)
    }

    @Test fun `saved query preview includes all conditions without editing the specification`() {
        val spec = CanonicalEntityQuery("Health", setOf(CanonicalEntityClass.ORIENTATION), setOf("health"),
            mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.0)), unplaced = true)
        val preview = canonicalQueryPreview(spec)
        for (word in listOf("Health", "Орієнтир", "#health", "Важливість", "≥ 7", "Не розміщені", "Сортування")) assertThat(preview).contains(word)
    }

    @Test fun `save captures latest input even before any background results arrive`() = runTest(dispatcher) {
        val vm = model(SavedStateHandle(), MutableSharedFlow())
        vm.setSearchText("last keystroke")
        vm.applyFilters(setOf(CanonicalEntityClass.ORIENTATION), setOf("health"),
            mapOf(CanonicalScoreDimension.IMPACT to CanonicalScoreRange(min = 3.0)))
        vm.setUnplaced(true)
        vm.setScoreSort(CanonicalScoreDimension.IMPACT, true)
        assertThat(vm.uiState.value).isEqualTo(EntityLibraryUiState.Loading)
        assertThat(vm.currentQuery()).isEqualTo(CanonicalEntityQuery("last keystroke", setOf(CanonicalEntityClass.ORIENTATION), setOf("health"),
            mapOf(CanonicalScoreDimension.IMPACT to CanonicalScoreRange(min = 3.0)),
            CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.IMPACT), true))
    }
}
