package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


@Composable
fun HoldMenu2Button(
    items: List<HoldMenuItem>,
    onSelect: (Int) -> Unit,
                    modifier: Modifier = Modifier,
                    controller: HoldMenu2Controller = rememberHoldMenu2(),
                    longPressDuration: Long = 400,
                    onTap: (() -> Unit)? = null,
                    iconPosition: IconPosition = IconPosition.START,
                    menuAlignment: MenuAlignment = MenuAlignment.START,
                    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    var buttonAnchor by remember { mutableStateOf(Offset.Zero) }
    var buttonSize by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(screenWidth, screenHeight) {
        controller.setScreenDimensions(screenWidth, screenHeight, density)
    }

    Box(
        modifier = modifier
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            val size = coords.size
            buttonSize = Offset(size.width.toFloat(), size.height.toFloat())
            buttonAnchor = Offset(
                pos.x + size.width / 2f,
                pos.y + size.height / 2f
            )
        }
        // Gesture 1: Tap detection
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    Log.e("HOLDMENU2", "👆 Single TAP detected!")

                    val globalTouch = Offset(
                        buttonAnchor.x + offset.x - buttonSize.x / 2f,
                        buttonAnchor.y + offset.y - buttonSize.y / 2f
                    )

                    controller.open(
                        anchor = buttonAnchor,
                        touch = globalTouch,
                        items = items,
                        onSelect = onSelect,
                        iconPosition = iconPosition,
                        menuAlignment = menuAlignment,
                        isDragMode = false
                    )
                }
            )
        }
        // Gesture 2: Long press + drag detection
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    Log.e("HOLDMENU2", "🔥 LONG PRESS detected!")

                    val globalTouch = Offset(
                        buttonAnchor.x + offset.x - buttonSize.x / 2f,
                        buttonAnchor.y + offset.y - buttonSize.y / 2f
                    )

                    controller.open(
                        anchor = buttonAnchor,
                        touch = globalTouch,
                        items = items,
                        onSelect = onSelect,
                        iconPosition = iconPosition,
                        menuAlignment = menuAlignment,
                        isDragMode = true
                    )
                },
                onDrag = { change, _ ->
                    val globalPos = Offset(
                        buttonAnchor.x + change.position.x - buttonSize.x / 2f,
                        buttonAnchor.y + change.position.y - buttonSize.y / 2f
                    )
                    controller.updateHover(globalPos)
                },
                onDragEnd = {
                    val hover = controller.state.hoverIndex
                    Log.e("HOLDMENU2", "✅ Drag ended on: $hover")
                    if (hover >= 0) {
                        onSelect(hover)
                    }
                    controller.close()
                },
                onDragCancel = {
                    Log.e("HOLDMENU2", "❌ Drag cancelled")
                    controller.close()
                }
            )
        }
    ) {
        content()
    }
}
