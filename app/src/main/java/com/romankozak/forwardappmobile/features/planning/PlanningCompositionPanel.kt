package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

@Composable
fun PlanningCompositionPanel(owner: PlanningReferenceOwner) {
    val model: PlanningCompositionViewModel = hiltViewModel(key = "planning-composition:${owner.kind}:${owner.id}")
    val frame by model.projection.collectAsStateWithLifecycle()
    val current = frame.forOwner(owner) ?: return // Never adopt a free-form Day item.
    Column {
        Text("Participation", style = MaterialTheme.typography.titleSmall)
        Text(current.contextLabel, style = MaterialTheme.typography.bodySmall)
        PlanningParticipationActions(current, model)
        PlanningCompositionContent(current, frame, model)
    }
}

@Composable
internal fun PlanningCompositionContent(current: PlanningParticipantSummary,
    frame: PlanningCommitmentCompositionProjection, model: PlanningCompositionViewModel) {
    val error by model.error.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val embodiments by model.embodiments.collectAsStateWithLifecycle()
    val id = current.commitment.id
    var picker by rememberSaveable(id.value) { mutableStateOf<String?>(null) }
    var trace by rememberSaveable(id.value) { mutableStateOf<Boolean?>(null) }
    val parents = frame.outgoing(id)
    val children = frame.incoming(id)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (parents.isNotEmpty()) {
            Text("Part of", style = MaterialTheme.typography.titleSmall)
            parents.forEach { edge ->
                PlanningCompositionRow(edge.participant, { model.open(edge.participant) },
                    remove = { model.remove(edge) }, enabled = !busy, semantic = { model.semantic(edge.participant) })
            }
            TextButton(onClick = { trace = true }) { Text("Trace upward") }
        }
        if (children.isNotEmpty()) {
            Text("Components", style = MaterialTheme.typography.titleSmall)
            children.forEachIndexed { index, edge ->
                PlanningCompositionRow(edge.participant, { model.open(edge.participant) },
                    remove = { model.remove(edge) }, enabled = !busy,
                    up = if (index > 0) ({ model.move(id, edge, -1) }) else null,
                    down = if (index < children.lastIndex) ({ model.move(id, edge, 1) }) else null,
                    semantic = { model.semantic(edge.participant) })
            }
            TextButton(onClick = { trace = false }) { Text("View all components") }
        }
        Row {
            TextButton(enabled = !busy, onClick = { picker = "parent" }) { Text("Add to…") }
            TextButton(enabled = !busy, onClick = { picker = "component" }) { Text("+ Add component") }
        }
        if (current.scope.kind == PlanningScopeKind.STRATEGIC_ARC) {
            TextButton(enabled = !busy, onClick = { picker = "orientation" }) { Text("+ Add Orientation as component") }
        }
        Row {
            TextButton(onClick = { model.semantic(current) }) { Text("Open semantic entity") }
            if (CanonicalEntityRef.Orientation(current.commitment.orientationId) in embodiments) {
                TextButton(onClick = { model.workspace(current) }) { Text("Open Workspace") }
            }
        }
        error?.let { Text("Не вдалося змінити зв’язок. Перевірте доступність учасників та відсутність циклу.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    when (picker) {
        "orientation" -> PlanningEntityPickerDialog(onDismiss = { picker = null }, onSelected = {
            model.addComponent(id, it.ref.id)
        }, allowedClasses = setOf(CanonicalEntityClass.ORIENTATION))
        "parent", "component" -> PlanningCommitmentPickerDialog(
            participants = frame.relationCandidates(id, addingParent = picker == "parent"),
            title = if (picker == "parent") "Add to…" else "Add component",
            onDismiss = { picker = null }, onSelected = { selected ->
                if (picker == "parent") model.link(id, selected.commitment.id) else model.link(selected.commitment.id, id)
                picker = null
            })
    }
    trace?.let { upward -> PlanningTraceDialog(frame, current, upward, { trace = null }, model::open) }
}

@Composable
internal fun PlanningCompositionRow(item: PlanningParticipantSummary, open: () -> Unit,
    remove: (() -> Unit)? = null, enabled: Boolean = true, up: (() -> Unit)? = null,
    down: (() -> Unit)? = null, semantic: (() -> Unit)? = null) {
    var menu by rememberSaveable(item.commitment.id.value) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).clickable(onClick = open).padding(vertical = 10.dp)) {
            Text(item.entity.title, style = MaterialTheme.typography.bodyMedium)
            Text(listOf(item.contextLabel, item.statusLabel).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (remove != null || semantic != null) Box {
            IconButton(enabled = enabled, onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Relation actions") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                up?.let { DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; it() }) }
                down?.let { DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; it() }) }
                semantic?.let { DropdownMenuItem(text = { Text("Open semantic entity") }, onClick = { menu = false; it() }) }
                remove?.let { DropdownMenuItem(text = { Text("Remove relation") }, onClick = { menu = false; it() }) }
            }
        }
    }
}
