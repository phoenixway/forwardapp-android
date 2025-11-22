package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.lifestate.LifeStateChatUiState
import com.romankozak.forwardappmobile.ui.screens.lifestate.LifeStateChatViewModel
import com.romankozak.forwardappmobile.ui.screens.lifestate.LifeStateViewModel
import com.romankozak.forwardappmobile.ui.screens.lifestate.AnalysisContent
import com.romankozak.forwardappmobile.ui.screens.lifestate.ChatSection

@Composable
fun StrategicManagementScreen(
  navController: NavController,
  viewModel: StrategicManagementViewModel = hiltViewModel(),
) {
  val currentTab by viewModel.currentTab.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val mainScreenViewModel: MainScreenViewModel =
    hiltViewModel(remember(navController.currentBackStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) })

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      StrategicManagementBottomNav(
        currentTab = currentTab,
        onTabSelected = viewModel::onTabSelected,
        onHomeClick = { navController.popBackStack() },
      )
    },
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues)) {
      if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = uiState.error!!)
        }
      } else {
        when (currentTab) {
          StrategicManagementTab.DASHBOARD -> {
            DashboardContent(
              projects = uiState.dashboardProjects,
              navController = navController,
              onRevealProject = {
                mainScreenViewModel.onEvent(MainScreenEvent.RevealProjectInHierarchy(it))
              },
            )
          }

          StrategicManagementTab.AI_INSIGHTS -> {
            AiAnalysisPane()
          }
          StrategicManagementTab.AI_CHAT -> {
            AiChatPane()
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardContent(
  projects: List<Project>,
  navController: NavController,
  onRevealProject: (String) -> Unit,
) {
  val motivationalPhrases = listOf(
      "The secret of getting ahead is getting started.",
      "The best time to plant a tree was 20 years ago. The second best time is now.",
      "It’s not whether you get knocked down, it’s whether you get up.",
      "The harder you work for something, the greater you’ll feel when you achieve it.",
      "Dream bigger. Do bigger."
  )
  val randomPhrase = remember { motivationalPhrases.random() }

  val (missionProjects, otherProjects) = remember(projects) {
      projects.partition { it.tags?.contains("mission") == true }
  }
  val sortedProjects = remember(missionProjects, otherProjects) {
      missionProjects + otherProjects
  }

  Column(
    modifier =
      Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 16.dp)
  ) {
    Text(
      text = "Dashboard",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = randomPhrase,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 16.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "Key Steps",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.padding(bottom = 16.dp),
    )

    ProjectsLazyColumn(
      projects = sortedProjects,
      navController = navController,
      onRevealProject = onRevealProject,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun AiAnalysisPane(
  viewModel: LifeStateViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "AI Life Analysis",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
    )
    when {
      uiState.isLoading -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator()
          Text("Preparing analysis…", style = MaterialTheme.typography.bodyMedium)
        }
      }
      uiState.error != null -> {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Failed to load analysis", color = MaterialTheme.colorScheme.error)
          Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall)
          TextButton(onClick = { viewModel.loadAnalysis(force = true) }) {
            Text("Retry")
          }
        }
      }
      uiState.analysis != null -> {
        AnalysisContent(
          analysis = uiState.analysis!!,
          onRegenerateAnalysis = { viewModel.loadAnalysis(force = true) },
          onBackgroundAnalysis = { viewModel.enqueueBackgroundAnalysis() },
          chatSection = null,
        )
      }
    }
  }
}

@Composable
private fun AiChatPane(
  lifeStateViewModel: LifeStateViewModel = hiltViewModel(),
  chatViewModel: LifeStateChatViewModel = hiltViewModel(),
) {
  val uiState by lifeStateViewModel.uiState.collectAsState()
  val chatState by chatViewModel.uiState.collectAsState()

  LaunchedEffect(uiState.analysis) {
    uiState.analysis?.let { chatViewModel.attachContext(it) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    when {
      uiState.isLoading -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator()
          Text("Preparing analysis context…", style = MaterialTheme.typography.bodyMedium)
        }
      }
      uiState.error != null -> {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Unable to load analysis", color = MaterialTheme.colorScheme.error)
          Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall)
          TextButton(onClick = { lifeStateViewModel.loadAnalysis(force = true) }) {
            Text("Retry")
          }
        }
      }
      uiState.analysis != null -> {
        val analysis = uiState.analysis!!
        ChatSection(
          state = chatState,
          onInputChange = chatViewModel::onInputChange,
          onSend = { chatViewModel.sendMessage(analysis) },
          onRegenerate = { chatViewModel.regenerate(analysis) },
          onRegenerateMessage = { msg -> chatViewModel.regenerateFromMessage(msg, analysis) },
          onQuickPrompt = { prompt -> chatViewModel.sendQuickPrompt(prompt, analysis) },
        )
      }
    }
  }
}

@Composable
private fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.height(120.dp),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}


@Composable
private fun ProjectListItem(
    project: Project,
    onItemClick: () -> Unit,
    onRevealClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onItemClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = project.name.firstOrNull()?.uppercase() ?: "P",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }

            IconButton(onClick = onRevealClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Reveal Project",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProjectsLazyColumn(
  projects: List<Project>,
  navController: NavController,
  onRevealProject: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    items(projects) { project ->
      ProjectListItem(
          project = project,
          onItemClick = { navController.navigate("goal_detail_screen/${project.id}") },
          onRevealClick = {
              onRevealProject(project.id)
              navController.popBackStack()
          }
      )
    }
  }
}
