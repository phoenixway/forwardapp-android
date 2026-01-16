package com.romankozak.forwardappmobile.features.daymanagement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DayManagementBottomBar(
    onAddTask: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarButton(
            icon = Icons.Outlined.Add,
            label = "Add Task",
            onClick = onAddTask
        )
        BarButton(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun BarButton(
    icon: ImageVector,
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface))
    }
}
