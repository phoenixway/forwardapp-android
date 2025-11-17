/*package com.romankozak.forwardappmobile.ui.holdmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    onDismiss: () -> Unit,
) {
    if (!state.isOpen) return

    val density = LocalDensity.current

    val menuWidth = 220.dp
    val itemHeight = 48.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val menuHeightPx = itemHeightPx * state.items.size
    val margin = with(density) { 8.dp.toPx() }

    val popupX = state.anchor.x - menuWidthPx / 2f
    val popupY = state.anchor.y - menuHeightPx - margin

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // === 1. КАПЧЕР ТАПУ ПОЗА МЕНЮ — НЕ БЛокуємо pointerInput на Box ===
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(state) {
                    awaitPointerEventScope {
                        val down = awaitFirstDown()
                        val pos = down.position

                        val inside =
                            pos.x in popupX..(popupX + menuWidthPx) &&
                                    pos.y in popupY..(popupY + menuHeightPx)

                        if (!inside) {
                            onDismiss()
                        }
                    }
                }
        )

        // === 2. ВЛАСНЕ МЕНЮ ===
        Column(
            modifier = Modifier
                .offset { IntOffset(popupX.toInt(), popupY.toInt()) }
                .width(menuWidth)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp)
                )
                .padding(vertical = 4.dp)
                // ⬇ PointerInput тільки на меню (drag-selection)
                .pointerInput(state) {
                    awaitPointerEventScope {
                        var active = true
                        val down = awaitFirstDown()

                        while (active) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()

                            val relativeY = change.position.y - popupY
                            val idx = (relativeY / itemHeightPx).toInt()

                            selectedIndex =
                                if (idx in state.items.indices) idx else null

                            if (change.changedToUp()) {
                                selectedIndex?.let { state.items[it].onSelect() }
                                onDismiss()
                                active = false
                            }
                        }
                    }
                }
        ) {

            state.items.forEachIndexed { idx, itm ->
                val selected = idx == selectedIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else
                                Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(itm.label)
                    Icon(itm.icon, contentDescription = null)
                }
            }
        }
    }
}


@Composable
fun HoldMenuOverlay_MINIMAL(state: HoldMenuState, onDismiss: () -> Unit) {
    if (!state.isOpen) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    awaitFirstDown()
                    onDismiss()
                }
            }
    ) {
        Column(
            Modifier
                .offset { IntOffset(state.anchor.x.toInt(), state.anchor.y.toInt()) }
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            state.items.forEach {
                Text(
                    it.label,
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { it.onSelect(); onDismiss() }
                )
            }
        }
    }
}*/
