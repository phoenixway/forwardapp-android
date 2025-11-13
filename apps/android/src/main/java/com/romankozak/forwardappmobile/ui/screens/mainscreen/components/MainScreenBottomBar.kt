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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode

@Composable
fun MainScreenBottomBar(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    planningMode: PlanningMode,
    onPlanningModeChange: (PlanningMode) -> Unit,
    onPlaceholderAction: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "bottom_nav_arrow")

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PlanningModeSelector(planningMode, onPlanningModeChange)
                    SmallBottomNavButton(text = "Inbox", icon = Icons.Outlined.Inbox, onClick = onPlaceholderAction)
                    SmallBottomNavButton(text = "Contexts", icon = Icons.Outlined.AccountTree, onClick = onPlaceholderAction)
                    SmallBottomNavButton(text = "Tracker", icon = Icons.Outlined.TrackChanges, onClick = onPlaceholderAction)
                    MoreActionsBottomNavButton(
                        onInsightsClick = onPlaceholderAction,
                        onRemindersClick = onPlaceholderAction,
                        onAiChatClick = onPlaceholderAction,
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable { onExpandedChange(!isExpanded) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                    modifier =
                        Modifier
                            .rotate(arrowRotation)
                            .height(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModernBottomNavButton(text = "Search", icon = Icons.Outlined.Search, onClick = onPlaceholderAction)
                ModernBottomNavButton(text = "Day", icon = Icons.Outlined.WbSunny, onClick = onPlaceholderAction)
                ModernBottomNavButton(text = "Home", icon = Icons.Outlined.Home, onClick = onPlaceholderAction)
                ModernBottomNavButton(text = "Recent", icon = Icons.Outlined.History, onClick = onPlaceholderAction)
                ModernBottomNavButton(text = "Strategy", icon = Icons.Outlined.Domain, onClick = onPlaceholderAction)
            }
        }
    }
}

@Composable
private fun PlanningModeSelector(
    currentMode: PlanningMode,
    onModeChange: (PlanningMode) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val items =
        remember {
            listOf(
                "All" to (Icons.AutoMirrored.Outlined.List to PlanningMode.All),
                "Today" to (Icons.Outlined.Today to PlanningMode.Today),
                "Medium" to (Icons.Outlined.QueryStats to PlanningMode.Medium),
                "Long" to (Icons.Outlined.TrackChanges to PlanningMode.Long),
            )
        }
    val (label, iconAndMode) = items.first { it.second.second == currentMode }
    val (icon, _) = iconAndMode

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isMenuExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    else Color.Transparent,
                )
                .clickable { isMenuExpanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .widthIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
        items.forEach { (text, pair) ->
            val (iconVector, mode) = pair
            DropdownMenuItem(
                text = { Text(text) },
                leadingIcon = { Icon(iconVector, contentDescription = text) },
                onClick = {
                    onModeChange(mode)
                    isMenuExpanded = false
                },
            )
        }
    }
}

@Composable
private fun ModernBottomNavButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SmallBottomNavButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .widthIn(min = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun MoreActionsBottomNavButton(
    onInsightsClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onAiChatClick: () -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .clickable { isMenuOpen = true }
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "More",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )

        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Insights") },
                onClick = {
                    isMenuOpen = false
                    onInsightsClick()
                },
                leadingIcon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Reminders") },
                onClick = {
                    isMenuOpen = false
                    onRemindersClick()
                },
                leadingIcon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("AI Chat") },
                onClick = {
                    isMenuOpen = false
                    onAiChatClick()
                },
                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
            )
        }
    }
}
