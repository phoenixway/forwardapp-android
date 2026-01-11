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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenEvent
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
    onContextsClick: () -> Unit,
    aiChatEnabled: Boolean,
    aiInsightsEnabled: Boolean,
    aiLifeManagementEnabled: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { showMenu = true }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More Actions",
                tint = primary.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp),
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(0.dp, (-50).dp) // Adjust offset to position above the button
        ) {
            DropdownMenuItem(
                text = { Text("Contexts") },
                leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = "Contexts") },
                onClick = {
                    onContextsClick()
                    showMenu = false
                }
            )
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
internal fun ExpandingProjectHierarchyBottomNav(
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    onShowCommandDeck: () -> Unit,
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    planningModesEnabled: Boolean,
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
    onContextsClick: () -> Unit,
    onEvent: (ProjectHierarchyScreenEvent) -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowAnimation")

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ModernBottomNavButton(text = "Search", icon = Icons.Outlined.Search, isSelected = false, onClick = { onToggleSearch(true) })
                CommandDeckNavButton(onClick = onShowCommandDeck)
                ModernBottomNavButton(text = "Home", icon = Icons.Outlined.Home, onClick = onHomeClick)
                ModernBottomNavButton(text = "Recent", icon = Icons.Outlined.History, onClick = onRecentsClick)
                MoreActionsBottomNavButton(
                    onInsightsClick = onInsightsClick,
                    onShowReminders = onShowReminders,
                    onAiChatClick = onAiChatClick,
                    onLifeStateClick = onLifeStateClick,
                    onTacticsClick = onTacticsClick,
                    aiChatEnabled = aiChatEnabled,
                    aiInsightsEnabled = aiInsightsEnabled,
                    aiLifeManagementEnabled = aiLifeManagementEnabled,
                    onContextsClick = onContextsClick,
                )
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
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val fillAlpha = if (isSelected) 0.10f else 0.10f
    val borderAlpha = if (isSelected) 0.22f else 0.22f

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = fillAlpha))
                .border(
                    width = 1.dp,
                    color = primary.copy(alpha = borderAlpha),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = primary.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun CommandDeckNavButton(
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            Text(
                text = "⌬",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primary.copy(alpha = 0.9f)
            )
        }
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
