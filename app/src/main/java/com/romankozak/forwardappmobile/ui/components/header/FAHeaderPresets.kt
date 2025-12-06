package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun TodayHeader(): FAHeaderConfig {
    val primaryColor = MaterialTheme.colorScheme.primary
    return FAHeaderConfig(
        backgroundStyle = FAHeaderBackground.CommandDeck,
        left = {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Today", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Operative Mode • ${FAHeaderUtils.currentDate()}",
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
                    .size(40.dp)
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
                        width = 1.5.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌁",
                    fontSize = 28.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun StrategyHeader(onModeClick: () -> Unit): FAHeaderConfig {
    val primaryColor = MaterialTheme.colorScheme.primary
    return FAHeaderConfig(
        backgroundStyle = FAHeaderBackground.CommandDeck,
        left = {
            Column(horizontalAlignment = Alignment.Start) {
                Text("Strategy", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
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
                    .size(40.dp)
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
                        width = 1.5.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌁",
                    fontSize = 28.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun StrategicArcHeader(onModeClick: () -> Unit): FAHeaderConfig {
    val primaryColor = MaterialTheme.colorScheme.primary
    return FAHeaderConfig(
        backgroundStyle = FAHeaderBackground.CommandDeck,
        left = {
            Column(horizontalAlignment = Alignment.Start) {
                Text("Strategic Arc", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
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
                    .size(40.dp)
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
                        width = 1.5.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌁",
                    fontSize = 28.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun CommandDeckHeaderPreset(): FAHeaderConfig {
    val primaryColor = MaterialTheme.colorScheme.primary
    return FAHeaderConfig(
        backgroundStyle = FAHeaderBackground.CommandDeck,
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
                    .size(40.dp)
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
                        width = 1.5.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌬",
                    fontSize = 28.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
