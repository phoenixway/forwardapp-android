package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AspectChildrenSection(model: SemanticEntityEditorModel, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Дочірні аспекти (${model.aspectChildren.size})", style = MaterialTheme.typography.titleMedium)
        if (model.aspectChildren.isEmpty()) Text("Немає дочірніх аспектів",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        model.aspectChildren.forEach { child ->
            ListItem(
                headlineContent = { Text(child.title) },
                supportingContent = { Text("Дочірній Aspect") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable(enabled = !model.isSaving && !model.isRelationMutationInFlight) {
                    onOpen(child.destination().subjectId)
                },
            )
        }
    }
}
