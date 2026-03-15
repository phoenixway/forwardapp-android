package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

        ViewActionsMenuButton(
            state = state,
            contentColor = contentColor,
            onActionClick = actions.onCapabilityViewActionClick,
        )

        ViewModeToggle(state, actions, contentColor, holdMenuController)

        OptionsMenu(state, actions, contentColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewActionsMenuButton(
    state: NavPanelState,
    contentColor: Color,
    onActionClick: (String) -> Unit,
) {
    if (state.viewActions.isEmpty()) return

    var showSheet by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showSheet = true },
        modifier = Modifier.size(34.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Дії поточного виду",
            tint = contentColor.copy(alpha = 0.85f),
        )
    }

    if (!showSheet) return

    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = { showSheet = false },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Дії поточного виду",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            state.viewActions.forEach { action ->
                ListItem(
                    headlineContent = { Text(action.title) },
                    supportingContent = {
                        action.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clickable {
                                onActionClick(action.id)
                                showSheet = false
                            },
                )
            }
        }
    }
}
