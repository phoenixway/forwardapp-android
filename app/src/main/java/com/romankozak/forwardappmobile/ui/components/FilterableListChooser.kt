// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/FilterableListChooser.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.GoalList
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun FilterableListChooser(
    title: String,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    topLevelLists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String> = emptySet(),
    onAddNewList: (id: String, parentId: String?, name: String) -> Unit,
) {
    var isCreatingMode by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var parentForNewList by remember { mutableStateOf<GoalList?>(null) }
    var highlightedListId by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(highlightedListId) {
        if (highlightedListId != null) {
            delay(2000L)
            highlightedListId = null
        }
    }

    LaunchedEffect(isCreatingMode) {
        // Auto-focus logic when creating mode changes
    }

    LaunchedEffect(Unit) {
        if (!isCreatingMode) {
            searchFocusRequester.requestFocus()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
            ) {
                val currentTitle = if (isCreatingMode) {
                    parentForNewList?.let { "Новий підсписок для '${it.name}'" } ?: "Новий список верхнього рівня"
                } else {
                    title
                }

                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                if (isCreatingMode) {
                    var isError by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = newListName,
                        onValueChange = {
                            newListName = it
                            isError = it.isNotBlank() && it.length < 3
                        },
                        label = { Text("Назва нового списку") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                        singleLine = true,
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text("Мінімум 3 символи")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newListName.isNotBlank() && newListName.length >= 3) {
                                    val newId = UUID.randomUUID().toString()
                                    onAddNewList(newId, parentForNewList?.id, newListName)
                                    highlightedListId = newId
                                    isCreatingMode = false
                                    keyboardController?.hide()
                                }
                            },
                        ),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = {
                                isCreatingMode = false
                                keyboardController?.hide()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Text("Скасувати")
                        }

                        Button(
                            onClick = {
                                val newId = UUID.randomUUID().toString()
                                onAddNewList(newId, parentForNewList?.id, newListName)
                                highlightedListId = newId
                                isCreatingMode = false
                                keyboardController?.hide()
                            },
                            enabled = newListName.isNotBlank() && newListName.length >= 3,
                        ) {
                            Text("Створити")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = onFilterTextChanged,
                        label = { Text("Пошук списків") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Пошук",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = { onFilterTextChanged("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Очистити пошук",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.5).dp),
                    ) {
                        if (filterText.isBlank()) {
                            item {
                                val isAlreadyAtRoot = currentParentId == null
                                SelectableRootItem(
                                    isEnabled = !isAlreadyAtRoot,
                                    onSelect = {
                                        onConfirm(null)
                                        onDismiss()
                                    },
                                )

                                if (topLevelLists.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }

                        if (topLevelLists.isEmpty() && filterText.isNotBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Списки не знайдено",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(topLevelLists, key = { it.id }) { list ->
                            RecursiveSelectableListItem(
                                list = list,
                                childMap = childMap,
                                level = 0,
                                expandedIds = expandedIds,
                                onToggleExpanded = onToggleExpanded,
                                onSelect = { selectedId ->
                                    onConfirm(selectedId)
                                    onDismiss()
                                },
                                disabledIds = disabledIds,
                                highlightedListId = highlightedListId,
                                onAddSublistRequest = { parent ->
                                    parentForNewList = parent
                                    isCreatingMode = true
                                    newListName = ""
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                parentForNewList = null
                                isCreatingMode = true
                                newListName = ""
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Створити",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Створити новий")
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Скасувати")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRootItem(
    isEnabled: Boolean,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(
                enabled = isEnabled,
                onClick = onSelect,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    1.5.dp,
                    if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    RoundedCornerShape(4.dp),
                ),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Корінь (верхній рівень)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecursiveSelectableListItem(
    list: GoalList,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    disabledIds: Set<String>,
    highlightedListId: String?,
    onAddSublistRequest: (parentList: GoalList) -> Unit,
) {
    val isExpanded = list.id in expandedIds
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isEnabled = list.id !in disabledIds
    val isHighlighted = list.id == highlightedListId
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 500),
        label = "highlight-animation",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .clickable(
                    enabled = isEnabled,
                    onClick = { onSelect(list.id) },
                    interactionSource = interactionSource,
                    indication = null,
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width((level * 16).dp))

            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleExpanded(list.id) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = list.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isEnabled) {
                IconButton(
                    onClick = { onAddSublistRequest(list) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Додати підсписок до ${list.name}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (level > 0) {
            Box(
                modifier = Modifier
                    .width((level * 16).dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }

        if (isExpanded && hasChildren) {
            for (child in children) {
                RecursiveSelectableListItem(
                    list = child,
                    childMap = childMap,
                    level = level + 1,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    onSelect = onSelect,
                    disabledIds = disabledIds,
                    highlightedListId = highlightedListId,
                    onAddSublistRequest = onAddSublistRequest,
                )
            }
        }
    }
}