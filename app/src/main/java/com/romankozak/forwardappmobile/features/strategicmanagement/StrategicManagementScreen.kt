package com.romankozak.forwardappmobile.features.strategicmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.lifestate.AnalysisContent
import com.romankozak.forwardappmobile.features.lifestate.ChatSection
import com.romankozak.forwardappmobile.features.lifestate.LifeStateChatViewModel
import com.romankozak.forwardappmobile.features.lifestate.LifeStateViewModel
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentChooserScreen
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.ContextLinkList
import com.romankozak.forwardappmobile.ui.components.orderToken
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder
import com.romankozak.forwardappmobile.ui.screens.common.ProjectListItem
import java.net.URLEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategicManagementScreen(
    navController: NavController,
    viewModel: StrategicManagementViewModel = hiltViewModel(),
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val linkedAttachmentIds by viewModel.linkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val scope = rememberCoroutineScope()
    var showAttachmentChooser by remember { mutableStateOf(false) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
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

    if (isScopeLinksSheetVisible) {
        val contexts = uiState.dashboardProjects
        val availableAttachmentById = attachmentOptions.associateBy { it.id }
        val validAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentById.keys }
        val urlIds = validAttachmentIds.filter { id -> availableAttachmentById[id]?.linkType == LinkType.URL }
        val obsidianIds = validAttachmentIds.filter { id -> availableAttachmentById[id]?.linkType == LinkType.OBSIDIAN }
        val generalAttachmentIds =
            validAttachmentIds.filter { id ->
                availableAttachmentById[id]?.linkType !in setOf(LinkType.URL, LinkType.OBSIDIAN)
            }
        val items =
            buildList {
                addAll(contexts.map { ConnectionItemUi(it.id, it.name, ConnectionType.CONTEXT) })
                addAll(
                    generalAttachmentIds.map { id ->
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "Вкладення ${id.take(8)}",
                            type = ConnectionType.ATTACHMENT,
                        )
                    },
                )
                addAll(
                    urlIds.map { id ->
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "URL ${id.take(8)}",
                            type = ConnectionType.URL,
                        )
                    },
                )
                addAll(
                    obsidianIds.map { id ->
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "Obsidian ${id.take(8)}",
                            type = ConnectionType.OBSIDIAN_NOTE,
                        )
                    },
                )
            }
        val sortedItems = sortConnectionsByOrder(items, connectionsOrder)

        ModalBottomSheet(onDismissRequest = viewModel::dismissScopeLinksSheet) {
            ConnectionsPanel(
                items = sortedItems,
                onConnectionClick = { item ->
                    if (item.type == ConnectionType.CONTEXT) {
                        navController.navigate("goal_detail_screen/${item.id}")
                    } else {
                        navController.navigate("attachments_library_screen") {
                            launchSingleTop = true
                            restoreState = true
                        }
                        runCatching {
                            navController.getBackStackEntry("attachments_library_screen")
                                .savedStateHandle["attachment_library_query"] = item.id
                        }
                    }
                },
                onConnectionRemove = { item ->
                    if (item.type == ConnectionType.CONTEXT) {
                        viewModel.removeStrategicLink(item.id)
                    } else {
                        viewModel.removeAttachmentLink(item.id)
                    }
                },
                onAddConnection = { type ->
                    when (type) {
                        AddConnectionType.CONTEXT -> {
                            val disabledIds = contexts.joinToString(",") { it.id }
                            val title = URLEncoder.encode("Додати стратегічний контекст", "UTF-8")
                            val route =
                                if (disabledIds.isBlank()) {
                                    "list_chooser_screen/$title"
                                } else {
                                    "list_chooser_screen/$title?disabledIds=$disabledIds"
                                }
                            navController.navigate(route)
                        }
                        AddConnectionType.ATTACHMENT -> {
                            viewModel.dismissScopeLinksSheet()
                            scope.launch {
                                delay(160)
                                showAttachmentChooser = true
                            }
                        }
                        AddConnectionType.EXTERNAL_LINK -> showAddUrlDialog = true
                        AddConnectionType.OBSIDIAN_NOTE -> showAddObsidianDialog = true
                    }
                },
                onConnectionsReordered = { reordered ->
                    viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAttachmentChooser) {
        StrategicAttachmentChooser(
            options = attachmentOptions,
            preselected = linkedAttachmentIds.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addAttachmentLink)
                showAttachmentChooser = false
            },
        )
    }

    if (showAddUrlDialog) {
        AddWebLinkDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url, name ->
                viewModel.addUrlLink(url, name)
                showAddUrlDialog = false
            },
        )
    }

    if (showAddObsidianDialog) {
        AddObsidianLinkDialog(
            onDismiss = { showAddObsidianDialog = false },
            onConfirm = { noteName, displayName ->
                viewModel.addObsidianLink(noteName, displayName)
                showAddObsidianDialog = false
            },
        )
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
        onAddClick = null,
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

@Composable
private fun StrategicAttachmentChooser(
    options: List<ScopeAttachmentOption>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    AttachmentChooserScreen(
        options = options.map { AttachmentOption(id = it.id, name = it.name, linkType = it.linkType) },
        preselected = preselected,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
