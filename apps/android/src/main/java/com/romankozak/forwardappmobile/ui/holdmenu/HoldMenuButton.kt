package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

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
                Log.e("HOLDMENU", "📌 Button center = $center")
            }
            .pointerInput(Unit) {
                Log.e("HOLDMENU", "🎯 pointerInput STARTED")
                detectTapGestures(
                    onLongPress = {
                        Log.e("HOLDMENU", "🔥 onLongPress fired, center=$center")
                        onLongPress(center)
                    }
                )
            }
    ) {
        content()
    }
}
