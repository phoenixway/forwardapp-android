package com.romankozak.forwardappmobile.features.daymanagement.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TodayStageHeaderUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `menu and swipe change selection without invoking Ready`() {
        val selections = mutableListOf<DayManagementTab>()
        val ready = mutableListOf<DayManagementTab>()
        ui.setContent {
            var selected by remember { mutableStateOf(DayManagementTab.DAY_START) }
            MaterialTheme {
                TodayStageHeader(
                    selectedTab = selected,
                    runtimeState = DayManagementRuntimeState(),
                    onSelectTab = { selected = it; selections += it },
                    onReady = { ready += it },
                )
            }
        }

        ui.onNodeWithContentDescription("Вибрати стадію дня").performClick()
        ui.onNodeWithText("Themes").performClick()
        ui.onNodeWithTag("today-stage-controller").performTouchInput { swipeLeft() }
        ui.onNodeWithText("Day priorities").assertExists()
        ui.runOnIdle {
            assertEquals(listOf(DayManagementTab.DAY_THEMES, DayManagementTab.DAY_FOCUSES), selections)
            assertEquals(emptyList<DayManagementTab>(), ready)
        }

        ui.onNodeWithText("Ready").performClick()
        ui.runOnIdle { assertEquals(listOf(DayManagementTab.DAY_FOCUSES), ready) }
    }
}
