package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*

@Composable
internal fun BigPictureMutationControls(
    ready: BigPictureUiState.Ready,
    viewModel: BigPictureMutationViewModel,
    onPickEntity: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val pendingEntity by viewModel.pendingEntity.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    var addMenu by remember { mutableStateOf(false) }
    var groupTitle by rememberSaveable { mutableStateOf("") }
    var groupDialog by rememberSaveable { mutableStateOf(false) }
    var groupDestination by rememberSaveable { mutableStateOf(false) }
    var reorder by remember { mutableStateOf(false) }
    val parent = ready.structuralDestination()
    val index = ready.structure ?: return

    if (state.busy) CircularProgressIndicator(Modifier.size(20.dp))
    IconButton(onClick = { addMenu = true }, enabled = !state.busy) { Icon(Icons.Default.Add, "Додати") }
    DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
        DropdownMenuItem(text = { Text("Існуючу сутність") }, onClick = { addMenu = false; onPickEntity() })
        DropdownMenuItem(text = { Text("Групу / секцію") }, onClick = { addMenu = false; groupDialog = true })
        DropdownMenuItem(
            text = { Text("Змінити порядок") }, enabled = parent != null,
            onClick = { addMenu = false; reorder = true },
        )
        if (clipboard != null) DropdownMenuItem(text = { Text("Скасувати переміщення") }, onClick = viewModel::cancelCut)
    }
    if (pendingEntity != null && !state.busy) BigPictureDestinationDialog(
        items = index.items,
        onDismiss = viewModel::cancelAdd,
        onChoose = viewModel::addEntity,
        error = state.message?.takeIf { it.startsWith("Не вдалося") },
    )
    if (groupDialog) AlertDialog(
        onDismissRequest = { groupDialog = false },
        title = { Text("Нова група / секція") },
        text = { OutlinedTextField(value = groupTitle, onValueChange = { groupTitle = it }, label = { Text("Назва") }) },
        confirmButton = {
            TextButton(enabled = groupTitle.isNotBlank(), onClick = { groupDialog = false; groupDestination = true }) { Text("Далі") }
        },
        dismissButton = { TextButton(onClick = { groupDialog = false }) { Text("Скасувати") } },
    )
    if (groupDestination) BigPictureDestinationDialog(
        items = index.items,
        onDismiss = { groupDestination = false },
        onChoose = { destination -> groupDestination = false; viewModel.addGroup(groupTitle, destination) },
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
) {
    var expanded by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    IconButton(enabled = enabled, onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "Дії розміщення") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Вирізати / перемістити") }, onClick = { expanded = false; onCut(node) })
        DropdownMenuItem(text = { Text("Прибрати з Big Picture") }, onClick = { expanded = false; confirm = true })
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Прибрати з Big Picture?") },
        text = { Text(if (node is H1TypedNode.Container)
            "Секцію буде прибрано, її дочірні елементи залишаться рівнем вище. Сутності не видаляються."
            else "Буде прибрано лише це розміщення. Сутність та інші її розміщення залишаться. Спочатку перемістіть дочірні елементи, якщо вони є.") },
        confirmButton = { TextButton(onClick = { confirm = false; onRemove(node) }) { Text("Прибрати") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Скасувати") } },
    )
}
