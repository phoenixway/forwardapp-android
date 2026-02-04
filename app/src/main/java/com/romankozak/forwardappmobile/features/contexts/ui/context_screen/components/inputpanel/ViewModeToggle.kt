package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.*

@Composable
fun ViewModeToggle(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    holdMenuController: HoldMenu2Controller,
) {
    // Фільтрація доступних екранів на основі Capabilities
    val availableViews = remember(state.activeCapabilities, state.isProjectManagementEnabled) {
        ContextViewMode.entries.filter { mode ->
            val capId = mode.toCapabilityId()
            // Backlog завжди доступний, решта - за дозволом
            mode == ContextViewMode.BACKLOG || state.activeCapabilities.contains(capId)
        }.sortedBy { it.order() }.reversed()
    }

    val menuItems = availableViews.map { viewMode ->
        HoldMenuItem(label = viewMode.displayName(), icon = viewMode.toIcon())
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
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
        }
    }
}

// Helpers
private fun ContextViewMode.toCapabilityId() = CapabilityId(this.name.lowercase())
private fun ContextViewMode.order() = when(this) {
    ContextViewMode.DASHBOARD -> 0
    ContextViewMode.BACKLOG -> 1
    ContextViewMode.INBOX -> 2
    ContextViewMode.ADVANCED -> 3
    ContextViewMode.ATTACHMENTS -> 4
}
private fun ContextViewMode.displayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
private fun ContextViewMode.toIcon() = when (this) {
    ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
    ContextViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
    ContextViewMode.ADVANCED -> Icons.Outlined.Dashboard
    ContextViewMode.ATTACHMENTS -> Icons.Default.Attachment
    ContextViewMode.DASHBOARD -> Icons.Outlined.ViewModule
}
private fun ContextViewMode.getDefaultInputMode() = when (this) {
    ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
    else -> InputMode.AddGoal
}
