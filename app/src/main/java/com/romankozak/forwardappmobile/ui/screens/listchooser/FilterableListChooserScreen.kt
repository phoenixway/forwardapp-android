// File: FilterableListChooserScreen.kt
package com.romankozak.forwardappmobile.ui.screens.listchooser

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.* // Важливо: переконайтесь, що цей імпорт є
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.data.database.models.GoalList
import kotlinx.coroutines.delay
import java.util.*
import com.romankozak.forwardappmobile.R


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FilterableListChooserScreen(
    title: String,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    chooserUiState: ChooserUiState,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onConfirm: (String?) -> Unit,
    currentParentId: String?,
    disabledIds: Set<String> = emptySet(),
    onAddNewList: (id: String, parentId: String?, name: String) -> Unit,
    showDescendants: Boolean,
    onToggleShowDescendants: () -> Unit,
) {
    var isCreatingMode by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var parentForNewList by remember { mutableStateOf<GoalList?>(null) }
    var highlightedListId by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    LaunchedEffect(highlightedListId) {
        if (highlightedListId != null) {
            delay(3000L)
            highlightedListId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = if (isCreatingMode) {
                            parentForNewList?.let { "Новий підсписок: '${it.name}'" }
                                ?: "Новий список"
                        } else title,
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() with
                                    slideOutVertically { it } + fadeOut()
                        }
                    ) { titleText ->
                        Text(
                            text = titleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCreatingMode)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isCreatingMode) {
                val fabScale by animateFloatAsState(
                    targetValue = if (listState.isScrollInProgress) 0.8f else 1f,
                    animationSpec = spring(),
                    label = "fabScaleAnimation"
                )

                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        parentForNewList = null
                        newListName = ""
                        isCreatingMode = true
                    },
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 16.dp)
                        .scale(fabScale),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 16.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, stringResource(R.string.create_button))
                        AnimatedVisibility(visible = !listState.isScrollInProgress) {
                            Row {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.create_button),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = isCreatingMode,
            transitionSpec = {
                (slideInVertically { it } + fadeIn(tween(300)) + expandVertically()) with
                        (slideOutVertically { -it } + fadeOut(tween(200)) + shrinkVertically())
            },
            // --- ПОЧАТОК КЛЮЧОВОЇ ЗМІНИ ---
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Відступ від Scaffold (для TopAppBar)
                .imePadding()         // <-- ОСЬ ЦЕЙ РЯДОК ВСЕ ВИРІШУЄ
            // --- КІНЕЦЬ КЛЮЧОВОЇ ЗМІНИ ---
        ) { creating ->
            if (creating) {
                CreateListForm(
                    name = newListName,
                    onNameChange = { newListName = it },
                    onCancel = {
                        isCreatingMode = false
                        keyboardController?.hide()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onCreate = {
                        val id = UUID.randomUUID().toString()
                        onAddNewList(id, parentForNewList?.id, newListName)
                        highlightedListId = id
                        isCreatingMode = false
                        keyboardController?.hide()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = onFilterTextChanged,
                        label = { Text(stringResource(R.string.search_lists)) },
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .focusRequester(searchFocusRequester),
                        singleLine = true,
                        leadingIcon = {
                            AnimatedContent(targetState = filterText.isNotEmpty(), label = "searchIconAnimation") { hasText ->
                                if (hasText) {
                                    Icon(Icons.Filled.Search, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Outlined.Search, null)
                                }
                            }
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = filterText.isNotEmpty(),
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                IconButton(
                                    onClick = {
                                        onFilterTextChanged("")
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                ) {
                                    Icon(Icons.Default.Close, stringResource(R.string.clear_search))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // ... (решта коду Column залишається без змін)

                    AnimatedVisibility(
                        visible = filterText.isNotBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onToggleShowDescendants()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            color = if (showDescendants)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (showDescendants) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (showDescendants)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.show_nested_lists),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (showDescendants)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                AnimatedContent(targetState = showDescendants, label = "showDescendantsTextAnimation") { isOn ->
                                    Text(
                                        text = stringResource(if (isOn) R.string.enabled else R.string.disabled),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (showDescendants)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    if (filterText.isNotBlank()) {
                        val filteredCount = chooserUiState.topLevelLists.size
                        val listsWord = if (filteredCount == 1)
                            stringResource(R.string.list_singular)
                        else
                            stringResource(R.string.lists_plural)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.found_lists, filteredCount, listsWord),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (chooserUiState.topLevelLists.isEmpty()) {
                            item {
                                EnhancedEmptyState(hasFilter = filterText.isNotBlank())
                            }
                        } else {
                            if (filterText.isBlank()) {
                                item {
                                    RootListItem(
                                        text = stringResource(R.string.root_level),
                                        isEnabled = currentParentId != null,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onConfirm(null)
                                        }
                                    )
                                }
                            }

                            items(chooserUiState.topLevelLists, key = { it.id }) { list ->
                                RecursiveSelectableListItem(
                                    list = list,
                                    childMap = chooserUiState.childMap,
                                    level = 0,
                                    expandedIds = expandedIds,
                                    onToggleExpanded = onToggleExpanded,
                                    onSelect = { id ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConfirm(id)
                                    },
                                    disabledIds = disabledIds,
                                    highlightedListId = highlightedListId,
                                    onAddSublistRequest = { parent ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        parentForNewList = parent
                                        newListName = ""
                                        isCreatingMode = true
                                    },
                                    filterText = filterText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ... решта файлу залишається без змін ...
// CreateListForm, EnhancedEmptyState, RecursiveSelectableListItem, etc.
// ...
@Composable
private fun CreateListForm(
    name: String,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(
                Icons.Outlined.CreateNewFolder,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.creating_new_list),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    onNameChange(it)
                    isError = it.isNotBlank() && it.length < 3
                },
                label = { Text(stringResource(R.string.new_list_name_label)) },
                placeholder = { Text(stringResource(R.string.new_list_name_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                isError = isError,
                supportingText = {
                    if (isError) {
                        Text(stringResource(R.string.minimum_3_characters))
                    } else if (name.isNotBlank()) {
                        Text(stringResource(R.string.character_counter, name.length))
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.length >= 3) onCreate()
                    }
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                leadingIcon = {
                    Icon(Icons.Outlined.Label, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onCreate,
                    enabled = name.length >= 3,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.create))
                }
            }
        }
    }
}

@Composable
private fun EnhancedEmptyState(hasFilter: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (hasFilter) Icons.Outlined.SearchOff else Icons.Outlined.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasFilter)
                    stringResource(R.string.lists_not_found)
                else
                    stringResource(R.string.no_lists),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasFilter)
                    stringResource(R.string.try_change_search_criteria)
                else
                    stringResource(R.string.create_first_list_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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
    filterText: String
) {
    val isExpanded = list.id in expandedIds
    val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
    val isEnabled = list.id !in disabledIds
    val isHighlighted = list.id == highlightedListId
    val haptic = LocalHapticFeedback.current

    val cardElevation by animateFloatAsState(
        targetValue = if (isHighlighted) 8f else 2f,
        animationSpec = spring(),
        label = "cardElevationAnimation"
    )

    val cardColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer
            !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(500),
        label = "cardColorAnimation"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "rotationAnimation"
    )

    Column {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardElevation.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(list.id)
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width((level * 16).dp))

                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleExpanded(list.id)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = highlightText(list.name, filterText),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.weight(1f)
                )

                if (children.isNotEmpty()) {
                    Text(
                        text = "${children.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (isEnabled) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddSublistRequest(list)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_sublist),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (isExpanded && children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
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
                    onAddSublistRequest = onAddSublistRequest,
                    filterText = filterText
                )
                if (child != children.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun highlightText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val start = text.indexOf(query, ignoreCase = true)
    return if (start >= 0) {
        buildAnnotatedString {
            append(text.substring(0, start))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                append(text.substring(start, start + query.length))
            }
            append(text.substring(start + query.length))
        }
    } else androidx.compose.ui.text.AnnotatedString(text)
}

@Composable
fun RootListItem(
    text: String,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) { onClick() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = if (isEnabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}