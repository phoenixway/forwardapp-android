package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode

@Composable
private fun PlanningModeSelector(
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val planningModes =
        remember {
            listOf(
                "All" to (Icons.AutoMirrored.Outlined.List to PlanningMode.All),
                "Today" to (Icons.Outlined.Today to PlanningMode.Today),
                "Medium" to (Icons.Outlined.QueryStats to PlanningMode.Medium),
                "Long" to (Icons.Outlined.TrackChanges to PlanningMode.Long),
            )
        }

    val (currentText, currentData) = planningModes.first { it.second.second == currentMode }
    val (currentIcon, _) = currentData

    Box {
        val backgroundColor = if (isMenuExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent
        val contentColor =
            if (isMenuExpanded) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.8f,
                )
            }

        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .clickable { isMenuExpanded = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .widthIn(min = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = currentText,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentText,
                fontSize = 9.sp,
                fontWeight = if (isMenuExpanded) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            Column {
                Text(
                    text = "Режим планування",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                HorizontalDivider()
            }
            planningModes.forEach { (text, data) ->
                val (icon, mode) = data
                DropdownMenuItem(
                    text = { Text(text) },
                    leadingIcon = { Icon(icon, contentDescription = text) },
                    onClick = {
                        onPlanningModeChange(mode)
                        isMenuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun MoreActionsBottomNavButton(
    onInsightsClick: () -> Unit,
    onShowReminders: () -> Unit,
    onAiChatClick: () -> Unit,
    onLifeStateClick: () -> Unit,
    onTacticsClick: () -> Unit, // Added
    aiChatEnabled: Boolean,
    aiInsightsEnabled: Boolean,
    aiLifeManagementEnabled: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable { showMenu = true }
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "More Actions",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "More",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(0.dp, (-50).dp) // Adjust offset to position above the button
        ) {
            DropdownMenuItem(
                text = { Text("Tactics") },
                leadingIcon = { Icon(Icons.Outlined.MilitaryTech, contentDescription = "Tactics") },
                onClick = {
                    onTacticsClick()
                    showMenu = false
                }
            )
            if (aiLifeManagementEnabled) {
                DropdownMenuItem(
                    text = { Text("AI Life-Management") },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Life-Management") },
                    onClick = {
                        onLifeStateClick()
                        showMenu = false
                    }
                )
            }
            if (aiInsightsEnabled) {
                DropdownMenuItem(
                    text = { Text("Insights") },
                    leadingIcon = { Icon(Icons.Outlined.Lightbulb, contentDescription = "Insights") },
                    onClick = {
                        onInsightsClick()
                        showMenu = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Reminders") },
                leadingIcon = { Icon(Icons.Outlined.Notifications, contentDescription = "Reminders") },
                onClick = {
                    onShowReminders()
                    showMenu = false
                }
            )
            if (aiChatEnabled) {
                DropdownMenuItem(
                    text = { Text("AI-Chat") },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI-Chat") },
                    onClick = {
                        onAiChatClick()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun ExpandingBottomNav(
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    planningModesEnabled: Boolean,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDayPlanClick: () -> Unit,
    onHomeClick: () -> Unit,
    onStrManagementClick: () -> Unit,
    strategicManagementEnabled: Boolean,
    aiChatEnabled: Boolean,
    aiInsightsEnabled: Boolean,
    aiLifeManagementEnabled: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    
    onAiChatClick: () -> Unit,
    onActivityTrackerClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onShowReminders: () -> Unit,
    onLifeStateClick: () -> Unit,
    onTacticsClick: () -> Unit, // Added
    onEvent: (MainScreenEvent) -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (planningModesEnabled) {
                        PlanningModeSelector(
                            currentMode = currentMode,
                            onPlanningModeChange = onPlanningModeChange,
                        )
                    }
                    SmallBottomNavButton(
                        text = "Inbox",
                        icon = Icons.Outlined.Inbox,
                        onClick = { onEvent(MainScreenEvent.OpenInboxProject) },
                    )
                    SmallBottomNavButton(
                        text = "Contexts",
                        icon = Icons.Outlined.AccountTree,
                        onClick = onContextsClick,
                    )
                    SmallBottomNavButton(
                        text = "Tracker",
                        icon = Icons.Outlined.TrackChanges,
                        onClick = { onEvent(MainScreenEvent.NavigateToActivityTrackerScreen) },
                    )
                    MoreActionsBottomNavButton(
                        onInsightsClick = onInsightsClick,
                        onShowReminders = onShowReminders,
                        onAiChatClick = onAiChatClick,
                        onLifeStateClick = onLifeStateClick,
                        onTacticsClick = onTacticsClick, // Added
                        aiChatEnabled = aiChatEnabled,
                        aiInsightsEnabled = aiInsightsEnabled,
                        aiLifeManagementEnabled = aiLifeManagementEnabled,
                    )
                }
            }

            
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onExpandedChange(!isExpanded)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier =
                            Modifier
                                .size(20.dp)
                                .rotate(arrowRotation),
                    )
                }
            }


            
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModernBottomNavButton(text = "Search", icon = Icons.Outlined.Search, isSelected = false, onClick = { onToggleSearch(true) })
                ModernBottomNavButton(text = "Day", icon = Icons.Outlined.WbSunny, onClick = onDayPlanClick)
                ModernBottomNavButton(text = "Home", icon = Icons.Outlined.Home, onClick = onHomeClick)
                ModernBottomNavButton(text = "Recent", icon = Icons.Outlined.History, onClick = onRecentsClick)
                if (strategicManagementEnabled) {
                    ModernBottomNavButton(text = "Strategy", icon = Icons.Outlined.Domain, onClick = onStrManagementClick)
                }

            }
        }
    }
}

@Composable
fun ModernBottomNavButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        }

    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        }

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
        )
    }
}

@Composable
private fun SmallBottomNavButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        }

    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        }

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .widthIn(min = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
        )
    }
}
