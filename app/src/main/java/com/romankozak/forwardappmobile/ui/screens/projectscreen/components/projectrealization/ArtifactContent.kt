package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact

@Composable
fun ArtifactContent(
    artifact: ProjectArtifact?,
    isManagementEnabled: Boolean,
    onSaveArtifact: (String) -> Unit,
) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити артефакти.")
        return
    }

    var currentContent by remember { mutableStateOf(artifact?.content ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = currentContent,
            onValueChange = { currentContent = it },
            label = { Text("Project Artifact Content") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            singleLine = false,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSaveArtifact(currentContent) },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentContent != (artifact?.content ?: "")
        ) {
            Text("Save Artifact")
        }
    }
}
