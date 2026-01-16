package com.romankozak.forwardappmobile.features.attachments.specific_types.note

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

@Composable
fun NoteEditorScreen(
  navController: NavController,
  viewModel: NoteEditorViewModel = hiltViewModel(),
) {
  val noteId: String? = navController.currentBackStackEntry?.arguments?.getString("noteId")
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(noteId) { noteId?.let { viewModel.loadNote(it) } }

  UniversalEditorScreen(
    title = "Edit Note",
    onSave = { content, _ -> // Ignore cursor position for now
      viewModel.saveNote(content)
      navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
      navController.popBackStack()
    },
    onAutoSave = { content, _ ->
      viewModel.saveNote(content)
    },
    onNavigateBack = { navController.popBackStack() },
    viewModel = viewModel.universalEditorViewModel,
    navController = navController,
    contentFocusRequester = focusRequester,
  )
}
