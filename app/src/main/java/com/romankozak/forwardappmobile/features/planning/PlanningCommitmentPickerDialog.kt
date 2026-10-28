package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.planning.PlanningParticipantSummary

@Composable
fun PlanningCommitmentPickerDialog(participants: List<PlanningParticipantSummary>, onDismiss: () -> Unit,
    onSelected: (PlanningParticipantSummary) -> Unit, title: String = "Add to…") {
    var text by rememberSaveable { mutableStateOf("") }
    val matches = remember(participants, text) {
        participants.filter { it.entity.title.contains(text, true) || it.contextLabel.contains(text, true) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Пошук") }, singleLine = true)
            if (matches.isEmpty()) Text("Немає доступних учасників", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(matches, key = { it.commitment.id.value }) { item ->
                    Column(Modifier.fillMaxWidth().clickable { onSelected(item) }.padding(vertical = 12.dp)) {
                        Text(item.entity.title)
                        Text("${item.entity.orientationKind?.name.orEmpty()} · ${item.contextLabel}", style = MaterialTheme.typography.labelSmall)
                        if (item.statusLabel.isNotEmpty()) Text(item.statusLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } })
}
