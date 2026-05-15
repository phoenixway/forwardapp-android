package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ModernBottomNavButton
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab

private const val EXPANDED_ARROW_ROTATION = 180f
private const val COLLAPSED_ARROW_ROTATION = 0f
private const val EXPANSION_DAMPING_RATIO = 0.8f
private const val FADE_DURATION_MILLIS = 150

@Composable
fun DayManagementBottomNav(
    currentTab: DayManagementTab,
    onTabSelected: (DayManagementTab) -> Unit,
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val arrowRotation by
        animateFloatAsState(
            targetValue =
                if (isExpanded) {
                    EXPANDED_ARROW_ROTATION
                } else {
                    COLLAPSED_ARROW_ROTATION
                },
            label = "arrowAnimation",
        )

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
            ExpandedAnalyticsRow(
                visible = isExpanded,
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
            ExpandHandle(
                isExpanded = isExpanded,
                arrowRotation = arrowRotation,
                onToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                },
            )
            MainNavigationRow(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                onHomeClick = onHomeClick,
                onInboxClick = onInboxClick,
            )
        }
    }
}

@Composable
private fun ExpandedAnalyticsRow(
    visible: Boolean,
    currentTab: DayManagementTab,
    onTabSelected: (DayManagementTab) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            expandVertically(
                animationSpec =
                    spring(
                        dampingRatio = EXPANSION_DAMPING_RATIO,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) + fadeIn(tween(FADE_DURATION_MILLIS)),
        exit =
            shrinkVertically(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) + fadeOut(tween(FADE_DURATION_MILLIS)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ModernBottomNavButton(
                text = "Analytics",
                icon = Icons.Outlined.Analytics,
                isSelected = currentTab == DayManagementTab.ANALYTICS,
                onClick = { onTabSelected(DayManagementTab.ANALYTICS) },
            )
        }
    }
}

@Composable
private fun ExpandHandle(
    isExpanded: Boolean,
    arrowRotation: Float,
    onToggle: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.width(32.dp).height(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape,
            ) {}
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp).rotate(arrowRotation),
            )
        }
    }
}

@Composable
private fun MainNavigationRow(
    currentTab: DayManagementTab,
    onTabSelected: (DayManagementTab) -> Unit,
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ModernBottomNavButton(
            text = "Journal",
            icon = Icons.Outlined.Timeline,
            isSelected = currentTab == DayManagementTab.JOURNAL,
            onClick = { onTabSelected(DayManagementTab.JOURNAL) },
        )
        ModernBottomNavButton(
            text = "Plan",
            icon = Icons.AutoMirrored.Outlined.ListAlt,
            isSelected = currentTab == DayManagementTab.DAY_PLAN,
            onClick = { onTabSelected(DayManagementTab.DAY_PLAN) },
        )
        ModernBottomNavButton(
            text = "Contexts",
            icon = Icons.Outlined.Home,
            onClick = onHomeClick,
        )
        ModernBottomNavButton(
            text = "Dashboard",
            icon = Icons.Outlined.Dashboard,
            isSelected = currentTab == DayManagementTab.DASHBOARD,
            onClick = { onTabSelected(DayManagementTab.DASHBOARD) },
        )
        ModernBottomNavButton(
            text = "Inbox",
            icon = Icons.Outlined.Inbox,
            isSelected = false,
            onClick = onInboxClick,
        )
    }
}
