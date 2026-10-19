package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BigPictureSourceDeleteViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val target = BigPictureSourceDeleteTarget(CanonicalEntityRef.Orientation("source"), "Meaning", 3)
    private val available = MutableStateFlow(mapOf(target.ref to target))
    private val actions = mockk<BigPictureTargetActions>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { actions.observeTargets() } returns available
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `request and Cancel never dispatch source deletion`() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(dispatcher) { vm.targets.collect() }
        vm.request(target)
        assertThat(vm.state.value.pending).isEqualTo(target)
        vm.cancel()
        vm.confirm()
        coVerify(exactly = 0) { actions.deleteSource(any()) }
    }

    @Test fun `double confirmation sends one typed versioned lifecycle request`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { actions.deleteSource(target) } coAnswers { gate.await() }
        val vm = model()
        backgroundScope.launch(dispatcher) { vm.targets.collect() }
        vm.request(target)
        vm.confirm()
        vm.confirm()
        vm.cancel()
        assertThat(vm.state.value.busy).isTrue()
        gate.complete(Unit)
        vm.confirm()
        coVerify(exactly = 1) { actions.deleteSource(target) }
        assertThat(vm.state.value.pending).isNull()
        assertThat(vm.state.value.message).isEqualTo("Джерело видалено")
    }

    @Test fun `lifecycle error stays in confirmation and never reports success`() = runTest(dispatcher) {
        coEvery { actions.deleteSource(target) } throws IllegalArgumentException("version conflict")
        val vm = model()
        backgroundScope.launch(dispatcher) { vm.targets.collect() }
        vm.request(target)
        vm.confirm()
        assertThat(vm.state.value.error).contains("version conflict")
        assertThat(vm.state.value.pending).isEqualTo(target)
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.state.value.message).isNull()
    }

    @Test fun `unsupported or stale target cannot open confirmation`() = runTest(dispatcher) {
        val vm = model()
        backgroundScope.launch(dispatcher) { vm.targets.collect() }
        vm.request(target.copy(version = 2))
        assertThat(vm.state.value.pending).isNull()
        available.value = emptyMap()
        vm.request(target)
        vm.confirm()
        coVerify(exactly = 0) { actions.deleteSource(any()) }
    }

    @Test fun `new ViewModel never restores or replays prior confirmation`() = runTest(dispatcher) {
        val original = model()
        backgroundScope.launch(dispatcher) { original.targets.collect() }
        original.request(target)
        val recreated = model()
        recreated.confirm()
        assertThat(recreated.state.value.pending).isNull()
        coVerify(exactly = 0) { actions.deleteSource(any()) }
    }

    private fun model() = BigPictureSourceDeleteViewModel(actions)
}
