package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.workspace.WorkspaceRelationsRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalRelationsCapabilityRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceRelationsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val capabilities = mockk<CanonicalRelationsCapabilityRepository>()
    private val relations = mockk<WorkspaceRelationsRepository>()
    private val workspaces = mockk<WorkspaceDao> {
        coEvery { getContextBackedForContextId(any()) } returns null
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    @Test fun `disabled preserves baseline semantic surface and enabled observes one bulk projection`() = runBlocking<Unit> {
        val enabled = MutableStateFlow(false)
        val projection = MutableStateFlow(WorkspaceRelationsProjection("w", emptyList()))
        every { capabilities.observeEnabled("w") } returns enabled
        every { relations.observe("w") } returns projection
        val vm = WorkspaceRelationsViewModel(SavedStateHandle(mapOf("workspaceId" to "w", "listId" to "other")), capabilities, relations, workspaces)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            scope.launch { vm.state.collect {} }
            assertThat(vm.state.value.enabled).isFalse()
            verify(exactly = 0) { relations.observe(any()) }
            enabled.value = true
            withTimeout(5_000) { vm.state.first { it.projection != null } }
            verify(exactly = 1) { relations.observe("w") }
            projection.value = WorkspaceRelationsProjection("w", listOf(WorkspaceRelationItem(WorkspaceRelationFamily.PLANNING,
                "DAY_TASK", "t", "Task", listOf("SOURCE"), WorkspaceRelationDirection.ENTITY_TO_WORKSPACE, null, "Day task")))
            withTimeout(5_000) { vm.state.first { it.projection?.relations?.size == 1 } }
            enabled.value = false
            withTimeout(5_000) { vm.state.first { !it.enabled } }
            assertThat(vm.state.value.projection).isNull()
            verify(exactly = 1) { capabilities.observeEnabled("w") }
            confirmVerified(capabilities, relations)
            coVerify(exactly = 0) { workspaces.getContextBackedForContextId(any()) }
        } finally { scope.cancel() }
    }
    @Test fun `missing canonical Workspace ID never falls back to Context identity`() = runBlocking<Unit> {
        val vm = WorkspaceRelationsViewModel(SavedStateHandle(mapOf("sourceContextId" to "legacy")), capabilities, relations, workspaces)
        assertThat(vm.state.first().enabled).isFalse()
        verify { capabilities wasNot Called; relations wasNot Called }
    }
    @Test fun `unavailable relation read yields safe state without lifecycle writes`() = runBlocking<Unit> {
        every { capabilities.observeEnabled("w") } returns flowOf(true)
        every { relations.observe("w") } returns flow { error("read unavailable") }
        val vm = WorkspaceRelationsViewModel(SavedStateHandle(mapOf("workspaceId" to "w")), capabilities, relations, workspaces)
        val state = withTimeout(5_000) { vm.state.first { it.error != null } }
        assertThat(state.enabled).isTrue()
        assertThat(state.projection).isNull()
    }

    @Test fun `Recents route reads existing relations from verified canonical Workspace`() = runTest(dispatcher) {
        val owner = mockk<WorkspaceEntity> {
            every { id } returns "legacy"
            every { isDeleted } returns false
        }
        val item = WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, "ORIENTATION", "subject", "Meaning",
            listOf("SUPPORTS"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY, null, "Orientation")
        val projection = WorkspaceRelationsProjection("legacy", listOf(item))
        coEvery { workspaces.getContextBackedForContextId("legacy") } returns owner
        every { capabilities.observeEnabled("legacy") } returns flowOf(true)
        every { relations.observe("legacy") } returns flowOf(projection)
        val vm = WorkspaceRelationsViewModel(SavedStateHandle(mapOf("listId" to "legacy")), capabilities, relations, workspaces)
        val state = withTimeout(5_000) { vm.state.first { it.projection != null } }
        assertThat(state.projection).isEqualTo(projection)
        verify(exactly = 1) { relations.observe("legacy") }
    }

    @Test fun `missing or deleted canonical owner does not read bindings by Context locator`() = runTest(dispatcher) {
        val deleted = mockk<WorkspaceEntity> { every { isDeleted } returns true }
        coEvery { workspaces.getContextBackedForContextId("deleted") } returns deleted
        for (id in listOf("missing", "deleted")) {
            val vm = WorkspaceRelationsViewModel(SavedStateHandle(mapOf("listId" to id)), capabilities, relations, workspaces)
            backgroundScope.launch(dispatcher) { vm.state.collect {} }
            runCurrent()
            assertThat(vm.state.value.enabled).isFalse()
            assertThat(vm.state.value.projection).isNull()
            coVerify(exactly = 1) { workspaces.getContextBackedForContextId(id) }
        }
        verify { capabilities wasNot Called; relations wasNot Called }
    }
}
