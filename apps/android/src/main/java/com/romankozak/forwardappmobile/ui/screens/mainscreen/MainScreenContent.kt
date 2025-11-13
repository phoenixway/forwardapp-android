package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState

@Composable
fun MainScreenContent(
    state: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
    ) {
        state.errorMessage?.let { message ->
            ErrorBanner(
                text = message,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }

        AnimatedVisibility(
            visible = state.isLoading && !state.hasProjects,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(
            visible = !state.isLoading || state.hasProjects,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (state.projects.isEmpty()) {
                EmptyState(
                    onAddProject = { onEvent(MainScreenEvent.ShowCreateDialog) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                )
            } else {
                ProjectList(
                    projects = state.projects.sortedBy { it.goalOrder },
                    onEdit = { onEvent(MainScreenEvent.ShowEditDialog(it)) },
                    onDelete = { onEvent(MainScreenEvent.RequestDelete(it)) },
                )
            }
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        items(
            items = projects,
            key = { it.id },
        ) { project ->
            ProjectListItem(
                project = project,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ProjectListItem(
    project: Project,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onEdit(project.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                ) {
                    Text(
                        text = project.name.ifBlank { "Без назви" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    project.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = { onEdit(project.id) },
                    modifier = Modifier.semantics { contentDescription = "Редагувати проєкт" },
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = { onDelete(project.id) },
                    modifier = Modifier.semantics { contentDescription = "Видалити проєкт" },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                }
            }

            ProjectMetaSection(project = project)
        }
    }
}

@Composable
private fun ProjectMetaSection(project: Project) {
    val chips = mutableListOf<String>()
    project.parentId?.let { chips.add("Батьківський: $it") }
    if (project.isCompleted) chips.add("Завершено")
    project.tags?.firstOrNull()?.let { chips.add("#$it") }
    project.projectStatusText?.let { chips.add(it) }
    if (chips.isEmpty()) {
        AssistChip(
            onClick = {},
            label = { Text("ID: ${project.id.take(6)}") },
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            chips.take(3).forEach { label ->
                AssistChip(
                    onClick = {},
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Ще немає жодного проєкту",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Створіть перший проєкт, щоб перевірити роботу нової бази даних SQLDelight.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onAddProject) {
                Text("Створити проєкт")
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}
