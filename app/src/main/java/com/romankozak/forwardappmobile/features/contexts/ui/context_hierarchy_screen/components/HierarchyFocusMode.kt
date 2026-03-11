package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem

@Composable
internal fun rememberHierarchyFocusMode(breadcrumbs: List<BreadcrumbItem>): Boolean {
    if (breadcrumbs.isEmpty()) return false

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodySmall

    return remember(breadcrumbs, configuration.screenWidthDp, density.density, textStyle) {
        val availableWidthPx =
            with(density) {
                configuration.screenWidthDp.dp.toPx() - 32.dp.toPx()
            }

        val allTextWidth = textMeasurer.measure("All", style = textStyle).size.width.toFloat()
        val breadcrumbTextWidth =
            breadcrumbs.sumOf { item ->
                textMeasurer.measure(item.name, style = textStyle).size.width
            }.toFloat()

        val homeWidthPx =
            with(density) {
                18.dp.toPx() + 4.dp.toPx() + allTextWidth + 4.dp.toPx() + 16.dp.toPx() + 4.dp.toPx()
            }
        val chipWidthsPx =
            with(density) {
                breadcrumbTextWidth +
                    (breadcrumbs.size * 24.dp.toPx()) +
                    ((breadcrumbs.size - 1).coerceAtLeast(0) * (16.dp.toPx() + 8.dp.toPx()))
            }
        val spacingWidthsPx =
            with(density) {
                ((breadcrumbs.size + 1) * 4.dp.toPx())
            }

        homeWidthPx + chipWidthsPx + spacingWidthsPx > availableWidthPx
    }
}
