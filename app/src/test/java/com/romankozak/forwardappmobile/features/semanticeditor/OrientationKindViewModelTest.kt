package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrientationKindViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val command = mockk<CanonicalOrientationKindCoordinator>()
    private val plan = OrientationKindPlan("id", "Meaning", 1, OrientationKind.INTENT, "revision", 1, 2, 1)
    @Before fun setup() { Dispatchers.setMain(dispatcher); coEvery { command.canOffer("id") } returns true; coEvery { command.preview("id") } returns plan }
    @After fun reset() { Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle(mapOf("subjectId" to "id"))) = OrientationKindViewModel(saved, command)

    @Test fun `explicit different kind is mandatory and cancellation creates nothing`() = runTest(dispatcher) {
        val vm = vm(); vm.open(); vm.confirm()
        vm.select(OrientationKind.INTENT); vm.select(OrientationKind.GOAL)
        assertThat(vm.state.value.target).isNull()
        vm.select(OrientationKind.PROJECT); vm.cancel(); vm.confirm()
        coVerify(exactly = 0) { command.transform(any(), any(), any()) }
    }
    @Test fun `double submission dispatches once and successful reload event is consumable once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<String>()
        coEvery { command.transform(plan, OrientationKind.PROJECT, any()) } coAnswers { gate.await() }
        val vm = vm(); vm.open(); vm.select(OrientationKind.PROJECT); vm.confirm(); vm.confirm(); vm.cancel()
        assertThat(vm.state.value.busy).isTrue(); gate.complete("id")
        assertThat(vm.consumeCompleted()).isTrue(); assertThat(vm.consumeCompleted()).isFalse()
        coVerify(exactly = 1) { command.transform(plan, OrientationKind.PROJECT, any()) }
    }
    @Test fun `failure retains confirmation and error without claiming completion`() = runTest(dispatcher) {
        coEvery { command.transform(any(), any(), any()) } throws IllegalStateException("Changed")
        val vm = vm(); vm.open(); vm.select(OrientationKind.ONGOING_STANDARD); vm.confirm()
        assertThat(vm.state.value.error).isEqualTo("Changed"); assertThat(vm.state.value.open).isTrue()
        assertThat(vm.state.value.busy).isFalse(); assertThat(vm.consumeCompleted()).isFalse()
    }
    @Test fun `SavedState cannot replay a transform and eligibility can refresh after Aspect conversion`() = runTest(dispatcher) {
        coEvery { command.canOffer("id") } returns false
        val vm = vm(SavedStateHandle(mapOf("subjectId" to "id", "pendingKind" to "PROJECT", "completed" to true)))
        vm.confirm(); assertThat(vm.state.value.open).isFalse()
        coEvery { command.canOffer("id") } returns true
        vm.refreshEligibility(); assertThat(vm.state.value.eligible).isTrue()
        coVerify(exactly = 0) { command.transform(any(), any(), any()) }
    }
}
