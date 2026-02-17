package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.keyproblems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
fun KeyProblemsView(
    modifier: Modifier = Modifier,
    description: String,
    focusContexts: List<Context>,
    pickerContextOptions: List<ProjectOption>,
    onDescriptionChange: (String) -> Unit,
    onAddFocusContext: (String) -> Unit,
    onRemoveFocusContext: (String) -> Unit,
) {
    var showContextPicker by remember { mutableStateOf(false) }
    var descriptionValue by remember { mutableStateOf(TextFieldValue(description)) }

    LaunchedEffect(description) {
        if (description != descriptionValue.text) {
            descriptionValue = TextFieldValue(text = description, selection = TextRange(description.length))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Опис ключових проблем",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = descriptionValue,
                    onValueChange = {
                        descriptionValue = it
                        onDescriptionChange(it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    placeholder = { Text("Що зараз є головною проблемою?") },
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Контексти для фокусу",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { showContextPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати контекст",
                        )
                    }
                }

                if (focusContexts.isEmpty()) {
                    Text(
                        text = "Контексти ще не вибрано",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        focusContexts.forEach { context ->
                            AssistChip(
                                onClick = {},
                                label = { Text(context.name) },
                                trailingIcon = {
                                    IconButton(onClick = { onRemoveFocusContext(context.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Прибрати",
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions = pickerContextOptions,
            attachmentOptions = emptyList<AttachmentOption>(),
            preselectedContextIds = focusContexts.map { it.id }.toSet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showContextPicker = false },
            onContextSelected = { id ->
                onAddFocusContext(id)
                showContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = null,
            onCreateDocument = null,
        )
    }
}
