package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------
// DATA MODELS
// ---------------------------------------------

data class DeckCategory(val title: String, val color: Color, val actions: List<DeckAction>)

data class DeckAction(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val color: Color,
  val onClick: () -> Unit,
)

data class DeckAnalytics(val category: String, val status: LevelStatus, val count: Int)

enum class LevelStatus {
  OK,
  ATTENTION,
  CRITICAL,
}

fun levelColor(status: LevelStatus): Color =
  when (status) {
    LevelStatus.OK -> Color(0xFF4CAF50) // green
    LevelStatus.ATTENTION -> Color(0xFFFFC107) // yellow
    LevelStatus.CRITICAL -> Color(0xFFF44336) // red
  }

// ---------------------------------------------
// MAIN SCREEN
// ---------------------------------------------

@Composable
fun AnimatedCommandDeck(
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
  val categories = remember {
    listOf(
      DeckCategory(
        title = "AI Tools",
        color = Color(0xFF9D7BFF),
        actions =
          listOf(
            DeckAction(
              "AI Chat",
              "Chat with AI",
              Icons.Outlined.QuestionAnswer,
              Color(0xFF9D7BFF),
              onNavigateToAiChat,
            ),
            DeckAction(
              "AI Life Management",
              "AI-driven insights",
              Icons.Outlined.AutoAwesome,
              Color(0xFFDA9CFF),
              onNavigateToAiLifeManagement,
            ),
          ),
      ),
      DeckCategory(
        title = "Personal Ops",
        color = Color(0xFF5DACFF),
        actions =
          listOf(
            DeckAction(
              "Inbox",
              "Process tasks",
              Icons.Outlined.Inbox,
              Color(0xFF77B6FF),
              onNavigateToInbox,
            ),
            DeckAction(
              "Tracker",
              "Track time & habits",
              Icons.Outlined.Analytics,
              Color(0xFFFF6D6D),
              onNavigateToTracker,
            ),
            DeckAction(
              "Reminders",
              "What’s due?",
              Icons.Outlined.Notifications,
              Color(0xFFFFD93D),
              onNavigateToReminders,
            ),
            DeckAction(
              "Attachments",
              "Files & notes",
              Icons.Outlined.AttachFile,
              Color(0xFF7CE8FF),
              onNavigateToAttachments,
            ),
          ),
      ),
      DeckCategory(
        title = "System",
        color = Color(0xFFFFA85D),
        actions =
          listOf(
            DeckAction(
              "Import/Export",
              "Sync data",
              Icons.Outlined.ImportExport,
              Color(0xFFFFA85D),
              onNavigateToImportExport,
            ),
            DeckAction(
              "Scripts",
              "Automation tools",
              Icons.Outlined.Code,
              Color(0xFFB0FF61),
              onNavigateToScripts,
            ),
            DeckAction(
              "Projects",
              "Hierarchy view",
              Icons.Outlined.AccountTree,
              Color(0xFF5DE2FF),
              onNavigateToProjectHierarchy,
            ),
            DeckAction(
              "Search",
              "Search everything",
              Icons.Outlined.Search,
              Color(0xFFF57AFF),
              onNavigateToGlobalSearch,
            ),
            DeckAction(
              "Settings",
              "Adjust the system",
              Icons.Outlined.Settings,
              Color(0xFFA6B3C3),
              onNavigateToSettings,
            ),
          ),
      ),
    )
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Text("Command Deck", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
    val analytics =
      listOf(
        DeckAnalytics(category = "AI Tools", status = LevelStatus.OK, count = 0),
        DeckAnalytics(category = "Personal Ops", status = LevelStatus.ATTENTION, count = 3),
        DeckAnalytics(category = "System", status = LevelStatus.CRITICAL, count = 1),
      )

    AnalyticsOverviewBar(analytics)

    categories.forEach { category -> AnimatedDeckCategory(category) }
  }
}

// ---------------------------------------------
// CATEGORY BLOCK (animated)
// ---------------------------------------------

@Composable
fun AnimatedDeckCategory(category: DeckCategory) {
  var expanded by remember { mutableStateOf(true) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

    // Header row (click to expand)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { expanded = !expanded }
          .background(Color.White.copy(alpha = 0.05f))
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(34.dp).clip(CircleShape).background(category.color.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = category.color)
      }

      Spacer(Modifier.width(12.dp))

      Text(
        text = category.title,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.weight(1f),
      )

      Icon(
        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = Color.LightGray,
      )
    }

    // Animated section
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + expandVertically(expandFrom = Alignment.Top) + scaleIn(),
      exit = fadeOut() + shrinkVertically() + scaleOut(),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        category.actions.forEachIndexed { i, action -> AnimatedDeckAction(action, i) }
      }
    }
  }
}

// ---------------------------------------------
// ACTION CARD (animated)
// ---------------------------------------------

@Composable
fun AnimatedDeckAction(action: DeckAction, index: Int) {
  // Delay animation per item
  val delay = index * 50

  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(delay.toLong())
    visible = true
  }

  AnimatedVisibility(
    visible = visible,
    enter =
      fadeIn(animationSpec = tween(350)) +
        slideInVertically(animationSpec = tween(350), initialOffsetY = { it / 2 }),
    exit = fadeOut(),
  ) {
    DeckActionCard(action)
  }
}

// ---------------------------------------------
// Visual card
// ---------------------------------------------

@Composable
fun DeckActionCard(action: DeckAction) {
  Card(
    onClick = action.onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp), clip = false),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Icon capsule
      Box(
        modifier =
          Modifier.size(44.dp).clip(CircleShape).background(action.color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          action.icon,
          contentDescription = null,
          tint = action.color,
          modifier = Modifier.size(26.dp),
        )
      }

      Column(Modifier.weight(1f)) {
        Text(action.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(action.subtitle, fontSize = 13.sp, color = Color.LightGray)
      }
    }
  }
}

@Composable
fun AnalyticsOverviewBar(analytics: List<DeckAnalytics>) {

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(alpha = 0.04f))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    analytics.forEachIndexed { index, item ->
      var visible by remember { mutableStateOf(false) }

      LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60).toLong())
        visible = true
      }

      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(),
      ) {
        AnalyticsRow(item)
      }
    }
  }
}

@Composable
fun AnalyticsRow(item: DeckAnalytics) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(item.category, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)

    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(12.dp).clip(CircleShape).background(levelColor(item.status)))

      Spacer(Modifier.width(8.dp))

      Text(
        when (item.status) {
          LevelStatus.OK -> "OK"
          LevelStatus.ATTENTION -> "Attention"
          LevelStatus.CRITICAL -> "Critical"
        },
        color = Color.LightGray,
        fontSize = 14.sp,
      )

      Spacer(Modifier.width(12.dp))

      Text("${item.count} items", color = Color.Gray, fontSize = 13.sp)
    }
  }
}
