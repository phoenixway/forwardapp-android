package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority

internal enum class EditTaskTab(val title: String) {
    Main("Основне"), Details("Деталі"), Repeat("Повтор"), Notes("Нотатки"), Realizes("Реалізує"),
}

/** Content-sized labels: narrow screens and large fonts scroll instead of splitting words. */
@Composable
internal fun TaskPropertiesTabs(selectedTab: EditTaskTab, onTabSelected: (EditTaskTab) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        EditTaskTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text(tab.title, maxLines = 1, softWrap = false, style = MaterialTheme.typography.titleSmall) },
            )
        }
    }
}

internal fun TaskPriority.taskPropertiesLabel() = when (this) {
    TaskPriority.NONE -> "Без пріоритету"
    TaskPriority.LOW -> "Низький"
    TaskPriority.MEDIUM -> "Середній"
    TaskPriority.HIGH -> "Високий"
    TaskPriority.CRITICAL -> "Критичний"
}
