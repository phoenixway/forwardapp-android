package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.ui.graphics.vector.ImageVector
import com.romankozak.forwardappmobile.core.theme.InputPanelColors

/**
 * Внутрішня модель для опису елемента меню опцій.
 */
internal data class MenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isVisible: Boolean = true,
    val isDestructive: Boolean = false,
)

internal typealias InputPanelUtils = MenuItem

/**
 * Логіка вибору кольорової схеми панелі залежно від режиму введення.
 */
internal fun getPanelColors(
    mode: InputMode,
    theme: InputPanelColors,
): PanelColors {
    val style =
        when (mode) {
            InputMode.AddGoal -> theme.addGoal
            InputMode.AddConnectionNote -> theme.addQuickRecord
            InputMode.AddDirection -> theme.addGoal
            InputMode.AddQuickRecord -> theme.addQuickRecord
            InputMode.SearchInList -> theme.searchInList
            InputMode.SearchGlobal -> theme.searchGlobal
            InputMode.AddProjectLog, InputMode.AddMilestone -> theme.addProjectLog
        }
    return PanelColors(
        containerColor = style.backgroundColor,
        contentColor = style.textColor,
        accentColor = style.textColor,
        inputFieldColor = style.inputFieldColor,
    )
}
