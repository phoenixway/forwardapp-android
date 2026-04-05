package com.romankozak.forwardappmobile.ui.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val StatusChipShape = RoundedCornerShape(10.dp)
private val StatusChipHeight = 24.dp
private val StatusChipCompactHorizontalPadding = 6.dp
private val StatusChipHorizontalPadding = 8.dp
private val StatusChipVerticalPadding = 4.dp
private val StatusChipIconSize = 12.dp

data class UnifiedStatusChipSpec(
    val text: String,
    val icon: ImageVector? = null,
    val contentColor: Color? = null,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedStatusRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun UnifiedStatusRow(
    items: List<UnifiedStatusChipSpec>,
    modifier: Modifier = Modifier,
    defaultContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    UnifiedStatusRow(modifier = modifier) {
        items.forEach { item ->
            UnifiedMetaChip(
                text = item.text,
                icon = item.icon,
                contentColor = item.contentColor ?: defaultContentColor,
                onClick = item.onClick,
            )
        }
    }
}

@Composable
fun UnifiedMetaChip(
    text: String,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = StatusChipShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(
            modifier =
                if (text.isBlank()) {
                    Modifier
                        .height(StatusChipHeight)
                        .heightIn(min = StatusChipHeight)
                        .padding(horizontal = StatusChipCompactHorizontalPadding, vertical = StatusChipVerticalPadding)
                } else {
                    Modifier
                        .height(StatusChipHeight)
                        .heightIn(min = StatusChipHeight)
                        .padding(horizontal = StatusChipHorizontalPadding, vertical = StatusChipVerticalPadding)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(StatusChipIconSize),
                    tint = contentColor,
                )
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
