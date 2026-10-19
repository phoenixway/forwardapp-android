package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.workspace.WorkspaceSemanticBootstrapCoordinator
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceSemanticBootstrapViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val coordinator = mockk<WorkspaceSemanticBootstrapCoordinator>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `Context-backed and standalone product actions prefill Workspace scalars only`() {
        listOf("backed", "native").forEach { workspaceId ->
            val vm = model(SavedStateHandle(mapOf("workspaceId" to workspaceId)))
            vm.open("Canonical Workspace", "# Markdown\n\nDescription")
            assertThat(vm.state.value.open).isTrue()
            assertThat(vm.state.value.title).isEqualTo("Canonical Workspace")
            assertThat(vm.state.value.description).isEqualTo("# Markdown\n\nDescription")
            assertThat(vm.state.value.entityClass).isNull()
            assertThat(vm.state.value.bindingType).isNull()
            assertThat(vm.state.value.canSubmit).isFalse()
        }
        coVerify(exactly = 0) { coordinator.createAndBind(any()) }
    }

    @Test fun `explicit class binding kind and nonblank title are required`() {
        val vm = model()
        vm.open("Title", null)
        vm.chooseBinding(WorkspaceBindingType.SUPPORTS)
        vm.submit()
        vm.chooseClass(SemanticEntityClass.ORIENTATION)
        vm.submit()
        assertThat(vm.state.value.canSubmit).isFalse()
        vm.chooseKind(OrientationKind.INTENT)
        assertThat(vm.state.value.canSubmit).isTrue()
        vm.edit(title = " ")
        vm.submit()
        coVerify(exactly = 0) { coordinator.createAndBind(any()) }
    }

    @Test fun `Orientation confirms edited defaults and opens exact Universal Semantic Editor once`() = runTest(dispatcher) {
        coEvery { coordinator.createAndBind(any()) } returns "new-orientation"
        val vm = prepared()
        vm.edit(title = "Edited", description = "## Meaning")
        vm.submit()
        vm.submit()
        coVerify(exactly = 1) { coordinator.createAndBind(match {
            it.workspaceId == "workspace" && it.subjectType == ManagedSubjectType.ORIENTATION &&
                it.orientationKind == OrientationKind.INTENT && it.title == "Edited" &&
                it.description == "## Meaning" && it.bindingType == WorkspaceBindingType.REALIZES
        }) }
        assertThat(vm.state.value.open).isFalse()
        assertThat(vm.consumeCreatedDestination()).isEqualTo(NavTarget.SemanticEntityEditor("new-orientation"))
        assertThat(vm.consumeCreatedDestination()).isNull()
    }

    @Test fun `Aspect explicitly chosen discards Orientation kind and uses canonical type`() = runTest(dispatcher) {
        coEvery { coordinator.createAndBind(any()) } returns "new-aspect"
        val vm = prepared()
        vm.chooseClass(SemanticEntityClass.ASPECT)
        assertThat(vm.state.value.kind).isNull()
        vm.submit()
        coVerify(exactly = 1) { coordinator.createAndBind(match {
            it.subjectType == ManagedSubjectType.ASPECT && it.orientationKind == null
        }) }
    }

    @Test fun `double tap dismiss and edits while saving cannot duplicate or replace request`() = runTest(dispatcher) {
        val gate = CompletableDeferred<String>()
        coEvery { coordinator.createAndBind(any()) } coAnswers { gate.await() }
        val vm = prepared()
        vm.submit()
        vm.submit()
        vm.cancel()
        vm.open("Replacement", null)
        vm.edit(title = "Replacement")
        vm.chooseClass(SemanticEntityClass.ASPECT)
        assertThat(vm.state.value.saving).isTrue()
        assertThat(vm.state.value.title).isEqualTo("Workspace title")
        gate.complete("new-id")
        coVerify(exactly = 1) { coordinator.createAndBind(any()) }
    }

    @Test fun `cancel before confirmation creates nothing`() {
        val vm = prepared()
        vm.cancel()
        vm.submit()
        assertThat(vm.state.value.open).isFalse()
        coVerify(exactly = 0) { coordinator.createAndBind(any()) }
    }

    @Test fun `binding failure exposes error preserves editable draft and allows explicit retry`() = runTest(dispatcher) {
        coEvery { coordinator.createAndBind(any()) } throws IllegalArgumentException("EMBODIES conflict")
        val vm = prepared()
        vm.submit()
        assertThat(vm.state.value.error).contains("EMBODIES")
        assertThat(vm.state.value.saving).isFalse()
        assertThat(vm.state.value.open).isTrue()
        assertThat(vm.state.value.title).isEqualTo("Workspace title")
        assertThat(vm.consumeCreatedDestination()).isNull()
        coEvery { coordinator.createAndBind(any()) } returns "retry-id"
        vm.chooseBinding(WorkspaceBindingType.SUPPORTS)
        vm.submit()
        assertThat(vm.consumeCreatedDestination()).isEqualTo(NavTarget.SemanticEntityEditor("retry-id"))
    }

    @Test fun `SavedState completed result restores only navigation never creation`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("workspaceId" to "workspace"))
        coEvery { coordinator.createAndBind(any()) } returns "created-id"
        prepared(saved).submit()
        val restored = model(saved)
        restored.submit()
        assertThat(restored.consumeCreatedDestination()).isEqualTo(NavTarget.SemanticEntityEditor("created-id"))
        assertThat(model(saved).consumeCreatedDestination()).isNull()
        coVerify(exactly = 1) { coordinator.createAndBind(any()) }
    }

    @Test fun `SavedState interrupted confirmation never replays and warns before explicit new action`() {
        val saved = SavedStateHandle(mapOf("workspaceId" to "workspace", "workspaceSemanticBootstrap.submitted" to true))
        val restored = model(saved)
        restored.submit()
        assertThat(restored.state.value.open).isFalse()
        restored.open("Workspace", null)
        assertThat(restored.state.value.error).contains("Перевірте")
        assertThat(restored.state.value.entityClass).isNull()
        assertThat(restored.state.value.canSubmit).isFalse()
        coVerify(exactly = 0) { coordinator.createAndBind(any()) }
    }

    @Test fun `missing canonical Workspace key cannot fall back to Context id`() {
        val vm = model(SavedStateHandle(mapOf("listId" to "legacy")))
        vm.open("Legacy", null)
        assertThat(vm.state.value.open).isFalse()
        coVerify(exactly = 0) { coordinator.createAndBind(any()) }
    }

    private fun model(saved: SavedStateHandle = SavedStateHandle(mapOf("workspaceId" to "workspace"))) =
        WorkspaceSemanticBootstrapViewModel(saved, coordinator)

    private fun prepared(saved: SavedStateHandle = SavedStateHandle(mapOf("workspaceId" to "workspace"))) = model(saved).apply {
        open("Workspace title", "Description")
        chooseClass(SemanticEntityClass.ORIENTATION)
        chooseKind(OrientationKind.INTENT)
        chooseBinding(WorkspaceBindingType.REALIZES)
    }
}
