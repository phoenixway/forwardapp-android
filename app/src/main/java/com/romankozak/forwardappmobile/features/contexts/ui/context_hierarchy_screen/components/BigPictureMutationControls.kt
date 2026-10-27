package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*

@Composable
internal fun BigPictureMutationControls(
    ready: BigPictureUiState.Ready,
    viewModel: BigPictureMutationViewModel,
    blocked: Boolean = false,
    onPickEntity: (H1StructuralParent) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    var addMenu by remember { mutableStateOf(false) }
    var groupTitle by rememberSaveable { mutableStateOf("") }
    var groupDialog by rememberSaveable { mutableStateOf(false) }
    var groupDestination by rememberSaveable(key = "bigPictureGroupAddDestination") { mutableStateOf<String?>(null) }
    var reorder by remember { mutableStateOf(false) }
    val parent = ready.structuralDestination()
    val index = ready.structure ?: return
    if (blocked) { CircularProgressIndicator(Modifier.size(20.dp)); return }

    if (state.busy) CircularProgressIndicator(Modifier.size(20.dp))
    IconButton(onClick = { reorder = true }, enabled = !state.busy && parent != null) {
        Icon(Icons.Default.SwapVert, "Змінити порядок")
    }
    IconButton(onClick = { addMenu = true }, enabled = !state.busy && (parent != null || clipboard != null)) { Icon(Icons.Default.Add, "Додати") }
    DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
        DropdownMenuItem(text = { Text("Існуючу сутність") }, enabled = !state.busy && parent != null,
            onClick = { addMenu = false; parent?.let(onPickEntity) })
        DropdownMenuItem(text = { Text("Групу / секцію") }, enabled = !state.busy && parent != null,
            onClick = {
                addMenu = false
                parent?.let { groupDestination = encodeStructuralParent(it); groupDialog = true }
            })
        if (clipboard != null) DropdownMenuItem(text = { Text("Скасувати переміщення") }, onClick = viewModel::cancelCut)
    }
    if (groupDialog) AlertDialog(
        onDismissRequest = { groupDialog = false },
        title = { Text("Нова група / секція") },
        text = { OutlinedTextField(value = groupTitle, onValueChange = { groupTitle = it }, label = { Text("Назва") }) },
        confirmButton = {
            TextButton(enabled = !state.busy && groupTitle.isNotBlank() && groupDestination != null, onClick = {
                groupDestination?.let { viewModel.addGroup(groupTitle, decodeStructuralParent(it)) }
                groupDialog = false
                groupDestination = null
            }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = { groupDialog = false }) { Text("Скасувати") } },
    )
    if (reorder && parent != null) BigPictureReorderDialog(
        items = index.children(parent),
        onDismiss = { reorder = false },
        onApply = { reorder = false; viewModel.reorder(parent, it) },
    )
}

@Composable
internal fun BigPictureNodeMenu(
    node: H1TypedNode,
    enabled: Boolean,
    onCut: (H1TypedNode) -> Unit,
    onRemove: (H1TypedNode) -> Unit,
    onRemoveSubtree: (H1TypedNode.Occurrence) -> Unit = {},
    hasChildren: Boolean = false,
    onRename: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    openLabel: String = "Відкрити редактор",
    sourceDeleteTarget: BigPictureSourceDeleteTarget? = null,
    onDeleteSource: ((BigPictureSourceDeleteTarget) -> Unit)? = null,
    onOpenSemantic: (() -> Unit)? = null,
    semanticOpenLabel: String = "Відкрити семантичну сутність",
) {
    var expanded by remember { mutableStateOf(false) }
    var removal by remember(node) { mutableStateOf<BigPictureRemovalConfirmation?>(null) }
    val sourceAction = sourceDeleteMenuTarget(node, sourceDeleteTarget)
    IconButton(enabled = enabled || onOpen != null, onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, "Дії розміщення")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (onRename != null) {
            DropdownMenuItem(text = { Text("Перейменувати") }, enabled = enabled,
                onClick = { expanded = false; onRename() })
        }
        onOpen?.let { open ->
            DropdownMenuItem(text = { Text(openLabel) }, onClick = { expanded = false; open() })
        }
        onOpenSemantic?.let { open ->
            DropdownMenuItem(text = { Text(semanticOpenLabel) }, onClick = { expanded = false; open() })
        }
        DropdownMenuItem(text = { Text("Вирізати / перемістити") }, enabled = enabled, onClick = { expanded = false; onCut(node) })
        DropdownMenuItem(text = { Text("Прибрати з Navigation") }, enabled = enabled, onClick = {
            expanded = false
            removal = bigPictureRemovalConfirmation(node, hasChildren)
        })
        if (sourceAction != null && onDeleteSource != null) {
            DropdownMenuItem(text = { Text(sourceAction.actionLabel, color = MaterialTheme.colorScheme.error) },
                enabled = enabled, onClick = { expanded = false; onDeleteSource(sourceAction) })
        }
    }
    removal?.let { confirmation ->
        AlertDialog(
            onDismissRequest = { removal = null },
            title = { Text(confirmation.title) },
            text = { Text(confirmation.message) },
            confirmButton = { TextButton(enabled = enabled, onClick = {
                removal = null
                if (confirmation.recursive) onRemoveSubtree(node as H1TypedNode.Occurrence)
                else onRemove(node)
            }) { Text(confirmation.confirmLabel) } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text("Скасувати") } },
        )
    }
}
