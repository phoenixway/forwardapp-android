package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun MenuPopup(
    state: HoldMenuState,
    onChangeState: (HoldMenuState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen || state.items.isEmpty()) {
        Log.e("HOLDMENU", "❌ MenuPopup: nothing to draw")
        return
    }

    val density = LocalDensity.current
    val menuWidth = 220.dp
    val itemHeight = 44.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        // Конвертуємо розміри в px
        val menuWidthPx = with(density) { menuWidth.toPx() }
        val menuHeightPx = with(density) { (itemHeight * state.items.size).toPx() }

        // Позиція меню відносно anchor
        val desiredX = state.anchor.x - menuWidthPx / 2f
        val desiredY = state.anchor.y - menuHeightPx - 8f  // 8dp відступ над натиском

        // Обмежуємо popup межами вікна
        val offsetX = desiredX.toInt().coerceAtLeast(0)
        val offsetY = desiredY.toInt().coerceAtLeast(0)

        Log.e("HOLDMENU", "📍 MenuPopup offset = ($offsetX, $offsetY)")

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .width(menuWidth)
                .background(Color(0xFF222222), RoundedCornerShape(12.dp))
        ) {
            state.items.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            state.onItemSelected?.invoke(index)
                            onChangeState(state.copy(isOpen = false))
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        color = Color.White
                    )
                }
            }
        }
    }
}
