package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.*
import org.junit.Assert.*
import org.junit.Test

class MainScreenContextMenuActionsTest {
    @Test fun `all former Arc FAB commands retain distinct callbacks`() {
        val events = mutableListOf<String>()
        val actions = strategicArcMenuActions(true, true,
            { events += "previous" }, { events += "next" }, { events += "beacon" },
            { events += "group" }, { events += "artifact" }, { events += "links" })
        actions.forEach { it.invokeIfEnabled() }
        assertEquals(listOf("previous", "next", "beacon", "group", "artifact", "links"), events)
    }

    @Test fun `Arc loading error and absent previous month fail closed`() {
        var count = 0
        strategicArcMenuActions(false, true, { count++ }, { count++ }, { count++ }, { count++ }, { count++ }, { count++ })
            .forEach { assertFalse(it.enabled); it.invokeIfEnabled() }
        assertEquals(0, count)
        val actions = strategicArcMenuActions(true, false, {}, {}, {}, {}, {}, {})
        assertFalse(actions.single { it.id == "arc-previous" }.enabled)
        assertTrue(actions.filterNot { it.id == "arc-previous" }.all { it.enabled })
    }

    @Test fun `unified Arc menu contains source exactly once alongside all former FAB actions`() {
        val actions = strategicComposerActions(StrategicArcTab.QUESTS, false, {}, {}, {}) +
            strategicArcMenuActions(true, true, {}, {}, {}, {}, {}, {})
        assertEquals(10, actions.size)
        assertEquals(10, actions.map { it.id }.toSet().size)
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
