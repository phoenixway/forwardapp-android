package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.*
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
            val views =
                ContextViewMode.entries.filter { mode ->
                    state.activeCapabilities.contains(mode.toCapabilityId())
                }

            if (views.isEmpty()) {
                listOf(ContextViewMode.DASHBOARD)
            } else {
                views
            }.sortedBy { it.orderPriority() }.reversed()
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

private fun ContextViewMode.toCapabilityId(): CapabilityId {
    return when (this) {
        ContextViewMode.ADVANCED -> CapabilityId("advanced")
        // Для інших використовуємо назву в нижньому регістрі (inbox, backlog, notes...)
        else -> CapabilityId(this.name.lowercase(Locale.ROOT))
    }
}

private fun ContextViewMode.orderPriority() =
    when (this) {
        ContextViewMode.DASHBOARD -> 0
        ContextViewMode.BACKLOG -> 1
        ContextViewMode.INBOX -> 2
        ContextViewMode.ADVANCED -> 3
        ContextViewMode.ATTACHMENTS -> 4
        ContextViewMode.DIRECTION -> 5
        ContextViewMode.NOTES -> 6
        ContextViewMode.LOG -> 7
        ContextViewMode.ARTIFACT -> 8
        ContextViewMode.VET_CASE -> 9
    }

private fun ContextViewMode.displayName(): String {
    return this.name.lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        .replace("_", " ")
}

private fun ContextViewMode.toIcon(): ImageVector =
    when (this) {
        ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
        ContextViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
        ContextViewMode.ADVANCED -> Icons.Outlined.Dashboard
        ContextViewMode.ATTACHMENTS -> Icons.Default.Attachment
        ContextViewMode.DASHBOARD -> Icons.Outlined.ViewModule
        ContextViewMode.DIRECTION -> Icons.Outlined.Explore
        ContextViewMode.NOTES -> Icons.Outlined.Description
        ContextViewMode.VET_CASE -> Icons.Default.MedicalServices
        ContextViewMode.LOG -> Icons.Outlined.History
        ContextViewMode.ARTIFACT -> Icons.Outlined.Inventory2
    }

private fun ContextViewMode.getDefaultInputMode() =
    when (this) {
        ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
        ContextViewMode.DIRECTION -> InputMode.AddDirection
        else -> InputMode.AddGoal
    }
