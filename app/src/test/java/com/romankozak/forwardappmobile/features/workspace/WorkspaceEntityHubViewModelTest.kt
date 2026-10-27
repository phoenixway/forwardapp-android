package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.workspace.WorkspaceRelationsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalRelationsCapabilityRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceEntityHubViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val relations = mockk<WorkspaceRelationsRepository>()
    private val capabilities = mockk<CanonicalRelationsCapabilityRepository>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }

    @Test fun `preview reacts to canonical projection even when full capability is disabled`() = runTest(dispatcher) {
        val enabled = MutableStateFlow(false)
        val snapshot = MutableStateFlow(WorkspaceRelationsProjection("w", emptyList()))
        every { relations.observe("w") } returns snapshot
        every { capabilities.observeEnabled("w") } returns enabled
        val vm = WorkspaceEntityHubViewModel(SavedStateHandle(mapOf("workspaceId" to "w", "sourceContextId" to "legacy")), relations, capabilities)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.enabled).isFalse()
        snapshot.value = snapshot.value.copy(embodiedAspect = EmbodiedAspectRelations("a", "Renamed source", emptyList()))
        assertThat(vm.state.value.projection?.embodiedAspect?.title).isEqualTo("Renamed source")
        enabled.value = true
        assertThat(vm.state.value.enabled).isTrue()
        snapshot.value = WorkspaceRelationsProjection("w", emptyList())
        assertThat(vm.state.value.projection?.embodiedAspect).isNull()
        verify(exactly = 1) { relations.observe("w"); capabilities.observeEnabled("w") }
        confirmVerified(relations, capabilities)
    }

    @Test fun `missing canonical ID never resolves legacy Context or performs reads`() = runTest(dispatcher) {
        val vm = WorkspaceEntityHubViewModel(SavedStateHandle(mapOf("sourceContextId" to "legacy")), relations, capabilities)
        val state = vm.state.first { !it.loading }
        assertThat(state.projection).isNull()
        verify { relations wasNot Called; capabilities wasNot Called }
    }

    @Test fun `read failure safe without write or stale relation fallback`() = runTest(dispatcher) {
        every { relations.observe("w") } returns flow { error("unavailable") }
        every { capabilities.observeEnabled("w") } returns flowOf(true)
        val vm = WorkspaceEntityHubViewModel(SavedStateHandle(mapOf("workspaceId" to "w")), relations, capabilities)
        val state = vm.state.first { it.error != null }
        assertThat(state.projection).isNull()
        assertThat(state.loading).isFalse()
    }
}
