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

data class UnifiedListItemColors(
    val container: Color? = null,
    val border: Color? = null,
)

private data class ResolvedUnifiedListItemColors(
    val container: Color,
    val border: Color,
)

data class UnifiedListItemSurfaceLayout(
    val modifier: Modifier = Modifier,
    val contentPadding: PaddingValues =
        PaddingValues(
            horizontal = UnifiedListItemTokens.HorizontalPadding,
            vertical = UnifiedListItemTokens.VerticalPadding,
        ),
)

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
    layout: UnifiedListItemSurfaceLayout = UnifiedListItemSurfaceLayout(),
    colors: UnifiedListItemColors = UnifiedListItemColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedState = if (isSelected) UnifiedItemState.SELECTED else state
    val resolvedColors = resolvedState.resolveColors(colors)
    Surface(
        modifier = layout.modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UnifiedListItemTokens.CornerRadius),
        color = resolvedColors.container,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border =
            if (resolvedState == UnifiedItemState.SELECTED) {
                BorderStroke(2.dp, resolvedColors.border)
            } else {
                BorderStroke(1.dp, resolvedColors.border)
            },
    ) {
        Column(modifier = Modifier.padding(layout.contentPadding), content = content)
    }
}

@Composable
private fun UnifiedItemState.resolveColors(overrides: UnifiedListItemColors): ResolvedUnifiedListItemColors {
    val defaultColors =
        when (this) {
            UnifiedItemState.SELECTED ->
                ResolvedUnifiedListItemColors(
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                )
            UnifiedItemState.COMPLETED ->
                ResolvedUnifiedListItemColors(
                    container = MaterialTheme.colorScheme.surfaceContainer,
                    border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            UnifiedItemState.OVERDUE ->
                ResolvedUnifiedListItemColors(
                    container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
                    border = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                )
            UnifiedItemState.DISABLED ->
                ResolvedUnifiedListItemColors(
                    container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
            UnifiedItemState.DEFAULT ->
                ResolvedUnifiedListItemColors(
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
        }

    return ResolvedUnifiedListItemColors(
        container = overrides.container ?: defaultColors.container,
        border = overrides.border ?: defaultColors.border,
    )
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
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(UnifiedListItemTokens.ActionButtonSize),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(UnifiedListItemTokens.ActionIconSize),
        )
    }
}
