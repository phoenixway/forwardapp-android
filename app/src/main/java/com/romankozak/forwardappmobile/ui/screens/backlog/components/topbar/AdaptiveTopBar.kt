package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType

/**
 * Спрощений AdaptiveTopBar без навігаційної панелі
 * Навігація перенесена в ModernInputPanel
 */
@Composable
fun AdaptiveTopBar(
    isSelectionModeActive: Boolean,
    title: String,
    selectedCount: Int,
    areAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onMoreActions: (GoalActionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = if (isSelectionModeActive) 4.dp else 1.dp,
        modifier = modifier.statusBarsPadding(),
    ) {
        if (isSelectionModeActive) {
            Column(modifier = Modifier.statusBarsPadding()) {
                ListTitleBar(title = title)
                MultiSelectTopAppBar(
                    selectedCount = selectedCount,
                    areAllSelected = areAllSelected,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onDelete = onDelete,
                    onToggleComplete = onToggleComplete,
                    onMoreActions = onMoreActions,
                )
            }
        } else {
            // Простий заголовок без навігації
            SubcomposeLayout(modifier = Modifier.statusBarsPadding()) { constraints ->
                val titleTextPlaceable = subcompose(AdaptiveTopBarSlot.TITLE_TEXT) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }.first().measure(constraints)

                // Завжди показуємо тільки заголовок
                val titleBarPlaceable = subcompose(AdaptiveTopBarSlot.TITLE_BAR) {
                    ListTitleBar(title = title)
                }.first().measure(constraints)

                val totalHeight = titleBarPlaceable.height
                layout(constraints.maxWidth, totalHeight) {
                    titleBarPlaceable.placeRelative(0, 0)
                }
            }
        }
    }
}

private enum class AdaptiveTopBarSlot {
    TITLE_TEXT,
    TITLE_BAR
}