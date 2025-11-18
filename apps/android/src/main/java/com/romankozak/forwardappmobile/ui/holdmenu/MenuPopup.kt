package com.romankozak.forwardappmobile.ui.holdmenu

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.isActive

@Composable
fun MenuPopup(state: HoldMenuState) {
    Log.e("HOLDMENU", "🎨 MenuPopup START")

    val density = LocalDensity.current


    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val menuWidth = 220.dp
        val itemH = 44.dp

        val menuWidthPx = with(density) { menuWidth.toPx() }
        val menuHeightPx = with(density) { (itemH * state.items.size).toPx() }

        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }

        val desiredX = state.anchor.x - menuWidthPx / 2f
        val desiredY = state.anchor.y - menuHeightPx - 8f

        val finalX = desiredX.coerceIn(0f, screenW - menuWidthPx)
        val finalY = desiredY.coerceIn(0f, screenH - menuHeightPx)

        Column(
            modifier = Modifier
                .offset { IntOffset(finalX.toInt(), finalY.toInt()) }
                .width(menuWidth)
                .background(Color(0xFF222222), RoundedCornerShape(12.dp))
        ) {
            Log.e("HOLDMENU", "📦 Drawing ${state.items.size} items")

            state.items.forEachIndexed { index, label ->
                val isHover = index == state.hoverIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemH)
                        .background(if (isHover) Color(0xFF444444) else Color.Transparent),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
    Log.e("HOLDMENU", "📏 Constraints = ${maxWidth} x ${maxHeight}")

}
