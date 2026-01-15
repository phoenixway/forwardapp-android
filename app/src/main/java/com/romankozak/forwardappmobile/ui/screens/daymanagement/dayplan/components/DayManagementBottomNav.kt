package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components

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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementTab
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ModernBottomNavButton

@Composable
fun DayManagementBottomNav(
    currentTab: DayManagementTab,
    onTabSelected: (DayManagementTab) -> Unit,
    onHomeClick: () -> Unit,
    onInboxClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowAnimation")

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
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(150)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ModernBottomNavButton(
                        text = "Analytics",
                        icon = Icons.Outlined.Analytics,
                        isSelected = currentTab == DayManagementTab.ANALYTICS,
                        onClick = { onTabSelected(DayManagementTab.ANALYTICS) }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(32.dp).clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                },
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                                ModernBottomNavButton(
                    text = "Track",
                    icon = Icons.Outlined.Timeline,
                    isSelected = currentTab == DayManagementTab.TRACK,
                    onClick = { onTabSelected(DayManagementTab.TRACK) }
                )
                ModernBottomNavButton(
                    text = "Plan",
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    isSelected = currentTab == DayManagementTab.PLAN,
                    onClick = { onTabSelected(DayManagementTab.PLAN) }
                )
                ModernBottomNavButton(text = "Contexts", icon = Icons.Outlined.Home, onClick = onHomeClick)
                ModernBottomNavButton(
                    text = "Dashboard",
                    icon = Icons.Outlined.Dashboard,
                    isSelected = currentTab == DayManagementTab.DASHBOARD,
                    onClick = { onTabSelected(DayManagementTab.DASHBOARD) }
                )
                ModernBottomNavButton(
                    text = "Inbox",
                    icon = Icons.Outlined.Inbox,
                    isSelected = false, // It's not a tab, so it's never selected
                    onClick = onInboxClick
                )
            }
        }
    }
}