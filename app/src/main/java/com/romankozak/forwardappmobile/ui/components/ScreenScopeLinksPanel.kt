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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))

        ScopeLinkGroupCard(
            groupTitle = "Контексти",
            emptyText = "Додай контекст до екрана",
            links = contextLinks,
            onAddClick = onAddContextClick,
            onOpenClick = onContextClick,
            onRemoveClick = onContextRemove,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        ScopeLinkGroupCard(
            groupTitle = "Вкладення",
            emptyText = "Додай вкладення до екрана",
            links = attachmentLinks,
            onAddClick = onAddAttachmentClick,
            onOpenClick = onAttachmentClick,
            onRemoveClick = onAttachmentRemove,
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
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                FilledTonalIconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Додати")
                }
            }

            if (links.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
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
