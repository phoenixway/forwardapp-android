package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen

@Composable
fun CustomListEditorScreen(
  navController: NavController,
  viewModel: CustomListEditorViewModel = hiltViewModel(),
) {
  val listId: String? = navController.currentBackStackEntry?.arguments?.getString("listId")

  LaunchedEffect(listId) { listId?.let { viewModel.loadCustomList(it) } }

  UniversalEditorScreen(
    title = "Edit Custom List",
    onSave = { content, cursorPosition ->
      viewModel.saveCustomList(content, cursorPosition)
      navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
      navController.popBackStack()
    },
    onNavigateBack = { navController.popBackStack() },
    viewModel = viewModel.universalEditorViewModel,
    navController = navController,
  )
}
