package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.context.ContextViewPolicy
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import java.util.Locale

@Composable
fun ViewModeToggle(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    holdMenuController: HoldMenu2Controller,
) {
    // 1. Фільтрація доступних екранів
    val availableViews =
        remember(state.activeCapabilities) {
            val views = ContextViewPolicy.availableViews(state.activeCapabilities)
            if (views.isEmpty()) listOf(ContextViewMode.DASHBOARD) else views
        }

    // 2. Створення елементів меню
    val menuItems =
        remember(availableViews) {
            availableViews.map { viewMode ->
                HoldMenuItem(
                    label = viewMode.displayName(),
                    icon = viewMode.toIcon(),
                )
            }
        }

    // Якщо доступний лише один режим (Беклог), можна приховати перемикач або заблокувати
    if (availableViews.size <= 1 && state.currentView == ContextViewMode.BACKLOG) {
        // Можна нічого не малювати або малювати неактивну іконку
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = contentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
    ) {
        Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            HoldMenu2Button(
                items = menuItems,
                controller = holdMenuController,
                onSelect = { index ->
                    val selected = availableViews[index]
                    actions.onViewChange(selected)
                    actions.onInputModeSelected(selected.getDefaultInputMode())
                },
                modifier = Modifier.size(40.dp).padding(2.dp),
            ) {
                Icon(
                    imageVector = state.currentView.toIcon(),
                    contentDescription = "Switch View",
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
        }
    }
}

// --- HELPERS (Мапінг станів на системні ID та ресурси) ---

private fun ContextViewMode.displayName(): String {
    return this.name.lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        .replace("_", " ")
}

private fun ContextViewMode.toIcon(): ImageVector =
    when (this) {
        ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
        ContextViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
        ContextViewMode.CONNECTIONS -> Icons.Default.Attachment
        ContextViewMode.DASHBOARD -> Icons.Outlined.ViewModule
        ContextViewMode.DIRECTION -> Icons.Outlined.Explore
        ContextViewMode.LOG -> Icons.Outlined.History
        ContextViewMode.ARTIFACT -> Icons.Outlined.Inventory2
        ContextViewMode.KEY_PROBLEMS -> Icons.Outlined.Description
        ContextViewMode.ADVANCED,
        ContextViewMode.NOTES,
        ContextViewMode.VET_CASE,
        -> Icons.Outlined.Description
    }

private fun ContextViewMode.getDefaultInputMode() =
    when (this) {
        ContextViewMode.INBOX -> InputMode.AddQuickRecord
        ContextViewMode.CONNECTIONS -> InputMode.AddConnectionNote
        ContextViewMode.DIRECTION -> InputMode.AddDirection
        ContextViewMode.LOG -> InputMode.AddProjectLog
        else -> InputMode.AddGoal
    }
