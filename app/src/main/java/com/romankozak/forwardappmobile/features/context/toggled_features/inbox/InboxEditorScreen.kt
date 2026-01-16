package com.romankozak.forwardappmobile.features.context.toggled_features.inbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

@Composable
fun InboxEditorScreen(
  navController: NavController,
  viewModel: InboxEditorViewModel = hiltViewModel(),
) {
  val inboxId: String? = navController.currentBackStackEntry?.arguments?.getString("inboxId")
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(inboxId) { inboxId?.let { viewModel.loadInboxItem(it) } }

  UniversalEditorScreen(
    title = "Edit Inbox Item",
    onSave = { content, _ -> // Ignore cursor position for now
      viewModel.saveInboxItem(content)
      navController.popBackStack()
    },
    onAutoSave = { content, _ ->
      viewModel.saveInboxItem(content)
    },
    onNavigateBack = { navController.popBackStack() },
    viewModel = viewModel.universalEditorViewModel,
    navController = navController,
    contentFocusRequester = focusRequester,
  )
}
