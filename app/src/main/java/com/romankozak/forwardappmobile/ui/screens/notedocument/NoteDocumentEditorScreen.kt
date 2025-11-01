package com.romankozak.forwardappmobile.ui.screens.notedocument

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

@Composable
fun NoteDocumentEditorScreen(
  navController: NavController,
  viewModel: NoteDocumentEditorViewModel = hiltViewModel(),
) {
  val backStackEntry = navController.currentBackStackEntry
  val documentId: String? =
    backStackEntry?.arguments?.getString("documentId")
      ?: backStackEntry?.arguments?.getString("listId")

  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(documentId) {
    if (documentId == null) {
      delay(300)
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  LaunchedEffect(documentId) { documentId?.let { viewModel.loadDocument(it) } }

  UniversalEditorScreen(
    title = stringResource(R.string.note_editor_edit_title),
    onSave = { content, cursorPosition ->
      viewModel.saveDocument(content, cursorPosition)
      navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
      navController.popBackStack()
    },
    onNavigateBack = { navController.popBackStack() },
    viewModel = viewModel.universalEditorViewModel,
    navController = navController,
    contentFocusRequester = focusRequester,
  )
}
