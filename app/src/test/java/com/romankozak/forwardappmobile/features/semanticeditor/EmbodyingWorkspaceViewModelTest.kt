package com.romankozak.forwardappmobile.features.semanticeditor

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.workspace.SemanticEmbodyingWorkspaceCoordinator
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
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
class EmbodyingWorkspaceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `action prefills saved semantic name and description with exact Orientation or Aspect identity`() {
        for (type in SemanticEntityClass.entries) {
            val vm = EmbodyingWorkspaceViewModel(mockk())
            vm.open(model().copy(entityClass = type))
            val request = vm.state.value.request!!
            assertThat(request.subjectId).isEqualTo("subject")
            assertThat(request.expectedVersion).isEqualTo(7)
            assertThat(request.name).isEqualTo("Saved name")
            assertThat(request.description).isEqualTo("**Markdown**")
            assertThat(request.subjectType).isEqualTo(if (type == SemanticEntityClass.ORIENTATION) ManagedSubjectType.ORIENTATION else ManagedSubjectType.ASPECT)
        }
    }

    @Test fun `existing EMBODIES or another editor mutation disables creation`() {
        val bound = model().copy(workspaceBindings = listOf(WorkspaceBindingItem("binding", "workspace", "Existing", WorkspaceBindingType.EMBODIES, true)))
        val vm = EmbodyingWorkspaceViewModel(mockk())
        for (blocked in listOf(bound, model().copy(isSaving = true), model().copy(isRelationMutationInFlight = true))) {
            assertThat(blocked.canCreateEmbodyingWorkspace).isFalse()
            vm.open(blocked)
            assertThat(vm.state.value.request).isNull()
        }
    }

    @Test fun `cancel and recreation never submit creation`() {
        val command = mockk<SemanticEmbodyingWorkspaceCoordinator>()
        val vm = EmbodyingWorkspaceViewModel(command)
        vm.open(model()); vm.setName("Changed"); vm.cancel(); vm.confirm()
        EmbodyingWorkspaceViewModel(command).confirm()
        coVerify(exactly = 0) { command.create(any(), any()) }
    }

    @Test fun `double confirmation submits once and success navigation result is consumed once`() = runTest(dispatcher) {
        val command = mockk<SemanticEmbodyingWorkspaceCoordinator>()
        val pending = CompletableDeferred<String>()
        coEvery { command.create(any(), any()) } coAnswers { pending.await() }
        val vm = EmbodyingWorkspaceViewModel(command)
        vm.open(model()); vm.setName("New Workspace"); vm.setDescription("Edited Markdown")
        vm.confirm(); vm.confirm(); vm.cancel(); vm.setName("Ignored while busy")
        assertThat(vm.state.value.busy).isTrue()
        pending.complete("canonical-workspace")
        assertThat(vm.consumeCreatedId()).isEqualTo("canonical-workspace")
        assertThat(vm.consumeCreatedId()).isNull()
        coVerify(exactly = 1) { command.create(match { it.subjectId == "subject" && it.name == "New Workspace" && it.description == "Edited Markdown" }, any()) }
    }

    @Test fun `failure preserves editable request without success or automatic retry`() = runTest(dispatcher) {
        val command = mockk<SemanticEmbodyingWorkspaceCoordinator>()
        coEvery { command.create(any(), any()) } throws IllegalStateException("Binding conflict")
        val vm = EmbodyingWorkspaceViewModel(command)
        vm.open(model()); vm.confirm()
        assertThat(vm.state.value.error).isEqualTo("Binding conflict")
        assertThat(vm.state.value.request).isNotNull()
        assertThat(vm.state.value.busy).isFalse()
        assertThat(vm.consumeCreatedId()).isNull()
        coVerify(exactly = 1) { command.create(any(), any()) }
    }

    @Test fun `blank Workspace name cannot submit`() {
        val command = mockk<SemanticEmbodyingWorkspaceCoordinator>()
        val vm = EmbodyingWorkspaceViewModel(command)
        vm.open(model()); vm.setName(" "); vm.confirm()
        coVerify(exactly = 0) { command.create(any(), any()) }
    }

    private fun model() = SemanticEntityEditorModel("subject", SemanticEntityClass.ORIENTATION,
        "Unsaved semantic draft", "Saved name", "**Markdown**", 7)
}
