package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextScreenContextReadModelTest {
    @Test
    fun `shell-free System owner keeps presentation without manufacturing Context`() {
        val id = SystemContexts.INBOX.raw
        val presentation =
            ContextPresentation(
                id = id,
                name = "Canonical Inbox",
                description = "Canonical description",
                parentId = null,
                roleCode = null,
                order = 7L,
                tags = listOf("canonical"),
            )

        val result =
            contextScreenReadModel(
                contextId = id,
                presentations = listOf(presentation),
                contexts = emptyList(),
            )

        assertEquals(presentation, result.presentation)
        assertNull(result.rawContext)
    }
}
