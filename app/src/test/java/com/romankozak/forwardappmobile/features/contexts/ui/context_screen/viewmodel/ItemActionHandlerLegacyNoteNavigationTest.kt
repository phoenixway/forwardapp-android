package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ItemActionHandlerLegacyNoteNavigationTest {
    @Test
    fun `placed Note click navigates by Note ID not historical Context or placement ID`() = runTest {
        val listener = mockk<ItemActionHandler.ResultListener>(relaxed = true)
        every { listener.isSelectionModeActive() } returns false
        val handler = handler(backgroundScope, "workspace-a", listener)

        handler.onItemClick(noteItem())

        verify(exactly = 1) {
            listener.requestNavigation(
                NavTarget.LegacyNote(
                    noteId = "independent-note",
                    workspaceId = "workspace-a",
                ),
            )
        }
        verify(exactly = 0) { listener.requestNavigation(any<String>()) }
        verify(exactly = 0) { listener.toggleSelection(any()) }
    }

    @Test
    fun `selection mode selects placement without navigating to Note`() = runTest {
        val listener = mockk<ItemActionHandler.ResultListener>(relaxed = true)
        every { listener.isSelectionModeActive() } returns true
        val handler = handler(backgroundScope, "workspace-a", listener)

        handler.onItemClick(noteItem())

        verify(exactly = 1) { listener.toggleSelection("placement-id") }
        verify(exactly = 0) { listener.requestNavigation(any<NavTarget>()) }
        verify(exactly = 0) { listener.requestNavigation(any<String>()) }
    }

    private fun handler(
        scope: CoroutineScope,
        workspaceId: String,
        listener: ItemActionHandler.ResultListener,
    ): ItemActionHandler {
        val clipboard = mockk<BacklogClipboardUseCase>(relaxed = true)
        every { clipboard.clipboardPayload } returns MutableStateFlow(null)

        return ItemActionHandler(
            contextRepository = mockk<ContextRepository>(relaxed = true),
            goalRepository = mockk<GoalRepository>(relaxed = true),
            inboxRepository = mockk<InboxRepository>(relaxed = true),
            recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true),
            backlogClipboardUseCase = clipboard,
            scope = scope,
            projectIdFlow = MutableStateFlow(workspaceId),
            resultListener = listener,
        )
    }

    private fun noteItem(): BacklogItemContent.NoteItem {
        val note =
            LegacyNoteEntity(
                id = "independent-note",
                contextId = "retired-context",
                title = "Standalone Note",
                content = "Canonical content",
            )
        val placement =
            BacklogItem(
                id = "placement-id",
                contextId = "workspace-a",
                itemType = BacklogItemTypeValues.NOTE,
                entityId = note.id,
                order = 0L,
            )
        return BacklogItemContent.NoteItem(note, placement)
    }
}
