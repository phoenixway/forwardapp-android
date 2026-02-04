package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.core.theme.InputPanelColors

/**
 * Логіка вибору кольорів для панелі. 
 * Тепер функція internal, тому ModernInputPanel її бачить.
 */
internal fun getPanelColors(
    mode: InputMode, 
    theme: InputPanelColors
): PanelColors {
    val style = when (mode) {
        InputMode.AddGoal -> theme.addGoal
        InputMode.AddQuickRecord -> theme.addQuickRecord
        InputMode.SearchInList -> theme.searchInList
        InputMode.SearchGlobal -> theme.searchGlobal
        InputMode.AddProjectLog, InputMode.AddMilestone -> theme.addProjectLog
    }
    return PanelColors(
        containerColor = style.backgroundColor,
        contentColor = style.textColor,
        accentColor = style.textColor,
        inputFieldColor = style.inputFieldColor
    )
}

// Також переконайтеся, що OptionsMenu тут не має слова 'private'
@Composable
internal fun OptionsMenu(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
) {
    // ... ваш існуючий код OptionsMenu ...
}
