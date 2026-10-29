package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestStatus
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.planning.PlanningReferencePresentation
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow

@Composable
internal fun ArcQuestCard(
    quest: ArcQuestEntity,
    contextName: String?,
    onQuestClick: () -> Unit,
    onOpenContext: (String) -> Unit,
    onCreateMission: () -> Unit,
    onEditQuest: (String, String?) -> Unit,
    onDeleteQuest: () -> Unit,
    onCarryToCurrentArc: (() -> Unit)?,
    dragHandleModifier: Modifier,
    references: List<PlanningReferencePresentation> = emptyList(),
    onOpenReference: (CanonicalEntityRef) -> Unit = {},
    isDragging: Boolean = false,
    planningContext: @Composable () -> Unit = {},
    onPlanningContext: (() -> Unit)? = null,
    onToggleCompleted: (() -> Unit)? = null,
    completionEnabled: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var editVisible by remember { mutableStateOf(false) }
    var editTitle by remember(quest.id, quest.title) { mutableStateOf(quest.title) }
    var editDescription by remember(quest.id, quest.description) { mutableStateOf(quest.description.orEmpty()) }
    var editTab by remember { mutableStateOf(0) }
    val linkedContextId = quest.linkedContextId
    val completed = quest.status == ArcQuestStatus.DONE.name

    Column {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onQuestClick),
            color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = if (isDragging) 8.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                UnifiedItemCheckbox(
                    checked = completed,
                    onCheckedChange = { onToggleCompleted?.invoke() },
                    style = UnifiedCheckboxStyle.Square,
                    modifier = dragHandleModifier.semantics {
                        contentDescription = if (completed) "Позначити активним" else "Позначити завершеним"
                    },
                    colors = unifiedCheckboxColors(checked = MaterialTheme.colorScheme.primary,
                        uncheckedBorder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    enabled = completionEnabled && onToggleCompleted != null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f).padding(top = 5.dp)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(450)),
                        textDecoration = if (completed) TextDecoration.LineThrough else null,
                        color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!quest.description.isNullOrBlank()) {
                        Text(
                            text = quest.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (references.isNotEmpty() || !contextName.isNullOrBlank()) {
                        UnifiedStatusRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            references.forEach { reference ->
                                UnifiedMetaChip(
                                    text = reference.questBadgeLabel(),
                                    icon = Icons.Outlined.Link,
                                    onClick = { onOpenReference(reference.entity.ref) },
                                )
                            }
                            if (!contextName.isNullOrBlank()) {
                                UnifiedMetaChip(
                                    text = contextName,
                                    icon = Icons.Outlined.Link,
                                    onClick = linkedContextId?.let { id -> { onOpenContext(id) } },
                                )
                            }
                        }
                    }
                    planningContext()
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "ArcQuest actions",
                            modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                    ) {
                        if (onPlanningContext != null) {
                            DropdownMenuItem(text = { Text("Planning context · Add to…") }, onClick = {
                                menuExpanded = false
                                onPlanningContext()
                            })
                        }
                        if (onCarryToCurrentArc != null) {
                            DropdownMenuItem(
                                text = { Text("В поточну арку") },
                                leadingIcon = { Icon(Icons.Outlined.ArrowForward, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCarryToCurrentArc()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Редагувати") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                editVisible = true
                            },
                        )
                        if (linkedContextId != null) {
                            DropdownMenuItem(
                                text = { Text("Відкрити контекст") },
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenContext(linkedContextId)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add to Tactical") },
                            leadingIcon = { Icon(Icons.Outlined.RocketLaunch, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onCreateMission()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Видалити") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDeleteQuest()
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    if (editVisible) {
        AlertDialog(
            onDismissRequest = { editVisible = false },
            title = { Text("Редагувати ArcQuest") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabRow(selectedTabIndex = editTab) {
                        listOf("Overview", "Planning").forEachIndexed { index, label ->
                            Tab(selected = editTab == index, onClick = { editTab = index }, text = { Text(label) })
                        }
                    }
                    if (editTab == 0) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Назва") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Опис") },
                            minLines = 3,
                        )
                        com.romankozak.forwardappmobile.features.planning.PlanningItemReferences(
                            com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                                com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.ARC_QUEST, quest.id),
                            includeComposition = false,
                        )
                    } else com.romankozak.forwardappmobile.features.planning.PlanningCompositionPanel(
                        com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                            com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.ARC_QUEST, quest.id))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditQuest(editTitle, editDescription)
                        editVisible = false
                    },
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { editVisible = false }) {
                    Text("Скасувати")
                }
            },
        )
    }
}
