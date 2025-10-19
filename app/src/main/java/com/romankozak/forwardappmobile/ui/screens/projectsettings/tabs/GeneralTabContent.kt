package com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditor

@Composable
fun GeneralTabContent(
    title: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    titleLabel: String,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit,
    onExpandDescriptionClick: () -> Unit,
    tags: List<String>? = null,
    onAddTag: ((String) -> Unit)? = null,
    onRemoveTag: ((String) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(titleLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        }

        item {
            LimitedMarkdownEditor(
                value = description,
                onValueChange = onDescriptionChange,
                maxHeight = 150.dp,
                onExpandClick = onExpandDescriptionClick,
                modifier = Modifier.fillMaxWidth(),
                onCopy = { /* TODO */ },
            )
        }

        if (tags != null && onAddTag != null && onRemoveTag != null) {
            item {
                TagsSection(
                    tags = tags,
                    onAddTag = onAddTag,
                    onRemoveTag = onRemoveTag
                )
            }
        }
    }
}

@Composable
fun TagsSection(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Теги", style = MaterialTheme.typography.titleMedium)

            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        TagItem(tag = tag, onRemove = { onRemoveTag(tag) })
                    }
                }
            } else {
                Text(
                    "Теги відсутні",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("Новий тег") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(newTag)
                        newTag = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Додати тег")
                }
            }
        }
    }
}

@Composable
fun TagItem(tag: String, onRemove: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = tag, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Видалити тег")
            }
        }
    }
}