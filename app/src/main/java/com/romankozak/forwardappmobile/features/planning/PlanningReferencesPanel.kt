package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel

@Composable
fun PlanningReferencesPanel(owner: PlanningReferenceOwner, compact: Boolean = false) {
    val model: PlanningReferencesViewModel = hiltViewModel(key = "planning-references:${owner.kind}:${owner.id}")
    LaunchedEffect(owner) { model.selectOwner(owner) }
    val links by model.links.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (compact) PlanningRelationSectionHeading("Сутності", enabled = !busy, onAdd = { picker = true }) else {
            Text("Пов’язані сутності", style = MaterialTheme.typography.titleSmall)
            Text("Зв’язки зберігаються одразу", style = MaterialTheme.typography.labelSmall)
        }
        links.forEach { link ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).clickable { model.open(link.entity.ref) }.padding(vertical = 8.dp)) {
                    Text(link.entity.title, style = if (compact) MaterialTheme.typography.bodyMedium else LocalTextStyle.current)
                    Text("${link.entity.ref.entityClass.displayLabel()} · ${if (link.row.role == "SOURCE") "На основі" else "Пов’язано"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (compact) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current)
                }
                if (link.row.role == "RELATED") {
                    if (compact) IconButton(enabled = !busy, onClick = { model.remove(link.row) }) {
                        Icon(Icons.Default.Close, contentDescription = "Прибрати зв’язок: ${link.entity.title}", modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else TextButton(enabled = !busy, onClick = { model.remove(link.row) }) { Text("Прибрати зв’язок") }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!compact) TextButton(enabled = !busy, onClick = { picker = true }) { Text("Пов’язати сутність") }
    }
    if (picker) PlanningEntityPickerDialog(onDismiss = { picker = false }, onSelected = {
        picker = false
        model.attach(it.ref)
    }, host = model)
}
