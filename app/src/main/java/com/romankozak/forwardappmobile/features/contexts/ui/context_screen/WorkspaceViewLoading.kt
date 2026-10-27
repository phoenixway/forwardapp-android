package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState

internal fun isWorkspaceViewReady(state: ContextUiState, sessionView: ContextViewMode): Boolean =
    state.isInitialViewResolved && !state.isContextSwitching && state.currentViewMode == sessionView

/** No temporary view/title/tab selection is rendered before presentation preferences resolve. */
@Composable
internal fun WorkspaceViewLoading() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
