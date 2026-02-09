package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TodayHeader:
 *  - верх: ліворуч "Today", по центру DayPlanHeaderContent
 *  - низ (праворуч): навігація по днях + "енергетичний" кружок
 */
@Composable
fun TodayHeader(
    date: Long?,
    statsText: String? = null,
): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return FreeFormHeaderLayout(
        // ----------------------
        // 💜 TOP LEFT: TODAY
        // ----------------------
        topLeft = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Today",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Column {
                    Text(
                        text = "The Alpha and Omega of everything",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        color = primaryColor.copy(alpha = 0.7f),
                    )
                    if (!statsText.isNullOrBlank()) {
                        Text(
                            text = statsText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        },
        // ----------------------
        // 💛 TOP RIGHT: ENERGY ICON + Day Navigation
        // ----------------------
        topRight = {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.2.dp,
                                color = primaryColor.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "⌁",
                        fontSize = 22.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        // ----------------------
        // 💙 BOTTOM CENTER: Day Navigation
        // ----------------------
        bottomCenter = {
            // MOVED TO TOP RIGHT
        },
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
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Text(
                    text = "Long-term planning mode",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                )
            }
        },
        right = {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f),
                                    ),
                            ),
                        )
                        .border(
                            width = 1.2.dp,
                            color = primaryColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "❂",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
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
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Text(
                    text = "The current Arc of your story",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                )
            }
        },
        right = {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f),
                                    ),
                            ),
                        )
                        .border(
                            width = 1.2.dp,
                            color = primaryColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⟲",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

/**
 * Command Deck / Dashboard header.
 */
@Composable
fun CommandDeckHeaderPreset(
    onClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null,
): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        onClick = onClick,
        left = {
            Column {
                Text(
                    text = "Forward",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Score your goals!",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                )
            }
        },
        right = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (rightContent != null) {
                    rightContent()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.25f),
                                            primaryColor.copy(alpha = 0.08f),
                                        ),
                                ),
                            )
                            .border(
                                width = 1.2.dp,
                                color = primaryColor.copy(alpha = 0.4f),
                                shape = CircleShape,
                            )
                            .let { base ->
                                if (onRightClick != null) {
                                    base.clickable { onRightClick() }
                                } else {
                                    base
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⌬",
                        fontSize = 22.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    )
}

/**
 * Tactics header.
 */
@Composable
fun TacticsHeader(): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column {
                Text(
                    text = "Tactics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your job is to make the impossible possible.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                )
            }
        },
        right = {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f),
                                    ),
                            ),
                        )
                        .border(
                            width = 1.2.dp,
                            color = primaryColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⦿",
                    fontSize = 23.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

/**
 * Core header.
 */
@Composable
fun CoreHeader(): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column {
                Text(
                    text = "Core",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your primary beacons.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                )
            }
        },
        right = {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f),
                                    ),
                            ),
                        )
                        .border(
                            width = 1.2.dp,
                            color = primaryColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⌘",
                    fontSize = 22.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}
