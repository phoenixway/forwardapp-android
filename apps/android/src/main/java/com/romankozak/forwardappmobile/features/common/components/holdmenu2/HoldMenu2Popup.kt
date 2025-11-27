package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.romankozak.forwardappmobile.ui.theme.HoldMenuColors
import com.romankozak.forwardappmobile.ui.theme.LocalHoldMenuColors

@Composable
fun HoldMenu2Popup(
    state: HoldMenu2State,
    onItemClick: (Int) -> Unit = {}
) {
    val layout = state.layout ?: return

    Log.e("HOLDMENU2", "🎨 Popup rendering, items=${state.items.size}, hover=${state.hoverIndex}, isDragMode=${state.isDragMode}")

    val density = LocalDensity.current
    val menuWidth = with(density) { layout.menuWidth.toDp() }
    val itemHeight = with(density) { layout.itemHeight.toDp() }
    val holdMenuColors = LocalHoldMenuColors.current

    val scale by animateFloatAsState(
        targetValue = if (state.isOpen) 1f else 0.92f,
                                     animationSpec = spring(
                                         dampingRatio = Spring.DampingRatioMediumBouncy,
                                         stiffness = Spring.StiffnessLow
                                     ),
                                     label = "menu_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (state.isOpen) 1f else 0f,
                                     animationSpec = tween(150),
                                     label = "menu_alpha"
    )

    Box(
        modifier = Modifier
        .offset { layout.menuTopLeft }
        .graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
        .width(menuWidth)
        .shadow(
            elevation = 20.dp,
            shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
        )
        .background(
            color = holdMenuColors.background,
            shape = RoundedCornerShape(22.dp)
        )
        .border(
            width = 1.dp,
            color = holdMenuColors.border,
            shape = RoundedCornerShape(22.dp)
        )
        .padding(vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = when (state.menuAlignment) {
                MenuAlignment.START -> Alignment.Start
                MenuAlignment.CENTER -> Alignment.CenterHorizontally
                MenuAlignment.END -> Alignment.End
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            state.items.forEachIndexed { index, item ->
                MenuItemRow(
                    item = item,
                    isHover = index == state.hoverIndex,
                    itemHeight = itemHeight,
                    iconPosition = state.iconPosition,
                    menuAlignment = state.menuAlignment,
                    colors = holdMenuColors,
                    // В tap mode додаємо можливість кліку
                    onClick = if (!state.isDragMode) {
                        { onItemClick(index) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}


@Composable
private fun MenuItemRow(
    item: HoldMenuItem,
    isHover: Boolean,
    itemHeight: Dp,
    iconPosition: IconPosition,
    menuAlignment: MenuAlignment,
    colors: HoldMenuColors,
    onClick: (() -> Unit)? = null,
) {
    val offsetX by animateDpAsState(
        targetValue = if (isHover) 6.dp else 0.dp,
                                    animationSpec = tween(90),
                                    label = "offset"
    )

    val backgroundColor = if (isHover) colors.itemHoverBackground else Color.Transparent
    val textColor = if (isHover) colors.itemText else colors.itemTextMuted
    val fontWeight = if (isHover) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = Modifier
        .fillMaxWidth()
        .height(itemHeight)
        .offset(x = offsetX)
        .padding(horizontal = 6.dp, vertical = 4.dp)
        .background(backgroundColor, RoundedCornerShape(14.dp))
        .then(
            if (onClick != null) {
                // Tap mode - додаємо clickable
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                                   indication = null
                ) {
                    onClick()
                }
            } else {
                Modifier
            }
        ),
        contentAlignment = when (menuAlignment) {
            MenuAlignment.START -> Alignment.CenterStart
            MenuAlignment.END -> Alignment.CenterEnd
            MenuAlignment.CENTER -> Alignment.Center
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            if (iconPosition == IconPosition.START)
                item.icon?.let {
                    Icon(
                        imageVector = it,
                         contentDescription = null,
                         tint = textColor,
                         modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    item.label,
                     color = textColor,
                     fontSize = 15.sp,
                     fontWeight = fontWeight
                )

                if (iconPosition == IconPosition.END)
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
