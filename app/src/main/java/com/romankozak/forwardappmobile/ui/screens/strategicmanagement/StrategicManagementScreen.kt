
package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    viewModel: StrategicManagementViewModel = hiltViewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel(remember { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }) 

    Scaffold(
        bottomBar = {
            StrategicManagementBottomNav(
                currentTab = currentTab,
                onTabSelected = viewModel::onTabSelected,
                onHomeClick = { navController.popBackStack() }
            )
        }
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
                        ProjectList(
                            projects = uiState.dashboardProjects,
                            navController = navController,
                            onRevealProject = { mainScreenViewModel.onEvent(MainScreenEvent.RevealProjectInHierarchy(it)) }
                        )
                    }
                    StrategicManagementTab.ELEMENTS -> {
                        ProjectList(
                            projects = uiState.elementsProjects,
                            navController = navController,
                            onRevealProject = { mainScreenViewModel.onEvent(MainScreenEvent.RevealProjectInHierarchy(it)) }
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
fun ProjectList(projects: List<Project>, navController: NavController, onRevealProject: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(projects) { project ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { navController.navigate("goal_detail_screen/${project.id}") }) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open Project")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { 
                            onRevealProject(project.id)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = "Open Location")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Location")
                        }
                    }
                }
            }
        }
    }
}
