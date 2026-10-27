package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BigPictureNode
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BigPictureWorkspaceRenameTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CanonicalWorkspaceRepository>()
    private val target = BigPictureWorkspaceRenameTarget(CanonicalEntityRef.Workspace("workspace"), "Old")
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `cancel blank and unchanged titles never write`() = runTest(dispatcher) {
        val vm = BigPictureWorkspaceRenameViewModel(repository)
        vm.request(target)
        vm.cancel()
        vm.confirm()
        vm.request(target)
        vm.edit("  ")
        vm.confirm()
        assertThat(vm.state.value.target).isEqualTo(target)
        vm.edit(" Old ")
        vm.confirm()
        assertThat(vm.state.value.target).isNull()
        verify { repository wasNot Called }
    }

    @Test fun `rename dispatches exact canonical Workspace once not occurrence identity`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.rename("workspace", "New", any()) } coAnswers { gate.await() }
        val vm = BigPictureWorkspaceRenameViewModel(repository)
        vm.request(target)
        vm.edit(" New ")
        vm.confirm()
        vm.confirm()
        vm.cancel()
        assertThat(vm.state.value.busy).isTrue()
        gate.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.rename("workspace", "New", any()) }
        assertThat(vm.state.value.target).isNull()
    }

    @Test fun `failure keeps entered title and target for explicit retry`() = runTest(dispatcher) {
        coEvery { repository.rename(any(), any(), any()) } throws IllegalStateException("Deleted")
        val vm = BigPictureWorkspaceRenameViewModel(repository)
        vm.request(target)
        vm.edit("New")
        vm.confirm()
        advanceUntilIdle()
        assertThat(vm.state.value.target).isEqualTo(target)
        assertThat(vm.state.value.name).isEqualTo("New")
        assertThat(vm.state.value.error).contains("Deleted")
        assertThat(vm.state.value.busy).isFalse()
        coEvery { repository.rename(any(), any(), any()) } returns Unit
        vm.confirm()
        advanceUntilIdle()
        assertThat(vm.state.value.target).isNull()
    }

    @Test fun `PRIMARY and LINK cards resolve same Workspace but adaptive semantic cards do not`() {
        val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace"), "Old")
        assertThat(workspaceRenameTarget(node(workspace, PlacementKind.PRIMARY))).isEqualTo(target)
        assertThat(workspaceRenameTarget(node(workspace, PlacementKind.LINK))).isEqualTo(target)
        val semantic = CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect"), "Meaning")
        assertThat(workspaceRenameTarget(node(semantic, PlacementKind.PRIMARY)
            .copy(operationalEmbodiment = CanonicalEntityRef.Workspace("workspace")))).isNull()
        assertThat(workspaceRenameTarget(node(semantic, PlacementKind.PRIMARY).copy(entity = null))).isNull()
    }

    private fun node(entity: CanonicalEntitySummary, kind: PlacementKind) = BigPictureNode(
        HierarchyPlacement(PlacementId("appearance"), HierarchyId.GENERAL,
            HierarchyTargetRef(if (entity.ref is CanonicalEntityRef.Workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT,
                entity.ref.id), null, kind, 0, 1, 1, null, false, 1), entity, 0, 0)
}
