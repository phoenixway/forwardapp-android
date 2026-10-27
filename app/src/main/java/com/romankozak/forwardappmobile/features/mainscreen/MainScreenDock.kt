package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.DashboardBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.recent.RecentViewModel

/** Route-scoped local state above one persistent, route-independent global dock. */
@Composable
internal fun MainScreenDock(
    route: String,
    globalActions: BottomPanelGlobalActions,
    recentViewModel: RecentViewModel,
    contextualContent: @Composable () -> Unit,
) {
    val drafts = rememberSaveableStateHolder()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(route) {
        focus.clearFocus()
        keyboard?.hide()
    }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        drafts.SaveableStateProvider(route) { contextualContent() }
        DashboardBottomPanel(globalActions, recentViewModel, journalFirst = journalFirstForMainScreenRoute(route))
    }
}
