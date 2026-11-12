package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.*
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualActionsButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StrategicArcToolsSheetTest {
    @get:Rule val ui = createComposeRule()

    private val ids = listOf("quest-source", "quest-clear", "quests", "artifact", "arc-overview",
        "arc-all-participants", "arc-beacon", "arc-group", "arc-open-artifact", "arc-links")

    @Test fun `every existing action survives grouping exactly once with original flags and handlers`() {
        val events = mutableListOf<String>()
        val actions = ids.mapIndexed { index, id ->
            ContextualAction(id, id, selected = index % 2 == 0, enabled = index != 1) { events += id }
        }
        val grouped = groupArcTools(actions)
        assertEquals(actions.size, grouped.values.flatten().size)
        actions.forEach { original -> assertSame(original, grouped.values.flatten().single { it.id == original.id }) }
        grouped.values.flatten().forEach { it.invokeIfEnabled() }
        assertEquals(ids.filterNot { it == "quest-clear" }.toSet(), events.toSet())
        assertEquals(listOf("arc-overview", "arc-all-participants"), grouped.getValue(ArcToolGroup.CONTENT).map { it.id })
    }

    @Test fun `unknown action stays accessible instead of silently disappearing`() {
        var invoked = false
        val action = ContextualAction("future", "Додаткова дія") { invoked = true }
        ui.setContent { MaterialTheme { StrategicArcToolsSheet(listOf(action)) { it.invokeIfEnabled() } } }
        ui.onNodeWithText("Інші дії").assertExists()
        ui.onNodeWithText(action.label).performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.runOnIdle { assertTrue(invoked) }
    }

    @Test fun `selected view is explicit and disabled actions remain disabled`() {
        val actions = ids.map { id -> ContextualAction(id, id, selected = id == "quests" || id == "arc-overview",
            enabled = id != "quest-clear") {} }
        ui.setContent { MaterialTheme { StrategicArcToolsSheet(actions) { it.invokeIfEnabled() } } }
        listOf("Перегляд", "Склад списку", "Додати до арки", "Інструменти арки")
            .forEach { ui.onNodeWithText(it).assertExists() }
        ui.onNodeWithText("Період").assertDoesNotExist()
        ui.onNodeWithText("quests").assertIsSelected()
        ui.onNodeWithText("artifact").assertIsNotSelected()
        ui.onNodeWithText("Квести / фокуси").assertIsSelected()
        ui.onNodeWithText("Очистити чернетку").assertIsNotEnabled()
    }

    @Test fun `clear draft invokes only existing clear action and closes sheet`() {
        var cleared = 0
        val actions = listOf(ContextualAction("quest-clear", "Очистити ArcQuest") { cleared++ })
        ui.setContent { MaterialTheme {
            ContextualActionsButton("Арка", actions, sheetContent = { selected -> StrategicArcToolsSheet(actions, selected) })
        } }
        ui.onNodeWithContentDescription("Дії: Арка").performClick()
        ui.onNodeWithText("Очищення прибирає лише текст із поля введення, не збережені квести.").assertExists()
        ui.onNodeWithText("Очистити чернетку").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.onNodeWithText("Очистити чернетку").assertDoesNotExist()
        ui.runOnIdle { assertEquals(1, cleared) }
    }

    @Test fun `creation and tools dispatch distinct original commands`() {
        val events = mutableListOf<String>()
        val ids = listOf("quest-source", "arc-beacon", "arc-group", "arc-open-artifact", "arc-links")
        val actions = ids.map { id -> ContextualAction(id, id) { events += id } }
        ui.setContent { MaterialTheme { StrategicArcToolsSheet(actions) { it.invokeIfEnabled() } } }
        ids.forEach { id -> ui.onNodeWithText(id).performSemanticsAction(SemanticsActions.OnClick) { it() } }
        ui.runOnIdle { assertEquals(ids, events) }
    }

    @Test fun `empty groups are omitted`() {
        ui.setContent { MaterialTheme { StrategicArcToolsSheet(emptyList()) {} } }
        listOf("Перегляд", "Склад списку", "Період", "Додати до арки", "Інструменти арки", "Інші дії")
            .forEach { ui.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test fun `month header exposes navigation and disables unavailable previous arc`() {
        val events = mutableListOf<String>()
        ui.setContent { MaterialTheme {
            StrategicArcMonthHeader("2026-10", false, { events += "previous" }, { events += "next" })
        } }
        ui.onNodeWithText("Жовтень 2026").assertExists()
        ui.onNodeWithContentDescription("Попередня арка").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Наступна арка").performClick()
        ui.runOnIdle { assertEquals(listOf("next"), events) }
    }
}
