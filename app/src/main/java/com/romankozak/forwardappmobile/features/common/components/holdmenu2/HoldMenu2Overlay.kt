package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.theme.LocalHoldMenuColors

@Composable
fun HoldMenu2Overlay(
    controller: HoldMenu2Controller,
    modifier: Modifier = Modifier
) {
    val state = controller.state
    val holdMenuColors = LocalHoldMenuColors.current

    AnimatedVisibility(
        visible = state.isOpen && state.items.isNotEmpty(),
                       enter = fadeIn(tween(150)),
                       exit = fadeOut(tween(100)),
    ) {
        Log.e("HOLDMENU2", "🎬 Overlay: rendering, isDragMode=${state.isDragMode}")

        Box(
            modifier = modifier
            .fillMaxSize()
            .pointerInput(state.isDragMode) {
                if (!state.isDragMode) {
                    // Tap mode - детектимо тапи
                    detectTapGestures { offset ->
                        val layout = state.layout ?: return@detectTapGestures

                        // Перевіряємо чи тап всередині меню
                        val relativeX = offset.x - layout.menuTopLeft.x
                        val relativeY = offset.y - layout.menuTopLeft.y

                        if (relativeX >= 0 && relativeX <= layout.menuWidth &&
                            relativeY >= 0 && relativeY <= layout.menuHeight) {
                            // Тап всередині меню - розраховуємо індекс
                            val index = (relativeY / layout.itemHeight).toInt()
                            .coerceIn(0, state.items.size - 1)

                            Log.e("HOLDMENU2", "🎯 Menu item $index tapped")
                            state.onItemSelected?.invoke(index)
                            controller.close()
                            } else {
                                // Тап поза меню - закриваємо
                                Log.e("HOLDMENU2", "🚪 Tapped outside menu - closing")
                                controller.close()
                            }
                    }
                }
            }
        ) {
            // --- BACKDROP ---
            Box(
                Modifier
                .fillMaxSize()
                .background(holdMenuColors.scrim)
                .blur(18.dp)
            )

            // --- POPUP ---
            HoldMenu2Popup(state = state)
        }
    }
}
