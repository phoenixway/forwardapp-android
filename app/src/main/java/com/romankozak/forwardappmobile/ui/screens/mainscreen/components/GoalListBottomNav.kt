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
                "Daily" to (Icons.Outlined.Today to PlanningMode.Daily),
                "Medium" to (Icons.Outlined.QueryStats to PlanningMode.Medium),
                "Long" to (Icons.Outlined.TrackChanges to PlanningMode.Long),
            )
        }

    val (currentText, currentData) = planningModes.first { it.second.second::class == currentMode::class }
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
internal fun ExpandingBottomNav(
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDayPlanClick: () -> Unit,
    onHomeClick: () -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    
    onAiChatClick: () -> Unit,
    onActivityTrackerClick: () -> Unit,
    onInsightsClick: () -> Unit,
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
                    PlanningModeSelector(
                        currentMode = currentMode,
                        onPlanningModeChange = onPlanningModeChange,
                    )

                    SmallBottomNavButton(
                        text = "Insights",
                        icon = Icons.Outlined.Lightbulb,
                        onClick = onInsightsClick,
                    )
                    SmallBottomNavButton(
                        text = "AI-Chat",
                        icon = Icons.Outlined.AutoAwesome,
                        
                        onClick = onAiChatClick,
                    )

                    Box {
                        SmallBottomNavButton(
                            text = "More",
                            icon = Icons.Outlined.MoreHoriz,
                            onClick = { showMoreMenu = !showMoreMenu },
                            isSelected = showMoreMenu,
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            offset = DpOffset((-16).dp, (-8).dp),
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .clip(RoundedCornerShape(16.dp)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Global Search") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Global Search") },
                                onClick = {
                                    onGlobalSearchClick()
                                    showMoreMenu = false
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.AccountBox,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                        )
                                        Column {
                                            Text(
                                                "AI Inbox",
                                                style =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                    ),
                                            )
                                            Text(
                                                "Messages from AI",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                },
                                onClick = { showMoreMenu = false },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
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
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ModernBottomNavButton(text = "Search", icon = Icons.Outlined.Search, isSelected = false, onClick = { onToggleSearch(true) })
                ModernBottomNavButton(text = "Day", icon = Icons.Outlined.WbSunny, onClick = onDayPlanClick)
                ModernBottomNavButton(text = "Home", icon = Icons.Outlined.Home, onClick = onHomeClick)
                ModernBottomNavButton(text = "Recent", icon = Icons.Outlined.History, onClick = onRecentsClick)
                ModernBottomNavButton(text = "Contexts", icon = Icons.Outlined.Style, onClick = onContextsClick)
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
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .widthIn(min = 60.dp),
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
