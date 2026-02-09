package com.romankozak.forwardappmobile.features.strategicmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.lifestate.AnalysisContent
import com.romankozak.forwardappmobile.features.lifestate.ChatSection
import com.romankozak.forwardappmobile.features.lifestate.LifeStateChatViewModel
import com.romankozak.forwardappmobile.features.lifestate.LifeStateViewModel
import com.romankozak.forwardappmobile.ui.components.ContextLinkList
import com.romankozak.forwardappmobile.ui.screens.common.ProjectListItem
import java.net.URLEncoder

@Composable
fun StrategicManagementScreen(
    navController: NavController,
    viewModel: StrategicManagementViewModel = hiltViewModel(),
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val mainScreenViewModel: ContextHierarchyScreenViewModel =
        hiltViewModel(remember(navController.currentBackStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) })

    LaunchedEffect(navController) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle
            ?.getStateFlow<String?>("list_chooser_result", null)
            ?.collect { result ->
                if (result != null) {
                    savedStateHandle["list_chooser_result"] = null
                    if (result != "root") {
                        viewModel.addStrategicLink(result)
                    }
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) { // Removed padding(paddingValues) here
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
                            onRevealProject = { projectId ->
                                mainScreenViewModel.onEvent(ContextHierarchyScreenEvent.RevealContextInHierarchy(projectId))
                            },
                            onRemoveProject = { projectId ->
                                viewModel.removeStrategicLink(projectId)
                            },
                            scaffoldPadding = paddingValues,
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
    projects: List<Context>,
    navController: NavController,
    onRevealProject: (String) -> Unit,
    onRemoveProject: (String) -> Unit,
    scaffoldPadding: PaddingValues, // New parameter for Scaffold's padding
    modifier: Modifier = Modifier,
) {
    val (missionProjects, otherProjects) =
        remember(projects) {
            projects.partition { it.tags?.contains("mission") == true }
        }
    val sortedProjects =
        remember(missionProjects, otherProjects) {
            missionProjects + otherProjects
        }

    ContextLinkList(
        items = sortedProjects,
        onAddClick = {
            val disabledIds = sortedProjects.joinToString(",") { it.id }
            val title = URLEncoder.encode("Додати стратегічний контекст", "UTF-8")
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
            onRevealProject(project.id)
            navController.popBackStack()
        },
        onRemoveClick = { project ->
            onRemoveProject(project.id)
        },
        contentPadding =
            PaddingValues(
                bottom = scaffoldPadding.calculateBottomPadding(),
            ),
        modifier = modifier.fillMaxSize(),
        emptyTitle = "Немає стратегічних посилань",
        emptyBody = "Додайте контексти, які хочете бачити у стратегічному блоці.",
    )
}

@Composable
private fun AiAnalysisPane(viewModel: LifeStateViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier =
            Modifier
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
        modifier =
            Modifier
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
private fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
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
private fun ProjectsLazyColumn(
    projects: List<Context>,
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
                },
            )
        }
    }
}
