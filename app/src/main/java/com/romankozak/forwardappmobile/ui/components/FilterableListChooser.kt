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
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import kotlinx.coroutines.delay
import java.util.UUID

private data class CreateListDraft(
    val name: String,
    val parent: Context?,
)

@Composable
@Suppress("LongParameterList")
fun FilterableListChooser(
    title: String,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    topLevelLists: List<Context>,
    childMap: Map<String, List<Context>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String> = emptySet(),
    onAddNewList: (id: String, parentId: String?, name: String) -> Unit,
) {
    var isCreatingMode by remember { mutableStateOf(false) }
    var createListDraft by remember { mutableStateOf(CreateListDraft(name = "", parent = null)) }
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
        if (!isCreatingMode) {
            searchFocusRequester.requestFocus()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp),
            ) {
                val currentTitle =
                    if (isCreatingMode) {
                        createListDraft.parent?.let { "Новий підсписок для '${it.name}'" } ?: "Новий список верхнього рівня"
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
                    CreateListSection(
                        draft = createListDraft,
                        focusRequester = searchFocusRequester,
                        onDraftChange = { createListDraft = it },
                        onCancel = {
                            isCreatingMode = false
                            keyboardController?.hide()
                        },
                        onCreate = { name, parentId ->
                            val newId = UUID.randomUUID().toString()
                            onAddNewList(newId, parentId, name)
                            highlightedListId = newId
                            isCreatingMode = false
                            keyboardController?.hide()
                        },
                    )
                } else {
                    BrowseListsSection(
                        filterText = filterText,
                        onFilterTextChanged = onFilterTextChanged,
                        topLevelLists = topLevelLists,
                        childMap = childMap,
                        expandedIds = expandedIds,
                        onToggleExpanded = onToggleExpanded,
                        currentParentId = currentParentId,
                        disabledIds = disabledIds,
                        highlightedListId = highlightedListId,
                        focusRequester = searchFocusRequester,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm,
                        onStartCreateRoot = {
                            createListDraft = CreateListDraft(name = "", parent = null)
                            isCreatingMode = true
                        },
                        onStartCreateChild = { parent ->
                            createListDraft = CreateListDraft(name = "", parent = parent)
                            isCreatingMode = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateListSection(
    draft: CreateListDraft,
    focusRequester: FocusRequester,
    onDraftChange: (CreateListDraft) -> Unit,
    onCancel: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    val isError = draft.name.isNotBlank() && draft.name.length < 3
    val canCreate = draft.name.isNotBlank() && draft.name.length >= 3

    OutlinedTextField(
        value = draft.name,
        onValueChange = { onDraftChange(draft.copy(name = it)) },
        label = { Text("Назва нового списку") },
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text("Мінімум 3 символи")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (canCreate) {
                        onCreate(draft.name, draft.parent?.id)
                    }
                },
            ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.align(Alignment.End),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(
            onClick = onCancel,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Text("Скасувати")
        }

        Button(
            onClick = { onCreate(draft.name, draft.parent?.id) },
            enabled = canCreate,
        ) {
            Text("Створити")
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun BrowseListsSection(
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    topLevelLists: List<Context>,
    childMap: Map<String, List<Context>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String>,
    highlightedListId: String?,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onStartCreateRoot: () -> Unit,
    onStartCreateChild: (Context) -> Unit,
) {
    SearchListsField(
        filterText = filterText,
        onFilterTextChanged = onFilterTextChanged,
        focusRequester = focusRequester,
    )

    Spacer(modifier = Modifier.height(16.dp))

    ListsChooserContent(
        filterText = filterText,
        topLevelLists = topLevelLists,
        childMap = childMap,
        expandedIds = expandedIds,
        onToggleExpanded = onToggleExpanded,
        currentParentId = currentParentId,
        disabledIds = disabledIds,
        highlightedListId = highlightedListId,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onStartCreateChild = onStartCreateChild,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onStartCreateRoot,
            colors =
                ButtonDefaults.textButtonColors(
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

@Composable
private fun SearchListsField(
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = filterText,
        onValueChange = onFilterTextChanged,
        label = { Text("Пошук списків") },
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
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
}

@Composable
@Suppress("LongParameterList")
private fun ListsChooserContent(
    filterText: String,
    topLevelLists: List<Context>,
    childMap: Map<String, List<Context>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String>,
    highlightedListId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onStartCreateChild: (Context) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
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
                    HorizontalDivider(
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
                    modifier =
                        Modifier
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
                onAddSubProjectRequest = onStartCreateChild,
            )
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    enabled = isEnabled,
                    onClick = onSelect,
                    interactionSource = interactionSource,
                    indication = null,
                ).padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .border(
                        1.5.dp,
                        if (isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        RoundedCornerShape(4.dp),
                    ),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Корінь (верхній рівень)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color =
                if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecursiveSelectableListItem(
    list: Context,
    childMap: Map<String, List<Context>>,
    level: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    disabledIds: Set<String>,
    highlightedListId: String?,
    onAddSubProjectRequest: (parentList: Context) -> Unit,
) {
    val isExpanded = list.id in expandedIds
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isEnabled = list.id !in disabledIds
    val isHighlighted = list.id == highlightedListId
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> Color.Transparent
            },
        animationSpec = tween(durationMillis = 500),
        label = "highlight-animation",
    )

    Column {
        SelectableListRow(
            list = list,
            level = level,
            hasChildren = hasChildren,
            isExpanded = isExpanded,
            isEnabled = isEnabled,
            backgroundColor = backgroundColor,
            interactionSource = interactionSource,
            onToggleExpanded = onToggleExpanded,
            onSelect = onSelect,
            onAddSubProjectRequest = onAddSubProjectRequest,
        )
        ChildDivider(level = level)
        ChildListItems(
            children = children,
            isExpanded = isExpanded,
            childMap = childMap,
            level = level,
            expandedIds = expandedIds,
            onToggleExpanded = onToggleExpanded,
            onSelect = onSelect,
            disabledIds = disabledIds,
            highlightedListId = highlightedListId,
            onAddSubProjectRequest = onAddSubProjectRequest,
        )
    }
}

@Composable
private fun SelectableListRow(
    list: Context,
    level: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isEnabled: Boolean,
    backgroundColor: Color,
    interactionSource: MutableInteractionSource,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    onAddSubProjectRequest: (Context) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .clickable(
                    enabled = isEnabled,
                    onClick = { onSelect(list.id) },
                    interactionSource = interactionSource,
                    indication = null,
                ).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width((level * 16).dp))
        ExpandToggle(
            hasChildren = hasChildren,
            isExpanded = isExpanded,
            onToggle = { onToggleExpanded(list.id) },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = list.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (isEnabled) {
            IconButton(
                onClick = { onAddSubProjectRequest(list) },
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
}

@Composable
private fun ExpandToggle(
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    if (hasChildren) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onToggle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Spacer(modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun ChildDivider(level: Int) {
    if (level > 0) {
        Box(
            modifier =
                Modifier
                    .width((level * 16).dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun ChildListItems(
    children: List<Context>,
    isExpanded: Boolean,
    childMap: Map<String, List<Context>>,
    level: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    disabledIds: Set<String>,
    highlightedListId: String?,
    onAddSubProjectRequest: (Context) -> Unit,
) {
    if (!isExpanded || children.isEmpty()) return

    children.forEach { child ->
        RecursiveSelectableListItem(
            list = child,
            childMap = childMap,
            level = level + 1,
            expandedIds = expandedIds,
            onToggleExpanded = onToggleExpanded,
            onSelect = onSelect,
            disabledIds = disabledIds,
            highlightedListId = highlightedListId,
            onAddSubProjectRequest = onAddSubProjectRequest,
        )
    }
}
