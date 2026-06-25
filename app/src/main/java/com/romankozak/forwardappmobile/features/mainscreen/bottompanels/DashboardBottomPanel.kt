package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.CommandDeckGlobalRailPanel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel

@Composable
fun DashboardBottomPanel(
    globalActions: BottomPanelGlobalActions,
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    CommandDeckGlobalRailPanel(
        actions = globalActions,
        recentViewModel = recentViewModel,
    )
}
