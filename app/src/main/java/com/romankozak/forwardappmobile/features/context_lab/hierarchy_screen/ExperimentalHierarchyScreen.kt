package com.romankozak.forwardappmobile.features.context_lab.hierarchy_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.context.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalHierarchyScreen(viewModel: ExperimentalHierarchyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onResume()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Experimental Hierarchy") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.contexts) { context ->
                ContextHierarchyItem(
                    context = context,
                    isActive = context.id == uiState.activeContextId,
                    onClick = {
                        viewModel.onEvent(ExperimentalHierarchyEvent.ActivateContext(context.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun ContextHierarchyItem(
    context: Context,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = context.id.raw, style = MaterialTheme.typography.titleMedium)
                Text(text = "Role: ${context.role.label}", style = MaterialTheme.typography.bodySmall)
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active Context",
                    tint = Color.Green,
                )
            }
        }
    }
}
