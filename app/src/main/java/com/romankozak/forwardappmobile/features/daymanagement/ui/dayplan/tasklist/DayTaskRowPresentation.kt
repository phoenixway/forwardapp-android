package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayTheme

/** Accessibility text only; the visual priority is a colored dot. */
internal fun TaskPriority.dayTaskPriorityDescription(): String? = when (this) {
    TaskPriority.CRITICAL -> "Пріоритет: критичний"
    TaskPriority.HIGH -> "Пріоритет: високий"
    TaskPriority.MEDIUM -> "Пріоритет: середній"
    TaskPriority.LOW -> "Пріоритет: низький"
    TaskPriority.NONE -> null
}

internal fun hasVisibleTaskThemes(themes: List<DayTheme>, selected: Set<String>): Boolean =
    themes.any { it.isActive && it.id in selected }

/** LazyColumn may also contain a theme summary; never interpret its indices as task indices. */
internal fun reorderDayTaskRows(tasks: List<DayTaskWithReminder>, fromKey: Any?, toKey: Any?): List<DayTaskWithReminder> {
    val from = tasks.indexOfFirst { it.dayTask.id == fromKey }
    val to = tasks.indexOfFirst { it.dayTask.id == toKey }
    if (from < 0 || to < 0 || from == to) return tasks
    return tasks.toMutableList().apply { add(to, removeAt(from)) }
}
