package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet

enum class BottomPanelGlobalRailMode {
    COMPACT,
    FULL,
}

@Composable
fun CommandDeckGlobalRailPanel(
    actions: BottomPanelGlobalActions,
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    BottomPanelSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomPanelGlobalRail(
                actions = actions,
                mode = BottomPanelGlobalRailMode.FULL,
                recentViewModel = recentViewModel,
            )
        }
    }
}

@Composable
fun BottomPanelGlobalRail(
    actions: BottomPanelGlobalActions,
    mode: BottomPanelGlobalRailMode = BottomPanelGlobalRailMode.COMPACT,
    additionalActions: List<MoreSheetAction> = emptyList(),
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    var showRecentSheet by remember { mutableStateOf(false) }
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    if (mode == BottomPanelGlobalRailMode.FULL) {
        BottomPanelIconButton(
            imageVector = Icons.Outlined.History,
            contentDescription = "Недавні",
            onClick = { showRecentSheet = true },
        )
    }
    if (mode == BottomPanelGlobalRailMode.FULL) {
        BottomPanelIconButton(
            imageVector = Icons.Outlined.MoveToInbox,
            contentDescription = "Inbox",
            onClick = actions.onNavigateToInbox,
        )
    }
    BottomPanelIconButton(
        imageVector = Icons.Outlined.AutoAwesome,
        contentDescription = "Magicbox",
        onClick = actions.onNavigateToGlobalSearch,
    )
    BottomPanelIconButton(
        imageVector = Icons.Outlined.AccountTree,
        contentDescription = "Ієрархія орієнтирів",
        onClick = actions.onNavigateToProjectHierarchy,
    )
    BottomPanelMoreActionButton(
        actions = actions,
        additionalActions = additionalActions,
    )

    NewRecentListsSheet(
        showSheet = showRecentSheet,
        recentItems = recentItems,
        onDismiss = { showRecentSheet = false },
        onItemClick = { item ->
            showRecentSheet = false
            actions.onNavigateToRecentItem(item)
        },
        onPinClick = recentViewModel::onPinClick,
    )
}
