package com.romankozak.forwardappmobile.ui.screens.commanddeck.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DashboardBottomBar(
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToMore: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    // Infinite neon wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            tween(5500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "wave_shift"
    )

    val waveIntensity by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            tween(3800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "wave_intensity"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.02f),
                        primary.copy(alpha = 0.06f + waveIntensity),
                        primary.copy(alpha = 0.02f),
                    ),
                    start = androidx.compose.ui.geometry.Offset(waveShift, 0f),
                    end = androidx.compose.ui.geometry.Offset(-waveShift, 300f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.35f + waveIntensity),
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.35f + waveIntensity),
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BarButton(
                icon = Icons.Outlined.Inbox,
                label = "Inbox",
                onClick = onNavigateToInbox
                )

            BarButton(
                icon = Icons.Outlined.Analytics,
                label = "Tracker",
                onClick = onNavigateToTracker
            )

            BarButton(
                icon = Icons.Outlined.AccountTree,
                onClick = onNavigateToProjectHierarchy,

                label = "Projects",

            )
            BarButton(Icons.Outlined.Notifications, "Reminders", onNavigateToReminders)
            BarButton(Icons.Outlined.MoreHoriz, "More", onNavigateToMore)
        }
    }
}

@Composable
private fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = primary.copy(alpha = 0.22f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = primary.copy(alpha = 0.9f))
        }
    }
}

@Preview
@Composable
fun DashboardBottomBarPreview() {
    DashboardBottomBar({}, {}, {}, {}, {})
}

