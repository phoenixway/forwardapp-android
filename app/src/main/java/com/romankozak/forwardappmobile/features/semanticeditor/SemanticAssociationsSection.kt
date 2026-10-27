package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass

/** One picker entry; endpoint types select existing domain relation families, never a generic edge. */
@Composable
internal fun SemanticAssociationsSection(
    model: SemanticEntityEditorModel,
    onAdd: () -> Unit,
    onRemoveAssociation: (String) -> Unit,
    onRemoveGraphRelation: (String, Long) -> Unit,
    onOpen: (String) -> Unit,
) {
    OutlinedButton(onClick = onAdd, enabled = !model.isSaving && !model.isRelationMutationInFlight) {
        Icon(Icons.Default.Add, null)
        Text("Додати семантичну сутність")
    }
    if (model.canEditAspects) AspectAssociationsSection(model, null, onRemoveAssociation, onOpen)
    if (model.canEditOrientationRelations) OrientationGraphRelationsSection(model, null, onRemoveGraphRelation, onOpen)
    if (model.entityClass == SemanticEntityClass.ASPECT) {
        RelationSectionHeader("Пов’язані орієнтири", model.isRelationMutationInFlight, null)
        if (model.orientationAssociations.isEmpty()) Text("Немає зв’язків", style = MaterialTheme.typography.bodyMedium)
        model.orientationAssociations.forEach { association ->
            RelationRow(
                title = association.title,
                subtitle = "Орієнтир → цей Aspect · ${association.relationType.displayLabel()}" +
                    if (association.isPrimary) " · основний Aspect" else "",
                enabled = !model.isSaving && !model.isRelationMutationInFlight,
                onRemove = { onRemoveAssociation(association.refId) },
                onOpen = { onOpen(association.orientationId) },
            )
        }
    }
}
