package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AccountTree
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
    journalFirst: Boolean = false,
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
                journalFirst = journalFirst,
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
    journalFirst: Boolean = false,
) {
    var showRecentSheet by remember { mutableStateOf(false) }
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    if (mode == BottomPanelGlobalRailMode.FULL) {
        BottomPanelIconButton(
            imageVector = if (journalFirst) Icons.AutoMirrored.Outlined.MenuBook else Icons.Outlined.AccountTree,
            contentDescription = if (journalFirst) "Показати журнал життя" else "Navigation",
            onClick = if (journalFirst) actions.onNavigateToTracker else actions.onNavigateToProjectHierarchy,
        )
    }
    if (mode == BottomPanelGlobalRailMode.FULL) {
        BottomPanelIconButton(
            imageVector = if (journalFirst) Icons.Outlined.AccountTree else Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = if (journalFirst) "Navigation" else "Показати журнал життя",
            onClick = if (journalFirst) actions.onNavigateToProjectHierarchy else actions.onNavigateToTracker,
        )
    }
    BottomPanelIconButton(
        imageVector = Icons.Outlined.AutoAwesome,
        contentDescription = "Magicbox",
        onClick = actions.onNavigateToGlobalSearch,
    )
    BottomPanelIconButton(
        imageVector = Icons.Default.Add,
        contentDescription = "Створити",
        onClick = actions.onShowCreateActions,
    )
    BottomPanelMoreActionButton(
        actions = actions,
        additionalActions = additionalActions,
        onShowRecents = if (mode == BottomPanelGlobalRailMode.FULL) ({ showRecentSheet = true }) else null,
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
