package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.MainScreenScaffold

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val viewModel: MainScreenViewModel = LocalAppComponent.current.mainScreenViewModel
    MainScreenScaffold()
}