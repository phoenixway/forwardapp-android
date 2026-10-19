package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*

/** Legacy evidence is readable/removable, never promoted to a typed identity by raw-ID guessing. */
@Composable
fun PlanningItemReferences(
    owner: PlanningReferenceOwner,
    compatibilityLinks: List<Pair<String, String>> = emptyList(),
    onRemoveCompatibility: (String) -> Unit = {},
    onOpenCompatibility: ((String) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlanningReferencesPanel(owner)
        if (compatibilityLinks.isNotEmpty()) {
            Text("Попередні робочі зв’язки", style = MaterialTheme.typography.labelMedium)
            compatibilityLinks.forEach { (id, title) ->
                Row(Modifier.fillMaxWidth()) {
                    if (onOpenCompatibility != null) TextButton(onClick = { onOpenCompatibility(id) }, modifier = Modifier.weight(1f)) { Text(title) }
                    else Text(title, Modifier.weight(1f))
                    TextButton(onClick = { onRemoveCompatibility(id) }) { Text("Прибрати зв’язок") }
                }
            }
        }
    }
}
