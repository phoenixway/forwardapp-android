package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.LifeManagementStatusPanelViewModel
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en-w360dp-h800dp")
class CommandDeckAccordionUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun openingASectionCollapsesPreviousContentAndRestoresAfterRecreation() {
        val recent = mockk<RecentViewModel>(relaxed = true) {
            every { recentItems } returns MutableStateFlow(listOf(
                RecentItem("one", RecentItemType.PROJECT, 1L, "Recent object", "target")))
        }
        val levels = mockk<LifeManagementStatusPanelViewModel>(relaxed = true) {
            every { statuses } returns MutableStateFlow(emptyList())
        }
        val deck = mockk<CommandDeckViewModel>(relaxed = true) {
            every { isCategoryExpanded(any(), any()) } answers { secondArg<Boolean>() }
        }
        var selected: SessionMode? = null
        val restoration = StateRestorationTester(ui)
        restoration.setContent {
            MaterialTheme {
                CommandDeckAccordion(
                    session = SessionModeState(), latestSessionReason = null,
                    onModeSelected = { selected = it }, onOpenModeContext = {},
                    commandDeckViewModel = deck, onRecentItemClick = {},
                    insightsContent = { Text("Insight body") },
                    levelsViewModel = levels, recentViewModel = recent,
                )
            }
        }
        ui.onNodeWithText("Recent object").assertIsDisplayed()
        ui.onNodeWithText("Session mode").performClick()
        ui.onNodeWithText("Recent object").assertDoesNotExist()
        ui.onNodeWithText("EXECUTION").performClick()
        assertEquals(SessionMode.EXECUTION, selected)
        ui.onNodeWithText("AI Insights").performClick()
        ui.onNodeWithText("EXECUTION").assertDoesNotExist()
        ui.onNodeWithText("Insight body").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Insight body").assertIsDisplayed()
        ui.onNodeWithText("Recents").performClick()
        ui.onNodeWithText("Insight body").assertDoesNotExist()
        ui.onNodeWithText("Recent object").assertIsDisplayed()
        ui.onNodeWithText("Recents").performClick()
        ui.onNodeWithText("Recent object").assertDoesNotExist()
    }
}
