package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LogActivityHandlerTest {
    private val repository = mockk<ContextLogRepository>(relaxed = true)

    @Test fun `standalone Workspace comment and milestone use exact canonical owner without Context adapter`() = runTest {
        val handler = LogActivityHandler(repository, mockk(relaxed = true), ContextStateManager(backgroundScope), backgroundScope, true) { fail(it) }
        handler.addProjectComment("entry", "workspace")
        handler.addMilestone("step", "workspace")
        runCurrent()
        coVerify(exactly = 1) { repository.addWorkspaceLogEntry("workspace", "COMMENT", "entry") }
        coVerify(exactly = 1) { repository.addWorkspaceLogEntry("workspace", "MILESTONE", "step") }
        coVerify(exactly = 0) { repository.addContextLogEntry(any(), any(), any(), any()) }
    }

    @Test fun `explicit legacy route retains provenance adapter`() = runTest {
        val handler = LogActivityHandler(repository, mockk(relaxed = true), ContextStateManager(backgroundScope), backgroundScope, false) { fail(it) }
        handler.addProjectComment("entry", "legacy")
        runCurrent()
        coVerify(exactly = 1) { repository.addContextLogEntry("legacy", "COMMENT", "entry", null) }
        coVerify(exactly = 0) { repository.addWorkspaceLogEntry(any(), any(), any()) }
    }

    @Test fun `rejected write reports error instead of crashing and restores cleared draft`() = runTest {
        val state = state(backgroundScope)
        val errors = mutableListOf<String>()
        coEvery { repository.addWorkspaceLogEntry(any(), any(), any()) } throws IllegalArgumentException("Unavailable owner")
        val handler = LogActivityHandler(repository, mockk(relaxed = true), state, backgroundScope, true, errors::add)
        handler.addProjectComment("unsaved", "workspace")
        runCurrent()
        assertEquals("unsaved", state.uiState.value.inputValue.text)
        assertEquals(1, errors.size)
    }

    @Test fun `failed write never overwrites newer input or another Workspace`() = runTest {
        val state = state(backgroundScope)
        state.setInputValue(TextFieldValue("Next draft"))
        coEvery { repository.addWorkspaceLogEntry(any(), any(), any()) } throws IllegalStateException("Disabled")
        val handler = LogActivityHandler(repository, mockk(relaxed = true), state, backgroundScope, true) {}
        handler.addProjectComment("old", "workspace")
        runCurrent()
        assertEquals("Next draft", state.uiState.value.inputValue.text)
        state.setInputValue(TextFieldValue(""))
        handler.addProjectComment("old", "another-workspace")
        runCurrent()
        assertEquals("", state.uiState.value.inputValue.text)
    }

    @Test fun `cancellation is not reported as authoring failure and blanks do not write`() = runTest {
        var errors = 0
        coEvery { repository.addWorkspaceLogEntry(any(), any(), any()) } throws CancellationException()
        val handler = LogActivityHandler(repository, mockk(relaxed = true), ContextStateManager(backgroundScope), backgroundScope, true) { errors++ }
        handler.addProjectComment("", "workspace")
        handler.addProjectComment("cancel", "workspace")
        runCurrent()
        assertEquals(0, errors)
        coVerify(exactly = 1) { repository.addWorkspaceLogEntry(any(), any(), any()) }
    }

    private fun state(scope: kotlinx.coroutines.CoroutineScope) = ContextStateManager(scope).apply {
        updateState { it.copy(presentation = ContextPresentation("workspace", "Work", null, null, null, 0, null), inputMode = InputMode.AddProjectLog) }
    }
}
