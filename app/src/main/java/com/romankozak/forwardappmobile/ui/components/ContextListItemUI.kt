


package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context

private const val CONTEXT_ROW_INDENT_DP = 24
private const val DRAGGING_ROW_ALPHA = 0.6f

private data class ProjectRowState(
    val indentation: androidx.compose.ui.unit.Dp,
    val backgroundColor: Color,
)

private data class ProjectRowContentState(
    val context: Context,
    val hasChildren: Boolean,
    val displayName: AnnotatedString?,
    val showFocusButton: Boolean,
    val indentation: androidx.compose.ui.unit.Dp,
    val isCurrentlyDragging: Boolean,
)

private data class ProjectRowActions(
    val onListClick: (String) -> Unit,
    val onToggleExpanded: (Context) -> Unit,
    val onMenuRequested: (Context) -> Unit,
    val onFocusRequested: (Context) -> Unit,
)

@Composable
@Suppress("LongParameterList")
fun ProjectRow(
    list: Context,
    level: Int,
    hasChildren: Boolean,
    onListClick: (String) -> Unit,
    onToggleExpanded: (list: Context) -> Unit,
    onMenuRequested: (list: Context) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    showFocusButton: Boolean,
    onFocusRequested: (list: Context) -> Unit,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                Color.Transparent
            },
        animationSpec = tween(durationMillis = 500),
        label = "Highlight Animation",
    )
    val rowState =
        remember(level, backgroundColor) {
            ProjectRowState(
                indentation = (level * CONTEXT_ROW_INDENT_DP).dp,
                backgroundColor = backgroundColor,
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(rowState.backgroundColor),
    ) {
        HoverDivider(
            isVisible = isHovered && !isDraggingDown && !isCurrentlyDragging,
            indentation = rowState.indentation,
        )
        ProjectRowContent(
            state =
                ProjectRowContentState(
                    context = list,
                    hasChildren = hasChildren,
                    displayName = displayName,
                    showFocusButton = showFocusButton,
                    indentation = rowState.indentation,
                    isCurrentlyDragging = isCurrentlyDragging,
                ),
            actions =
                ProjectRowActions(
                    onListClick = onListClick,
                    onToggleExpanded = onToggleExpanded,
                    onMenuRequested = onMenuRequested,
                    onFocusRequested = onFocusRequested,
                ),
        )
        HoverDivider(
            isVisible = isHovered && isDraggingDown && !isCurrentlyDragging,
            indentation = rowState.indentation,
        )
    }
}

@Composable
private fun HoverDivider(
    isVisible: Boolean,
    indentation: androidx.compose.ui.unit.Dp,
) {
    if (!isVisible) return

    HorizontalDivider(
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = indentation),
    )
}

@Composable
private fun ProjectRowContent(
    state: ProjectRowContentState,
    actions: ProjectRowActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { actions.onListClick(state.context.id) }
                .alpha(if (state.isCurrentlyDragging) DRAGGING_ROW_ALPHA else 1f)
                .padding(start = state.indentation)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectExpandButton(
            hasChildren = state.hasChildren,
            isExpanded = state.context.isExpanded,
            onToggleExpanded = { actions.onToggleExpanded(state.context) },
        )

        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = state.displayName ?: AnnotatedString(state.context.name),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        FocusButton(
            visible = state.showFocusButton,
            onClick = { actions.onFocusRequested(state.context) },
        )

        IconButton(onClick = { actions.onMenuRequested(state.context) }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії зі списком")
        }
    }
}

@Composable
private fun ProjectExpandButton(
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (hasChildren) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "Згорнути/Розгорнути",
                )
            }
        }
    }
}

@Composable
private fun FocusButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Сфокусуватися",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
