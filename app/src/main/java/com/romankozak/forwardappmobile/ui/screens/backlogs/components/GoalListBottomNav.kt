package com.romankozak.forwardappmobile.ui.screens.backlogs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.backlogs.PlanningMode

@Composable
internal fun GoalListBottomNav(
    navController: NavController,
    isSearchActive: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onModeSelectorClick: () -> Unit,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavButton(
                text = "Track",
                icon = { Icon(Icons.Outlined.Timeline, contentDescription = "Activity Tracker") },
                onClick = { navController.navigate("activity_tracker_screen") }
            )
            BottomNavButton(
                text = "Search",
                icon = { Icon(Icons.Outlined.Search, contentDescription = "Global Search") },
                onClick = onGlobalSearchClick
            )
            BottomNavButton(
                text = "Filter",
                icon = { Icon(Icons.Outlined.FilterList, contentDescription = "Filter") },
                isSelected = isSearchActive,
                onClick = { onToggleSearch(true) }
            )
            BottomNavButton(
                text = "Contexts",
                icon = { Icon(Icons.Outlined.Style, contentDescription = "Contexts") },
                onClick = onContextsClick
            )
            BottomNavButton(
                text = "Recent",
                icon = { Icon(Icons.Outlined.History, contentDescription = "Recent Lists") },
                onClick = onRecentsClick
            )

            val (currentIcon, currentLabel) = when (currentMode) {
                is PlanningMode.Daily -> Icons.Outlined.Today to "Daily"
                is PlanningMode.Medium -> Icons.Outlined.QueryStats to "Medium"
                is PlanningMode.Long -> Icons.Outlined.TrackChanges to "Long"
                else -> Icons.AutoMirrored.Outlined.List to "All"
            }

            BottomNavButton(
                text = currentLabel,
                icon = { Icon(currentIcon, contentDescription = "Change planning mode") },
                isSelected = !isSearchActive && (currentMode !is PlanningMode.All),
                onClick = onModeSelectorClick
            )
            BottomNavButton(
                text = "AI Inbox",
                icon = { Icon(Icons.Outlined.AccountBox, contentDescription = "Messages from AI") },
                onClick = {}
            )
            BottomNavButton(
                text = "AI Chat",
                icon = { Icon(Icons.Outlined.AddComment, contentDescription = "Chat with AI") },
                onClick = {}
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    text: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Просто викликаємо передану іконку
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
        Text(
            text = text,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor
        )
    }
}
