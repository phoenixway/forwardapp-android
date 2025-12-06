package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons


/**
 * TodayHeader:
 *  - верх: ліворуч "Today", по центру DayPlanHeaderContent
 *  - низ (праворуч): навігація по днях + "енергетичний" кружок
 */
@Composable
fun TodayHeader(): HeaderLayout {

    val primaryColor = MaterialTheme.colorScheme.primary // Use primaryColor consistently

    return FreeFormHeaderLayout(
        // ----------------------
        // 💜 TOP LEFT: TODAY
        // ----------------------
        topLeft = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },

        // ----------------------
        // 💛 TOP RIGHT: ENERGY ICON
        // ----------------------
        topRight = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⌁",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        // ----------------------
        // 💙 BOTTOM CENTER: Removed navigation
        // ----------------------
        bottomCenter = null // Explicitly set to null if no content
    )
}


/**
 * StrategyHeader: простий Left + (опис) + Right-іконка.
 */
@Composable
fun StrategyHeader(onModeClick: () -> Unit): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Strategy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Long-term planning mode",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        },
        right = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌁",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * Strategic Arc header.
 */
@Composable
fun StrategicArcHeader(onModeClick: () -> Unit): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Strategic Arc",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "April • Expansion Arc",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        },
        right = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌁",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * Command Deck / Dashboard header.
 */
@Composable
fun CommandDeckHeaderPreset(): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column {
                Text(
                    text = "ForwardApp",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Command & Control",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        },
        right = {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌬",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
