package com.romankozak.forwardappmobile.features.globalsearch

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CompactOmniboxUiTest {
    @get:Rule val ui = createComposeRule()

    private var submitted = emptyList<String>()

    private fun showOmnibox() {
        ui.setContent {
            MaterialTheme {
                var query by remember { mutableStateOf("") }
                CompactOmnibox(
                    uiState = GlobalSearchUiState(query = query),
                    currentMode = OmniboxMode.DataSearch,
                    modePalette = rememberModePalette(OmniboxMode.DataSearch),
                    onShowCreate = {},
                    onShowRecents = {},
                    recentsSelected = false,
                    onSetMode = {},
                    onQueryChange = { query = it },
                    onClearQuery = { query = "" },
                    onSubmit = { submitted = submitted + query },
                    onKeyDown = { false },
                    focusRequester = remember { FocusRequester() },
                )
            }
        }
    }

    @Test fun `keyboard Done submits current query once and works after clearing`() {
        showOmnibox()
        ui.onNode(hasSetTextAction()).performTextInput("health")
        ui.onNode(hasSetTextAction()).performImeAction()
        ui.onNodeWithContentDescription("очистити").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("finance")
        ui.onNode(hasSetTextAction()).performImeAction()
        ui.runOnIdle { assertEquals(listOf("health", "finance"), submitted) }
    }

    @Test fun `search button submits the same current query`() {
        showOmnibox()
        ui.onNode(hasSetTextAction()).performTextInput("health")
        ui.onNodeWithContentDescription("Виконати пошук").performClick()
        ui.runOnIdle { assertEquals(listOf("health"), submitted) }
    }
}
