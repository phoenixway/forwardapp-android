package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ScopeLinkItem(
    val id: String,
    val title: String,
)

@Composable
fun ScreenScopeLinksPanel(
    title: String,
    contextLinks: List<ScopeLinkItem>,
    attachmentLinks: List<ScopeLinkItem>,
    onAddContextClick: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContextRemove: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var contextsExpanded by remember { mutableStateOf(true) }
    var attachmentsExpanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))

        ScopeLinkGroupCard(
            groupTitle = "Контексти",
            emptyText = "Додай контекст до екрана",
            links = contextLinks,
            onAddClick = onAddContextClick,
            onOpenClick = onContextClick,
            onRemoveClick = onContextRemove,
            expanded = contextsExpanded,
            onToggleExpanded = { contextsExpanded = !contextsExpanded },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        ScopeLinkGroupCard(
            groupTitle = "Вкладення",
            emptyText = "Додай вкладення до екрана",
            links = attachmentLinks,
            onAddClick = onAddAttachmentClick,
            onOpenClick = onAttachmentClick,
            onRemoveClick = onAttachmentRemove,
            expanded = attachmentsExpanded,
            onToggleExpanded = { attachmentsExpanded = !attachmentsExpanded },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ScopeLinkGroupCard(
    groupTitle: String,
    emptyText: String,
    links: List<ScopeLinkItem>,
    onAddClick: () -> Unit,
    onOpenClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verticalPadding = if (expanded) 10.dp else 6.dp
    val itemSpacing = if (expanded) 8.dp else 0.dp

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = groupTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (!expanded) {
                    Text(
                        text = "${links.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    )
                }
                FilledTonalIconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Додати")
                }
            }

            if (expanded && links.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (expanded) {
                links.forEach { link ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenClick(link.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { onOpenClick(link.id) }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Відкрити",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onRemoveClick(link.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Видалити",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
