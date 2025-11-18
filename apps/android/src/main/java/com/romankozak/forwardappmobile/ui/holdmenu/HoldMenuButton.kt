package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HoldMenuButton(
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 350L,
    onLongPress: (anchor: Offset, touch: Offset) -> Unit,
    content: @Composable () -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                center = Offset(
                    pos.x + size.width / 2f,
                    pos.y + size.height / 2f
                )
            }
            .pointerInput(holdDurationMs) {
                awaitEachGesture {
                    // 1. Чекаємо перший дотик
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position

                    // 2. Чекаємо або UP, або timeout (long press)
                    val up = withTimeoutOrNull(holdDurationMs) {
                        waitForUpOrCancellation()
                    }

                    if (up == null) {
                        // ❗ НІХТО не відпустив палець за holdDurationMs → long press
                        Log.e("HOLDMENU", "🔥 LONG PRESS → center=$center, touch=$start")
                        onLongPress(center, start)
                    } else {
                        // Це був короткий тап — нічого не робимо
                        Log.e("HOLDMENU", "⏳ Short tap → ignore")
                    }
                }
            }
    ) {
        content()
    }
}
