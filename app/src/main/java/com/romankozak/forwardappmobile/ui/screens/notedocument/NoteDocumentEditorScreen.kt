package com.romankozak.forwardappmobile.ui.screens.notedocument

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun NoteDocumentEditorScreen(
  navController: NavController,
  startEdit: Boolean = false,
  viewModel: NoteDocumentEditorViewModel = hiltViewModel(),
) {
  val backStackEntry = navController.currentBackStackEntry
  val documentId: String? =
    backStackEntry?.arguments?.getString("documentId")
      ?: backStackEntry?.arguments?.getString("listId")

  val focusRequester = remember { FocusRequester() }
  val view = LocalView.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(documentId) {
    if (documentId == null) {
      delay(300)
      focusRequester.requestFocus()
      val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
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
    onWikiLinkClick = { link ->
      coroutineScope.launch {
        val targetId = viewModel.findDocumentIdByName(link)
        if (targetId != null) {
          navController.navigate("note_document_screen/$targetId?startEdit=false")
        } else {
          viewModel.universalEditorViewModel.showError("Не зміг відкрити вкладення \"$link\"")
        }
      }
    },
    viewModel = viewModel.universalEditorViewModel,
    navController = navController,
    contentFocusRequester = focusRequester,
    startInEditMode = startEdit || documentId == null,
  )
}
