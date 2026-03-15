package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private data class TabWidths(
    val active: Int,
    val inactive: Int,
)

@Composable
fun AdaptiveSegmentedControl(
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    SubcomposeLayout(modifier = modifier.clip(RoundedCornerShape(12.dp))) { constraints ->
        val maxWidth = constraints.maxWidth

        val activeTabWithTextWidth =
            measureActiveTabWithTextWidth(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                constraints = constraints,
            )
        val inactiveTabIconOnlyWidth = measureInactiveTabIconWidth(tabs = tabs, constraints = constraints)

        val totalInactiveTabsWidth = inactiveTabIconOnlyWidth * (tabs.size - 1)

        val expandedLayoutFits = (activeTabWithTextWidth + totalInactiveTabsWidth) <= maxWidth

        layout(width = maxWidth, height = 60.dp.roundToPx()) {
            if (expandedLayoutFits) {
                val remainingWidth = maxWidth - activeTabWithTextWidth
                val inactiveTabCalculatedWidth = if (tabs.size > 1) remainingWidth / (tabs.size - 1) else 0
                val expandedPlaceables =
                    measureExpandedLayout(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        constraints = constraints,
                        tabWidths =
                            TabWidths(
                                active = activeTabWithTextWidth,
                                inactive = inactiveTabCalculatedWidth,
                            ),
                    )

                var x = 0
                expandedPlaceables.forEach { placeable ->
                    placeable.place(x, 0)
                    x += placeable.width
                }
            } else {
                val scrollablePlaceable =
                    measureScrollableLayout(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        constraints = constraints,
                        scrollState = scrollState,
                    )
                scrollablePlaceable?.place(0, 0)
            }
        }
    }
}

private fun SubcomposeMeasureScope.measureActiveTabWithTextWidth(
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    constraints: Constraints,
): Int =
    subcompose("activeTabWithText") {
        TabContent(tab = tabs[selectedTabIndex], isSelected = true, onSelected = {}, showText = true)
    }.firstOrNull()?.measure(constraints)?.width ?: 0

private fun SubcomposeMeasureScope.measureInactiveTabIconWidth(
    tabs: List<SegmentedTab>,
    constraints: Constraints,
): Int =
    subcompose("inactiveTabIconOnly") {
        TabContent(tab = tabs.first(), isSelected = false, onSelected = {}, showText = false)
    }.firstOrNull()?.measure(constraints)?.width ?: 0

private fun SubcomposeMeasureScope.measureExpandedLayout(
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    constraints: Constraints,
    tabWidths: TabWidths,
): List<Placeable> {
    return tabs.mapIndexedNotNull { index, tab ->
        val isSelected = selectedTabIndex == index
        val currentTabWidth = if (isSelected) tabWidths.active else tabWidths.inactive

        subcompose(index) {
            TabContent(
                tab = tab,
                isSelected = isSelected,
                onSelected = { onTabSelected(index) },
                showText = isSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }.firstOrNull()?.measure(
            constraints.copy(
                minWidth = currentTabWidth,
                maxWidth = currentTabWidth,
            ),
        )
    }
}

private fun SubcomposeMeasureScope.measureScrollableLayout(
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    constraints: Constraints,
    scrollState: androidx.compose.foundation.ScrollState,
): Placeable? =
    subcompose("scrollableContent") {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                TabContent(
                    tab = tab,
                    isSelected = selectedTabIndex == index,
                    onSelected = { onTabSelected(index) },
                    showText = selectedTabIndex == index,
                )
            }
        }
    }.firstOrNull()?.measure(constraints)

@Composable
private fun TabContent(
    tab: SegmentedTab,
    isSelected: Boolean,
    onSelected: () -> Unit,
    showText: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(onClick = onSelected)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = if (showText) Arrangement.spacedBy(8.dp) else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(20.dp),
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            if (showText) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
