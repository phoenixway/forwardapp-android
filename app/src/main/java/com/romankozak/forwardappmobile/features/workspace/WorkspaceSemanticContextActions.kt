package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.navigation.NavTarget

internal data class WorkspaceSemanticEditAction(val label: String, val explanation: String, val destination: NavTarget.SemanticEntityEditor)

/** Delegates to the owning semantic editor, never to Workspace Properties or Navigation. */
internal fun WorkspaceRelationsProjection.semanticContextEditAction(): WorkspaceSemanticEditAction? {
    if (embodiedAspect != null && embodiedOrientation != null) return null
    embodiedAspect?.let { return WorkspaceSemanticEditAction(
        "Редагувати таксономію та зв’язки Aspect «${it.title}»",
        "Відкриється семантичний редактор Aspect. Розташування Workspace у Navigation не змінюється.",
        NavTarget.SemanticEntityEditor(it.aspectId)) }
    embodiedOrientation?.let { return WorkspaceSemanticEditAction(
        "Редагувати належність і зв’язки Orientation «${it.title}»",
        "Відкриється семантичний редактор Orientation. BELONGS_TO не змінює розташування Workspace у Navigation.",
        NavTarget.SemanticEntityEditor(it.orientationId)) }
    return null
}

@Composable
internal fun WorkspaceSemanticContextActions(projection: WorkspaceRelationsProjection?, onOpen: (NavTarget) -> Unit, compact: Boolean = false) {
    val action = projection?.semanticContextEditAction() ?: return
    Column {
        OutlinedButton(onClick = { onOpen(action.destination) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Text(action.label, modifier = Modifier.padding(start = 8.dp))
        }
        if (!compact) Text(action.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
