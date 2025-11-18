package com.romankozak.forwardappmobile.ui.holdmenu2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HoldMenu2Button(
    modifier: Modifier = Modifier,
    controller: HoldMenu2Controller,
    items: List<String>,
    onSelect: (Int) -> Unit,
    holdDurationMs: Long = 350,
    content: @Composable () -> Unit
) {
    var anchor by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                anchor = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val touch = down.position

                        val job = launch {
                            delay(holdDurationMs)
                            controller.open(anchor, touch, items, onSelect)
                        }

                        val up = waitForUpOrCancellation()
                        job.cancel()
                    }
                }
            }
    ) {
        content()
    }
}

