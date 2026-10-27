package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.day.liveTaskPriorities
import com.romankozak.forwardappmobile.features.planning.PlanningItemReferences
import com.romankozak.forwardappmobile.features.planning.PlanningRelationSectionHeading

@Composable
internal fun TaskRealizesTab(task: DayTask?, legacyLinks: List<Pair<String, String>>, onRemoveLegacy: (String) -> Unit,
    onOpenLegacy: (String) -> Unit, viewModel: TaskRealizesViewModel = hiltViewModel()) {
    LaunchedEffect(task?.id) { task?.let { viewModel.select(it.id) } }
    val observed by viewModel.state.collectAsStateWithLifecycle()
    val state = observed.takeIf { it.task?.id == task?.id } ?: TaskRealizesState()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var picker by remember(task?.id) { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    val current = state.task?.takeIf { it.id == task?.id }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        RealizationHeading("Теми", enabled = current != null && !busy, onAdd = { picker = "themes" })
        state.themes.filter { it.id in state.selectedThemeIds }.forEach { theme ->
            RealizationRow(theme.title, busy) { viewModel.toggleTheme(theme.id) }
        }
        Spacer(Modifier.height(8.dp))
        RealizationHeading("Пріоритети", enabled = current != null && !busy, onAdd = { picker = "priorities" })
        current?.let { liveTaskPriorities(it, state.priorities) }.orEmpty().forEach { priority ->
            RealizationRow(priority.title, busy, subtitle = priority.type.priorityLabel()) { viewModel.togglePriority(priority.id) }
        }
        if (state.suggestedThemeIds.isNotEmpty()) TextButton(enabled = !busy, onClick = viewModel::acceptSuggestedThemes) {
            Text("Додати теми вибраних пріоритетів (${state.suggestedThemeIds.size})")
        }
        Spacer(Modifier.height(8.dp))
        task?.let { PlanningItemReferences(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, it.id),
            legacyLinks, onRemoveLegacy, onOpenLegacy, compact = true) }
        TextButton(onClick = { showHelp = true }, modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
            Text("Про зв’язки", style = MaterialTheme.typography.labelSmall)
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Зв’язки завдання") },
        text = { Text("Теми, пріоритети та канонічні зв’язки зберігаються одразу. Теми та пріоритети беруться з дня цього завдання, не обов’язково сьогодні.\n\nПрибирання зв’язку не видаляє пов’язану сутність. Теми вибраних пріоритетів додаються лише за окремою дією." +
            if (legacyLinks.isNotEmpty()) "\n\nЗміни попередніх робочих зв’язків потребують кнопки збереження вгорі." else "") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Зрозуміло") } })
    if (picker != null) AlertDialog(onDismissRequest = { picker = null },
        title = { Text(if (picker == "themes") "Теми дня завдання" else "Фокуси та відповідальності дня") },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            if (picker == "themes") {
                if (state.themes.isEmpty()) Text("На цей день тем немає")
                state.themes.forEach { theme ->
                    FilterChip(selected = theme.id in state.selectedThemeIds, enabled = !busy,
                        onClick = { viewModel.toggleTheme(theme.id) }, label = { Text(theme.title) })
                }
            } else {
                if (state.priorities.isEmpty()) Text("На цей день пріоритетів немає")
                state.priorities.forEach { priority ->
                    FilterChip(selected = priority.id in current?.realizedDayFocusItemIds.orEmpty(), enabled = !busy,
                        onClick = { viewModel.togglePriority(priority.id) }, label = { Text("${priority.type.priorityLabel()}: ${priority.title}") })
                }
            }
        } }, confirmButton = { TextButton(onClick = { picker = null }) { Text("Готово") } })
}

@Composable
private fun RealizationHeading(title: String, enabled: Boolean, onAdd: () -> Unit) {
    PlanningRelationSectionHeading(title, enabled, onAdd)
}
@Composable private fun RealizationRow(title: String, busy: Boolean, subtitle: String? = null, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(enabled = !busy, onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Прибрати зв’язок: $title", modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
internal fun DayFocusType.priorityLabel() = if (this == DayFocusType.FOCUS) "Фокус" else "Відповідальність"

/** All cards in this navigation entry share one bulk priority catalog; no per-card DB lookup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTaskRealizedPriorityChips(task: DayTask, onOpenTask: () -> Unit, viewModel: DayTaskPriorityCatalogViewModel = hiltViewModel()) {
    val catalog by viewModel.priorities.collectAsStateWithLifecycle()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        liveTaskPriorities(task, catalog[task.dayPlanId].orEmpty()).forEach { priority ->
            SuggestionChip(onClick = onOpenTask, label = { Text("${priority.type.priorityLabel()}: ${priority.title}") })
        }
    }
}
