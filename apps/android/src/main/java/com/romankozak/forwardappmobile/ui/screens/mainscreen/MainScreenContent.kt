package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val hierarchyItems =
        remember(state.projects) {
            state.projects.toHierarchyItems()
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
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
                    onAddProject = { onEvent(MainScreenEvent.ShowCreateDialog()) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                )
            } else {
                ProjectHierarchyList(
                    items = hierarchyItems,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ProjectHierarchyList(
    items: List<ProjectListItem>,
    onEvent: (MainScreenEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        items(items = items, key = { it.project.id }) { item ->
            ProjectHierarchyCard(
                item = item,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ProjectHierarchyCard(
    item: ProjectListItem,
    onEvent: (MainScreenEvent) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width((item.depth * 16).dp))
        Card(
            modifier = Modifier.weight(1f),
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
                    if (item.hasChildren) {
                        IconButton(onClick = { onEvent(MainScreenEvent.ToggleProjectExpanded(item.project.id)) }) {
                            Icon(
                                imageVector =
                                    if (item.project.isExpanded) Icons.Default.KeyboardArrowDown
                                    else Icons.Default.KeyboardArrowRight,
                                contentDescription = "Перемкнути підпроєкти",
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                    ) {
                        Text(
                            text = item.project.name.ifBlank { "Без назви" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        item.project.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { onEvent(MainScreenEvent.ShowEditDialog(item.project.id)) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Редагувати")
                    }
                    IconButton(onClick = { onEvent(MainScreenEvent.RequestDelete(item.project.id)) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Видалити")
                    }
                }

                ProjectMetaSection(item.project)

                Divider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onEvent(MainScreenEvent.ShowCreateDialog(item.project.id)) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text("Підпроєкт")
                    }
                    if (!item.hasChildren) {
                        AssistChip(onClick = {}, label = { Text("ID: ${item.project.id.take(6)}") })
                    }
                }
            }
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

    if (chips.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        chips.take(3).forEach { label ->
            AssistChip(onClick = {}, label = { Text(label) })
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

private data class ProjectListItem(
    val project: Project,
    val depth: Int,
    val hasChildren: Boolean,
)

private fun List<Project>.toHierarchyItems(): List<ProjectListItem> {
    if (isEmpty()) return emptyList()

    val normalized =
        map { project ->
            project to project.parentId?.takeIf { it.isNotBlank() }
        }
    val childrenMap =
        normalized.groupBy(
            keySelector = { it.second },
            valueTransform = { it.first },
        )

    val result = mutableListOf<ProjectListItem>()

    fun traverse(nodes: List<Project>, depth: Int) {
        nodes
            .sortedBy { it.goalOrder }
            .forEach { project ->
                val children = childrenMap[project.id].orEmpty()
                result += ProjectListItem(project, depth, children.isNotEmpty())
                if (project.isExpanded && children.isNotEmpty()) {
                    traverse(children, depth + 1)
                }
            }
    }

    traverse(childrenMap[null].orEmpty(), depth = 0)
    return result
}
