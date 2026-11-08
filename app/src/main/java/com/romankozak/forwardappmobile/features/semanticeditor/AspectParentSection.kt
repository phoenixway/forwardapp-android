package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun AspectParentSection(
    model: SemanticEntityEditorModel,
    onChoose: () -> Unit,
    onMakeRoot: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val enabled = !model.isSaving && !model.isRelationMutationInFlight
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RelationSectionHeader("Батьківський аспект", !enabled, onChoose,
            actionLabel = if (model.aspectParentId == null) "Вибрати" else "Змінити")
        val parentId = model.aspectParentId
        if (parentId == null) Text("Кореневий аспект", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            RelationRow(model.aspectTitles[parentId] ?: "Батьківський аспект недоступний",
                "Містить цей аспект", enabled, {}, canRemove = false,
                onOpen = if (parentId in model.aspectTitles) ({ onOpen(parentId) }) else null, showReadOnly = false)
            TextButton(onClick = onMakeRoot, enabled = enabled) { Text("Зробити кореневим") }
        }
    }
}

@Composable
internal fun AspectParentConfirmation(
    model: SemanticEntityEditorModel,
    pending: PendingSemanticRelation.AspectParent,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val enabled = !model.isSaving && !model.isRelationMutationInFlight
    val parentId = pending.aspectId
    val validParent = parentId == null || (parentId in model.aspectTitles && parentId !in model.aspectParentExcludedIds)
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text(if (parentId == null) "Зробити Aspect кореневим?" else "Змінити батьківський Aspect?") },
        text = { Text(buildString {
            if (parentId == null) append("«${model.name}» більше не матиме батьківського Aspect.")
            else append("«${model.name}» стане дочірнім аспектом «${model.aspectTitles[parentId] ?: "Недоступний Aspect"}».")
            append("\n\nЙого нащадки збережуться. Розміщення в Navigation не зміняться.")
        }) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = enabled && validParent) { Text("Підтвердити") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text("Скасувати") } },
    )
}
