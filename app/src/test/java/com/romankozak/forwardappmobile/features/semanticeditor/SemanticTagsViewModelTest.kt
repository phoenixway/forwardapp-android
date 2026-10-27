package com.romankozak.forwardappmobile.features.semanticeditor

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticTagRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class SemanticTagsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repo = mockk<CanonicalSemanticTagRepository>()
    @Before fun main() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    @Test fun `editor adds and removes exact typed tag for Orientation and Aspect`() = runTest(dispatcher) {
        every { repo.observeTags(any()) } returns flowOf(emptySet())
        coEvery { repo.addTag(any(), any(), any()) } just Runs
        coEvery { repo.removeTag(any(), any(), any()) } just Runs
        val vm = SemanticTagsViewModel(repo)
        for (kind in ManagedSubjectType.entries) {
            val ref = ManagedSubjectRef(kind, "same-raw-id")
            vm.show(ref); vm.setInput("#HEALTH"); vm.add(); vm.remove("health")
            coVerify(exactly = 1) { repo.addTag(ref, "#HEALTH", any()) }
            coVerify(exactly = 1) { repo.removeTag(ref, "health", any()) }
        }
    }
    @Test fun `editor observes live membership without manual refresh`() = runTest(dispatcher) {
        val tags = MutableStateFlow(setOf("health"))
        every { repo.observeTags(any()) } returns tags
        val vm = SemanticTagsViewModel(repo)
        backgroundScope.launch(dispatcher) { vm.tags.collect {} }
        vm.show(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, "o"))
        assertThat(vm.tags.value).containsExactly("health")
        tags.value = setOf("finance")
        assertThat(vm.tags.value).containsExactly("finance")
    }
    @Test fun `failed mutation retains editable input and reports error`() = runTest(dispatcher) {
        coEvery { repo.addTag(any(), any(), any()) } throws IllegalArgumentException("Deleted owner")
        val vm = SemanticTagsViewModel(repo)
        vm.show(ManagedSubjectRef(ManagedSubjectType.ASPECT, "a")); vm.setInput("health"); vm.add()
        assertThat(vm.editing.value.input).isEqualTo("health")
        assertThat(vm.editing.value.error).isEqualTo("Deleted owner")
        assertThat(vm.editing.value.busy).isFalse()
    }
    @Test fun `double submit cannot dispatch duplicate writes and recreation cannot replay input`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repo.addTag(any(), any(), any()) } coAnswers { gate.await() }
        val vm = SemanticTagsViewModel(repo)
        vm.show(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, "o")); vm.setInput("health"); vm.add(); vm.add()
        coVerify(exactly = 1) { repo.addTag(any(), any(), any()) }
        gate.complete(Unit)
        SemanticTagsViewModel(repo).show(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, "o"))
        coVerify(exactly = 1) { repo.addTag(any(), any(), any()) }
    }
}
