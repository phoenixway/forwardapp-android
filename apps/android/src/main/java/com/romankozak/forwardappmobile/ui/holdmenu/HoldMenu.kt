package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.layout.positionInWindow

private const val TAG = "HOLDMENU"

// ---------------------- STATE --------------------------

data class HoldMenuItem(
    val label: String,
    val icon: ImageVector?,
    val onClick: () -> Unit
)

data class HoldMenuState(
    val isOpen: Boolean = false,
    val anchor: Offset = Offset.Zero,    // в window-координатах
    val items: List<HoldMenuItem> = emptyList(),
    val selectedIndex: Int? = null

)

// DSL-builder для кнопки
class HoldMenuBuilder {
    internal val items = mutableListOf<HoldMenuItem>()

    fun item(
        label: String,
        icon: ImageVector? = null,
        onClick: () -> Unit
    ) {
        items += HoldMenuItem(label, icon, onClick)
    }
}

// ---------------------- BUTTON --------------------------
@Composable
fun HoldMenuButton(
    icon: ImageVector,
    state: MutableState<HoldMenuState>,
    modifier: Modifier = Modifier,
    holdDelayMs: Long = 150L,
    builder: HoldMenuBuilder.() -> Unit,
) {
    val pressScope = rememberCoroutineScope()

    val builderObj = remember { HoldMenuBuilder() }
    builderObj.items.clear()
    builderObj.builder()

    var btnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .size(40.dp)
            .onGloballyPositioned { coords ->
                btnCoords = coords
                Log.e("HOLDMENU", "📍 BUTTON posInWindow = ${coords.positionInWindow()}")
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        Log.e("HOLDMENU", "🟦 Waiting...")
                        val down = awaitFirstDown()
                        val localPos = down.position

                        val job = pressScope.launch {
                            delay(holdDelayMs)

                            val btn = btnCoords
                            val anchor = if (btn != null) {
                                val btnPos = btn.positionInWindow()
                                val result = btnPos + localPos
                                Log.e("HOLDMENU", "📌 BTN pos=$btnPos → anchor=$result")
                                result
                            } else {
                                localPos
                            }

                            state.value = HoldMenuState(
                                isOpen = true,
                                anchor = anchor,
                                items = builderObj.items.toList()
                            )
                        }

                        val event = awaitPointerEvent()
                        val ch = event.changes.first()
                        if (ch.changedToUp() || !ch.pressed) job.cancel()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}


// ---------------------- OVERLAY --------------------------
@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    onChangeState: (HoldMenuState) -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isOpen) return

    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }

    val menuWidthDp = 220.dp
    val itemHeightDp = 44.dp

    val itemHpx = with(density) { itemHeightDp.toPx() }
    val menuHpx = state.items.size * itemHpx + with(density) { 16.dp.toPx() } // padding

    val anchor = state.anchor

    // ---- Horizontal positioning (safe clamp) ----
    val centerX = anchor.x
    val xLeft = centerX - with(density) { (menuWidthDp / 2).toPx() }
    val xClamped = xLeft.coerceIn(0f, screenW - with(density) { menuWidthDp.toPx() })

    // ---- Vertical positioning with edge handling ----
    val spaceBelow = screenH - anchor.y
    val spaceAbove = anchor.y

    val yPx = when {
        // if menu fits below button
        spaceBelow > menuHpx + 12 -> anchor.y + 12f

        // if fits above button
        spaceAbove > menuHpx + 12 -> anchor.y - menuHpx - 12f

        // DOESN’T fit anywhere: clamp inside screen
        else -> (anchor.y - menuHpx / 2).coerceIn(0f, screenH - menuHpx)
    }

    val xDp = with(density) { xClamped.toDp() }
    val yDp = with(density) { yPx.toDp() }

    // ---------------- DRAG SELECT ---------------------
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(state.items, state.anchor, state.selectedIndex) {
                awaitPointerEventScope {

                    Log.e("HOLDMENU", "🎯 Start drag-select")

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val pos = change.position

                        // compute relative click Y inside menu
                        val relY = pos.y - yPx
                        val index = (relY / itemHpx).toInt()
                        val safeIndex =
                            if (index in state.items.indices) index else null

                        Log.e("HOLDMENU", "👉 Pointer=$pos relY=$relY index=$index")

                        // UP = finish
                        if (change.changedToUp()) {
                            if (safeIndex != null) {
                                Log.e("HOLDMENU", "✔ SELECT ${state.items[safeIndex].label}")
                                state.items[safeIndex].onClick()
                            } else {
                                Log.e("HOLDMENU", "❌ No selection — dismiss")
                            }
                            onDismiss()
                            break
                        }

                        // highlight change
                        if (safeIndex != state.selectedIndex) {
                            Log.e("HOLDMENU", "🔄 Highlight index=$safeIndex")
                            onChangeState(state.copy(selectedIndex = safeIndex))
                        }

                        change.consume()
                    }
                }
            }
    ) {
        // ---- MENU UI ----
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = xDp.roundToPx(),
                        y = yDp.roundToPx()
                    )
                }
                .width(menuWidthDp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                state.items.forEachIndexed { i, item ->
                    val selected = i == state.selectedIndex

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .background(
                                if (selected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else
                                    Color.Transparent
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = item.label,
                                tint = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        Text(
                            item.label,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun LayoutCoordinates.findRoot(): LayoutCoordinates {
    var curr: LayoutCoordinates = this
    while (curr.parentLayoutCoordinates != null) {
        curr = curr.parentLayoutCoordinates!!
    }
    return curr
}
