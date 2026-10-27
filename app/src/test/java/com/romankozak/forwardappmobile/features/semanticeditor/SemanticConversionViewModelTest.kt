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
class SemanticConversionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val coordinator = mockk<CanonicalAspectConversionCoordinator>()
    private val plan = AspectConversionPlan("a", "Treatment", 1, null, listOf("child"), 2, 1, 1, mapOf("child:child" to 1))
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { coordinator.canOffer("a") } returns true
        coEvery { coordinator.preview("a") } returns plan
    }
    @After fun reset() { Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle(mapOf("subjectId" to "a"))) = SemanticConversionViewModel(saved, coordinator)

    @Test fun `type choice is explicit and cancellation dispatches no conversion`() = runTest(dispatcher) {
        val vm = vm(); vm.open()
        assertThat(vm.state.value.kind).isNull()
        vm.confirm()
        vm.select(OrientationKind.GOAL)
        assertThat(vm.state.value.kind).isNull()
        vm.select(OrientationKind.PROJECT); vm.cancel(); vm.confirm()
        coVerify(exactly = 0) { coordinator.convert(any(), any(), any()) }
        assertThat(vm.state.value.open).isFalse()
    }

    @Test fun `double tap runs exactly one captured versioned conversion and completion is consumed once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<String>()
        coEvery { coordinator.convert(plan, OrientationKind.PROJECT, any()) } coAnswers { gate.await() }
        val vm = vm(); vm.open(); vm.select(OrientationKind.PROJECT)
        vm.confirm(); vm.confirm(); vm.cancel()
        assertThat(vm.state.value.busy).isTrue()
        gate.complete("a")
        assertThat(vm.consumeCompleted()).isTrue()
        assertThat(vm.consumeCompleted()).isFalse()
        assertThat(vm.state.value.eligible).isFalse()
        coVerify(exactly = 1) { coordinator.convert(plan, OrientationKind.PROJECT, any()) }
    }

    @Test fun `failure retains the kind and impact confirmation for a visible error without reporting success`() = runTest(dispatcher) {
        coEvery { coordinator.convert(any(), any(), any()) } throws IllegalStateException("Dependencies changed")
        val vm = vm(); vm.open(); vm.select(OrientationKind.PROJECT); vm.confirm()
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.state.value.open).isTrue()
        assertThat(vm.state.value.error).isEqualTo("Dependencies changed")
        assertThat(vm.state.value.kind).isEqualTo(OrientationKind.PROJECT)
        assertThat(vm.consumeCompleted()).isFalse()
    }

    @Test fun `SavedState recreation never replays a pending or completed command`() = runTest(dispatcher) {
        coEvery { coordinator.canOffer("a") } returns false
        val vm = vm(SavedStateHandle(mapOf("subjectId" to "a", "pendingConversion" to "PROJECT", "conversionCompleted" to true)))
        assertThat(vm.state.value.eligible).isFalse()
        assertThat(vm.state.value.open).isFalse()
        vm.confirm()
        coVerify(exactly = 0) { coordinator.convert(any(), any(), any()) }
    }

    @Test fun `unsupported dependency is explained in the dialog before confirmation`() = runTest(dispatcher) {
        coEvery { coordinator.preview("a") } throws IllegalStateException("Recurring source is unsupported")
        val vm = vm(); vm.open(); vm.select(OrientationKind.PROJECT); vm.confirm()
        assertThat(vm.state.value.error).contains("Recurring source")
        assertThat(vm.state.value.plan).isNull()
        coVerify(exactly = 0) { coordinator.convert(any(), any(), any()) }
    }
}
