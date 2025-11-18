package com.romankozak.forwardappmobile.features.projectscreen

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.MinimalInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuOverlay
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    projectId: String?,
) {
    val appComponent = LocalAppComponent.current
    val viewModel: ProjectScreenViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val holdMenuState = remember { mutableStateOf(HoldMenuState()) }

    val onHoldMenuSelect: (Int) -> Unit = { index ->
        Log.e("HOLDMENU", "🎉 Menu item selected: $index")
        when (index) {
            0 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Backlog))
            1 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Advanced))
            2 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Inbox))
            3 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Attachments))
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Main content
        MinimalInputPanel(
            inputMode = state.inputMode,
            onInputModeSelected = {
                viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(it))
            },
            holdMenuState = holdMenuState,
            onHoldMenuSelect = onHoldMenuSelect,
            modifier = Modifier.zIndex(1f)
        )

        // Overlay для меню - відображається поверх всього
        if (holdMenuState.value.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(999f)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                HoldMenuOverlay(
                    state = holdMenuState.value,
                    onChangeState = { holdMenuState.value = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}