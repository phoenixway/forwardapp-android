package com.romankozak.forwardappmobile.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.romankozak.forwardappmobile.ui.screens.chat.ChatScreen

const val CHAT_ROUTE = "chat_screen"

fun NavGraphBuilder.chatScreen(navController: NavController) {
    composable(CHAT_ROUTE) {
        ChatScreen(navController = navController)
    }
}
