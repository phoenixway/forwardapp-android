package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact

@Composable
fun ArtifactContent(
    artifact: ProjectArtifact?,
    isManagementEnabled: Boolean,
    onEditArtifact: (ProjectArtifact) -> Unit,
    onSaveArtifact: () -> Unit,
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
            val isDark = isSystemInDarkTheme()
            AndroidView(
                factory = { ctx ->
                    com.romankozak.forwardappmobile.ui.components.notesEditors.WebViewMarkdownViewer(ctx).apply {
                        layoutParams =
                            android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }
                },
                update = { viewer ->
                    viewer.renderMarkdown(artifact.content, isDark)
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { 
                    if (artifact != null) {
                        onEditArtifact(artifact)
                    } else {
                        onSaveArtifact()
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = isManagementEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редагувати Артефакт"
                )
            }
        }
    }
}
