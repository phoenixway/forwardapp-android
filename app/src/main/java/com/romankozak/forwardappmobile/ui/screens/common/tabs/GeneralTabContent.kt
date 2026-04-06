package com.romankozak.forwardappmobile.ui.screens.common.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditorActions
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditorState

data class GeneralTabState(
    val title: TextFieldValue,
    val description: TextFieldValue,
    val tags: List<String>? = null,
)

data class GeneralTabActions(
    val onTitleChange: (TextFieldValue) -> Unit,
    val onDescriptionChange: (TextFieldValue) -> Unit,
    val onExpandDescriptionClick: () -> Unit,
    val onAddTag: ((String) -> Unit)? = null,
    val onRemoveTag: ((String) -> Unit)? = null,
    val onCopyDescription: (() -> Unit)? = null,
)

@Composable
fun GeneralTabContent(
    state: GeneralTabState,
    actions: GeneralTabActions,
    titleLabel: String,
    extraContent: @Composable (() -> Unit)? = null,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = actions.onTitleChange,
                label = { Text(titleLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.titleMedium,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )
        }

        item {
            LimitedMarkdownEditor(
                state =
                    LimitedMarkdownEditorState(
                        value = state.description,
                        maxHeight = 200.dp,
                    ),
                actions =
                    LimitedMarkdownEditorActions(
                        onValueChange = actions.onDescriptionChange,
                        onExpandClick = actions.onExpandDescriptionClick,
                        onCopy = actions.onCopyDescription ?: {},
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.tags != null && actions.onAddTag != null && actions.onRemoveTag != null) {
            item {
                TagsSection(
                    tags = state.tags,
                    onAddTag = actions.onAddTag,
                    onRemoveTag = actions.onRemoveTag,
                )
            }
        }

        if (extraContent != null) {
            item {
                extraContent()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TagsSectionHeader()
            TagsSectionContent(tags = tags, onRemoveTag = onRemoveTag)
            AddTagRow(
                newTag = newTag,
                onNewTagChange = { newTag = it },
                onAddTag = {
                    onAddTag(newTag.trim())
                    newTag = ""
                },
            )
        }
    }
}

@Composable
private fun TagsSectionHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalOffer,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Теги",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSectionContent(
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
) {
    if (tags.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                TagItem(tag = tag, onRemove = { onRemoveTag(tag) })
            }
        }
        return
    }

    Text(
        text = "Теги відсутні",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun AddTagRow(
    newTag: String,
    onNewTagChange: (String) -> Unit,
    onAddTag: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = newTag,
            onValueChange = onNewTagChange,
            label = { Text("Новий тег") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        FilledTonalIconButton(
            onClick = onAddTag,
            enabled = newTag.isNotBlank(),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Додати тег",
            )
        }
    }
}

@Composable
fun TagItem(
    tag: String,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Видалити тег",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
