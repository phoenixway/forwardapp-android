package com.romankozak.forwardappmobile.core.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.ai.chat.ChatScreen

const val CHAT_ROUTE = NavigationRoutes.CHAT

fun NavGraphBuilder.chatScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
) {
    composable(CHAT_ROUTE) {
        ChatScreen(
            navController = navController,
            navigationManager = navigationManager,
        )
    }
}
