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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact

@Composable
fun ArtifactContent(
    artifact: ProjectArtifact?,
    isManagementEnabled: Boolean,
    onEditArtifact: (ProjectArtifact) -> Unit,
) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити артефакти.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (artifact == null || artifact.content.isBlank()) {
            Text(
                text = "Артефакт проекту порожній. Натисніть 'Редагувати', щоб додати вміст.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = artifact.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { artifact?.let { onEditArtifact(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = isManagementEnabled
        ) {
            Text("Редагувати Артефакт")
        }
    }
}
