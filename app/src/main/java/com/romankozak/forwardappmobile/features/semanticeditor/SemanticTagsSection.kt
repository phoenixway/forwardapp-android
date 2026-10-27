package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType

@Composable
internal fun SemanticTagsSection(model: SemanticEntityEditorModel, controller: SemanticTagsViewModel = hiltViewModel()) {
    val subject = ManagedSubjectRef(when (model.entityClass) {
        SemanticEntityClass.ORIENTATION -> ManagedSubjectType.ORIENTATION
        SemanticEntityClass.ASPECT -> ManagedSubjectType.ASPECT
    }, model.id)
    LaunchedEffect(subject) { controller.show(subject) }
    val tags by controller.tags.collectAsStateWithLifecycle()
    val editing by controller.editing.collectAsStateWithLifecycle()
    val enabled = !editing.busy && !model.isSaving && !model.isRelationMutationInFlight
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Теги", style = MaterialTheme.typography.titleMedium)
        Text("Зміни тегів зберігаються одразу.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.sorted().forEach { tag ->
                InputChip(selected = true, enabled = enabled, onClick = { controller.remove(tag) }, label = { Text("#$tag") },
                    trailingIcon = { Icon(Icons.Default.Close, "Прибрати тег $tag", Modifier.size(16.dp)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = editing.input, onValueChange = controller::setInput, enabled = enabled,
                label = { Text("Новий тег") }, singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = controller::add, enabled = enabled && editing.input.isNotBlank()) { Text("Додати") }
        }
        editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
