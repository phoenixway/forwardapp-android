package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ModernBottomNavButton

@Composable
fun DayPlanBottomNav(
    onHomeClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onDashboardClick: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ModernBottomNavButton(text = "Projects", icon = Icons.Default.Home, onClick = onHomeClick)
            ModernBottomNavButton(text = "Analytics", icon = Icons.Outlined.Analytics, onClick = onAnalyticsClick)
            ModernBottomNavButton(text = "Dashboard", icon = Icons.Outlined.Dashboard, onClick = onDashboardClick)
            ModernBottomNavButton(text = "Add", icon = Icons.Default.Add, onClick = onAddTaskClick)
        }
    }
}