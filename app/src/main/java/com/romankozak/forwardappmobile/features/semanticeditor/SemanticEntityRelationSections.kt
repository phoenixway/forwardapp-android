package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

@Composable
internal fun AspectAssociationsSection(
    model: SemanticEntityEditorModel,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    RelationSectionHeader("Аспекти", model.isRelationMutationInFlight, onAdd)
    if (model.aspectAssociations.isEmpty()) {
        Text("Немає зв’язків", style = MaterialTheme.typography.bodyMedium)
    } else {
        model.aspectAssociations.forEach { association ->
            RelationRow(
                title = association.title,
                subtitle =
                    association.relationType.displayLabel() +
                        if (association.isPrimary) " · основний" else "",
                enabled = !model.isRelationMutationInFlight,
                onRemove = { onRemove(association.refId) },
            )
        }
    }
}

@Composable
internal fun WorkspaceBindingsSection(
    model: SemanticEntityEditorModel,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    RelationSectionHeader("Робочі простори", model.isRelationMutationInFlight, onAdd)
    if (model.workspaceBindings.isEmpty()) {
        Text("Немає зв’язків", style = MaterialTheme.typography.bodyMedium)
    } else {
        model.workspaceBindings.forEach { binding ->
            RelationRow(
                title = binding.title,
                subtitle = binding.bindingType.displayLabel() + if (binding.isPrimary) " · основний" else "",
                enabled = !model.isRelationMutationInFlight,
                onRemove = { onRemove(binding.bindingId) },
            )
        }
    }
}

@Composable
internal fun OrientationGraphRelationsSection(
    model: SemanticEntityEditorModel,
    onAdd: () -> Unit,
    onRemove: (String, Long) -> Unit,
) {
    RelationSectionHeader("Зв’язки між орієнтирами", model.isRelationMutationInFlight, onAdd)
    if (model.orientationRelations.isEmpty()) {
        Text("Немає зв’язків", style = MaterialTheme.typography.bodyMedium)
    } else {
        model.orientationRelations.forEach { relation ->
            val directionLabel =
                when {
                    relation.relationType == OrientationRelationType.CONFLICTS_WITH ->
                        "Взаємний ↔"
                    relation.direction == OrientationRelationEditDirection.OUTGOING ->
                        "Вихідний →"
                    else -> "Вхідний ←"
                }
            val subtitle =
                "$directionLabel ${relation.relationType.displayLabel()}" +
                    if (relation.isEditable) "" else " · керується ієрархією"
            RelationRow(
                title = relation.title,
                subtitle = subtitle,
                enabled = !model.isRelationMutationInFlight,
                canRemove = relation.isEditable,
                onRemove = { onRemove(relation.relationId, relation.version) },
            )
        }
    }
}

@Composable
private fun RelationSectionHeader(title: String, inFlight: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAdd, enabled = !inFlight) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Додати")
        }
    }
}

@Composable
private fun RelationRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    canRemove: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            if (canRemove) {
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити зв’язок")
                }
            } else {
                Text("Лише перегляд", style = MaterialTheme.typography.labelSmall)
            }
        },
    )
}

@Composable
internal fun SemanticRelationTypeDialog(
    pending: PendingSemanticRelation?,
    mutationInFlight: Boolean,
    onDismiss: () -> Unit,
    onAddAspect: (AspectOrientationRelationType, Boolean) -> Unit,
    onAddWorkspace: (WorkspaceBindingType) -> Unit,
    onAddOrientation: (OrientationRelationType, OrientationRelationEditDirection?) -> Unit,
) {
    when (pending) {
        is PendingSemanticRelation.Aspect ->
            AspectTypeDialog(pending.aspectId, mutationInFlight, onDismiss, onAddAspect)
        is PendingSemanticRelation.Workspace ->
            WorkspaceTypeDialog(pending.workspaceId, mutationInFlight, onDismiss, onAddWorkspace)
        is PendingSemanticRelation.Orientation ->
            OrientationTypeDialog(pending.orientationId, mutationInFlight, onDismiss, onAddOrientation)
        null -> Unit
    }
}

