package com.romankozak.forwardappmobile.features.globalsearch.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType

@Composable
fun LinkSearchResultItem(
    result: GlobalLinkSearchResult,
    onClick: () -> Unit,
    onOpenInObsidian: () -> Unit,
    onGoToTargetProject: () -> Unit,
    onOpenUrl: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val linkType = result.link.linkData.type
    val displayData =
        getLinkDisplayData(
            linkType = linkType,
            onOpenInObsidian = onOpenInObsidian,
            onGoToTargetProject = onGoToTargetProject,
            onOpenUrl = onOpenUrl,
        )
    val scale = rememberLinkCardScale(interactionSource = interactionSource)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        LinkResultCardContent(
            result = result,
            linkType = linkType,
            displayData = displayData,
        )
    }
}

@Composable
private fun rememberLinkCardScale(interactionSource: MutableInteractionSource): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale_animation",
    )
    return scale
}

@Composable
private fun LinkResultCardContent(
    result: GlobalLinkSearchResult,
    linkType: LinkType?,
    displayData: LinkDisplayData,
) {
    Row(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        displayData.colors.container.copy(alpha = 0.5f),
                                        displayData.colors.container.copy(alpha = 0.3f),
                                    ),
                            ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            LinkResultLeadingIcon(displayData = displayData)
        }

        Spacer(modifier = Modifier.width(16.dp))

        LinkResultContent(
            result = result,
            linkType = linkType,
            displayData = displayData,
        )
        LinkResultActionButton(displayData = displayData)
    }
}

@Composable
private fun LinkResultLeadingIcon(displayData: LinkDisplayData) {
    Icon(
        imageVector = displayData.icon,
        contentDescription = "Іконка посилання",
        tint = displayData.colors.onContainer,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun RowScope.LinkResultContent(
    result: GlobalLinkSearchResult,
    linkType: LinkType?,
    displayData: LinkDisplayData,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = result.link.linkData.displayName ?: result.link.linkData.target,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))
        LinkResultProjectMeta(contextName = result.contextName)
        Spacer(modifier = Modifier.height(8.dp))
        LinkResultTypeChip(
            label = getLinkTypeLabel(linkType),
            color = displayData.colors.primary,
        )
    }
}

@Composable
private fun LinkResultProjectMeta(contextName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ListAlt,
            contentDescription = "Проект",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = contextName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LinkResultTypeChip(
    label: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LinkResultActionButton(displayData: LinkDisplayData) {
    if (displayData.actionHandler != null && displayData.actionIcon != null) {
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = displayData.actionHandler,
            modifier = Modifier.size(40.dp),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = displayData.colors.container.copy(alpha = 0.8f),
                    contentColor = displayData.colors.onContainer,
                ),
        ) {
            Icon(
                imageVector = displayData.actionIcon,
                contentDescription = displayData.actionDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun getLinkDisplayData(
    linkType: LinkType?,
    onOpenInObsidian: () -> Unit,
    onGoToTargetProject: () -> Unit,
    onOpenUrl: () -> Unit,
): LinkDisplayData =
    when (linkType) {
        LinkType.NOTE_DOCUMENT,
        LinkType.JOURNAL_DOCUMENT ->
            LinkDisplayData(
                icon = Icons.AutoMirrored.Filled.Note,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.tertiary,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                actionHandler = onGoToTargetProject,
                actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                actionDescription = "Відкрити документ",
            )
        LinkType.CHECKLIST ->
            LinkDisplayData(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.tertiary,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                actionHandler = onGoToTargetProject,
                actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                actionDescription = "Відкрити чекліст",
            )
        LinkType.MUSIC_NOTE ->
            LinkDisplayData(
                icon = Icons.AutoMirrored.Filled.Note,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.tertiary,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                actionHandler = onGoToTargetProject,
                actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                actionDescription = "Відкрити музичні ноти",
            )
        LinkType.URL ->
            LinkDisplayData(
                icon = Icons.Default.Language,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.tertiary,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                actionHandler = onOpenUrl,
                actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                actionDescription = "Відкрити посилання",
            )
        LinkType.OBSIDIAN ->
            LinkDisplayData(
                icon = Icons.Default.Link,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.secondary,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                actionHandler = onOpenInObsidian,
                actionIcon = Icons.AutoMirrored.Filled.Note,
                actionDescription = "Відкрити в Obsidian",
            )
        LinkType.CONTEXT ->
            LinkDisplayData(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.primary,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                actionHandler = onGoToTargetProject,
                actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                actionDescription = "Перейти до проекту",
            )
        null ->
            LinkDisplayData(
                icon = Icons.Default.BrokenImage,
                colors =
                    LinkColors(
                        primary = MaterialTheme.colorScheme.outline,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                actionHandler = null,
                actionIcon = null,
                actionDescription = "Invalid link",
            )
    }

private fun getLinkTypeLabel(linkType: LinkType?): String =
    when (linkType) {
        LinkType.URL -> "Attachment: web-url"
        LinkType.OBSIDIAN -> "Attachment: Obsidian note"
        LinkType.CONTEXT -> "Attachment: link to project"
        LinkType.NOTE_DOCUMENT -> "Attachment: note document"
        LinkType.JOURNAL_DOCUMENT -> "Attachment: journal document"
        LinkType.CHECKLIST -> "Attachment: checklist"
        LinkType.MUSIC_NOTE -> "Attachment: music note"
        null -> "Attachment: Unknown"
    }

private data class LinkDisplayData(
    val icon: ImageVector,
    val colors: LinkColors,
    val actionHandler: (() -> Unit)?,
    val actionIcon: ImageVector?,
    val actionDescription: String?,
)

private data class LinkColors(
    val primary: Color,
    val container: Color,
    val onContainer: Color,
)
