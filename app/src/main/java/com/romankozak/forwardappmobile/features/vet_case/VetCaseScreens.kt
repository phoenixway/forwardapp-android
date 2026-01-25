package com.romankozak.forwardappmobile.features.vet_case

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
fun VetCaseSummaryScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Summary Screen") })
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
            Text(text = "Vet Case Summary Screen", style = MaterialTheme.typography.headlineMedium)
            Text(text = "This is a placeholder for the Vet Case Summary.", style = MaterialTheme.typography.bodyMedium)
            Text(text = "(Capability: treatment_plan)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetCaseHistoryScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("History Screen") })
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
            Text(text = "Vet Case History Screen", style = MaterialTheme.typography.headlineMedium)
            Text(text = "This is a placeholder for the Vet Case History.", style = MaterialTheme.typography.bodyMedium)
            Text(text = "(Capability: treatment_plan)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
