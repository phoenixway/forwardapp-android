package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.ui.screens.common.ProjectListItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.ProjectHierarchyScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenEvent

@Composable
fun CoreLevelScreen(
    navController: NavController,
    viewModel: CoreLevelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainScreenViewModel: ProjectHierarchyScreenViewModel =
        hiltViewModel(navController.getBackStackEntry(MAIN_GRAPH_ROUTE))

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error!!)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Related",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(uiState.projects) { project ->
                ProjectListItem(
                    project = project,
                    onItemClick = { navController.navigate("goal_detail_screen/${project.id}") },
                    onRevealClick = {
                        mainScreenViewModel.onEvent(ProjectHierarchyScreenEvent.RevealProjectInHierarchy(project.id))
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
