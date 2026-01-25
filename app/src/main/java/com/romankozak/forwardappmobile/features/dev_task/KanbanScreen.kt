package com.romankozak.forwardappmobile.features.dev_task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kanban Screen") })
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Kanban Screen", style = MaterialTheme.typography.headlineMedium)
            Text(text = "This is a placeholder for the Kanban board.", style = MaterialTheme.typography.bodyMedium)
            Text(text = "(Capability: code_index)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
