package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
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

    @Test
    fun `retired and standalone owners resolve through presentation without live Context`() {
        val retired = presentation("retired-owner", "Retired owner")
        val standalone = presentation("standalone-owner", "Standalone owner")

        listOf(retired, standalone).forEach { presentation ->
            val result =
                contextScreenReadModel(
                    contextId = presentation.id,
                    presentations = listOf(presentation),
                    contexts = emptyList(),
                )

            assertEquals(presentation, result.presentation)
            assertNull(result.rawContext)
        }
    }

    @Test
    fun `unadmitted canonical owner stays unavailable without a Context`() {
        val result =
            contextScreenReadModel(
                contextId = "arbitrary-canonical",
                presentations = emptyList(),
                contexts = emptyList(),
            )

        assertNull(result.presentation)
        assertNull(result.rawContext)
    }

    @Test
    fun `live ordinary Context remains the raw compatibility owner`() {
        val context =
            Context(
                id = "ordinary-owner",
                name = "Legacy owner",
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val result =
            contextScreenReadModel(
                contextId = context.id,
                presentations = listOf(presentation(context.id, context.name)),
                contexts = listOf(context),
            )

        assertEquals(context, result.rawContext)
        assertEquals(context.name, result.presentation?.name)
    }

    private fun presentation(id: String, name: String) =
        ContextPresentation(
            id = id,
            name = name,
            description = "description-$id",
            parentId = "parent-$id",
            roleCode = "role-$id",
            order = 7L,
            tags = listOf("tag-$id"),
        )
}
