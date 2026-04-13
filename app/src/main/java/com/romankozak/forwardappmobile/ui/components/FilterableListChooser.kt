@file:Suppress("TooManyFunctions")

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private data class BrowseListsState(
    val filterText: String,
    val topLevelLists: List<Context>,
    val childMap: Map<String, List<Context>>,
    val expandedIds: Set<String>,
    val currentParentId: String?,
    val disabledIds: Set<String>,
    val highlightedListId: String?,
    val focusRequester: FocusRequester,
)

private data class BrowseListsActions(
    val onFilterTextChanged: (String) -> Unit,
    val onToggleExpanded: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onConfirm: (String?) -> Unit,
    val onStartCreateRoot: () -> Unit,
    val onStartCreateChild: (Context) -> Unit,
)

private data class ListChooserTreeState(
    val childMap: Map<String, List<Context>>,
    val expandedIds: Set<String>,
    val disabledIds: Set<String>,
    val highlightedListId: String?,
)

private data class ListChooserTreeActions(
    val onToggleExpanded: (String) -> Unit,
    val onSelect: (String?) -> Unit,
    val onAddSubProjectRequest: (Context) -> Unit,
)

private data class SelectableListRowState(
    val list: Context,
    val level: Int,
    val hasChildren: Boolean,
    val isExpanded: Boolean,
    val isEnabled: Boolean,
    val backgroundColor: Color,
    val interactionSource: MutableInteractionSource,
)

private data class FilterableListChooserUiState(
    val isCreatingMode: Boolean,
    val createListDraft: CreateListDraft,
    val highlightedListId: String?,
    val keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    val searchFocusRequester: FocusRequester,
)

private data class FilterableListChooserCallbacks(
    val onCreateModeChange: (Boolean) -> Unit,
    val onDraftChange: (CreateListDraft) -> Unit,
    val onHighlightedListIdChange: (String?) -> Unit,
    val onDismiss: () -> Unit,
    val onAddNewList: (id: String, parentId: String?, name: String) -> Unit,
)

