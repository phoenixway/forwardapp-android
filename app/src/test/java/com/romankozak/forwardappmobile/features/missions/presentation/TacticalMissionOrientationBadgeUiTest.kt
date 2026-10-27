package com.romankozak.forwardappmobile.features.missions.presentation

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.PlanningOrientationSourceBadge
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TacticalMissionOrientationBadgeUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `RELATED badge has distinct label and consumes its own click`() {
        var opened = 0
        var parent = 0
        ui.setContent { MaterialTheme {
            Box(Modifier.clickable { parent++ }) {
                PlanningOrientationSourceBadge(CanonicalEntitySummary(CanonicalEntityRef.Orientation("related"), "Related health"), related = true) { opened++ }
            }
        } }
        ui.onNodeWithText("Пов’язано · Related health").assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals(1, opened); assertEquals(0, parent) }
    }

    @Test fun `source title is a clickable badge and does not toggle its parent Mission`() {
        var opened = 0
        var toggled = 0
        ui.setContent { MaterialTheme {
            Box(Modifier.clickable { toggled++ }) {
                TacticalMissionOrientationBadge(CanonicalEntitySummary(CanonicalEntityRef.Orientation("exact"), "Health")) { opened++ }
            }
        } }
        ui.onNodeWithText("На основі · Health").assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals(1, opened); assertEquals(0, toggled) }
    }
}
