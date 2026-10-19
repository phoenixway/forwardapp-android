package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabActions
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

@Composable
internal fun WorkspaceIdentityForm(model: WorkspacePropertiesModel, onName: (String) -> Unit, onDescription: (String) -> Unit,
    tags: List<String>?, onAddTag: (String) -> Unit, onRemoveTag: (String) -> Unit) {
    var name by remember(model.workspaceId) { mutableStateOf(TextFieldValue(model.name)) }
    var description by remember(model.workspaceId) { mutableStateOf(TextFieldValue(model.description)) }
    var expandDescription by remember { mutableStateOf(false) }
    LaunchedEffect(model.name) { if (name.text != model.name) name = TextFieldValue(model.name) }
    LaunchedEffect(model.description) { if (description.text != model.description) description = TextFieldValue(model.description) }
    GeneralTabContent(
        state = GeneralTabState(name, description, tags),
        actions = GeneralTabActions(
            onTitleChange = { if (!model.isSaving) { name = it; onName(it.text) } },
            onDescriptionChange = { if (!model.isSaving) { description = it; onDescription(it.text) } },
            onExpandDescriptionClick = { expandDescription = true }, onAddTag = onAddTag, onRemoveTag = onRemoveTag,
        ), titleLabel = "Назва робочого простору",
        extraContent = { Column {
            Text("Теги зберігаються одразу; Save зберігає назву й опис.", style = MaterialTheme.typography.bodySmall)
            if (model.isSystem) Text("Системний робочий простір")
            model.roleCode?.let { Text("Роль: $it", style = MaterialTheme.typography.bodySmall) }
        } },
    )
    if (expandDescription) FullScreenMarkdownEditor(initialValue = description, onDismiss = { expandDescription = false },
        onSave = { description = TextFieldValue(it); onDescription(it); expandDescription = false })
}

@Composable
internal fun WorkspaceSemanticLinks(model: WorkspacePropertiesModel, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Семантичні зв’язки", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onAdd, enabled = !model.isRelationMutationInFlight) {
                Icon(Icons.Default.Add, null); Text("Додати")
            }
        }
        if (model.bindings.isEmpty()) Text("Немає зв’язків")
        model.bindings.forEach { binding -> ListItem(
            headlineContent = { Text(binding.title) },
            supportingContent = { Text("${binding.entityClass.displayLabel()} · ${binding.bindingType.displayLabel()}" +
                if (binding.isPrimary) " · основний" else "") },
            trailingContent = { IconButton(onClick = { onRemove(binding.bindingId) }, enabled = !model.isRelationMutationInFlight) {
                Icon(Icons.Default.DeleteOutline, "Видалити зв’язок")
            } },
        ) }
    }
}

@Composable
internal fun WorkspaceFeaturesContent(state: WorkspaceOperationalSettingsState, onToggle: (String, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Зміни можливостей зберігаються одразу", style = MaterialTheme.typography.bodySmall)
        workspaceFeatures.forEach { feature ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(feature.title)
                Switch(CapabilityId(feature.key) in state.enabled, onCheckedChange = { onToggle(feature.key, it) }, enabled = !state.busy)
            }
        }
    }
}

@Composable
internal fun WorkspaceBindingTypeDialog(subjectKey: String?, inFlight: Boolean, onDismiss: () -> Unit, onConfirm: (WorkspaceBindingType) -> Unit) {
    if (subjectKey == null) return
    var selected by remember(subjectKey) { mutableStateOf<WorkspaceBindingType?>(null) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() }, title = { Text("Тип зв’язку") },
        text = { Column {
            WorkspaceBindingType.entries.forEach { type ->
                Row(Modifier.fillMaxWidth().clickable(enabled = !inFlight) { selected = type }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == type, onClick = null); Text(type.displayLabel())
                }
            }
            if (selected == WorkspaceBindingType.EMBODIES) Text("EMBODIES є основним і унікальним для сутності та Workspace")
        } },
        confirmButton = { TextButton(onClick = { selected?.let(onConfirm) }, enabled = selected != null && !inFlight) { Text("Додати") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inFlight) { Text("Скасувати") } },
    )
}

private fun WorkspaceBindingType.displayLabel() = when (this) {
    WorkspaceBindingType.EMBODIES -> "Втілює"
    WorkspaceBindingType.REALIZES -> "Реалізує"
    WorkspaceBindingType.SUPPORTS -> "Підтримує"
    WorkspaceBindingType.MONITORS -> "Моніторить"
}
