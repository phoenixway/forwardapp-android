package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DayThemesScreen(
    initialDayPlanId: String,
    viewModel: DayThemesViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editedTheme by remember { mutableStateOf<DayTheme?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<DayTheme?>(null) }

    LaunchedEffect(initialDayPlanId) { viewModel.loadPlan(initialDayPlanId) }

    DayThemesContent(
        state = state,
        onAdd = { editedTheme = null; editorVisible = true },
        onEdit = { editedTheme = it; editorVisible = true },
        onDelete = { deleteCandidate = it },
        onActiveChange = viewModel::setThemeActive,
        onReorder = viewModel::reorderThemes,
    )
    if (editorVisible) {
        DayThemeEditorSheet(
            theme = editedTheme,
            onDismiss = { editorVisible = false },
            onSave = { draft ->
                viewModel.saveTheme(editedTheme?.id, draft)
                editorVisible = false
            },
        )
    }
    deleteCandidate?.let { theme ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Видалити тему?") },
            text = { Text("«${theme.title}» буде прибрано з усіх сутностей цього дня.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTheme(theme.id); deleteCandidate = null }) {
                    Text("Видалити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Скасувати") } },
        )
    }
}
@Composable
private fun DayThemesContent(
    state: DayThemesUiState,
    onAdd: () -> Unit,
    onEdit: (DayTheme) -> Unit,
    onDelete: (DayTheme) -> Unit,
    onActiveChange: (String, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var orderedThemes by remember(state.themes) { mutableStateOf(state.themes) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedThemes = orderedThemes.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onReorder(orderedThemes.map(DayTheme::id))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Теми дня", style = MaterialTheme.typography.titleMedium)
                val total = state.totalBudgetPercent
                Text(
                    text = "Розподілено $total зі 100%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (total > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Text("Додати")
            }
        }
        if (state.themes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(14.dp))
                Text("Тем ще немає", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Створіть кілька напрямків і розподіліть між ними бюджет дня.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(orderedThemes, key = DayTheme::id) { theme ->
                    ReorderableItem(reorderableState, key = theme.id) {
                        DayThemeCard(
                            theme = theme,
                            onEdit = { onEdit(theme) },
                            onDelete = { onDelete(theme) },
                            onActiveChange = { onActiveChange(theme.id, it) },
                            dragModifier = with(this) { Modifier.longPressDraggableHandle() },
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun DayThemeCard(
    theme: DayTheme,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    dragModifier: Modifier,
) {
    val color = themeColor(theme)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = if (theme.isActive) 0.08f else 0.025f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.DragIndicator,
                contentDescription = "Перетягнути тему",
                modifier = dragModifier.size(32.dp).padding(6.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(dayThemeIcon(theme.iconKey), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(theme.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                theme.comment.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("${theme.budgetPercent}%", style = MaterialTheme.typography.labelLarge, color = color)
            Switch(checked = theme.isActive, onCheckedChange = onActiveChange, modifier = Modifier.size(42.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) { Icon(Icons.Outlined.Edit, "Редагувати") }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Outlined.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DayThemeEditorSheet(
    theme: DayTheme?,
    onDismiss: () -> Unit,
    onSave: (DayThemeDraft) -> Unit,
) {
    var title by remember(theme?.id) { mutableStateOf(theme?.title.orEmpty()) }
    var comment by remember(theme?.id) { mutableStateOf(theme?.comment.orEmpty()) }
    var budget by remember(theme?.id) { mutableStateOf(theme?.budgetPercent?.toString() ?: "0") }
    var color by remember(theme?.id) { mutableStateOf(theme?.colorArgb ?: dayThemeColors[4]) }
    var icon by remember(theme?.id) { mutableStateOf(theme?.iconKey ?: "target") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
        ) {
            Text(if (theme == null) "Нова тема" else "Властивості теми", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Назва") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it.filter(Char::isDigit).take(3) },
                label = { Text("Бюджет дня, %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Коментар") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("Колір", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayThemeColors.forEach { value ->
                    Surface(
                        shape = CircleShape,
                        color = Color(value.toULong()),
                        border = if (color == value) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier.size(34.dp).clickable { color = value },
                    ) {}
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Іконка", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dayThemeIconKeys.forEach { key ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (icon == key) Color(color.toULong()).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (icon == key) BorderStroke(1.dp, Color(color.toULong())) else null,
                        modifier = Modifier.size(42.dp).clickable { icon = key },
                    ) { Box(contentAlignment = Alignment.Center) { Icon(dayThemeIcon(key), null) } }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onSave(DayThemeDraft(title, color, icon, comment, budget.toIntOrNull() ?: 0)) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Зберегти") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
