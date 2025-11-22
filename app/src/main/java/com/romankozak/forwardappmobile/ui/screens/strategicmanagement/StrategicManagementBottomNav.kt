
package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ModernBottomNavButton

@Composable
fun StrategicManagementBottomNav(
    currentTab: StrategicManagementTab,
    onTabSelected: (StrategicManagementTab) -> Unit,
    onHomeClick: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModernBottomNavButton(
                    text = "Dashboard",
                    icon = Icons.Outlined.Dashboard,
                    isSelected = currentTab == StrategicManagementTab.DASHBOARD,
                    onClick = { onTabSelected(StrategicManagementTab.DASHBOARD) }
                )
                ModernBottomNavButton(text = "Projects", icon = Icons.Outlined.Home, onClick = onHomeClick)
                ModernBottomNavButton(
                    text = "AI Insights",
                    icon = Icons.Outlined.Analytics,
                    isSelected = currentTab == StrategicManagementTab.AI_INSIGHTS,
                    onClick = { onTabSelected(StrategicManagementTab.AI_INSIGHTS) }
                )
                ModernBottomNavButton(
                    text = "Ask AI",
                    icon = Icons.Outlined.Chat,
                    isSelected = currentTab == StrategicManagementTab.AI_CHAT,
                    onClick = { onTabSelected(StrategicManagementTab.AI_CHAT) }
                )
            }
        }
    }
}