@Composable
private fun AspectTypeDialog(
    key: String,
    inFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AspectOrientationRelationType, Boolean) -> Unit,
) {
    var selected by remember(key) { mutableStateOf<AspectOrientationRelationType?>(null) }
    var primary by remember(key) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Тип зв’язку з Aspect") },
        text = {
            Column {
                AspectOrientationRelationType.entries.forEach { type ->
                    TypeChoice(type.displayLabel(), selected == type) {
                        selected = type
                        if (type != AspectOrientationRelationType.BELONGS_TO) primary = false
                    }
                }
                if (selected == AspectOrientationRelationType.BELONGS_TO) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = primary, onCheckedChange = { primary = it })
                        Text("Основний Aspect")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onConfirm(it, primary) } },
                enabled = selected != null && !inFlight,
            ) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inFlight) { Text("Скасувати") } },
    )
}

@Composable
private fun WorkspaceTypeDialog(
    key: String,
    inFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (WorkspaceBindingType) -> Unit,
) {
    var selected by remember(key) { mutableStateOf<WorkspaceBindingType?>(null) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Тип зв’язку з Workspace") },
        text = {
            Column {
                WorkspaceBindingType.entries.forEach { type ->
                    TypeChoice(type.displayLabel(), selected == type) { selected = type }
                }
                if (selected == WorkspaceBindingType.EMBODIES) {
                    Text("EMBODIES є основним і має бути унікальним для сутності та Workspace")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null && !inFlight,
            ) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inFlight) { Text("Скасувати") } },
    )
}

@Composable
private fun OrientationTypeDialog(
    key: String,
    inFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (OrientationRelationType, OrientationRelationEditDirection?) -> Unit,
) {
    var selected by remember(key) { mutableStateOf<OrientationRelationType?>(null) }
    var direction by remember(key) { mutableStateOf<OrientationRelationEditDirection?>(null) }
    val isMutual = selected == OrientationRelationType.CONFLICTS_WITH

    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Зв’язок між орієнтирами") },
        text = {
            Column {
                Text("Тип зв’язку", style = MaterialTheme.typography.labelLarge)
                OrientationRelationType.entries
                    .filterNot { it == OrientationRelationType.PART_OF }
                    .forEach { type ->
                        TypeChoice(type.displayLabel(), selected == type) {
                            if (!inFlight) selected = type
                        }
                    }

                if (isMutual) {
                    Text(
                        "Взаємний зв’язок. Порядок вузлів визначає canonical authority.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (selected != null) {
                    Text("Напрямок відносно поточного орієнтира")
                    TypeChoice(
                        "Вихідний: цей → вибраний",
                        direction == OrientationRelationEditDirection.OUTGOING,
                    ) {
                        if (!inFlight) direction = OrientationRelationEditDirection.OUTGOING
                    }
                    TypeChoice(
                        "Вхідний: вибраний → цей",
                        direction == OrientationRelationEditDirection.INCOMING,
                    ) {
                        if (!inFlight) direction = OrientationRelationEditDirection.INCOMING
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selected?.let { type ->
                        if (type == OrientationRelationType.CONFLICTS_WITH || direction != null) {
                            onConfirm(type, if (type == OrientationRelationType.CONFLICTS_WITH) null else direction)
                        }
                    }
                },
                enabled = selected != null && (isMutual || direction != null) && !inFlight,
            ) { Text("Додати") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inFlight) {
                Text("Скасувати")
            }
        },
    )
}

@Composable
private fun TypeChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}

private fun OrientationRelationType.displayLabel() =
    when (this) {
        OrientationRelationType.PART_OF -> "Є частиною"
        OrientationRelationType.SUPPORTS -> "Підтримує"
        OrientationRelationType.REALIZES -> "Реалізує"
        OrientationRelationType.DEPENDS_ON -> "Залежить від"
        OrientationRelationType.CONFLICTS_WITH -> "Конфліктує з"
        OrientationRelationType.PRECEDES -> "Передує"
        OrientationRelationType.REFINES -> "Уточнює"
        OrientationRelationType.DERIVED_FROM -> "Походить від"
    }

private fun AspectOrientationRelationType.displayLabel() =
    when (this) {
        AspectOrientationRelationType.BELONGS_TO -> "Належить до"
        AspectOrientationRelationType.RELEVANT_TO -> "Стосується"
    }

private fun WorkspaceBindingType.displayLabel() =
    when (this) {
        WorkspaceBindingType.EMBODIES -> "Втілює"
        WorkspaceBindingType.REALIZES -> "Реалізує"
        WorkspaceBindingType.SUPPORTS -> "Підтримує"
        WorkspaceBindingType.MONITORS -> "Моніторить"
    }
