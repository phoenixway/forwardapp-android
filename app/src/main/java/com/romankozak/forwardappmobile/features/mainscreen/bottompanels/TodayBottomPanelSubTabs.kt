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
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelTokens

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
        shape = RoundedCornerShape(BottomPanelTokens.SubTabCornerRadius),
        color =
            if (selected) {
                colorScheme.primary.copy(alpha = BottomPanelTokens.SubTabSelectedContainerAlpha)
            } else {
                Color.Transparent
            },
        border =
            BorderStroke(
                width = BottomPanelTokens.BorderWidth,
                color =
                    if (selected) {
                        colorScheme.primary.copy(alpha = BottomPanelTokens.SubTabSelectedBorderAlpha)
                    } else {
                        colorScheme.onSurface.copy(alpha = BottomPanelTokens.SubTabBorderAlpha)
                    },
            ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(BottomPanelTokens.SubTabButtonSize),
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
                modifier = Modifier.size(BottomPanelTokens.SubTabIconSize),
            )
        }
    }
}
