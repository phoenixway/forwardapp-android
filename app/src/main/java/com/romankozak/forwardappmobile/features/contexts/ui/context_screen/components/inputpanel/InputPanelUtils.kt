package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
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

/**
 * Логіка вибору кольорової схеми панелі залежно від режиму введення.
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

