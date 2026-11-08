package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualActionItems
import com.romankozak.forwardappmobile.features.mainscreen.formatStrategicArcMonth

internal enum class ArcToolGroup { VIEW, CONTENT, PERIOD, CREATE, TOOLS, EXTRA }

/** Disposition of existing actions only. Unknown future actions remain available. */
internal fun groupArcTools(actions: List<ContextualAction>): Map<ArcToolGroup, List<ContextualAction>> {
    val ids = linkedMapOf(
        ArcToolGroup.VIEW to listOf("quests", "artifact"),
        ArcToolGroup.CONTENT to listOf("arc-overview", "arc-all-participants"),
        ArcToolGroup.PERIOD to listOf("arc-previous", "arc-next"),
        ArcToolGroup.CREATE to listOf("quest-source", "arc-beacon", "arc-group", "quest-clear"),
        ArcToolGroup.TOOLS to listOf("arc-open-artifact", "arc-links"),
    )
    val known = ids.values.flatten().toSet()
    return ids.mapValues { (_, keys) -> keys.flatMap { id -> actions.filter { it.id == id } } } +
        (ArcToolGroup.EXTRA to actions.filterNot { it.id in known })
}

@Composable
internal fun StrategicArcToolsSheet(actions: List<ContextualAction>, arcKey: String?, onSelected: (ContextualAction) -> Unit) {
    val groups = groupArcTools(actions)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
        .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Стратегічна арка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        groups.getValue(ArcToolGroup.VIEW).takeIf { it.isNotEmpty() }?.let { modes ->
            ToolSection("Перегляд") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { action ->
                        FilterChip(selected = action.selected == true, enabled = action.enabled,
                            onClick = { onSelected(action) }, label = { Text(action.label) },
                            leadingIcon = { Icon(action.icon, null, Modifier.size(18.dp)) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        groups.getValue(ArcToolGroup.CONTENT).takeIf { it.isNotEmpty() }?.let { options ->
            ToolSection("Склад списку") {
                Column(Modifier.selectableGroup()) {
                    options.forEach { action ->
                        Row(Modifier.fillMaxWidth().selectable(selected = action.selected == true, enabled = action.enabled,
                            role = Role.RadioButton, onClick = { onSelected(action) }).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RadioButton(selected = action.selected == true, onClick = null, enabled = action.enabled)
                            Column(Modifier.weight(1f)) {
                                Text(if (action.id == "arc-overview") "Квести / фокуси" else "Усі учасники",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(if (action.id == "arc-overview") "Лише самостійні, без компонентів" else "З компонентами цієї арки",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        groups.getValue(ArcToolGroup.PERIOD).takeIf { it.isNotEmpty() }?.let { navigation ->
            ToolSection("Період") {
                arcKey?.takeIf { it.isNotBlank() }?.let {
                    Text(formatStrategicArcMonth(it), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    navigation.forEach { action ->
                        OutlinedButton(onClick = { onSelected(action) }, enabled = action.enabled, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                            Icon(action.icon, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(action.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        groups.getValue(ArcToolGroup.CREATE).takeIf { it.isNotEmpty() }?.let { creation ->
            ToolSection("Додати до арки") {
                creation.forEach { action ->
                    when (action.id) {
                        "quest-source" -> FilledTonalButton(onClick = { onSelected(action) }, enabled = action.enabled,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Icon(action.icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(action.label, style = MaterialTheme.typography.labelLarge)
                        }
                        "quest-clear" -> TextButton(onClick = { onSelected(action) }, enabled = action.enabled) {
                            Icon(action.icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("Очистити чернетку", style = MaterialTheme.typography.labelMedium)
                        }
                        else -> ContextualActionItems(listOf(action), onSelected)
                    }
                }
                Text("Очищення прибирає лише текст із поля введення, не збережені квести.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        groups.getValue(ArcToolGroup.TOOLS).takeIf { it.isNotEmpty() }?.let { tools ->
            ToolSection("Інструменти арки") {
                ContextualActionItems(tools, onSelected)
                Text("Артефакт — нотатка арки; зв’язки — прикріплені матеріали.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        groups.getValue(ArcToolGroup.EXTRA).takeIf { it.isNotEmpty() }?.let { extra ->
            ToolSection("Інші дії") { ContextualActionItems(extra, onSelected) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ToolSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), content = content)
        }
    }
}
