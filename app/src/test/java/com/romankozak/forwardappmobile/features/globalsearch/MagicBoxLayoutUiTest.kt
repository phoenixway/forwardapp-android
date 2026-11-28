package com.romankozak.forwardappmobile.features.globalsearch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MagicBoxLayoutUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `limited height keeps full size mode buttons and search filters clear of the bottom bar`() {
        ui.setContent {
            MaterialTheme {
                var query by remember { mutableStateOf("") }
                val mode = OmniboxMode.DataSearch
                val palette = rememberModePalette(mode)
                Box(Modifier.fillMaxWidth().height(360.dp)) {
                    MagicBoxLayout(
                        palette = palette,
                        content = {
                            if (query.isBlank()) {
                                SearchHistoryHintStrip(
                                    history = List(12) { "Long recent search query ${it + 1}" },
                                    containerColor = palette.secondaryActionContainer,
                                    onHistoryClick = { query = it },
                                )
                            } else {
                                MagicBoxResultsSurface(palette) {
                                    MagicBoxCanonicalResultContent(
                                        input = query,
                                        state = MagicBoxCanonicalState(
                                            input = query,
                                            results = listOf(
                                                CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect"), "Future plans"),
                                            ),
                                        ),
                                        attachmentState = MagicBoxAttachmentState(input = query),
                                        attachmentsAvailable = true,
                                        onLegacyResults = {},
                                        onSelectSaved = {},
                                        onEntity = {},
                                        onAttachment = {},
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            MagicBoxActionStrip(palette, false, 0, null, {}, {})
                            CompactOmnibox(
                                uiState = GlobalSearchUiState(query = query),
                                currentMode = mode,
                                modePalette = palette,
                                onShowCreate = {},
                                onShowRecents = {},
                                recentsSelected = false,
                                onSetMode = {},
                                onQueryChange = { query = it },
                                onClearQuery = { query = "" },
                                onSubmit = {},
                                onKeyDown = { false },
                                focusRequester = remember { FocusRequester() },
                            )
                        },
                    )
                }
            }
        }

        val modeButton = ui.onNodeWithContentDescription("Пошук даних")
        modeButton.assertIsDisplayed().assertHeightIsEqualTo(48.dp)
        ui.onNodeWithText("Останні запити").assertIsDisplayed()

        ui.onNode(hasSetTextAction()).performTextInput("future")
        modeButton.assertIsDisplayed().assertHeightIsEqualTo(48.dp)
        val filter = ui.onNodeWithText("Усі типи").assertIsDisplayed()
        val bottomBar = ui.onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        assertThat(filter.fetchSemanticsNode().boundsInRoot.bottom).isLessThan(bottomBar.top)
        filter.performClick()
        ui.onNodeWithText("Аспекти").performClick()
        ui.onNodeWithText("Аспекти").assertIsDisplayed()
    }
}
