package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

@Composable
internal fun AspectAssociationsSection(
    model: SemanticEntityEditorModel,
    onAdd: (() -> Unit)?,
    onRemove: (String) -> Unit,
    onOpen: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RelationSectionHeader("Аспекти", model.isRelationMutationInFlight || model.isSaving, onAdd, model.aspectAssociations.size)
        if (model.aspectAssociations.isEmpty()) {
            Text("Немає зв’язків", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            model.aspectAssociations.forEach { association ->
                RelationRow(
                    title = association.title,
                    subtitle = (if (association.relationType == AspectOrientationRelationType.BELONGS_TO)
                        "Класифікує цей орієнтир" else "Стосується цього орієнтира") + if (association.isPrimary) " · основний" else "",
                    enabled = !model.isRelationMutationInFlight && !model.isSaving,
                    onRemove = { onRemove(association.refId) },
                    onOpen = { onOpen(association.aspectId) },
                )
            }
        }
    }
}

@Composable
internal fun WorkspaceBindingsSection(
    model: SemanticEntityEditorModel,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onOpen: (WorkspaceBindingItem) -> Unit,
    addLabel: String = "Додати",
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RelationSectionHeader("Робочі простори", model.isRelationMutationInFlight || model.isSaving, onAdd, model.workspaceBindings.size,
            actionLabel = addLabel)
        if (model.workspaceBindings.isEmpty()) {
            Text("Немає зв’язків", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            model.workspaceBindings.forEach { binding ->
                RelationRow(
                    title = binding.title,
                    subtitle = binding.bindingType.compactCaption() + if (binding.isPrimary) " · основний" else "",
                    enabled = !model.isRelationMutationInFlight && !model.isSaving,
                    onRemove = { onRemove(binding.bindingId) },
                    onOpen = { onOpen(binding) },
                )
            }
        }
    }
}

@Composable
internal fun OrientationGraphRelationsSection(
    model: SemanticEntityEditorModel,
    onAdd: (() -> Unit)?,
    onRemove: (String, Long) -> Unit,
    onOpen: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RelationSectionHeader("Зв’язки між орієнтирами", model.isRelationMutationInFlight || model.isSaving, onAdd, model.orientationRelations.size)
        if (model.orientationRelations.isEmpty()) {
            Text("Немає зв’язків", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            model.orientationRelations.forEach { relation ->
                RelationRow(
                    title = relation.title,
                    subtitle = relation.compactCaption(),
                    enabled = !model.isRelationMutationInFlight && !model.isSaving,
                    canRemove = relation.isEditable,
                    onRemove = { onRemove(relation.relationId, relation.version) },
                    onOpen = { onOpen(relation.otherOrientationId) },
                )
            }
        }
    }
}

@Composable
internal fun SemanticRelationTypeDialog(
    pending: PendingSemanticRelation?,
    mutationInFlight: Boolean,
    onDismiss: () -> Unit,
    onAddAspect: (AspectOrientationRelationType, Boolean) -> Unit,
    onAddWorkspace: (WorkspaceBindingType) -> Unit,
    onAddOrientation: (OrientationRelationType, OrientationRelationEditDirection?) -> Unit,
    currentTitle: String = "Поточний орієнтир",
    relatedTitles: Map<CanonicalEntityRef, String> = emptyMap(),
) {
    when (pending) {
        is PendingSemanticRelation.Aspect ->
            AspectTypeDialog(pending.aspectId, mutationInFlight, onDismiss, onAddAspect,
                currentTitle, relatedTitles[CanonicalEntityRef.Aspect(pending.aspectId)] ?: "Вибраний аспект")
        is PendingSemanticRelation.AspectOrientation ->
            AspectTypeDialog("orientation:${pending.orientationId}", mutationInFlight, onDismiss, onAddAspect,
                relatedTitles[CanonicalEntityRef.Orientation(pending.orientationId)] ?: "Вибраний орієнтир", currentTitle)
        is PendingSemanticRelation.Workspace ->
            WorkspaceTypeDialog(pending.workspaceId, mutationInFlight, onDismiss, onAddWorkspace,
                relatedTitles[CanonicalEntityRef.Workspace(pending.workspaceId)] ?: "Вибраний робочий простір", currentTitle)
        is PendingSemanticRelation.Orientation ->
            OrientationTypeDialog(pending.orientationId, mutationInFlight, onDismiss, onAddOrientation,
                currentTitle, relatedTitles[CanonicalEntityRef.Orientation(pending.orientationId)] ?: "Вибраний орієнтир")
        is PendingSemanticRelation.AspectParent, null -> Unit
    }
}

