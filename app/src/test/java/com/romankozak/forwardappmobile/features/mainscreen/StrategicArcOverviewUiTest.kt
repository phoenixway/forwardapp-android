package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategicArcBottomPanelContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StrategicArcOverviewUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `mode is available only inside contextual menu and selection updates both choices`() {
        ui.setContent { MaterialTheme {
            var all by remember { mutableStateOf(false) }
            StrategicArcBottomPanelContent(StrategicArcTab.QUESTS, TextFieldValue(""), {}, {}, {}, {}, {},
                showAllParticipants = all, onShowAllParticipantsChange = { all = it })
        } }
        ui.onNodeWithText("Усі учасники").assertDoesNotExist()
        fun open() { ui.onNodeWithContentDescription("Дії: Стратегічна арка · Квести").performClick() }
        fun select(label: String) { ui.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick) { it() } }
        open()
        ui.onNodeWithText("Квести / фокуси").assertIsSelected()
        select("Усі учасники")
        ui.onNodeWithText("Усі учасники").assertDoesNotExist()
        open()
        ui.onNodeWithText("Усі учасники").assertIsSelected()
        ui.onNodeWithText("Квести / фокуси").assertIsNotSelected()
        select("Квести / фокуси")
        open()
        ui.onNodeWithText("Квести / фокуси").assertIsSelected()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `shared list menu state restores from SavedStateHandle and does not leak to another Arc`() = runTest {
        val handle = SavedStateHandle()
        val arc = MutableStateFlow("2026-10")
        val original = StrategicArcOverviewSelection(handle, arc, backgroundScope)
        runCurrent()
        assertFalse(original.showAll.value)
        original.select(true); runCurrent()
        assertTrue(original.showAll.value)
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = StrategicArcOverviewSelection(restoredHandle, arc, backgroundScope)
        runCurrent()
        assertTrue(restored.showAll.value)
        arc.value = "2026-11"; runCurrent()
        assertFalse(restored.showAll.value)
    }

    @Test fun `artifact menu does not expose quest inventory mode`() {
        ui.setContent { MaterialTheme {
            StrategicArcBottomPanelContent(StrategicArcTab.ARTIFACT, TextFieldValue(""), {}, {}, {}, {}, {},
                showAllParticipants = true, onShowAllParticipantsChange = {})
        } }
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Артефакт").performClick()
        ui.onNodeWithText("Усі учасники").assertDoesNotExist()
        ui.onNodeWithText("Квести / фокуси").assertDoesNotExist()
        ui.onNodeWithText("Квести арки").assertExists()
    }
}
