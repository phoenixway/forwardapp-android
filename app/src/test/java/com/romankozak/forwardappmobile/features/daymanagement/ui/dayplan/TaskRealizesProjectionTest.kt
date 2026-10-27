package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.*
import org.junit.Test

class TaskRealizesProjectionTest {
    private val task = DayTask(id = "t", dayPlanId = "future-day", title = "Task", realizedDayFocusItemIds = listOf("focus"))
    private val focus = DayFocusItem("focus", "future-day", "Focus")
    private val theme = DayTheme("theme", "future-day", "Theme")
    @Test fun `uses task day not system today and keeps references independent`() {
        val state = projectTaskRealizes(task, listOf(focus, focus.copy(id = "other", dayPlanId = "today")), DayThemeDocument(listOf(theme)))
        assertThat(state.priorities).containsExactly(focus)
        assertThat(state.selectedThemeIds).isEmpty()
        assertThat(state.suggestedThemeIds).isEmpty()
    }
    @Test fun `priority theme is suggestion not automatic membership`() {
        val state = projectTaskRealizes(task, listOf(focus), DayThemeDocument(listOf(theme), listOf(DayThemeAssignment("future-day", "focus", setOf("theme")))))
        assertThat(state.selectedThemeIds).isEmpty()
        assertThat(state.suggestedThemeIds).containsExactly("theme")
        assertThat(task.realizedDayFocusItemIds).containsExactly("focus")
    }
    @Test fun `selected themes are direct task assignment and not suggested twice`() {
        val doc = DayThemeDocument(listOf(theme), listOf(DayThemeAssignment("future-day", "t", setOf("theme")), DayThemeAssignment("future-day", "focus", setOf("theme"))))
        val state = projectTaskRealizes(task, listOf(focus), doc)
        assertThat(state.selectedThemeIds).containsExactly("theme"); assertThat(state.suggestedThemeIds).isEmpty()
    }
    @Test fun `deleted priority and inactive theme do not create suggestions`() {
        val state = projectTaskRealizes(task, listOf(focus.copy(isDeleted = true)), DayThemeDocument(listOf(theme.copy(isActive = false)), listOf(DayThemeAssignment("future-day", "focus", setOf("theme")))))
        assertThat(state.priorities).isEmpty(); assertThat(state.themes).isEmpty(); assertThat(state.suggestedThemeIds).isEmpty()
    }
    @Test fun `focus and responsibility labels distinct and empty task links valid`() {
        assertThat(DayFocusType.FOCUS.priorityLabel()).isEqualTo("Фокус")
        assertThat(DayFocusType.RESPONSIBILITY.priorityLabel()).isEqualTo("Відповідальність")
        assertThat(projectTaskRealizes(task.copy(realizedDayFocusItemIds = emptyList()), emptyList(), DayThemeDocument()).priorities).isEmpty()
    }
}
