package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Батьківський аспект", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onChoose, enabled = enabled) {
                Text(if (model.aspectParentId == null) "Вибрати" else "Змінити")
            }
        }
        val parentId = model.aspectParentId
        if (parentId == null) Text("Кореневий Aspect", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            ListItem(
                headlineContent = { Text(model.aspectTitles[parentId] ?: "Батьківський Aspect недоступний") },
                supportingContent = { Text("Цей Aspect є дочірнім") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable(enabled = enabled && parentId in model.aspectTitles) { onOpen(parentId) },
            )
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
