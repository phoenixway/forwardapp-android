package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority

@Composable
fun DesktopBacklogReader(
    title: String,
    items: List<SharedBacklogItem>,
    savingItemId: String?,
    deletingItemId: String?,
    editingItemId: String?,
    isCreatingItem: Boolean,
    draftTitle: String,
    draftDetails: String,
    draftPriority: SharedBacklogPriority,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC143F40)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Backlog Reader",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF6F1E8),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFBDD4D1),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onIntent(WorkspaceExplorerIntent.StartCreatingBacklogItem) },
                    enabled = !isCreatingItem,
                ) {
                    Text(if (isCreatingItem) "Creating..." else "New Item")
                }
            }
            if (isCreatingItem) {
                BacklogComposerCard(
                    draftTitle = draftTitle,
                    draftDetails = draftDetails,
                    draftPriority = draftPriority,
                    isSaving = savingItemId == NEW_ITEM_KEY,
                    onIntent = onIntent,
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = "Для цього контексту поки немає desktop-safe backlog data.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD9E3E2),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        BacklogRow(
                            item = item,
                            isSaving = savingItemId == item.id || deletingItemId == item.id,
                            isDeleting = deletingItemId == item.id,
                            isEditing = editingItemId == item.id,
                            draftTitle = draftTitle,
                            draftDetails = draftDetails,
                            draftPriority = draftPriority,
                            onIntent = onIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BacklogRow(
    item: SharedBacklogItem,
    isSaving: Boolean,
    isDeleting: Boolean,
    isEditing: Boolean,
    draftTitle: String,
    draftDetails: String,
    draftPriority: SharedBacklogPriority,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x1AFFFFFF),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.kind.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF6F1E8),
                    modifier =
                        Modifier
                            .background(Color(0x224FC3A1), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    text = if (item.isDone) "Done" else item.priority.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.isDone) Color(0xFFD9FDD3) else Color(0xFFFDF1CF),
                    modifier =
                        Modifier
                            .background(
                                if (item.isDone) Color(0x334CAF50) else Color(0x33D59B3D),
                                RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Text(
                text = if (isEditing) "Editing: ${item.title}" else item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            if (isEditing) {
                BacklogItemEditor(
                    draftTitle = draftTitle,
                    draftDetails = draftDetails,
                    draftPriority = draftPriority,
                    isSaving = isSaving,
                    onIntent = onIntent,
                )
            } else {
                item.details?.takeIf { it.isNotBlank() }?.let { details ->
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD9E3E2),
                    )
                }
            }
            Text(
                text =
                    when {
                        isDeleting -> "Deleting..."
                        isSaving -> "Saving..."
                        item.isDone -> "Позначено як виконане"
                        else -> "Позначити як виконане"
                    },
                style = MaterialTheme.typography.labelLarge,
                color = if (item.isDone) Color(0xFFD9FDD3) else Color(0xFFBDE7DE),
                modifier =
                    Modifier
                        .background(Color(0x1437F1CC), RoundedCornerShape(999.dp))
                        .clickable(enabled = !isSaving) {
                            onIntent(WorkspaceExplorerIntent.ToggleBacklogItemDone(item.id, !item.isDone))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            if (!isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Редагувати",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFF6F1E8),
                        modifier =
                            Modifier
                                .background(Color(0x1FFFFFFF), RoundedCornerShape(999.dp))
                                .clickable(enabled = !isSaving) {
                                    onIntent(WorkspaceExplorerIntent.StartEditingBacklogItem(item.id))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Text(
                        text = if (isDeleting) "Deleting..." else "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFD9D1),
                        modifier =
                            Modifier
                                .background(Color(0x33A64132), RoundedCornerShape(999.dp))
                                .clickable(enabled = !isSaving) {
                                    onIntent(WorkspaceExplorerIntent.DeleteBacklogItem(item.id))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BacklogComposerCard(
    draftTitle: String,
    draftDetails: String,
    draftPriority: SharedBacklogPriority,
    isSaving: Boolean,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x1457DFC1),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "New Backlog Item",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            BacklogItemEditor(
                draftTitle = draftTitle,
                draftDetails = draftDetails,
                draftPriority = draftPriority,
                isSaving = isSaving,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun BacklogItemEditor(
    draftTitle: String,
    draftDetails: String,
    draftPriority: SharedBacklogPriority,
    isSaving: Boolean,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = draftTitle,
            onValueChange = { onIntent(WorkspaceExplorerIntent.BacklogDraftTitleChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
        )
        OutlinedTextField(
            value = draftDetails,
            onValueChange = { onIntent(WorkspaceExplorerIntent.BacklogDraftDetailsChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Details") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SharedBacklogPriority.entries.forEach { priority ->
                Text(
                    text = priority.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (draftPriority == priority) Color.White else Color(0xFFD9E3E2),
                    modifier =
                        Modifier
                            .background(
                                if (draftPriority == priority) Color(0x3354B36B) else Color(0x1FFFFFFF),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable(enabled = !isSaving) {
                                onIntent(WorkspaceExplorerIntent.BacklogDraftPriorityChanged(priority))
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onIntent(WorkspaceExplorerIntent.SaveBacklogItem) },
                enabled = !isSaving && draftTitle.isNotBlank(),
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
            OutlinedButton(
                onClick = { onIntent(WorkspaceExplorerIntent.CancelBacklogEditing) },
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        }
    }
}

private const val NEW_ITEM_KEY = "__new_backlog_item__"
