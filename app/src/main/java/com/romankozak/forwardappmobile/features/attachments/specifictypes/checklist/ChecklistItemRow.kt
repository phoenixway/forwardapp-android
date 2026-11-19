package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChecklistItemRow(
    item: ChecklistItemUiModel,
    reorderableScope: ReorderableCollectionItemScope,
    showCheckbox: Boolean,
    isDragging: Boolean,
    shouldRequestFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onContentChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onRequestFocus: () -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onShowItemActions: (ChecklistItemUiModel) -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onLongPress: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var hasInputFocus by remember(item.id) { mutableStateOf(false) }
    var wasFocusedAtLeastOnce by remember(item.id) { mutableStateOf(false) }
    var editorValue by remember(item.id) {
        mutableStateOf(TextFieldValue(item.content, TextRange(item.content.length)))
    }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            hasInputFocus = true
            wasFocusedAtLeastOnce = false
            onFocusConsumed()
        }
    }

    LaunchedEffect(hasInputFocus) {
        if (hasInputFocus) {
            delay(1)
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(item.content, hasInputFocus) {
        if (!hasInputFocus && editorValue.text != item.content) {
            editorValue = TextFieldValue(item.content, TextRange(item.content.length))
        }
    }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelected()
                    },
                    onLongClick = onLongPress,
                ),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (isDragging) 4.dp else 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isSelectionMode) {
                IconToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.padding(top = 16.dp).size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = "Select item",
                    )
                }
            }

            if (showCheckbox) {
                IconToggleButton(
                    checked = item.isChecked,
                    onCheckedChange = { if (!isSelectionMode) onCheckedChange(it) },
                    modifier = Modifier.padding(top = 16.dp).size(32.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            if (item.isChecked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                        border =
                            if (!item.isChecked) {
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            } else {
                                null
                            },
                        modifier = Modifier.size(18.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (item.isChecked) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Checkbox",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(1.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (isSelectionMode) {
                    ChecklistReadOnlyText(
                        text = item.content.ifBlank { stringResource(R.string.checklist_item_placeholder) },
                        onWikiLinkClick = onWikiLinkClick,
                        onPlainTextClick = { onToggleSelected() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                } else if (hasInputFocus) {
                    OutlinedTextField(
                        value = editorValue,
                        onValueChange = { value ->
                            editorValue = value
                            onContentChange(value.text)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        wasFocusedAtLeastOnce = true
                                        hasInputFocus = true
                                    } else if (wasFocusedAtLeastOnce) {
                                        hasInputFocus = false
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.key == Key.Enter) {
                                        if (event.type == KeyEventType.KeyDown) focusManager.clearFocus()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp && event.key == Key.Tab) {
                                        focusManager.clearFocus()
                                    }
                                    false
                                },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.checklist_item_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        minLines = 1,
                        maxLines = 10,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                } else if (item.content.isBlank()) {
                    Text(
                        text = stringResource(R.string.checklist_item_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRequestFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                } else {
                    ChecklistReadOnlyText(
                        text = item.content,
                        onWikiLinkClick = onWikiLinkClick,
                        onPlainTextClick = { offset ->
                            editorValue = TextFieldValue(item.content, TextRange(offset))
                            onRequestFocus()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }

            IconButton(
                onClick = { onShowItemActions(item) },
                modifier =
                    with(reorderableScope) {
                        Modifier
                            .draggableHandle()
                            .padding(top = 16.dp)
                            .size(40.dp)
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

