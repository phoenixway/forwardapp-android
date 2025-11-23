package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.compareTo

@Composable
fun HoldMenu2Popup(state: HoldMenu2State) {
    val layout = state.layout ?: return

    Log.e("HOLDMENU2", "🎨 Popup rendering, items=${state.items.size}, hover=${state.hoverIndex}")

    val density = LocalDensity.current
    val menuWidth = with(density) { layout.menuWidth.toDp() }
    val itemHeight = with(density) { layout.itemHeight.toDp() }

    // Анімація появи/зникнення меню
    val scale by animateFloatAsState(
        targetValue = if (state.isOpen) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (state.isOpen) 1f else 0f,
        animationSpec = tween(200),
        label = "menu_alpha"
    )

    Box(
        modifier = Modifier
            .offset { layout.menuTopLeft }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .width(menuWidth)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(
                Color(0xFF2A2A2A),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 8.dp)
    ) {
        Column {
            state.items.forEachIndexed { index, item ->
                val isHover = index == state.hoverIndex

                MenuItemRow(
                    item = item,
                    isHover = isHover,
                    itemHeight = itemHeight,
                    iconPosition = state.iconPosition,
                    menuAlignment = state.menuAlignment,
                )
            }
        }

        // iOS-like hover tooltip
//        IOSStyleHoverLabel(
//            state = state,
//            menuWidth = menuWidth,
//            itemHeight = itemHeight,
//        )
    }
}

@Composable
private fun MenuItemRow(
    item: HoldMenuItem,
    isHover: Boolean,
    itemHeight: Dp,
    iconPosition: IconPosition,
    menuAlignment: MenuAlignment,
) {
    // Анімації для hover
    val scale by animateFloatAsState(
        targetValue = if (isHover) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "item_scale_${item.id}"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isHover) Color(0xFF3A3A3A) else Color.Transparent,
        animationSpec = tween(150),
        label = "item_bg_${item.id}"
    )

    val textColor by animateColorAsState(
        targetValue = if (isHover) Color.White else Color(0xFFCCCCCC),
        animationSpec = tween(150),
        label = "item_text_${item.id}"
    )

    val fontSize by animateFloatAsState(
        targetValue = if (isHover) 16f else 15f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "item_font_${item.id}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .scale(scale)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = when (menuAlignment) {
            MenuAlignment.START -> Alignment.CenterStart
            MenuAlignment.END -> Alignment.CenterEnd
            MenuAlignment.CENTER -> Alignment.Center
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Іконка та текст в залежності від iconPosition
            when (iconPosition) {
                IconPosition.START -> {
                    item.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = fontSize.sp,
                        fontWeight = if (isHover) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                IconPosition.END -> {
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = fontSize.sp,
                        fontWeight = if (isHover) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    item.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSStyleHoverLabel(
    state: HoldMenu2State,
    menuWidth: Dp,
    itemHeight: Dp,
) {
    val density = LocalDensity.current
    val hoverIndex = state.hoverIndex

    AnimatedVisibility(
        visible = hoverIndex in 0 until state.items.size,
        enter = fadeIn(tween(150)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.8f),
    ) {
        if (hoverIndex in 0 until state.items.size) {
            val item = state.items[hoverIndex]
            val offsetY = with(density) { (itemHeight * hoverIndex + itemHeight / 2).toPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(-(menuWidth.value.toInt() + 16), offsetY.toInt()) }
                    .wrapContentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                // iOS-like bubble зліва від меню
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .background(
                            Color(0xFF4A4A4A),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}