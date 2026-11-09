package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeDashboard
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import androidx.compose.runtime.mutableStateOf
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.LifeManagementStatusPanelSection
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.LifeManagementLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import io.mockk.mockk
import io.mockk.every
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en")
class CommandDeckV2UiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun allActionDoesNotToggleSection() {
        var all = 0
        var toggles = 0
        ui.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DashboardSectionHeader("Recents", isExpanded = false, onClick = { toggles++ },
                    action = { TextButton(onClick = { all++ }) { Text("All") } })
            }
        }
        ui.onNodeWithText("All").performClick()
        assertEquals(1, all)
        assertEquals(0, toggles)
        ui.onNodeWithContentDescription("Expand").performClick()
        assertEquals(1, toggles)
    }

    @Test fun unsetModeIsReadableAndPickerKeepsModeIdentity() {
        var selected: SessionMode? = null
        ui.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SessionModeDashboard(SessionModeState(), null, { selected = it }, false, {}, {})
            }
        }
        ui.onNodeWithText("Not selected").assertIsDisplayed()
        ui.onNodeWithText("Choose").performClick()
        ui.onNodeWithText("EXECUTION").performClick()
        assertEquals(SessionMode.EXECUTION, selected)
    }

    @Test fun activeModeOffersChangeWithoutExpandingDetails() {
        var expands = 0
        ui.setContent {
            MaterialTheme {
                SessionModeDashboard(SessionModeState(SessionMode.CONTROL, 1L), "reason", {}, false, { expands++ }, {})
            }
        }
        ui.onNodeWithText("CONTROL").assertIsDisplayed()
        ui.onNodeWithText("reason").assertDoesNotExist()
        ui.onNodeWithText("Change").performClick()
        assertEquals(0, expands)
        ui.onNodeWithText("RECOVERY").assertIsDisplayed()
    }
    @Test fun levelStatusDistinguishesLoadingHealthyAndProblems() {
        val statuses = mutableStateOf(emptyList<LifeManagementLevelStatus>())
        fun level(status: GeneralStatus) = LifeManagementLevelStatus(
            LifeManagementLevelId.MAIN_BEACONS, status, TransferStatus.COMPLETE,
            FreshnessStatus.FRESH, "", "", 0L)
        ui.setContent {
            MaterialTheme {
                LifeManagementStatusPanelSection(statuses.value, false, {}, {})
            }
        }
        ui.onNodeWithText("Loading level status").assertIsDisplayed()
        ui.runOnIdle { statuses.value = listOf(level(GeneralStatus.READY)) }
        ui.onNodeWithText("All levels are healthy").assertIsDisplayed()
        ui.runOnIdle { statuses.value = listOf(level(GeneralStatus.BLOCKED)) }
        ui.onNodeWithText("Blocked: 1 · Defected: 0").assertIsDisplayed()
        ui.onNodeWithText("All levels are healthy").assertDoesNotExist()
    }

    @Test fun recentsHandlesEmptySingleAndManyAndPreservesClickedIdentity() {
        val items = MutableStateFlow(emptyList<RecentItem>())
        val recent = mockk<RecentViewModel>(relaxed = true) { every { recentItems } returns items }
        val deck = mockk<CommandDeckViewModel>(relaxed = true) {
            every { isCategoryExpanded(any(), any()) } returns true
        }
        var opened: RecentItem? = null
        ui.setContent { MaterialTheme { DashboardRecentsSection({ opened = it }, deck, recent) } }
        ui.onNodeWithText("No recent items yet").assertIsDisplayed()
        val first = RecentItem("first", RecentItemType.PROJECT, 100L, "First", "target-first")
        ui.runOnIdle { items.value = listOf(first) }
        ui.onNodeWithText("First").performClick()
        assertEquals(first, opened)
        ui.runOnIdle { items.value = listOf(first) + (2..8).map {
            RecentItem("id-$it", RecentItemType.NOTE_DOCUMENT, 100L - it, "Recent $it", "target-$it")
        } }
        ui.onNodeWithText("Recent 4").assertExists()
        ui.onNodeWithText("Recent 5").assertDoesNotExist()
        ui.onNodeWithText("All").assertExists()
    }

}
