package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardContent(
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CoreQuickActions(
            onNavigateToProjectHierarchy,
            onNavigateToGlobalSearch,
            onNavigateToSettings,
            onNavigateToInbox,
            onNavigateToTracker,
            onNavigateToReminders,
            onNavigateToAiChat,
            onNavigateToAiLifeManagement,
            onNavigateToImportExport,
            onNavigateToAttachments,
            onNavigateToScripts
        )
    }
}

@Composable
fun CoreQuickActions(
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        QuickActionCard("Inbox", "View inbox", Icons.Outlined.Inbox, onNavigateToInbox)
        QuickActionCard("Tracker", "Track activity", Icons.Outlined.Analytics, onNavigateToTracker)
        QuickActionCard("Reminders", "See reminders", Icons.Outlined.Notifications, onNavigateToReminders)
        QuickActionCard("AI Chat", "Chat with AI", Icons.Outlined.Chat, onNavigateToAiChat)
        QuickActionCard("AI Life Management", "AI guidance", Icons.Outlined.AutoAwesome, onNavigateToAiLifeManagement)
        QuickActionCard("Import/Export", "Transfer data", Icons.Outlined.ImportExport, onNavigateToImportExport)
        QuickActionCard("Attachments", "All files & notes", Icons.Outlined.AttachFile, onNavigateToAttachments)
        QuickActionCard("Scripts", "Automation scripts", Icons.Outlined.Code, onNavigateToScripts)
        QuickActionCard("Проєкти", "Project hierarchy", Icons.Outlined.AccountTree, onNavigateToProjectHierarchy)
        QuickActionCard("Глобальний пошук", "Search anything", Icons.Outlined.Search, onNavigateToGlobalSearch)
        QuickActionCard("Налаштування", "App settings", Icons.Outlined.Settings, onNavigateToSettings)
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.18f)
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6200EE).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF6200EE)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFFCCCCCC)
                )
            }
        }
    }
}
