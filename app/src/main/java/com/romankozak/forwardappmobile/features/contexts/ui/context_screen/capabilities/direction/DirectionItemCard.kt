package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.direction

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity

@Composable
fun DirectionItemCard(
    state: DirectionItemCardState,
    actions: DirectionItemCardActions,
) {
    val linkedContextId = state.item.linkedContextId
    val actionButtonSize = 34.dp
    val actionIconSize = 18.dp
    val elevation by animateDpAsState(
        targetValue = if (state.isDragging) 6.dp else 0.dp,
        label = "directionElevation",
    )
    val cardModifier = rememberDirectionCardModifier(linkedContextId, actions)

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragIndicator,
                contentDescription = "Reorder direction",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = state.dragHandleModifier,
            )
            Spacer(modifier = Modifier.size(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                DirectionItemCardContent(
                    itemText = state.item.text,
                    linkedContextId = linkedContextId,
                    actionButtonSize = actionButtonSize,
                    actionIconSize = actionIconSize,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun DirectionItemCardContent(
    itemText: String,
    linkedContextId: String?,
    actionButtonSize: androidx.compose.ui.unit.Dp,
    actionIconSize: androidx.compose.ui.unit.Dp,
    actions: DirectionItemCardActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = itemText,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    DirectionItemCardActionsRow(
        linkedContextId = linkedContextId,
        actionButtonSize = actionButtonSize,
        actionIconSize = actionIconSize,
        actions = actions,
    )
}

@Composable
private fun DirectionItemCardActionsRow(
    linkedContextId: String?,
    actionButtonSize: androidx.compose.ui.unit.Dp,
    actionIconSize: androidx.compose.ui.unit.Dp,
    actions: DirectionItemCardActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionItemActionButton(
            onClick = actions.onEdit,
            buttonState =
                DirectionItemActionButtonState(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit direction",
                    actionButtonSize = actionButtonSize,
                    actionIconSize = actionIconSize,
                    tint = MaterialTheme.colorScheme.primary,
                ),
        )

        if (linkedContextId == null) {
            DirectionItemActionButton(
                onClick = actions.onToggleLink,
                buttonState =
                    DirectionItemActionButtonState(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Link direction",
                        actionButtonSize = actionButtonSize,
                        actionIconSize = actionIconSize,
                        tint = MaterialTheme.colorScheme.primary,
                    ),
            )
        }

        DirectionItemActionButton(
            onClick = actions.onCopy,
            buttonState =
                DirectionItemActionButtonState(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy direction",
                    actionButtonSize = actionButtonSize,
                    actionIconSize = actionIconSize,
                    tint = MaterialTheme.colorScheme.primary,
                ),
        )
        DirectionItemActionButton(
            onClick = actions.onCut,
            buttonState =
                DirectionItemActionButtonState(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Cut direction",
                    actionButtonSize = actionButtonSize,
                    actionIconSize = actionIconSize,
                    tint = MaterialTheme.colorScheme.primary,
                ),
        )
        DirectionItemActionButton(
            onClick = actions.onDelete,
            buttonState =
                DirectionItemActionButtonState(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete direction",
                    actionButtonSize = actionButtonSize,
                    actionIconSize = actionIconSize,
                    tint = MaterialTheme.colorScheme.error,
                ),
        )
    }
}

@Composable
private fun DirectionItemActionButton(
    onClick: () -> Unit,
    buttonState: DirectionItemActionButtonState,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonState.actionButtonSize),
        colors = IconButtonDefaults.iconButtonColors(contentColor = buttonState.tint),
    ) {
        Icon(
            imageVector = buttonState.imageVector,
            contentDescription = buttonState.contentDescription,
            modifier = Modifier.size(buttonState.actionIconSize),
        )
    }
}

private fun rememberDirectionCardModifier(
    linkedContextId: String?,
    actions: DirectionItemCardActions,
): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .then(
            if (linkedContextId != null) {
                Modifier.clickable { actions.onOpenLinkedContext(linkedContextId) }
            } else {
                Modifier
            },
        )

data class DirectionItemCardState(
    val item: DirectionItemEntity,
    val isDragging: Boolean,
    val dragHandleModifier: Modifier,
)

data class DirectionItemCardActions(
    val onEdit: () -> Unit,
    val onToggleLink: () -> Unit,
    val onDelete: () -> Unit,
    val onOpenLinkedContext: (String) -> Unit,
    val onCopy: () -> Unit,
    val onCut: () -> Unit,
)

private data class DirectionItemActionButtonState(
    val imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val actionButtonSize: androidx.compose.ui.unit.Dp,
    val actionIconSize: androidx.compose.ui.unit.Dp,
    val tint: androidx.compose.ui.graphics.Color,
)
