package com.romankozak.forwardappmobile.ui.holdmenu2

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun HoldMenu2Popup(state: HoldMenu2State) {
    val density = LocalDensity.current

    val menuWidth = 220.dp
    val itemH = 44.dp

    val menuWidthPx = with(density) { menuWidth.toPx() }
    val menuHeightPx = with(density) { (itemH * state.items.size).toPx() }

    val desiredX = state.anchor.x - menuWidthPx / 2f
    val desiredY = state.anchor.y - menuHeightPx - 8f

    Box(
        modifier = Modifier
            .offset {
                IntOffset(desiredX.roundToInt(), desiredY.roundToInt())
            }
            .width(menuWidth)
            .background(
                Color(0xFF222222),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column {
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
}

