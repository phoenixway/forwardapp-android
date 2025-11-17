package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@Composable
fun HoldMenuButton(
    modifier: Modifier = Modifier,
    onLongPress: (anchor: Offset, pointerId: PointerId) -> Unit,
    content: @Composable () -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .onGloballyPositioned { layout ->
                val pos = layout.positionInWindow()
                val size = layout.size
                center = Offset(
                    pos.x + size.width / 2f,
                    pos.y + size.height / 2f
                )
                Log.e("HOLDMENU", "📍 BUTTON center=$center")
            }
            .pointerInput(Unit) {

                awaitEachGesture {

                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pid = down.id

                    var longPressFired = false
                    var job: Job? = null

                    // Запускаємо long-press у звичайному CoroutineScope
                    job = scope.launch {
                        delay(350)
                        longPressFired = true
                        Log.e("HOLDMENU", "⏱ Long press → OPEN")
                        onLongPress(center, pid)
                    }

                    // обробка pointer рухів
                    while (true) {
                        val event = awaitPointerEvent()

                        val change = event.changes.firstOrNull { it.id == pid }
                            ?: event.changes.first()

                        // якщо користувач рухається ДО long-press → скасувати
                        if (!longPressFired && change.positionChange() != Offset.Zero) {
                            job?.cancel()
                            break
                        }

                        // якщо відпустив ДО long-press → скасувати
                        if (!longPressFired && change.changedToUpIgnoreConsumed()) {
                            job?.cancel()
                            break
                        }

                        // якщо longPress спрацював → виходимо з gesture
                        if (longPressFired) {
                            change.consume()
                            break
                        }

                        change.consume()
                    }
                }
            }
    ) {
        content()
    }
}
