package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.ui.components.ContextLinkList
import java.net.URLEncoder

@Composable
fun StrategicArcScreen(
    navController: NavController,
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainScreenViewModel: ContextHierarchyScreenViewModel =
        hiltViewModel(navController.getBackStackEntry(MAIN_GRAPH_ROUTE))

    LaunchedEffect(navController) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle
            ?.getStateFlow<String?>("list_chooser_result", null)
            ?.collect { result ->
                if (result != null) {
                    savedStateHandle["list_chooser_result"] = null
                    if (result != "root") {
                        viewModel.addArcLink(result)
                    }
                }
            }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error!!)
        }
    } else {
        ContextLinkList(
            title = "Стратегічні арки",
            items = uiState.projects,
            onAddClick = {
                val disabledIds = uiState.projects.joinToString(",") { it.id }
                val title = URLEncoder.encode("Додати стратегічну арку", "UTF-8")
                val route =
                    if (disabledIds.isBlank()) {
                        "list_chooser_screen/$title"
                    } else {
                        "list_chooser_screen/$title?disabledIds=$disabledIds"
                    }
                navController.navigate(route)
            },
            onItemClick = { project ->
                navController.navigate("goal_detail_screen/${project.id}")
            },
            onRevealClick = { project ->
                mainScreenViewModel.onEvent(ContextHierarchyScreenEvent.RevealContextInHierarchy(project.id))
                navController.popBackStack()
            },
            onRemoveClick = { project ->
                viewModel.removeArcLink(project.id)
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
        )
    }
}
