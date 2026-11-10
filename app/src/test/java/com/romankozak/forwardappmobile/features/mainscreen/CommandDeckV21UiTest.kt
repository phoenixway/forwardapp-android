package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.LifeManagementLevelStatus
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
class CommandDeckV21UiTest {
    @get:Rule val ui = createComposeRule()

    private fun level(status: GeneralStatus) = LifeManagementLevelStatus(
        LifeManagementLevelId.MAIN_BEACONS, status, TransferStatus.COMPLETE,
        FreshnessStatus.FRESH, "", "", 0L)

    @Test fun combinedCardOpensPickerAndDetailsAndKeepsBothRowsInBothThemes() {
        val dark = mutableStateOf(false)
        val session = mutableStateOf(SessionModeState())
        val levels = mutableStateOf(listOf(level(GeneralStatus.READY)))
        var openedLevels = 0
        var openedInsights = 0
        ui.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                CommandDeckStatusCardContent(session.value, levels.value,
                    { session.value = SessionModeState(it) }, { openedLevels++ }, { openedInsights++ })
            }
        }
        ui.onNodeWithText("Unset").assertIsDisplayed()
        ui.onNodeWithText("OK").assertIsDisplayed()
        ui.onNodeWithTag("deck-session-row").assertHeightIsEqualTo(50.dp).performClick()
        ui.onNodeWithText("EXECUTION").performClick()
        assertEquals(SessionMode.EXECUTION, session.value.mode)
        ui.onNodeWithTag("deck-levels-row").performClick()
        assertEquals(1, openedLevels)
        ui.onNodeWithTag("deck-insights-row").assertHeightIsEqualTo(50.dp).performClick()
        assertEquals(1, openedInsights)
        ui.runOnIdle { dark.value = true; levels.value = listOf(level(GeneralStatus.BLOCKED)) }
        ui.onNodeWithText("Blocked: 1 · Defected: 0").assertIsDisplayed()
        ui.onNodeWithTag("deck-session-row").assertExists()
        ui.onNodeWithTag("deck-levels-row").assertExists()
        ui.runOnIdle { levels.value = listOf(level(GeneralStatus.DEFECTED)) }
        ui.onNodeWithText("Blocked: 0 · Defected: 1").assertIsDisplayed()
    }

    @Test fun fourRecentsAre48dpAndKeepClickIdentityAndAllOpensExistingSheet() {
        val items = MutableStateFlow((1..5).map {
            RecentItem("id-$it", RecentItemType.PROJECT, 100L - it, "Recent $it", "target-$it")
        })
        val recent = mockk<RecentViewModel>(relaxed = true) { every { recentItems } returns items }
        val deck = mockk<CommandDeckViewModel>(relaxed = true) {
            every { isCategoryExpanded(any(), any()) } returns true
        }
        var opened: RecentItem? = null
        ui.setContent { MaterialTheme { DashboardRecentsSection({ opened = it }, deck, recent) } }
        (1..4).forEach { ui.onNodeWithTag("deck-recent-id-$it").assertHeightIsEqualTo(48.dp) }
        ui.onNodeWithTag("deck-recent-id-5").assertDoesNotExist()
        ui.onNodeWithTag("deck-recent-id-1").performClick()
        assertEquals(items.value.first(), opened)
        ui.onNodeWithText("All").performClick()
        // Existing full-sheet tabs confirm that All opens the shared implementation.
        ui.onNodeWithText("Недавні").assertExists()
    }

    @Test fun emptyRecentsCanCollapseAndExpand() {
        val recent = mockk<RecentViewModel>(relaxed = true) {
            every { recentItems } returns MutableStateFlow(emptyList())
        }
        val deck = mockk<CommandDeckViewModel>(relaxed = true) {
            every { isCategoryExpanded(any(), any()) } returns true
        }
        ui.setContent { MaterialTheme { DashboardRecentsSection({}, deck, recent) } }
        ui.onNodeWithText("No recent items yet").assertIsDisplayed()
        ui.onNodeWithContentDescription("Collapse").performClick()
        ui.onNodeWithText("No recent items yet").assertDoesNotExist()
        ui.onNodeWithContentDescription("Expand").performClick()
        ui.onNodeWithText("No recent items yet").assertIsDisplayed()
    }
    @Test fun recentRowsCanGrowForLargeSystemFont() {
        val recent = mockk<RecentViewModel>(relaxed = true) {
            every { recentItems } returns MutableStateFlow(listOf(
                RecentItem("large", RecentItemType.PROJECT, 1L, "Large text", "target")))
        }
        val deck = mockk<CommandDeckViewModel>(relaxed = true) {
            every { isCategoryExpanded(any(), any()) } returns true
        }
        ui.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                MaterialTheme { DashboardRecentsSection({}, deck, recent) }
            }
        }
        ui.onNodeWithTag("deck-recent-large").assertHeightIsAtLeast(48.dp)
        ui.onNodeWithText("Large text").assertIsDisplayed()
    }

}
