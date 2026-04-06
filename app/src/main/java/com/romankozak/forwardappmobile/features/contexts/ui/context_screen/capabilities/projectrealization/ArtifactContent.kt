package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization

import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.viewinterop.AndroidView
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.ui.components.notesEditors.WebViewMarkdownViewer

@Composable
fun ArtifactContent(
    modifier: Modifier = Modifier,
    artifact: ContextArtifact?,
    isManagementEnabled: Boolean,
    onEditArtifact: (ContextArtifact) -> Unit,
) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити артефакти.")
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        if (artifact == null || artifact.content.isBlank()) {
            Text(
                text = "Артефакт проекту порожній. Натисніть 'Редагувати', щоб додати вміст.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val isDark = isSystemInDarkTheme()
            AndroidView(
                factory = { ctx ->
                    WebViewMarkdownViewer(ctx).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }
                },
                update = { viewer ->
                    viewer.renderMarkdown(artifact.content, isDark)
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = {
                    artifact?.let(onEditArtifact)
                },
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = isManagementEnabled && artifact != null,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редагувати Артефакт",
                )
            }
        }
    }
}
