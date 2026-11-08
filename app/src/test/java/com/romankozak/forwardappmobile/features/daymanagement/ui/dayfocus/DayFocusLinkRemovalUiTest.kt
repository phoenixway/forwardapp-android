package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.sameReferenceAs
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.goalproperties.LinksTabContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DayFocusLinkRemovalUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `trash dispatches exact typed link and removing last row shows empty state`() {
        val old = RelatedLink(LinkType.CONTEXT, "same-id", "Old Context")
        val note = old.copy(type = LinkType.NOTE_DOCUMENT, displayName = "Note")
        ui.setContent {
            var links by remember { mutableStateOf(listOf(old, note)) }
            MaterialTheme {
                LinksTabContent(links, {}, {}, {}, {}, onRemoveLink = { error("Raw-ID removal must not run") },
                    showContextAction = false, onRemoveExactLink = { removed -> links = links.filterNot { it.sameReferenceAs(removed) } })
            }
        }
        ui.onAllNodesWithContentDescription("Remove Link")[0].performClick()
        ui.onNodeWithText("Old Context").assertDoesNotExist()
        ui.onNodeWithText("Note").assertExists()
        ui.onNodeWithContentDescription("Remove Link").performClick()
        ui.onNodeWithText("No links added yet.").assertExists()
    }
}
