package com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.backlogitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LimitedFlowRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    maxLines: Int = 2,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeables = subcompose(Unit, content).map {
            it.measure(constraints)
        }

        val spacingInPx = spacing.toPx().toInt()

        val lines = mutableListOf<List<Int>>()
        val currentLineItems = mutableListOf<Int>()
        var currentLineWidth = 0

        placeables.forEachIndexed { index, placeable ->
            if (currentLineWidth + placeable.width > constraints.maxWidth) {
                lines.add(currentLineItems.toList())
                currentLineItems.clear()
                currentLineWidth = 0
            }
            currentLineItems.add(index)
            currentLineWidth += placeable.width + spacingInPx
        }
        if (currentLineItems.isNotEmpty()) {
            lines.add(currentLineItems.toList())
        }

        val linesToShow = if (expanded) lines else lines.take(maxLines)
        val hasMore = lines.size > maxLines && !expanded

        val width = constraints.maxWidth
        val height = (linesToShow.size * (placeables.firstOrNull()?.height ?: 0) + (linesToShow.size - 1).coerceAtLeast(0) * spacingInPx).let {
            if (hasMore) it + 24.dp.toPx().toInt() else it
        }

        layout(width, height) {
            var y = 0
            linesToShow.forEach { lineIndices ->
                var x = 0
                lineIndices.forEach { index ->
                    placeables[index].placeRelative(x, y)
                    x += placeables[index].width + spacingInPx
                }
                y += (placeables.firstOrNull()?.height ?: 0) + spacingInPx
            }

            if (hasMore) {
                val moreIconMeasurable = subcompose("more_icon") {
                    Box(modifier = Modifier.clickable { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Show more",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }.first().measure(constraints)

                moreIconMeasurable.placeRelative(0, y)
            }
        }
    }
}