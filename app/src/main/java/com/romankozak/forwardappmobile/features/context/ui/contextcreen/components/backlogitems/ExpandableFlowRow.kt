package com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.backlogitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An expandable FlowRow that automatically detects overflow and shows expand/collapse indicators.
 * 
 * Key features:
 * - Collapsed state shows maximum of `maxLinesWhenCollapsed` lines
 * - Automatically detects if content overflows
 * - Shows ".." indicator when collapsed with overflow
 * - Shows collapse arrow only when expanded AND content had overflow
 * - Smooth expand/collapse transitions
 */
 @OptIn(ExperimentalLayoutApi::class)
 @Composable
fun <T> ExpandableFlowRow(
    items: List<T>,
    maxLinesWhenCollapsed: Int = 2,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 4.dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    var isExpanded by remember(items) { mutableStateOf(false) }
    var hasOverflow by remember(items) { mutableStateOf(false) }
    var collapsedHeight by remember(items) { mutableIntStateOf(0) }
    
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            // Main content items
            items.forEach { item ->
                Box { itemContent(item) }
            }
            
            // Expand indicator (always included for measurement)
            Box(
                modifier = Modifier
                    .then(
                        if (!isExpanded && hasOverflow) {
                            Modifier.clickable { isExpanded = true }
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (!isExpanded && hasOverflow) {
                    Text(
                        text = "..",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            
            // Collapse indicator (always included for measurement)
            Box(
                modifier = Modifier
                    .then(
                        if (isExpanded && hasOverflow) {
                            Modifier.clickable { isExpanded = false }
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (isExpanded && hasOverflow) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(0.6f)
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val hSpacing = horizontalSpacing.roundToPx()
        val vSpacing = verticalSpacing.roundToPx()
        
        val itemMeasurables = measurables.dropLast(2) // All except indicators
        val expandIndicatorMeasurable = measurables[measurables.size - 2]
        val collapseIndicatorMeasurable = measurables[measurables.size - 1]
        
        // Measure all items
        val itemPlaceables = itemMeasurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val expandIndicatorPlaceable = expandIndicatorMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val collapseIndicatorPlaceable = collapseIndicatorMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        
        // Layout items in rows
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        
        itemPlaceables.forEach { placeable ->
            val itemWidth = placeable.width + hSpacing
            
            if (currentRow.isNotEmpty() && currentRowWidth + itemWidth > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            
            currentRow.add(placeable)
            currentRowWidth += itemWidth
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        // Detect overflow
        val totalRows = rows.size
        val wouldOverflow = totalRows > maxLinesWhenCollapsed
        
        if (hasOverflow != wouldOverflow) {
            hasOverflow = wouldOverflow
        }
        
        // Determine which rows to show
        val rowsToShow = if (isExpanded || !hasOverflow) {
            rows
        } else {
            // Show up to maxLinesWhenCollapsed, leaving space for expand indicator
            val visibleRows = rows.take(maxLinesWhenCollapsed).toMutableList()
            
            // Add expand indicator to last visible row if there's space
            if (visibleRows.isNotEmpty()) {
                val lastRow = visibleRows.last().toMutableList()
                val lastRowWidth = lastRow.sumOf { it.width + hSpacing }
                
                if (lastRowWidth + expandIndicatorPlaceable.width <= constraints.maxWidth) {
                    lastRow.add(expandIndicatorPlaceable)
                    visibleRows[visibleRows.size - 1] = lastRow
                } else if (visibleRows.size > 1) {
                    // Try adding to second-to-last row
                    val secondLastRow = visibleRows[visibleRows.size - 2].toMutableList()
                    val secondLastRowWidth = secondLastRow.sumOf { it.width + hSpacing }
                    
                    if (secondLastRowWidth + expandIndicatorPlaceable.width <= constraints.maxWidth) {
                        secondLastRow.add(expandIndicatorPlaceable)
                        visibleRows[visibleRows.size - 2] = secondLastRow
                    } else {
                        // Create new row for indicator
                        visibleRows.add(listOf(expandIndicatorPlaceable))
                    }
                }
            }
            
            visibleRows
        }
        
        // Add collapse indicator if expanded and had overflow
        val finalRows = if (isExpanded && hasOverflow) {
            val mutableRows = rowsToShow.toMutableList()
            if (mutableRows.isNotEmpty()) {
                val lastRow = mutableRows.last().toMutableList()
                val lastRowWidth = lastRow.sumOf { it.width + hSpacing }
                
                if (lastRowWidth + collapseIndicatorPlaceable.width <= constraints.maxWidth) {
                    lastRow.add(collapseIndicatorPlaceable)
                    mutableRows[mutableRows.size - 1] = lastRow
                } else {
                    mutableRows.add(listOf(collapseIndicatorPlaceable))
                }
            }
            mutableRows
        } else {
            rowsToShow
        }
        
        // Calculate heights
        val rowHeights = finalRows.map { row ->
            row.maxOfOrNull { it.height } ?: 0
        }
        
        val totalHeight = rowHeights.sum() + vSpacing * (finalRows.size - 1).coerceAtLeast(0)
        
        // Store collapsed height for reference
        if (!isExpanded && hasOverflow && collapsedHeight == 0) {
            collapsedHeight = totalHeight
        }
        
        layout(constraints.maxWidth, totalHeight) {
            var yPosition = 0
            
            finalRows.forEachIndexed { rowIndex, row ->
                var xPosition = 0
                val rowHeight = rowHeights[rowIndex]
                
                row.forEach { placeable ->
                    placeable.placeRelative(
                        x = xPosition,
                        y = yPosition + (rowHeight - placeable.height) / 2
                    )
                    xPosition += placeable.width + hSpacing
                }
                
                yPosition += rowHeight + vSpacing
            }
        }
    }
}