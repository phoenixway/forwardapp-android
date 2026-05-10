package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink

@Composable
internal fun EnhancedRelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val chipColors = resolveRelatedLinkChipColors(link)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    Surface(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() },
                    )
                }
                .semantics {
                    contentDescription = "${link.type?.name ?: "LINK"}: ${link.displayName ?: link.target}"
                    role = Role.Button
                }
                .height(24.dp)
                .heightIn(min = 24.dp),
        shape = RoundedCornerShape(10.dp),
        color = chipColors.backgroundColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        EnhancedRelatedLinkChipContent(link = link, contentColor = chipColors.contentColor)
    }
}

@Composable
private fun resolveRelatedLinkChipColors(link: RelatedLink): RelatedLinkChipColors {
    val isContextLink = link.type == LinkType.CONTEXT
    return if (isContextLink) {
        RelatedLinkChipColors(
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.secondary,
        )
    } else if (link.type == LinkType.NOTE_DOCUMENT || link.type == LinkType.JOURNAL_DOCUMENT || link.type == LinkType.CHECKLIST || link.type == LinkType.MUSIC_NOTE) {
        RelatedLinkChipColors(
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
            contentColor = MaterialTheme.colorScheme.tertiary,
        )
    } else {
        RelatedLinkChipColors(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EnhancedRelatedLinkChipContent(
    link: RelatedLink,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector =
                when (link.type) {
                    LinkType.CONTEXT -> Icons.Default.AccountTree
                    LinkType.NOTE_DOCUMENT -> Icons.Default.Book
                    LinkType.JOURNAL_DOCUMENT -> Icons.Default.Book
                    LinkType.CHECKLIST -> Icons.Default.Link
                    LinkType.MUSIC_NOTE -> Icons.Default.Book
                    LinkType.URL -> Icons.Default.Link
                    LinkType.OBSIDIAN -> Icons.Default.Book
                    null -> Icons.Default.BrokenImage
                },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = link.displayName ?: link.target,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class RelatedLinkChipColors(
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)
