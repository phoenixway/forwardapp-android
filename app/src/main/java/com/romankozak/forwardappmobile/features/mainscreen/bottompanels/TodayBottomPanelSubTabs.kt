package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab

@Composable
fun TodaySubTabs(
    selectedTab: DayManagementTab,
    onTabSelected: (DayManagementTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayManagementTab.todaySubTabs().forEach { tab ->
            TodaySubTabButton(
                tab = tab,
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun TodaySubTabButton(
    tab: DayManagementTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                colorScheme.primary.copy(alpha = 0.18f)
            } else {
                Color.Transparent
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (selected) {
                        colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.08f)
                    },
            ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp),
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor =
                        if (selected) {
                            colorScheme.primary
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                ),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
