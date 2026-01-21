package com.romankozak.forwardappmobile.features.vet_case

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun VetCaseSummaryScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Vet Case Summary Screen", style = MaterialTheme.typography.headlineMedium)
        Text(text = "This is a placeholder for the Vet Case Summary.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun VetCaseHistoryScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Vet Case History Screen", style = MaterialTheme.typography.headlineMedium)
        Text(text = "This is a placeholder for the Vet Case History.", style = MaterialTheme.typography.bodyMedium)
    }
}
