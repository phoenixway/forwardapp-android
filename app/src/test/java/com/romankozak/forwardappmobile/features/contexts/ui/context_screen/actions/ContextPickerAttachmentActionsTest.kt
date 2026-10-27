package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextPickerAttachmentActionsTest {
    private val repository = mockk<ContextRepository>(relaxed = true)
    private fun actions() = ContextPickerActions(ContextPickerRepositories(repository, mockk(relaxed = true),
        mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
        mockk(relaxed = true), mockk(relaxed = true), "test")

    @Test fun `existing selection delegates exact attachment and Workspace without creating content`() = runBlocking {
        actions().onPickerAttachmentSelected("workspace", "attachment")
        coVerify(exactly = 1) { repository.linkAttachmentToContext("attachment", "workspace") }
        confirmVerified(repository)
    }

    @Test fun `placement failure reaches product error callback instead of silently disappearing`() = runBlocking {
        coEvery { repository.linkAttachmentToContext(any(), any()) } throws IllegalStateException("inactive")
        var error: String? = null
        actions().onPickerAttachmentSelected("workspace", "attachment") { error = it }
        assertThat(error).isEqualTo("inactive")
    }

    @Test fun `cancellation is not swallowed as an ordinary picker error`() = runBlocking {
        coEvery { repository.linkAttachmentToContext(any(), any()) } throws CancellationException("cancel")
        var notified = false
        val failure = runCatching { actions().onPickerAttachmentSelected("workspace", "attachment") { notified = true } }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
        assertThat(notified).isFalse()
    }
}
