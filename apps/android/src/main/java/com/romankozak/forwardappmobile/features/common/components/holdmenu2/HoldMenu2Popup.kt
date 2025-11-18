package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HoldMenu2Popup(state: HoldMenu2State) {
    val layout = state.layout ?: return

    Log.e("HOLDMENU2", "🎨 Popup rendering, items=${state.items.size}, hover=${state.hoverIndex}")

    val density = LocalDensity.current
    val menuWidth = with(density) { layout.menuWidth.toDp() }
    val itemHeight = with(density) { layout.itemHeight.toDp() }

    Box(
        modifier = Modifier
            .offset { layout.menuTopLeft }
            .width(menuWidth)
            .background(
                Color(0xFF2A2A2A),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 8.dp)
    ) {
        Column {
            state.items.forEachIndexed { index, label ->
                val isHover = index == state.hoverIndex
                val scale by animateFloatAsState(
                    targetValue = if (isHover) 1.05f else 1f,
                    label = "item_scale_$index"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .scale(scale)
                        .background(
                            if (isHover) Color(0xFF3A3A3A) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        color = if (isHover) Color.White else Color(0xFFCCCCCC),
                        fontSize = if (isHover) 16.sp else 15.sp
                    )
                }
            }
        }
    }
}