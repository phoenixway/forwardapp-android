package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.mainscreen.PlanningMode

@Composable
internal fun GoalListBottomNav(
    navController: NavController,
    isSearchActive: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SmallBottomNavButton(
                    text = "All",
                    icon = Icons.AutoMirrored.Outlined.List,
                    isSelected = currentMode is PlanningMode.All,
                    onClick = { onPlanningModeChange(PlanningMode.All) },
                )

                SmallBottomNavButton(
                    text = "Daily",
                    icon = Icons.Outlined.Today,
                    isSelected = currentMode is PlanningMode.Daily,
                    onClick = { onPlanningModeChange(PlanningMode.Daily) },
                )

                SmallBottomNavButton(
                    text = "Medium",
                    icon = Icons.Outlined.QueryStats,
                    isSelected = currentMode is PlanningMode.Medium,
                    onClick = { onPlanningModeChange(PlanningMode.Medium) },
                )

                SmallBottomNavButton(
                    text = "Long",
                    icon = Icons.Outlined.TrackChanges,
                    isSelected = currentMode is PlanningMode.Long,
                    onClick = { onPlanningModeChange(PlanningMode.Long) },
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
                                ).clip(RoundedCornerShape(16.dp)),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Global Search",
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                            ),
                                    )
                                }
                            },
                            onClick = {
                                onGlobalSearchClick()
                                showMoreMenu = false
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                    Column {
                                        Text(
                                            "AI Insights",
                                            style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                        )
                                        Text(
                                            "Smart recommendations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )

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
                            onClick = {
                                showMoreMenu = false
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModernBottomNavButton(
                    text = "Track",
                    icon = Icons.Outlined.Timeline,
                    onClick = { navController.navigate("activity_tracker_screen") },
                )

                ModernBottomNavButton(
                    text = "Filter",
                    icon = Icons.Outlined.FilterList,
                    isSelected = isSearchActive,
                    onClick = { onToggleSearch(true) },
                )

                ModernBottomNavButton(
                    text = "Contexts",
                    icon = Icons.Outlined.Style,
                    onClick = onContextsClick,
                )

                ModernBottomNavButton(
                    text = "Recent",
                    icon = Icons.Outlined.History,
                    onClick = onRecentsClick,
                )

                ModernBottomNavButton(
                    text = "AI Chat",
                    icon = Icons.Outlined.Chat,
                    onClick = { navController.navigate("chat_screen") },
                )
            }
        }
    }
}

@Composable
private fun ModernBottomNavButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else -> Color.Transparent
        }

    val contentColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else -> Color.Transparent
        }

    val contentColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
