package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller

@Composable
internal fun NavigationBar(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    holdMenuController: HoldMenu2Controller,
) {
    Row(
        modifier = Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackForwardButton(state, actions, contentColor)

        IconButton(onClick = actions.onNavigateHome, modifier = Modifier.size(34.dp)) {
            Text(
                text = "⌬",
                color = contentColor.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = actions.onRecentsClick, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.History, null, tint = contentColor.copy(alpha = 0.8f))
        }
        IconButton(onClick = actions.onShowCurrentContextInHierarchyFocus, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.MyLocation, null, tint = contentColor.copy(alpha = 0.8f))
        }
        IconButton(onClick = actions.onAddContextLink, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.Add, null, tint = contentColor.copy(alpha = 0.8f))
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = actions.onNavigateHome,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = "Показати головний екран",
                tint = contentColor.copy(alpha = 0.8f),
            )
        }
        IconButton(
            onClick = actions.onShowProjectHierarchy,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                Icons.Outlined.TravelExplore,
                contentDescription = "Search everywhere",
                tint = contentColor.copy(alpha = 0.8f),
            )
        }

        ViewModeToggle(state, actions, contentColor, holdMenuController)

        OptionsMenu(state, actions, contentColor)
    }
}
