package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import android.app.Application
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en")
class ChecklistItemActionsSheetTest {
    @get:Rule val ui = createComposeRule()

    @Test fun copyTargetsOnlyTheMenuItemAndClearsTemporarySelection() {
        val model = mockk<ChecklistViewModel>(relaxed = true)
        every { model.copySelectedToEntityClipboard() } returns 1
        assertEquals(1, model.putItemInEntityClipboard("source-item", cut = false))
        verifyOrder {
            model.onClearSelection()
            model.onItemLongPressed("source-item")
            model.copySelectedToEntityClipboard()
            model.onClearSelection()
        }
        verify(exactly = 0) { model.onDeleteItem(any()) }
        verify(exactly = 0) { model.cutSelectedToEntityClipboard() }
    }

    @Test fun cutUsesDeferredMoveAndDoesNotDeleteSourceFromTheMenu() {
        val model = mockk<ChecklistViewModel>(relaxed = true)
        every { model.cutSelectedToEntityClipboard() } returns 1
        assertEquals(1, model.putItemInEntityClipboard("source-item", cut = true))
        verifyOrder {
            model.onClearSelection()
            model.onItemLongPressed("source-item")
            model.cutSelectedToEntityClipboard()
            model.onClearSelection()
        }
        verify(exactly = 0) { model.onDeleteItem(any()) }
        verify(exactly = 0) { model.copySelectedToEntityClipboard() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun pasteDelegatesToSharedClipboardPipelineOfDestinationChecklist() {
        val model = mockk<ChecklistViewModel>(relaxed = true)
        every { model.canPasteChecklistItemsFromEntityClipboard() } returns true
        var dismissed = false
        ui.setContent {
            MaterialTheme {
                ChecklistItemActionsSheet("destination-item", model,
                    rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onDismiss = { dismissed = true }, onAddAttachmentLink = {}, onAddContextLink = {}, onMessage = {})
            }
        }
        ui.onNodeWithText("Paste").performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(true, dismissed)
        verify(exactly = 1) { model.pasteChecklistItemsFromEntityClipboard(any()) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun pasteIsDisabledWhenSharedClipboardHasNoSupportedItems() {
        val model = mockk<ChecklistViewModel>(relaxed = true)
        every { model.canPasteChecklistItemsFromEntityClipboard() } returns false
        ui.setContent {
            MaterialTheme {
                ChecklistItemActionsSheet("item", model,
                    rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onDismiss = {}, onAddAttachmentLink = {}, onAddContextLink = {}, onMessage = {})
            }
        }
        ui.onNodeWithText("Paste").assertIsNotEnabled()
        verify(exactly = 0) { model.pasteChecklistItemsFromEntityClipboard(any()) }
    }
}
