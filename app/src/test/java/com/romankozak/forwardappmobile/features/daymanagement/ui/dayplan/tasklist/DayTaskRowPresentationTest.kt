package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayTheme
import org.junit.Test

class DayTaskRowPresentationTest {
    private fun task(id: String) = DayTaskWithReminder(DayTask(id = id, dayPlanId = "day", title = id), null)

    @Test fun `priority has accessible descriptions while unset priority has no visual indicator`() {
        assertThat(TaskPriority.NONE.dayTaskPriorityDescription()).isNull()
        val descriptions = TaskPriority.entries.filter { it != TaskPriority.NONE }.map { it.dayTaskPriorityDescription() }
        assertThat(descriptions).doesNotContain(null)
        assertThat(descriptions.distinct()).hasSize(4)
    }

    @Test fun `empty stale or inactive theme assignments do not create metadata rows`() {
        val theme = DayTheme(id = "theme", dayPlanId = "day", title = "Health")
        assertThat(hasVisibleTaskThemes(listOf(theme), emptySet())).isFalse()
        assertThat(hasVisibleTaskThemes(emptyList(), setOf("theme"))).isFalse()
        assertThat(hasVisibleTaskThemes(listOf(theme), setOf("missing"))).isFalse()
        assertThat(hasVisibleTaskThemes(listOf(theme.copy(isActive = false)), setOf("theme"))).isFalse()
        assertThat(hasVisibleTaskThemes(listOf(theme), setOf("theme"))).isTrue()
    }

    @Test fun `DnD resolves stable task keys independently of the optional theme summary header`() {
        val tasks = listOf(task("a"), task("b"), task("c"))
        assertThat(reorderDayTaskRows(tasks, "b", "c").map { it.dayTask.id }).containsExactly("a", "c", "b").inOrder()
        assertThat(reorderDayTaskRows(tasks, "c", "a").map { it.dayTask.id }).containsExactly("c", "a", "b").inOrder()
        assertThat(reorderDayTaskRows(tasks, "a", "c").map { it.dayTask.id }).containsExactly("b", "c", "a").inOrder()
    }

    @Test fun `header unknown and same-row drops neither reorder nor dispatch changes`() {
        val tasks = listOf(task("a"), task("b"))
        assertThat(reorderDayTaskRows(tasks, "task-theme-summary", "a")).isSameInstanceAs(tasks)
        assertThat(reorderDayTaskRows(tasks, "a", "task-theme-summary")).isSameInstanceAs(tasks)
        assertThat(reorderDayTaskRows(tasks, "missing", "b")).isSameInstanceAs(tasks)
        assertThat(reorderDayTaskRows(tasks, "a", "a")).isSameInstanceAs(tasks)
    }

    @Test fun `reorder preserves completion metadata and exact original task instances`() {
        val first = task("a").let { it.copy(dayTask = it.dayTask.copy(completed = true, description = "Notes #health", points = 5,
            linkedProjectIds = listOf("workspace"), recurrenceSeriesId = "series", priority = TaskPriority.HIGH)) }
        val second = task("b")
        val result = reorderDayTaskRows(listOf(first, second), "a", "b")
        assertThat(result[0]).isSameInstanceAs(second)
        assertThat(result[1]).isSameInstanceAs(first)
        assertThat(result[1].dayTask.completed).isTrue()
        assertThat(result[1].dayTask.description).isEqualTo("Notes #health")
    }
}
