package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HoldMenuButton(
    modifier: Modifier = Modifier,
    onLongPress: (anchor: Offset) -> Unit,
    content: @Composable () -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                center = Offset(
                    x = pos.x + size.width / 2f,
                    y = pos.y + size.height / 2f
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    Log.e("HOLDMENU", "👇 Finger down")

                    val longPress = withTimeoutOrNull(500) {
                        // Чекаємо наступну подію (рух або відпускання)
                        awaitPointerEvent(PointerEventPass.Main)
                        null // Якщо дійшли сюди - палець рухався, не long press
                    }

                    if (longPress == null) {
                        // Таймаут спрацював - long press!
                        Log.e("HOLDMENU", "🔥 Long press! Opening menu at $center")
                        onLongPress(center)
                        // НЕ споживаємо - дозволяємо overlay обробляти подальші рухи
                    } else {
                        // Короткий тап або рух
                        Log.e("HOLDMENU", "👆 Not a long press")
                        down.consume()
                    }
                }
            }
    ) {
        content()
    }
}