private const val HIGHLIGHT_RESET_DELAY_MILLIS = 2000L
private const val MIN_LIST_NAME_LENGTH = 3
private const val TREE_INDENT_DP = 16
private const val CHOOSER_HEIGHT_FRACTION = 0.5f

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
            delay(HIGHLIGHT_RESET_DELAY_MILLIS)
            highlightedListId = null
        }
    }

    LaunchedEffect(isCreatingMode) {
        if (!isCreatingMode) {
            searchFocusRequester.requestFocus()
        }
    }

    FilterableListChooserDialog(
        title = title,
        filterText = filterText,
        onFilterTextChanged = onFilterTextChanged,
        topLevelLists = topLevelLists,
        childMap = childMap,
        expandedIds = expandedIds,
        onToggleExpanded = onToggleExpanded,
        onConfirm = onConfirm,
        currentParentId = currentParentId,
        disabledIds = disabledIds,
        state =
            FilterableListChooserUiState(
                isCreatingMode = isCreatingMode,
                createListDraft = createListDraft,
                highlightedListId = highlightedListId,
                keyboardController = keyboardController,
                searchFocusRequester = searchFocusRequester,
            ),
        callbacks =
            FilterableListChooserCallbacks(
                onCreateModeChange = { isCreatingMode = it },
                onDraftChange = { createListDraft = it },
                onHighlightedListIdChange = { highlightedListId = it },
                onDismiss = onDismiss,
                onAddNewList = onAddNewList,
            ),
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun FilterableListChooserDialog(
    title: String,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    topLevelLists: List<Context>,
    childMap: Map<String, List<Context>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onConfirm: (String?) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String>,
    state: FilterableListChooserUiState,
    callbacks: FilterableListChooserCallbacks,
) {
    Dialog(onDismissRequest = callbacks.onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                ChooserTitle(
                    title =
                        resolveChooserTitle(
                            title = title,
                            isCreatingMode = state.isCreatingMode,
                            draft = state.createListDraft,
                        ),
                )

                if (state.isCreatingMode) {
                    CreateListSection(
                        draft = state.createListDraft,
                        focusRequester = state.searchFocusRequester,
                        onDraftChange = callbacks.onDraftChange,
                        onCancel = {
                            callbacks.onCreateModeChange(false)
                            state.keyboardController?.hide()
                        },
                        onCreate = { name, parentId ->
                            val newId = UUID.randomUUID().toString()
                            callbacks.onAddNewList(newId, parentId, name)
                            callbacks.onHighlightedListIdChange(newId)
                            callbacks.onCreateModeChange(false)
                            state.keyboardController?.hide()
                        },
                    )
                } else {
                    BrowseListsSection(
                        state =
                            BrowseListsState(
                                filterText = filterText,
                                topLevelLists = topLevelLists,
                                childMap = childMap,
                                expandedIds = expandedIds,
                                currentParentId = currentParentId,
                                disabledIds = disabledIds,
                                highlightedListId = state.highlightedListId,
                                focusRequester = state.searchFocusRequester,
                            ),
                        actions =
                            BrowseListsActions(
                                onFilterTextChanged = onFilterTextChanged,
                                onToggleExpanded = onToggleExpanded,
                                onDismiss = callbacks.onDismiss,
                                onConfirm = onConfirm,
                                onStartCreateRoot = {
                                    callbacks.onDraftChange(CreateListDraft(name = "", parent = null))
                                    callbacks.onCreateModeChange(true)
                                },
                                onStartCreateChild = { parent ->
                                    callbacks.onDraftChange(CreateListDraft(name = "", parent = parent))
                                    callbacks.onCreateModeChange(true)
                                },
                            ),
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
    val isError = draft.name.isNotBlank() && draft.name.length < MIN_LIST_NAME_LENGTH
    val canCreate = draft.name.isNotBlank() && draft.name.length >= MIN_LIST_NAME_LENGTH

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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
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
private fun BrowseListsSection(
    state: BrowseListsState,
    actions: BrowseListsActions,
) {
    SearchListsField(
        filterText = state.filterText,
        onFilterTextChanged = actions.onFilterTextChanged,
        focusRequester = state.focusRequester,
    )

    Spacer(modifier = Modifier.height(16.dp))

    ListsChooserContent(
        filterText = state.filterText,
        topLevelLists = state.topLevelLists,
        currentParentId = state.currentParentId,
        treeState =
            ListChooserTreeState(
                childMap = state.childMap,
                expandedIds = state.expandedIds,
                disabledIds = state.disabledIds,
                highlightedListId = state.highlightedListId,
            ),
        actions =
            ListChooserTreeActions(
                onToggleExpanded = actions.onToggleExpanded,
                onSelect = { selectedId ->
                    actions.onConfirm(selectedId)
                    actions.onDismiss()
                },
                onAddSubProjectRequest = actions.onStartCreateChild,
            ),
    )

    Spacer(modifier = Modifier.height(16.dp))

    BrowseListsFooter(
        onStartCreateRoot = actions.onStartCreateRoot,
        onDismiss = actions.onDismiss,
    )
}

@Composable
private fun BrowseListsFooter(
    onStartCreateRoot: () -> Unit,
    onDismiss: () -> Unit,
) {
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
private fun ListsChooserContent(
    filterText: String,
    topLevelLists: List<Context>,
    currentParentId: String?,
    treeState: ListChooserTreeState,
    actions: ListChooserTreeActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * CHOOSER_HEIGHT_FRACTION).dp),
    ) {
        if (filterText.isBlank()) {
            item {
                val isAlreadyAtRoot = currentParentId == null
                SelectableRootItem(
                    isEnabled = !isAlreadyAtRoot,
                    onSelect = { actions.onSelect(null) },
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
                treeState = treeState,
                level = 0,
                actions = actions,
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
    treeState: ListChooserTreeState,
    level: Int,
    actions: ListChooserTreeActions,
) {
    val isExpanded = list.id in treeState.expandedIds
    val children = treeState.childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isEnabled = list.id !in treeState.disabledIds
    val isHighlighted = list.id == treeState.highlightedListId
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
            state =
                SelectableListRowState(
                    list = list,
                    level = level,
                    hasChildren = hasChildren,
                    isExpanded = isExpanded,
                    isEnabled = isEnabled,
                    backgroundColor = backgroundColor,
                    interactionSource = interactionSource,
                ),
            actions = actions,
        )
        ChildDivider(level = level)
        ChildListItems(
            children = children,
            isExpanded = isExpanded,
            treeState = treeState,
            level = level,
            actions = actions,
        )
    }
}

@Composable
private fun SelectableListRow(
    state: SelectableListRowState,
    actions: ListChooserTreeActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(state.backgroundColor)
                .clickable(
                    enabled = state.isEnabled,
                    onClick = { actions.onSelect(state.list.id) },
                    interactionSource = state.interactionSource,
                    indication = null,
                ).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width((state.level * TREE_INDENT_DP).dp))
        ExpandToggle(
            hasChildren = state.hasChildren,
            isExpanded = state.isExpanded,
            onToggle = { actions.onToggleExpanded(state.list.id) },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = state.list.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (state.isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (state.isEnabled) {
            IconButton(
                onClick = { actions.onAddSubProjectRequest(state.list) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Додати підсписок до ${state.list.name}",
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
                    .width((level * TREE_INDENT_DP).dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun ChildListItems(
    children: List<Context>,
    isExpanded: Boolean,
    treeState: ListChooserTreeState,
    level: Int,
    actions: ListChooserTreeActions,
) {
    if (!isExpanded || children.isEmpty()) return

    children.forEach { child ->
        RecursiveSelectableListItem(
            list = child,
            treeState = treeState,
            level = level + 1,
            actions = actions,
        )
    }
}

private fun resolveChooserTitle(
    title: String,
    isCreatingMode: Boolean,
    draft: CreateListDraft,
): String {
    if (!isCreatingMode) return title
    return draft.parent?.let { parent ->
        "Новий підсписок для '${parent.name}'"
    } ?: "Новий список верхнього рівня"
}

@Composable
private fun ChooserTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}
