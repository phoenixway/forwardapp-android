package com.romankozak.forwardappmobile.features.missions.presentation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TacticalIterationStatusPanelUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `status panel fills available width and stays compact without explanatory paragraph`() {
        val iteration = mockk<TacticalIteration> { every { status } returns TacticalIterationStatus.DRAFT }
        ui.setContent { MaterialTheme {
            Box(Modifier.fillMaxWidth().testTag("parent")) {
                TacticalIterationStatusPanel(iteration, 7, {}, {}, {}, {}, {}, {}, {})
            }
        } }
        val parent = ui.onNodeWithTag("parent").fetchSemanticsNode().boundsInRoot.width
        val panel = ui.onNodeWithTag("tactical-iteration-status").fetchSemanticsNode().boundsInRoot.width
        assertTrue(panel / parent in 0.85f..1f)
        // Allow host font metrics without forcing a fixed height that clips larger text.
        val bounds = ui.onNodeWithTag("tactical-iteration-status").getUnclippedBoundsInRoot()
        val height = bounds.bottom.value - bounds.top.value
        assertTrue("Status panel height: $height dp", height <= 84f)
        ui.onNodeWithText("Ітерація в розробці").assertIsDisplayed()
        ui.onAllNodes(hasText("Планування:", substring = true)).assertCountEquals(0)
        ui.onNodeWithContentDescription("Дії ітерації").assertIsDisplayed()
    }

    @Test fun `compact panel retains cycle creation and history handlers`() {
        var draft = 0
        var history = 0
        ui.setContent { MaterialTheme {
            TacticalIterationStatusPanel(null, null, { history++ }, {}, { draft++ }, {}, {}, {}, {})
        } }
        ui.onNodeWithContentDescription("Дії ітерації").performClick()
        ui.onNodeWithText("Draft new cycle").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.onNodeWithContentDescription("Дії ітерації").performClick()
        ui.onNodeWithText("Cycle history").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.runOnIdle { assertEquals(1, draft); assertEquals(1, history) }
    }
}
