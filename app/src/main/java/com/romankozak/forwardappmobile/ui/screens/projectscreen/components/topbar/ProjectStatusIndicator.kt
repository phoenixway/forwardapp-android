// File: ProjectStatusIndicator.kt
package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus
import kotlinx.coroutines.delay

data class StatusVisuals(
    val emoji: String,
    val color: Color,
)

@Composable
private fun getStatusVisuals(status: ProjectStatus): StatusVisuals =
    when (status) {
        ProjectStatus.NO_PLAN -> StatusVisuals("⚠️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatus.PLANNING -> StatusVisuals("📝", Color(0xFF9C27B0).copy(alpha = 0.3f))
        ProjectStatus.IN_PROGRESS -> StatusVisuals("▶️", Color(0xFF2196F3).copy(alpha = 0.3f))
        ProjectStatus.COMPLETED -> StatusVisuals("✅", Color(0xFF4CAF50).copy(alpha = 0.3f))
        ProjectStatus.ON_HOLD -> StatusVisuals("⏸️", Color(0xFFFF9800).copy(alpha = 0.3f))
        ProjectStatus.PAUSED -> StatusVisuals("⏳", Color(0xFFFFC107).copy(alpha = 0.3f))
    }


@Composable
fun ProjectStatusIndicator(
    status: ProjectStatus,
    statusText: String?,
    modifier: Modifier = Modifier,
) {
    val visuals = getStatusVisuals(status = status)

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter =
            fadeIn(
                animationSpec = tween(400, easing = EaseOut),
            ) +
                    slideInVertically(
                        animationSpec = tween(400, easing = EaseOut),
                        initialOffsetY = { it / 3 },
                    ),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    ).border(
                        width = 0.5.dp,
                        color = visuals.color.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 14.dp, vertical = 8.dp)
                    .alpha(0.85f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = visuals.emoji,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    Text(
                        text = status.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }

                AnimatedVisibility(
                    visible = !statusText.isNullOrBlank(),
                    enter =
                        fadeIn(
                            animationSpec = tween(250, delayMillis = 100),
                        ) +
                                expandVertically(
                                    animationSpec = tween(250, delayMillis = 100),
                                ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = statusText ?: "",
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }
            }

            // --- 👇 ПОЧАТОК ЗМІН ---
            // Блок, що відображав режим виду, видалено
            // --- 👆 КІНЕЦЬ ЗМІН ---

            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🙂",
                    fontSize = 10.sp,
                    modifier = Modifier.alpha(0.7f),
                )
            }
        }
    }
}