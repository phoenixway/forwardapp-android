package com.romankozak.forwardappmobile.features.daymanagement.ui

import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayStageHeaderTest {
    private val stages = DayManagementTab.todaySubTabs()

    @Test fun `six indicators use the requested order and swipes move one stage only`() {
        assertEquals(
            listOf("Day Start", "Themes", "Day priorities", "Plan", "Journal", "Finalization"),
            stages.map(DayManagementTab::stageTitle),
        )
        assertEquals(6, stages.size)
        assertEquals(DayManagementTab.DAY_FOCUSES, stageAfterSwipe(DayManagementTab.DAY_THEMES, -40f, 28f))
        assertEquals(DayManagementTab.DAY_START, stageAfterSwipe(DayManagementTab.DAY_THEMES, 40f, 28f))
        assertNull(stageAfterSwipe(DayManagementTab.DAY_THEMES, -20f, 28f))
        assertNull(stageAfterSwipe(stages.first(), 40f, 28f))
        assertNull(stageAfterSwipe(stages.last(), -40f, 28f))
        assertEquals(DayManagementTab.DAY_THEMES, stages.first().nextTodayStage())
        assertNull(stages.last().nextTodayStage())
        assertNull(DayManagementTab.DASHBOARD.nextTodayStage())
    }

    @Test fun `selected stage is independent from persisted readiness`() {
        val state = DayManagementRuntimeState(
            wokeAt = 1L,
            dayThemesFinalizedAt = 2L,
            dayFocusFinalizedAt = 3L,
            dayPlanFinalizedAt = 4L,
            implementationStartedAt = 5L,
        )
        assertEquals(listOf(true, true, true, true, false, false), stages.map { it.isReady(state) })
        assertEquals(DayManagementTab.JOURNAL, stageAfterSwipe(DayManagementTab.DAY_PLAN, -40f, 28f))
        assertFalse(DayManagementTab.JOURNAL.isReady(state))
        assertEquals(TodayStageReadyAction.START_FINALIZATION, DayManagementTab.JOURNAL.readyAction(state))
        assertEquals(TodayStageReadyAction.SLEEP,
            DayManagementTab.FINALIZATION.readyAction(state.copy(finalizationStartedAt = 6L)))
        assertTrue(DayManagementTab.FINALIZATION.isReady(state.copy(sleepAt = 7L)))
    }

    @Test fun `Ready commands are stage specific and completed stages do not repeat commands`() {
        val empty = DayManagementRuntimeState()
        assertEquals(
            listOf(
                TodayStageReadyAction.WAKE_UP,
                TodayStageReadyAction.FINALIZE_THEMES,
                TodayStageReadyAction.FINALIZE_FOCUS,
                TodayStageReadyAction.FINALIZE_PLAN,
                TodayStageReadyAction.START_FINALIZATION,
                null,
            ),
            stages.map { it.readyAction(empty) },
        )
        val open = empty.copy(wokeAt = 1L)
        assertNull(DayManagementTab.DAY_START.readyAction(open))
        assertNull(DayManagementTab.DAY_THEMES.readyAction(open.copy(dayThemesFinalizedAt = 2L)))
        assertEquals(TodayStageReadyAction.WAKE_UP, DayManagementTab.DAY_START.readyAction(open.copy(sleepAt = 3L)))
    }
}
