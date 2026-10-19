package com.romankozak.forwardappmobile.features.semanticeditor

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticEntityLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.SemanticEntityDeletionTarget
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SemanticEntityDeleteViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val lifecycle = mockk<CanonicalSemanticEntityLifecycleCoordinator>()
    private val target = SemanticEntityDeletionTarget("semantic", ManagedSubjectType.ORIENTATION, "Saved title", 7)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `linked Workspace opens exact canonical operational identity regardless of title and binding role`() {
        WorkspaceBindingType.entries.forEach { type ->
            assertThat(WorkspaceBindingItem("binding", "canonical-workspace", "semantic", type, false).destination())
                .isEqualTo(NavTarget.Workspace("canonical-workspace"))
        }
    }

    @Test fun `unsupported or unavailable identity exposes no deletion request`() = runTest(dispatcher) {
        coEvery { lifecycle.deletionTarget(any()) } returns null
        val vm = SemanticEntityDeleteViewModel(lifecycle)
        vm.prepare("specialized"); vm.request(); vm.confirm()
        assertThat(vm.state.value.target).isNull()
        coVerify(exactly = 0) { lifecycle.tombstone(any(), any(), any(), any()) }
    }

    @Test fun `capability read failure fails closed without offering delete`() = runTest(dispatcher) {
        coEvery { lifecycle.deletionTarget(any()) } throws IllegalStateException("Unavailable")
        val vm = SemanticEntityDeleteViewModel(lifecycle)
        vm.prepare("semantic")
        assertThat(vm.state.value.target).isNull()
        assertThat(vm.consumeDeleted()).isFalse()
    }

    @Test fun `cancel and recreation cannot replay destructive command`() = runTest(dispatcher) {
        coEvery { lifecycle.deletionTarget(any()) } returns target
        val vm = SemanticEntityDeleteViewModel(lifecycle)
        vm.prepare("semantic"); vm.request(); vm.cancel(); vm.confirm()
        SemanticEntityDeleteViewModel(lifecycle).confirm()
        coVerify(exactly = 0) { lifecycle.tombstone(any(), any(), any(), any()) }
        assertThat(vm.consumeDeleted()).isFalse()
    }

    @Test fun `double confirmation deletes exact captured source version once and closes only after commit`() = runTest(dispatcher) {
        for (type in ManagedSubjectType.entries) {
            clearMocks(lifecycle)
            val pending = CompletableDeferred<Unit>()
            coEvery { lifecycle.deletionTarget(any()) } returns target.copy(subjectType = type)
            coEvery { lifecycle.tombstone(any(), any(), any(), any()) } coAnswers { pending.await() }
            val vm = SemanticEntityDeleteViewModel(lifecycle)
            vm.prepare("semantic"); vm.request(); vm.confirm(); vm.confirm(); vm.cancel()
            assertThat(vm.state.value.busy).isTrue()
            assertThat(vm.consumeDeleted()).isFalse()
            pending.complete(Unit)
            assertThat(vm.consumeDeleted()).isTrue()
            assertThat(vm.consumeDeleted()).isFalse()
            vm.confirm()
            coVerify(exactly = 1) { lifecycle.tombstone("semantic", type, 7, any()) }
        }
    }

    @Test fun `failure keeps confirmation and editor open without false success`() = runTest(dispatcher) {
        coEvery { lifecycle.deletionTarget(any()) } returns target
        coEvery { lifecycle.tombstone(any(), any(), any(), any()) } throws IllegalStateException("End Planning participation first")
        val vm = SemanticEntityDeleteViewModel(lifecycle)
        vm.prepare("semantic"); vm.request(); vm.confirm()
        assertThat(vm.state.value.error).isEqualTo("End Planning participation first")
        assertThat(vm.state.value.confirming).isTrue()
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.consumeDeleted()).isFalse()
    }
}
