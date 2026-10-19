package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import org.junit.Test

class TodayTaskSourceActionTest {
    @Test fun `Day Start and Day Plan expose explicit entity-based task action`() {
        for (tab in listOf(DayManagementTab.DAY_START, DayManagementTab.DAY_PLAN)) {
            var pickerOpen = false
            val action = requireNotNull(todayTaskSourceAction(tab) { pickerOpen = true })
            assertThat(action.label).isEqualTo("Створити завдання на основі сутності")
            assertThat(pickerOpen).isFalse()
            action.onClick()
            assertThat(pickerOpen).isTrue()
        }
    }

    @Test fun `focus themes journal and summary composers retain their own actions`() {
        DayManagementTab.entries.filterNot { it == DayManagementTab.DAY_START || it == DayManagementTab.DAY_PLAN }
            .forEach { tab -> assertThat(todayTaskSourceAction(tab) { error("Unexpected task action") }).isNull() }
    }
}
