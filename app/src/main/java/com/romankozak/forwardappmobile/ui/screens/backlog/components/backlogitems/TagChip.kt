package com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TagChip1(
    text: String,
    onDismiss: () -> Unit,
    isDismissible: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                ).border(
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                ).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.15.sp,
                    fontSize = 10.sp,
                ),
            color = MaterialTheme.colorScheme.secondary,
        )
        if (isDismissible) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Remove tag",
                modifier =
                    Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismiss),
            )
        }
    }
}




// Enhanced TagChip with modern gradient design
@Composable
fun EnhancedTagChip(
    text: String,
    onDismiss: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDismissible: Boolean = true,
    isSelected: Boolean = false,
    tagType: TagType = TagType.HASHTAG,
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tag_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 3.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tag_elevation"
    )

    val colors = getTagColors(tagType, isSelected)

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .semantics {
                if (onClick != null) {
                    role = Role.Button
                    contentDescription = "Тег: $text"
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.borderStart,
                    colors.borderEnd
                )
            )
        ),
        onClick = onClick ?: {},
        interactionSource = interactionSource,
        enabled = onClick != null,
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.backgroundStart,
                            colors.backgroundEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Icon based on tag type
                Icon(
                    imageVector = when (tagType) {
                        TagType.HASHTAG -> Icons.Default.Tag
                        TagType.PROJECT -> Icons.Default.Tag
                    },
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(12.dp)
                )

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        fontSize = 12.sp
                    ),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Dismiss button
                if (isDismissible && onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Видалити тег",
                            tint = colors.content.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

// Compact version for inline text
@Composable
fun InlineTagChip(
    text: String,
    tagType: TagType = TagType.HASHTAG,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isInCompletedText: Boolean = false,
) {
    val colors = getTagColors(tagType, false)
    val finalColors = if (isInCompletedText) {
        colors.copy(
            content = colors.content.copy(alpha = 0.6f),
            backgroundStart = colors.backgroundStart.copy(alpha = 0.3f),
            backgroundEnd = colors.backgroundEnd.copy(alpha = 0.3f)
        )
    } else colors

    Surface(
        modifier = modifier
            .semantics {
                if (onClick != null) {
                    role = Role.Button
                    contentDescription = "Тег: $text"
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 0.8.dp,
            color = finalColors.borderStart.copy(alpha = 0.4f)
        ),
        onClick = onClick ?: {}
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            finalColors.backgroundStart,
                            finalColors.backgroundEnd
                        )
                    )
                )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp,
                    fontSize = 10.sp
                ),
                color = finalColors.content,
                modifier = Modifier.padding(
                    horizontal = 6.dp,
                    vertical = 3.dp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class TagType {
    HASHTAG,
    PROJECT
}

@Stable
data class TagColors(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val content: Color
)

@Composable
private fun getTagColors(
    tagType: TagType,
    isSelected: Boolean
): TagColors {
    return when (tagType) {
        TagType.HASHTAG -> {
            if (isSelected) {
                TagColors(
                    backgroundStart = MaterialTheme.colorScheme.primaryContainer,
                    backgroundEnd = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    borderStart = MaterialTheme.colorScheme.primary,
                    borderEnd = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                TagColors(
                    backgroundStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    backgroundEnd = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    borderStart = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    borderEnd = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    content = MaterialTheme.colorScheme.primary
                )
            }
        }
        TagType.PROJECT -> {
            if (isSelected) {
                TagColors(
                    backgroundStart = MaterialTheme.colorScheme.tertiaryContainer,
                    backgroundEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    borderStart = MaterialTheme.colorScheme.tertiary,
                    borderEnd = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                )
            } else {
                TagColors(
                    backgroundStart = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    backgroundEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                    borderStart = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    borderEnd = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                    content = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}