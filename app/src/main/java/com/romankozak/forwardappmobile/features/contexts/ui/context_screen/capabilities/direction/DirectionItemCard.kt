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
    item: DirectionItemEntity,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onEdit: () -> Unit,
    onToggleLink: () -> Unit,
    onDelete: () -> Unit,
    onOpenLinkedContext: (String) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
) {
    val linkedContextId = item.linkedContextId
    val actionButtonSize = 34.dp
    val actionIconSize = 18.dp
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 0.dp,
        label = "directionElevation",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .then(
                    if (linkedContextId != null) {
                        Modifier.clickable { onOpenLinkedContext(linkedContextId) }
                    } else {
                        Modifier
                    },
                ),
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
                modifier = dragHandleModifier,
            )
            Spacer(modifier = Modifier.size(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(actionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit direction",
                            modifier = Modifier.size(actionIconSize),
                        )
                    }

                    if (linkedContextId == null) {
                        IconButton(
                            onClick = onToggleLink,
                            modifier = Modifier.size(actionButtonSize),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = "Link direction",
                                modifier = Modifier.size(actionIconSize),
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(actionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy direction",
                            modifier = Modifier.size(actionIconSize),
                        )
                    }

                    IconButton(
                        onClick = onCut,
                        modifier = Modifier.size(actionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Cut direction",
                            modifier = Modifier.size(actionIconSize),
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(actionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete direction",
                            modifier = Modifier.size(actionIconSize),
                        )
                    }
                }
            }
        }
    }
}
