package com.romankozak.forwardappmobile.ui.screens.inbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen

@Composable
fun InboxEditorScreen(
    navController: NavController,
    viewModel: InboxEditorViewModel = hiltViewModel(),
) {
    val inboxId: String? = navController.currentBackStackEntry?.arguments?.getString("inboxId")

    LaunchedEffect(inboxId) {
        inboxId?.let {
            viewModel.loadInboxItem(it)
        }
    }

    UniversalEditorScreen(
        title = "Edit Inbox Item",
        onSave = { content ->
            viewModel.saveInboxItem(content)
            navController.popBackStack()
        },
        onNavigateBack = { navController.popBackStack() },
        viewModel = viewModel.universalEditorViewModel,
    )
}
