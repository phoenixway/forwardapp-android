package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun AspectChildrenSection(model: SemanticEntityEditorModel, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RelationSectionHeader("Дочірні аспекти", model.isSaving, null, model.aspectChildren.size)
        if (model.aspectChildren.isEmpty()) Text("Немає дочірніх аспектів",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        model.aspectChildren.forEach { child ->
            RelationRow(child.title, "Дочірній аспект", !model.isSaving && !model.isRelationMutationInFlight,
                {}, canRemove = false, onOpen = { onOpen(child.destination().subjectId) }, showReadOnly = false)
        }
    }
}
