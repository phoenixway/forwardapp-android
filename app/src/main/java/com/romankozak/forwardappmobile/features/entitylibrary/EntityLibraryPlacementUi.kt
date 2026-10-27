package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureDestinationDialog
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef

@Composable
internal fun EntityLibraryItemMenu(ref: CanonicalEntityRef, onAdd: (CanonicalEntityRef) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "Дії сутності") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Додати до Navigation") },
            onClick = { expanded = false; onAdd(ref) })
    }
}

/** Reuses the existing Big Picture read owner and typed destination selector, loaded only on demand. */
@Composable
internal fun EntityLibraryPlacementDialog(
    mutation: EntityLibraryPlacementState,
    onDismiss: () -> Unit,
    onChoose: (com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent) -> Unit,
    destinations: BigPictureViewModel = hiltViewModel(),
) {
    val state by destinations.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        is BigPictureUiState.Ready -> BigPictureDestinationDialog(
            items = current.structure?.items.orEmpty(),
            onDismiss = onDismiss, onChoose = onChoose,
            error = mutation.error, enabled = !mutation.busy,
        )
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Додати до Navigation") },
            text = {
                if (current is BigPictureUiState.Error) Text(current.message)
                else CircularProgressIndicator()
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
        )
    }
}
