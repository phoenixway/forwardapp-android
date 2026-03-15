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

@Composable
private fun HeaderTitleBlock(
    title: String,
    subtitle: String,
    titleTrailingContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (titleTrailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                titleTrailingContent()
            }
        }
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            color = primaryColor.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun HeaderGlyphBadge(
    glyph: String,
    modifier: Modifier = Modifier,
    glyphSize: androidx.compose.ui.unit.TextUnit = 22.sp,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
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
            text = glyph,
            fontSize = glyphSize,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * TodayHeader:
 *  - верх: ліворуч "Today", по центру DayPlanHeaderContent
 *  - низ (праворуч): навігація по днях + "енергетичний" кружок
 */
@Composable
fun TodayHeader(
    date: Long?,
    statsText: String? = null,
    titleTrailingContent: @Composable (() -> Unit)? = null,
): HeaderLayout {
    if (date != null) {
        // Keep parameter wired for future date-specific UI without changing current layout.
    }
    val primaryColor = MaterialTheme.colorScheme.primary

    return FreeFormHeaderLayout(
        // ----------------------
        // 💜 TOP LEFT: TODAY
        // ----------------------
        topLeft = {
            Column(horizontalAlignment = Alignment.Start) {
                HeaderTitleBlock(
                    title = "Today",
                    subtitle = "The Alpha and Omega of everything",
                    titleTrailingContent = titleTrailingContent,
                )
                Column {
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
            Column(horizontalAlignment = Alignment.End) { HeaderGlyphBadge(glyph = "⌁") }
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
fun StrategyHeader(
    onModeClick: () -> Unit,
    titleTrailingContent: @Composable (() -> Unit)? = null,
): HeaderLayout {
    return LeftCenterCombinedHeaderLayout(
        left = {
            HeaderTitleBlock(
                title = "Strategy",
                subtitle = "Long-term planning mode",
                titleTrailingContent = titleTrailingContent,
                modifier = Modifier.clickable(onClick = onModeClick),
            )
        },
        right = { HeaderGlyphBadge(glyph = "❂") },
    )
}

/**
 * Strategic Arc header.
 */
@Composable
fun StrategicArcHeader(
    onModeClick: () -> Unit,
    titleTrailingContent: @Composable (() -> Unit)? = null,
): HeaderLayout {
    return LeftCenterCombinedHeaderLayout(
        left = {
            HeaderTitleBlock(
                title = "Strategic Arc",
                subtitle = "The current Arc of your story",
                titleTrailingContent = titleTrailingContent,
                modifier = Modifier.clickable(onClick = onModeClick),
            )
        },
        right = { HeaderGlyphBadge(glyph = "⟲") },
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
    titleTrailingContent: @Composable (() -> Unit)? = null,
): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        onClick = onClick,
        left = {
            HeaderTitleBlock(
                title = "Forward",
                subtitle = "Score your goals!",
                titleTrailingContent = titleTrailingContent,
            )
        },
        right = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (rightContent != null) {
                    rightContent()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                HeaderGlyphBadge(
                    glyph = "⌬",
                    modifier =
                        if (onRightClick != null) {
                            Modifier.clickable { onRightClick() }
                        } else {
                            Modifier
                        },
                )
            }
        },
    )
}

/**
 * Tactics header.
 */
@Composable
fun TacticsHeader(titleTrailingContent: @Composable (() -> Unit)? = null): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tactics",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (titleTrailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        titleTrailingContent()
                    }
                }
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
fun CoreHeader(titleTrailingContent: @Composable (() -> Unit)? = null): HeaderLayout {
    val primaryColor = MaterialTheme.colorScheme.primary

    return LeftCenterCombinedHeaderLayout(
        left = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Core",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (titleTrailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        titleTrailingContent()
                    }
                }
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
