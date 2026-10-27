package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.*
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode
import org.junit.Assert.*
import org.junit.Test

class ContextualComposerActionsTest {
    @Test fun `Today exposes all six modes and preserves exact selected state and dispatch`() {
        DayManagementTab.todaySubTabs().forEach { current ->
            var selected: DayManagementTab? = null
            val actions = todayContextualActions(current, emptyList(), DayManagementRuntimeUiState(), { selected = it }, {}, {})
            val modes = actions.filter { it.id.startsWith("today-mode-") }
            assertEquals(6, modes.size)
            assertEquals(listOf(current.title), modes.filter { it.selected == true }.map { it.label })
            modes.forEachIndexed { index, action -> action.invokeIfEnabled(); assertEquals(DayManagementTab.todaySubTabs()[index], selected) }
        }
    }

    @Test fun `Today additional actions retain order labels and exact callbacks`() {
        val events = mutableListOf<String>()
        val actions = todayContextualActions(DayManagementTab.DAY_PLAN,
            listOf(MoreSheetAction("Source") { events += "source" }, MoreSheetAction("Plan ready") { events += "ready" }),
            DayManagementRuntimeUiState(), {}, {}, {})
        assertEquals(listOf("Source", "Plan ready"), actions.take(2).map { it.label })
        actions.take(2).forEach { it.invokeIfEnabled() }
        assertEquals(listOf("source", "ready"), events)
    }

    @Test fun `Finalization remains available only in its subtab and calls original command`() {
        var count = 0
        val actions = todayContextualActions(DayManagementTab.FINALIZATION, emptyList(), DayManagementRuntimeUiState(), {}, { count++ }, {})
        actions.single { it.id == "finalize" }.invokeIfEnabled()
        assertEquals(1, count)
        assertFalse(todayContextualActions(DayManagementTab.DAY_PLAN, emptyList(), DayManagementRuntimeUiState(), {}, {}, {}).any { it.id == "finalize" })
    }

    @Test fun `Strategic source clear and presentation actions remain distinct`() {
        val events = mutableListOf<String>()
        val actions = strategicComposerActions(StrategicArcTab.QUESTS, true, { events += it.name }, { events += "source" }, { events += "clear" })
        assertEquals(listOf("quest-source", "quest-clear", "quests", "artifact"), actions.map { it.id })
        assertEquals(listOf("quests"), actions.filter { it.selected == true }.map { it.id })
        actions.forEach { it.invokeIfEnabled() }
        assertEquals(listOf("source", "clear", "QUESTS", "ARTIFACT"), events)
    }

    @Test fun `Strategic empty draft cannot dispatch clear`() {
        var cleared = false
        val actions = strategicComposerActions(StrategicArcTab.ARTIFACT, false, {}, {}, { cleared = true })
        assertEquals(listOf("artifact"), actions.filter { it.selected == true }.map { it.id })
        actions.single { it.id == "quest-clear" }.invokeIfEnabled()
        assertFalse(cleared)
    }

    @Test fun `Tactics has source links paste streams and every original mode`() {
        val events = mutableListOf<String>()
        val actions = tacticsComposerActions(TacticsWorkspaceMode.PLAN, true, false,
            { events += it.name }, { events += "streams" }, { events += "paste" }, { events += "source" }, { events += "links" })
        assertEquals(7, actions.size)
        assertEquals(listOf("tactics-mode-PLAN"), actions.filter { it.selected == true }.map { it.id })
        actions.forEach { it.invokeIfEnabled() }
        assertEquals(setOf("source", "links", "paste", "streams", "STREAMS", "ALL", "PLAN"), events.toSet())
    }

    @Test fun `Tactics paste preserves disabled state`() {
        var pasted = false
        tacticsComposerActions(TacticsWorkspaceMode.ALL, false, false, {}, {}, { pasted = true }, {}, {})
            .single { it.id == "paste" }.invokeIfEnabled()
        assertFalse(pasted)
    }

    @Test fun `Execution keeps only admitted stream and mode commands`() {
        val actions = tacticsComposerActions(TacticsWorkspaceMode.STREAMS, true, true, {}, {}, {}, {}, {})
        assertEquals(setOf("streams-settings", "tactics-mode-STREAMS", "tactics-mode-ALL"), actions.map { it.id }.toSet())
        assertEquals(listOf("tactics-mode-STREAMS"), actions.filter { it.selected == true }.map { it.id })
    }
}
