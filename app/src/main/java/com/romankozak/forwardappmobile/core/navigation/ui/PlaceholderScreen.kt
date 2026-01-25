package com.romankozak.forwardappmobile.core.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderScreen(
    viewId: String?,
    screenId: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🚧 Placeholder 🚧",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "This screen has not been implemented yet.",
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "View ID: ${viewId ?: "Not Provided"}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Attempted Route: ${screenId ?: "Not Provided"}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
