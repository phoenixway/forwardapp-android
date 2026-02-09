package com.romankozak.forwardappmobile.ui.components.listitem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class UnifiedItemState {
    DEFAULT,
    SELECTED,
    COMPLETED,
    OVERDUE,
    DISABLED,
}

object UnifiedListItemTokens {
    val CornerRadius = 16.dp
    val HorizontalPadding = 16.dp
    val VerticalPadding = 14.dp
    val OuterVerticalSpacing = 6.dp
    val OuterHorizontalSpacing = 8.dp
    val LeadingMainSpacing = 16.dp
    val ActionButtonSize = 40.dp
    val ActionIconSize = 20.dp
}

@Composable
fun UnifiedListItemSurface(
    isSelected: Boolean,
    state: UnifiedItemState = UnifiedItemState.DEFAULT,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues =
        PaddingValues(
            horizontal = UnifiedListItemTokens.HorizontalPadding,
            vertical = UnifiedListItemTokens.VerticalPadding,
        ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedState = if (isSelected) UnifiedItemState.SELECTED else state
    val containerColor =
        when (resolvedState) {
            UnifiedItemState.SELECTED -> MaterialTheme.colorScheme.surfaceContainerHighest
            UnifiedItemState.COMPLETED -> MaterialTheme.colorScheme.surfaceContainer
            UnifiedItemState.OVERDUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
            UnifiedItemState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            UnifiedItemState.DEFAULT -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val borderColor =
        when (resolvedState) {
            UnifiedItemState.SELECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            UnifiedItemState.COMPLETED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            UnifiedItemState.OVERDUE -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            UnifiedItemState.DISABLED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            UnifiedItemState.DEFAULT -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UnifiedListItemTokens.CornerRadius),
        color = containerColor,
        shadowElevation = if (resolvedState == UnifiedItemState.SELECTED) 5.dp else 2.dp,
        tonalElevation = if (resolvedState == UnifiedItemState.SELECTED) 4.dp else 2.dp,
        border =
            if (resolvedState == UnifiedItemState.SELECTED) {
                BorderStroke(2.dp, borderColor)
            } else {
                BorderStroke(1.dp, borderColor)
            },
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun UnifiedListItemLayout(
    main: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    spacingBetweenLeadingAndMain: Dp = UnifiedListItemTokens.LeadingMainSpacing,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(spacingBetweenLeadingAndMain))
        }
        Column(modifier = Modifier.weight(1f), content = main)
        trailing?.invoke()
    }
}

@Composable
fun UnifiedTrailingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(UnifiedListItemTokens.ActionButtonSize),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(UnifiedListItemTokens.ActionIconSize),
        )
    }
}
