package com.romankozak.forwardappmobile.routes

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreen

const val MAIN_SCREEN_ROUTE = "main_screen"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = MAIN_SCREEN_ROUTE,
        ) {
            composable(MAIN_SCREEN_ROUTE) {
                MainScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}
