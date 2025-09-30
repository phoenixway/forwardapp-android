
package com.romankozak.forwardappmobile.ui.common.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ExpandableFlowRow(
    modifier: Modifier = Modifier,
    maxHeight: Dp,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    SubcomposeLayout(modifier = modifier.animateContentSize()) { constraints ->
        val contentMeasurable = subcompose("content", content).first()

        val intrinsicHeight = contentMeasurable.minIntrinsicHeight(constraints.maxWidth)
        val maxHeightPx = with(density) { maxHeight.toPx() }.roundToInt()

        showButton = intrinsicHeight > maxHeightPx

        val currentHeight = if (expanded || !showButton) intrinsicHeight else maxHeightPx

        val placeable = contentMeasurable.measure(constraints.copy(maxHeight = currentHeight))

        layout(placeable.width, currentHeight) {
            placeable.placeRelative(0, 0)

            if (showButton) {
                val buttonMeasurable = subcompose("button") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (expanded) "згорнути" else "..",
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                    }
                }.first()
                val buttonPlaceable = buttonMeasurable.measure(constraints.copy(minHeight = 0))
                buttonPlaceable.placeRelative(0, currentHeight - buttonPlaceable.height)
            }
        }
    }
}
