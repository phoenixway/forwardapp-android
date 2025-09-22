package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

@Composable
internal fun LinkItem(
    link: RelatedLink,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color =
            when (link.type) {
                LinkType.PROJECT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                LinkType.URL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                null -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            },
        border =
            BorderStroke(
                1.dp,
                when (link.type) {
                    LinkType.PROJECT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    LinkType.URL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    null -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector =
                        when (link.type) {
                            LinkType.PROJECT -> Icons.AutoMirrored.Filled.List
                            LinkType.URL -> Icons.Default.Language
                            LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                            null -> Icons.AutoMirrored.Filled.Note
                        },
                    contentDescription = null,
                    tint =
                        when (link.type) {
                            LinkType.PROJECT -> MaterialTheme.colorScheme.primary
                            LinkType.URL -> MaterialTheme.colorScheme.secondary
                            LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary
                            null -> MaterialTheme.colorScheme.tertiary
                        },
                    modifier = Modifier.size(20.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.displayName ?: link.target,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text =
                            when (link.type) {
                                LinkType.PROJECT -> "Проект"
                                LinkType.URL -> "Веб-посилання"
                                LinkType.OBSIDIAN -> "Obsidian нотатка"
                                null -> "broken"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Видалити посилання",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
