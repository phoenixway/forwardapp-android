// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/globalsearch/components/LinkSearchResultItem.kt

package com.romankozak.forwardappmobile.ui.screens.globalsearch.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.data.database.models.LinkType

@Composable
fun LinkSearchResultItem(
    result: GlobalLinkSearchResult,
    onClick: () -> Unit,
    onOpenInObsidian: () -> Unit,
    onGoToTargetList: () -> Unit,
    onOpenUrl: () -> Unit,
) {
    // --- ОНОВЛЕНО: Модернізована логіка анімації натискання ---
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale_animation"
    )

    val linkType = result.link.linkData.type
    val (icon, colors, actionHandler, actionIcon, actionDescription) = getLinkDisplayData(
        linkType = linkType,
        onOpenInObsidian = onOpenInObsidian,
        onGoToTargetList = onGoToTargetList,
        onOpenUrl = onOpenUrl
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            // --- ОНОВЛЕНО: Уніфіковані відступи ---
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.container.copy(alpha = 0.5f),
                                colors.container.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Іконка посилання",
                    tint = colors.onContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.link.linkData.displayName ?: result.link.linkData.target,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = "Список",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = result.listName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getLinkTypeLabel(linkType),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (actionHandler != null && actionIcon != null) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = actionHandler,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.container.copy(alpha = 0.8f),
                        contentColor = colors.onContainer
                    )
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getLinkDisplayData(
    linkType: LinkType,
    onOpenInObsidian: () -> Unit,
    onGoToTargetList: () -> Unit,
    onOpenUrl: () -> Unit
): LinkDisplayData {
    return when (linkType) {
        LinkType.URL -> LinkDisplayData(
            icon = Icons.Default.Language,
            colors = LinkColors(
                primary = MaterialTheme.colorScheme.tertiary,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            actionHandler = onOpenUrl,
            actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
            actionDescription = "Відкрити посилання"
        )
        LinkType.OBSIDIAN -> LinkDisplayData(
            icon = Icons.Default.Link, // Можна замінити на кастомну іконку Obsidian
            colors = LinkColors(
                primary = MaterialTheme.colorScheme.secondary,
                container = MaterialTheme.colorScheme.secondaryContainer,
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            actionHandler = onOpenInObsidian,
            actionIcon = Icons.AutoMirrored.Filled.Note,
            actionDescription = "Відкрити в Obsidian"
        )
        LinkType.GOAL_LIST -> LinkDisplayData(
            icon = Icons.AutoMirrored.Filled.ListAlt,
            colors = LinkColors(
                primary = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer,
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actionHandler = onGoToTargetList,
            actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
            actionDescription = "Перейти до списку"
        )
    }
}

private fun getLinkTypeLabel(linkType: LinkType): String {
    return when (linkType) {
        LinkType.URL -> "Attachment: web-url"
        LinkType.OBSIDIAN -> "Attachment: Obsidian note"
        LinkType.GOAL_LIST -> "Attachment: link to project"
    }
}

private data class LinkDisplayData(
    val icon: ImageVector,
    val colors: LinkColors,
    val actionHandler: (() -> Unit)?,
    val actionIcon: ImageVector?,
    val actionDescription: String?
)

private data class LinkColors(
    val primary: Color,
    val container: Color,
    val onContainer: Color
)