package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.features.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.features.mainscreen.models.MainScreenUiState
import androidx.compose.foundation.clickable

@Composable
fun MainScreenContent(
    state: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    onProjectClick: (String) -> Unit,
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
                .padding(start = 16.dp, end = 16.dp),
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
                    onProjectClick = onProjectClick,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun ProjectHierarchyList(
    items: List<ProjectListItem>,
    onEvent: (MainScreenEvent) -> Unit,
    onProjectClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = items, key = { it.project.id }) { item ->
            ProjectHierarchyCard(
                item = item,
                onEvent = onEvent,
                onProjectClick = onProjectClick,
            )
        }
    }
}

@Composable
private fun ProjectHierarchyCard(
    item: ProjectListItem,
    onEvent: (MainScreenEvent) -> Unit,
    onProjectClick: (String) -> Unit,
) {
    val indentation = (item.depth * 24).dp
    val rowBackground = MaterialTheme.colorScheme.surface
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(indentation))
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(rowBackground)
                    .clickable { onProjectClick(item.project.id) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (item.hasChildren) {
                    IconButton(onClick = { onEvent(MainScreenEvent.ToggleProjectExpanded(item.project.id)) }) {
                        Icon(
                            imageVector =
                                if (item.project.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Перемкнути підпроєкти",
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
            ) {
                Text(
                    text = item.project.name.ifBlank { "Без назви" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.project.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.project.projectStatusText?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    item.project.tags?.firstOrNull()?.let { AssistChip(onClick = {}, label = { Text("#" + it) }) }
                    if (item.project.isCompleted) AssistChip(onClick = {}, label = { Text("Завершено") })
                }
            }

            IconButton(onClick = { isMenuOpen = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії")
            }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Редагувати") },
                    onClick = {
                        isMenuOpen = false
                        onEvent(MainScreenEvent.ShowEditDialog(item.project.id))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Створити підпроєкт") },
                    onClick = {
                        isMenuOpen = false
                        onEvent(MainScreenEvent.ShowCreateDialog(item.project.id))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        isMenuOpen = false
                        onEvent(MainScreenEvent.RequestDelete(item.project.id))
                    },
                )
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
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