@Composable
private fun AspectTypeDialog(
    key: String,
    inFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AspectOrientationRelationType, Boolean) -> Unit,
    orientationTitle: String,
    aspectTitle: String,
) {
    var selected by remember(key) { mutableStateOf<AspectOrientationRelationType?>(null) }
    var primary by remember(key) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Зв’язок орієнтира з аспектом") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                AspectOrientationRelationType.entries.forEach { type ->
                    TypeChoice(type.displayLabel(), selected == type) {
                        selected = type
                        if (type != AspectOrientationRelationType.BELONGS_TO) primary = false
                    }
                }
                selected?.let { type ->
                    Text(aspectAssociationSentence(type, orientationTitle, aspectTitle), style = MaterialTheme.typography.bodyMedium)
                    Text(if (type == AspectOrientationRelationType.BELONGS_TO) "Класифікує орієнтир у цьому аспекті." else
                        "Має відношення до аспекту, але не обов’язково належить до нього.", style = MaterialTheme.typography.bodySmall)
                }
                if (selected == AspectOrientationRelationType.BELONGS_TO) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = primary, onCheckedChange = { primary = it })
                        Text("Основний аспект для класифікації")
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
    workspaceTitle: String,
    subjectTitle: String,
) {
    var selected by remember(key) { mutableStateOf<WorkspaceBindingType?>(null) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Як робочий простір пов’язаний із сутністю?") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                WorkspaceBindingType.entries.forEach { type ->
                    TypeChoice(type.displayLabel(), selected == type) { selected = type }
                }
                selected?.let { type ->
                    Text(workspaceBindingSentence(type, workspaceTitle, subjectTitle), style = MaterialTheme.typography.bodyMedium)
                    Text(type.explanation(), style = MaterialTheme.typography.bodySmall)
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
    currentTitle: String,
    selectedTitle: String,
) {
    var selected by remember(key) { mutableStateOf<OrientationRelationType?>(null) }
    var direction by remember(key) { mutableStateOf<OrientationRelationEditDirection?>(null) }
    val isMutual = selected == OrientationRelationType.CONFLICTS_WITH

    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Зв’язок між орієнтирами") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text("Тип зв’язку", style = MaterialTheme.typography.labelLarge)
                OrientationRelationType.entries
                    .filterNot { it == OrientationRelationType.PART_OF }
                    .forEach { type ->
                        TypeChoice(type.displayLabel(), selected == type) {
                            if (!inFlight) selected = type
                        }
                    }

                selected?.let { Text(it.explanation(), style = MaterialTheme.typography.bodySmall) }
                if (isMutual) {
                    Text(
                        orientationRelationSentence(requireNotNull(selected), currentTitle, selectedTitle),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (selected != null) {
                    Text("Яке речення описує ваш задум?", style = MaterialTheme.typography.labelLarge)
                    TypeChoice(
                        orientationRelationSentence(requireNotNull(selected), currentTitle, selectedTitle),
                        direction == OrientationRelationEditDirection.OUTGOING,
                    ) {
                        if (!inFlight) direction = OrientationRelationEditDirection.OUTGOING
                    }
                    TypeChoice(
                        orientationRelationSentence(requireNotNull(selected), currentTitle, selectedTitle, OrientationRelationEditDirection.INCOMING),
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

internal fun AspectOrientationRelationType.displayLabel() =
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
