package com.romankozak.forwardappmobile.features.projectchooser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectChooserScreen(
    navController: NavController,
    onProjectSelected: (Project) -> Unit
) {
    val appComponent = LocalAppComponent.current
    val viewModel: ProjectChooserViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Project") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedProject != null) {
                        IconButton(onClick = { onProjectSelected(uiState.selectedProject!!) }) {
                            Icon(Icons.Default.Check, contentDescription = "Select")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Text("Loading projects...", modifier = Modifier.padding(16.dp))
            } else if (uiState.error != null) {
                Text("Error: ${uiState.error}", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn {
                    items(uiState.projects) { project ->
                        ListItem(
                            headlineContent = { Text(project.name) },
                            supportingContent = { project.description?.let { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onProjectSelected(project) },
                            trailingContent = {
                                if (uiState.selectedProject?.id == project.id) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}