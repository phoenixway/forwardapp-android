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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent

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

          StrategicManagementTab.ELEMENTS -> {
            ProjectList(
              projects = uiState.elementsProjects,
              navController = navController,
              onRevealProject = {
                mainScreenViewModel.onEvent(MainScreenEvent.RevealProjectInHierarchy(it))
              },
            )
          }

          StrategicManagementTab.INSIGHTS -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("Insights")
            }
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

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      DashboardCard(title = "Progress", value = "72%", modifier = Modifier.weight(1f))
      DashboardCard(title = "Weekly Tasks", value = "5/8", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "Key Steps",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.padding(bottom = 16.dp),
    )

    ProjectsLazyColumn(
      projects = projects,
      navController = navController,
      onRevealProject = onRevealProject,
      modifier = Modifier.fillMaxWidth(),
    )
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
fun ProjectList(
  projects: List<Project>,
  navController: NavController,
  onRevealProject: (String) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 16.dp)
  ) {
    Text(
      text = "Elements",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      DashboardCard(title = "Progress", value = "72%", modifier = Modifier.weight(1f))
      DashboardCard(title = "Weekly Tasks", value = "5/8", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = "All Projects",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.padding(bottom = 16.dp),
    )

    ProjectsLazyColumn(
      projects = projects,
      navController = navController,
      onRevealProject = onRevealProject,
      modifier = Modifier.fillMaxWidth(),
    )
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
      Card(
        modifier =
          Modifier.fillMaxWidth().clickable {
            navController.navigate("goal_detail_screen/${project.id}")
          },
        shape = MaterialTheme.shapes.large,
        colors =
          CardDefaults.cardColors(
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
              modifier =
                Modifier.size(40.dp)
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

          IconButton(
            onClick = {
              onRevealProject(project.id)
              navController.popBackStack()
            }
          ) {
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
  }
}
