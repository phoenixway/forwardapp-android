package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RelationSectionHeader("Пов’язані орієнтири", model.isRelationMutationInFlight, null, model.orientationAssociations.size)
            if (model.orientationAssociations.isEmpty()) Text("Немає зв’язків", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            model.orientationAssociations.forEach { association ->
                RelationRow(
                    title = association.title,
                    subtitle = (if (association.relationType == AspectOrientationRelationType.BELONGS_TO)
                        "Належить до цього аспекту" else "Стосується цього аспекту") +
                        if (association.isPrimary) " · основний аспект" else "",
                    enabled = !model.isSaving && !model.isRelationMutationInFlight,
                    onRemove = { onRemoveAssociation(association.refId) },
                    onOpen = { onOpen(association.orientationId) },
                )
            }
        }
    }
}
