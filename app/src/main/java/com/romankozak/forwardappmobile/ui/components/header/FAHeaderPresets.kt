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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.ui.components.header.DayPlanHeaderContent

/**
 * TodayHeader:
 *  - верх: ліворуч "Today", по центру DayPlanHeaderContent
 *  - низ (праворуч): навігація по днях + "енергетичний" кружок
 */
@Composable
fun TodayHeader(
    dayPlan: DayPlan?,
    totalPointsEarned: Int,
    totalPointsAvailable: Int,
    bestCompletedPoints: Int,
    completedTasks: Int,
    totalTasks: Int,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
): HeaderLayout {

    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val dateText = dayPlan?.dateFormatted ?: "Unknown date"

    val statusColor = onSurface.copy(alpha = 0.65f)

    return FreeFormHeaderLayout(

        // ----------------------
        // 💜 TOP LEFT: TODAY
        // ----------------------
        topLeft = {
            Column {
                Text(
                    "Today",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.height(2.dp))

                // ------- compact status row -------
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        color = statusColor
                    )
                    Text(
                        text = "Tasks: $completedTasks / $totalTasks",
                        fontSize = 11.sp,
                        color = statusColor
                    )
                    Text(
                        text = "Points: $totalPointsEarned / $totalPointsAvailable",
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
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
                                primary.copy(alpha = 0.25f),
                                primary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = primary.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⌁",
                    fontSize = 22.sp,
                    color = primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        // ----------------------
        // 💙 BOTTOM CENTER: NAVIGATION
        // ----------------------
        bottomCenter = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IconButton(onClick = onNavigateToPreviousDay) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous day"
                    )
                }

                IconButton(
                    onClick = onNavigateToNextDay,
                    enabled = isNextDayNavigationEnabled
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next day",
                        tint = if (isNextDayNavigationEnabled)
                            primary
                        else
                            primary.copy(alpha = 0.4f)
                    )
                }
            }
        }
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
