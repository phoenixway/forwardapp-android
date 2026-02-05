package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

        IconButton(onClick = actions.onShowProjectHierarchy, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.AlternateEmail, null, tint = contentColor.copy(alpha = 0.7f))
        }

        Spacer(Modifier.weight(1f))

        ViewModeToggle(state, actions, contentColor, holdMenuController)

        OptionsMenu(state, actions, contentColor)
    }
}
