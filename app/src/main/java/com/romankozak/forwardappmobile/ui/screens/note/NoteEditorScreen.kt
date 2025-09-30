package com.romankozak.forwardappmobile.ui.screens.note

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen

@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val noteId: String? = navController.currentBackStackEntry?.arguments?.getString("noteId")

    LaunchedEffect(noteId) {
        noteId?.let {
            viewModel.loadNote(it)
        }
    }

    UniversalEditorScreen(
        title = "Edit Note",
        onSave = { content ->
            viewModel.saveNote(content)
            navController.popBackStack()
        },
        onNavigateBack = { navController.popBackStack() },
        viewModel = viewModel.universalEditorViewModel,
    )
}
