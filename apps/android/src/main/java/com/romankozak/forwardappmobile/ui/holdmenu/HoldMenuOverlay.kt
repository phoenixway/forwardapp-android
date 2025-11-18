package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    selectedIndex: Int?,
    onItemPositioned: (Int, Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen || state.items.isEmpty()) {
        return
    }

    val density = LocalDensity.current
    val menuWidth = 220.dp
    val itemH = 48.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        val menuWidthPx = with(density) { menuWidth.toPx() }
        val menuHeightPx = with(density) { (itemH * state.items.size).toPx() }

        val desiredX = state.anchor.x - menuWidthPx / 2f
        val desiredY = state.anchor.y - menuHeightPx - 16f

        val offsetX = desiredX.toInt().coerceAtLeast(8)
        val offsetY = desiredY.toInt().coerceAtLeast(8)

        Log.e("HOLDMENU", "📍 Menu offset = ($offsetX, $offsetY)")

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .width(menuWidth)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            state.items.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    label = "item_scale_$index"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemH)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            onItemPositioned(index, pos, coords.size)
                            Log.e("HOLDMENU", "📍 Item $index at $pos, size=${coords.size}")
                        }
                        .scale(scale)
                        .background(
                            if (isSelected) Color(0xFF3A3A3A) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                        fontSize = if (isSelected) 16.sp else 15.sp,
                    )
                }
            }
        }
    }
}