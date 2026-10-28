package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.planning.*

@Composable
internal fun PlanningTraceDialog(frame: PlanningCommitmentCompositionProjection, current: PlanningParticipantSummary,
    upward: Boolean, onDismiss: () -> Unit, open: (PlanningParticipantSummary) -> Unit) {
    var expanded by rememberSaveable(current.commitment.id.value, upward) { mutableStateOf(emptyList<String>()) }
    val rows = remember(frame, current, upward, expanded) { frame.trace(current.commitment.id, upward, expanded.toSet()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Column {
        Text(if (upward) "Trace upward" else "Components")
        Text(current.entity.title, style = MaterialTheme.typography.bodySmall)
    } }, text = {
        LazyColumn(Modifier.heightIn(max = 460.dp)) {
            items(rows, key = { it.key }) { row ->
                Column(Modifier.padding(start = (row.depth.coerceAtMost(4) * 12).dp)) {
                    PlanningCompositionRow(row.participant, { onDismiss(); open(row.participant) })
                    if (row.hasMore) TextButton(onClick = { expanded = expanded + row.key }) { Text("Show more…") }
                }
            }
            if (rows.isEmpty()) item { Text(if (upward) "No larger planning context" else "No components yet") }
            if (rows.size >= 200 || rows.any { it.depth == 12 }) item {
                Text("Показано частину графа. Відкрийте учасника, щоб продовжити.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } })
}
