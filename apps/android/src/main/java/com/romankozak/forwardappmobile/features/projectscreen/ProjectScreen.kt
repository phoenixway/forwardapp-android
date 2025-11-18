package com.romankozak.forwardappmobile.features.projectscreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.*
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.MinimalInputPanelV3
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode

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
    val holdMenu = rememberHoldMenu2()

    val onHoldMenuSelect: (Int) -> Unit = { index ->
        when (index) {
            0 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Backlog))
            1 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Advanced))
            2 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Inbox))
            3 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Attachments))
        }
    }

    val menuItems = remember {
        listOf(
            HoldMenuItem("Backlog", Icons.Outlined.ListAlt),
            HoldMenuItem("Advanced", Icons.Outlined.Dashboard),
            HoldMenuItem("Inbox", Icons.Outlined.Inbox),
            HoldMenuItem("Attachments", Icons.Outlined.AttachFile),
        )
    }

    Box(Modifier.fillMaxSize()) {
        MinimalInputPanelV3(
            inputMode = state.inputMode,
            onInputModeSelected = {
                viewModel.onEvent(
                    ProjectScreenViewModel.Event.SwitchInputMode(it)
                )
            },
            menuItems = menuItems,
            onMenuItemSelected = onHoldMenuSelect,
            onTap = {
                // Обробка одинарного тапу
                println("Single tap!")
            },
            holdMenuController = holdMenu,
            modifier = Modifier.zIndex(1f)
        )

        // Overlay для візуалізації меню
        HoldMenu2Overlay(
            controller = holdMenu,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)
        )
    }
}