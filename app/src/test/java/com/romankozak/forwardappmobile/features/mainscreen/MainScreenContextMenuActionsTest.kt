package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.*
import org.junit.Assert.*
import org.junit.Test

class MainScreenContextMenuActionsTest {
    @Test fun `Arc menu commands retain distinct callbacks without month navigation`() {
        val events = mutableListOf<String>()
        val actions = strategicArcMenuActions(true,
            { events += "beacon" },
            { events += "group" }, { events += "artifact" }, { events += "links" })
        actions.forEach { it.invokeIfEnabled() }
        assertEquals(listOf("beacon", "group", "artifact", "links"), events)
    }

    @Test fun `Arc loading error disables menu commands`() {
        var count = 0
        strategicArcMenuActions(false, { count++ }, { count++ }, { count++ }, { count++ })
            .forEach { assertFalse(it.enabled); it.invokeIfEnabled() }
        assertEquals(0, count)
        val actions = strategicArcMenuActions(true, {}, {}, {}, {})
        assertTrue(actions.all { it.enabled })
    }

    @Test fun `unified Arc menu contains source exactly once alongside all former FAB actions`() {
        val actions = strategicComposerActions(StrategicArcTab.QUESTS, false, {}, {}, {}) +
            strategicArcMenuActions(true, {}, {}, {}, {})
        assertEquals(8, actions.size)
        assertEquals(8, actions.map { it.id }.toSet().size)
        assertEquals(1, actions.count { it.id == "quest-source" })
    }

    @Test fun `calendar exists in every Today mode and Day Start retains wake and sleep`() {
        DayManagementTab.todaySubTabs().forEach { tab ->
            val events = mutableListOf<String>()
            val actions = todayDateActions(tab, { events += "calendar" }, { events += "wake" }, { events += "sleep" })
            assertTrue(events.isEmpty())
            assertEquals("Вибрати день", actions.first().label)
            actions.forEach { it.onClick() }
            assertEquals(if (tab == DayManagementTab.DAY_START) listOf("calendar", "wake", "sleep") else listOf("calendar"), events)
        }
    }
}
