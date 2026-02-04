package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller

@Composable
internal fun NavigationBar(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    holdMenuController: HoldMenu2Controller,
    modifier: Modifier = Modifier,
) {
    val lastNonSearchMode = remember { mutableStateOf<InputMode?>(null) }
    
    Row(
        modifier = modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackForwardButton(state, actions, contentColor)

        IconButton(onClick = actions.onShowProjectHierarchy, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.AlternateEmail, null, tint = contentColor.copy(alpha = 0.7f))
        }

        IconButton(onClick = actions.onRecentsClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Restore, null, tint = contentColor.copy(alpha = 0.7f))
        }

        // Пошук
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(if (state.inputMode == InputMode.SearchInList) contentColor.copy(alpha = 0.16f) else Color.Transparent)
                .combinedClickable(
                    onClick = { 
                        if (state.inputMode == InputMode.SearchInList) actions.onInputModeSelected(lastNonSearchMode.value ?: InputMode.AddGoal)
                        else { lastNonSearchMode.value = state.inputMode; actions.onInputModeSelected(InputMode.SearchInList) }
                    },
                    onLongClick = { actions.onInputModeSelected(InputMode.SearchGlobal) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Search, null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.weight(1f))

        // ВИПРАВЛЕНО: тепер передаємо єдиний state
        ViewModeToggle(state, actions, contentColor, holdMenuController)
        
        OptionsMenu(state, actions, contentColor)
    }
}
