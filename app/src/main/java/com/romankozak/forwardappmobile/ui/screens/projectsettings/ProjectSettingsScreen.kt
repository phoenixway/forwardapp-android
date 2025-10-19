@file:OptIn(ExperimentalMaterial3Api::class)
package com.romankozak.forwardappmobile.ui.screens.projectsettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.components.SuggestionUtils
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditor
import com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs.DisplayTabContent
import com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs.EvaluationTabContent
import com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs.RemindersTabContent

import androidx.compose.ui.platform.LocalContext

// Copied private enum from GoalEditScreen
private enum class SuggestionType {
    CONTEXT,
    TAG,
}

@Composable
fun ProjectSettingsScreen(
    navController: androidx.navigation.NavController,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allContexts by viewModel.allContextNames.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectSettingsEvent.NavigateBack -> {
                    event.message?.let {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_needed", true)
                    navController.popBackStack()
                }
                is ProjectSettingsEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    val tabs = when (uiState.editMode) {
        EditMode.PROJECT -> listOf("General", "Display")
        EditMode.GOAL -> listOf("General", "Evaluation", "Reminders")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (uiState.editMode) {
                        EditMode.PROJECT -> if (uiState.isNewGoal) "New Project" else "Edit Project"
                        EditMode.GOAL -> if (uiState.isNewGoal) "New Goal" else "Edit Goal"
                    }
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onSave() },
                        enabled = uiState.title.text.isNotBlank(),
                    ) {
                        Text("Save")
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = uiState.selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTabIndex == index,
                        onClick = { viewModel.onTabSelected(index) },
                        text = { Text(title) }
                    )
                }
            }
            when (tabs[uiState.selectedTabIndex]) {
                "General" -> GeneralTabContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    allContexts = allContexts,
                    allTags = allTags
                )
                "Display" -> DisplayTabContent(
                    showCheckboxes = uiState.showCheckboxes,
                    onShowCheckboxesChange = viewModel::onShowCheckboxesChange
                )
                "Evaluation" -> EvaluationTabContent(
                    uiState = uiState,
                    onViewModelAction = viewModel
                )
                "Reminders" -> RemindersTabContent(
                    reminderTime = uiState.reminderTime,
                    onSetReminder = viewModel::onSetReminder,
                    onClearReminder = viewModel::onClearReminder
                )
            }
        }
    }

    if (uiState.isDescriptionEditorOpen) {
        FullScreenMarkdownEditor(
            initialValue = uiState.description,
            onDismiss = { viewModel.closeDescriptionEditor() },
            onSave = { newText -> viewModel.onDescriptionChangeAndCloseEditor(newText) },
        )
    }
}

@Composable
private fun GeneralTabContent(
    uiState: ProjectSettingsUiState,
    viewModel: ProjectSettingsViewModel,
    allContexts: List<String>,
    allTags: List<String>,
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
            if (uiState.editMode == EditMode.GOAL) {
                EnhancedTextInputSection(
                    title = uiState.title,
                    onTextChange = viewModel::onTextChange,
                    allContexts = allContexts,
                    allTags = allTags,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTextChange,
                    label = { Text("Назва проекту") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        }

        item {
            LimitedMarkdownEditor(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                maxHeight = 150.dp,
                onExpandClick = { viewModel.openDescriptionEditor() },
                modifier = Modifier.fillMaxWidth(),
                onCopy = { /* TODO */ },
            )
        }
        
        if (uiState.editMode == EditMode.GOAL) {
            item {
                RelatedLinksSection(
                    relatedLinks = uiState.relatedLinks,
                    onRemoveLink = viewModel::onRemoveLinkAssociation,
                    onAddLink = viewModel::onAddLinkRequest,
                    onAddWebLink = viewModel::onAddWebLinkRequest,
                    onAddObsidianLink = viewModel::onAddObsidianLinkRequest,
                )
            }
        }
    }
}

// --- Copied from GoalEditScreen.kt ---

@Composable
private fun EnhancedTextInputSection(
    title: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    allContexts: List<String>,
    allTags: List<String>,
    modifier: Modifier = Modifier,
) {
    var showSuggestions by remember { mutableStateOf(value = false) }
    var filteredContexts by remember { mutableStateOf<List<String>>(emptyList()) }
    var filteredTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSuggestionType by remember { mutableStateOf<SuggestionType?>(null) }

    LaunchedEffect(title) {
        val suggestionState =
            processSuggestions(
                text = title.text,
                cursorPosition = title.selection.start,
                allContexts = allContexts,
                allTags = allTags,
            )

        filteredContexts = suggestionState.contexts
        filteredTags = suggestionState.tags
        currentSuggestionType = suggestionState.type
        showSuggestions = suggestionState.shouldShow
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = title,
            onValueChange = onTextChange,
            label = { Text("Назва цілі") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = createSupportingText(showSuggestions, currentSuggestionType),
        )

        SuggestionChipsRow(
            visible = showSuggestions,
            contexts = if (currentSuggestionType == SuggestionType.CONTEXT) filteredContexts else emptyList(),
            tags = if (currentSuggestionType == SuggestionType.TAG) filteredTags else emptyList(),
            onContextClick = { context ->
                handleContextSelection(title, onTextChange, context)
                showSuggestions = false
            },
            onTagClick = { tag ->
                handleTagSelection(title, onTextChange, tag)
                showSuggestions = false
            }
        )
    }
}

private data class SuggestionState(
    val contexts: List<String>,
    val tags: List<String>,
    val type: SuggestionType?,
    val shouldShow: Boolean,
)

private fun processSuggestions(
    text: String,
    cursorPosition: Int,
    allContexts: List<String>,
    allTags: List<String>,
): SuggestionState {
    val currentWordInfo =
        SuggestionUtils.getCurrentWord(text, cursorPosition)
            ?: return SuggestionState(emptyList(), emptyList(), null, false)

    val (currentWord, _) = currentWordInfo

    return when {
        isContextQuery(currentWord) -> {
            val query = currentWord!!.substring(1)
            val filtered = filterContexts(allContexts, query)
            SuggestionState(filtered, emptyList(), SuggestionType.CONTEXT, filtered.isNotEmpty())
        }
        isTagQuery(currentWord) -> {
            val query = currentWord!!.substring(1)
            val filtered = filterTags(allTags, query)
            SuggestionState(emptyList(), filtered, SuggestionType.TAG, filtered.isNotEmpty())
        }
        else -> SuggestionState(emptyList(), emptyList(), null, false)
    }
}

private fun isContextQuery(word: String?): Boolean = word?.startsWith("@") == true && word.length > 1

private fun isTagQuery(word: String?): Boolean = word?.startsWith("#") == true && word.length > 1

private fun filterContexts(
    contexts: List<String>,
    query: String,
): List<String> = contexts.filter { it.startsWith(query, ignoreCase = true) }.take(5)

private fun filterTags(
    tags: List<String>,
    query: String,
): List<String> =
    tags.filter { tag ->
        val tagWithoutSymbol = if (tag.startsWith("#")) tag.substring(1) else tag
        tagWithoutSymbol.startsWith(query, ignoreCase = true)
    }.take(5)

@Composable
private fun createSupportingText(
    showSuggestions: Boolean,
    currentSuggestionType: SuggestionType?,
): (@Composable () -> Unit)? {
    return if (showSuggestions) {
        {
            Text(
                text =
                when (currentSuggestionType) {
                    SuggestionType.CONTEXT -> "Доступні контексти (@)"
                    SuggestionType.TAG -> "Доступні теги (#)"
                    null -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        null
    }
}

private fun handleTagSelection(
    title: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    tag: String
) {
    val replacementResult =
        SuggestionUtils.replaceCurrentWord(
            title.text,
            title.selection.start,
            tag,
        )

    replacementResult?.let { (newText, newCursorPosition) ->
        onTextChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPosition),
            ),
        )
    }
}

private fun handleContextSelection(
    title: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    context: String,
) {
    val replacementResult =
        SuggestionUtils.replaceCurrentWord(
            title.text,
            title.selection.start,
            "@$context",
        )

    replacementResult?.let { (newText, newCursorPosition) ->
        onTextChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPosition),
            ),
        )
    }
}



@Composable
private fun RelatedLinksSection(
    relatedLinks: List<RelatedLink>,
    onRemoveLink: (String) -> Unit,
    onAddLink: () -> Unit,
    onAddWebLink: () -> Unit,
    onAddObsidianLink: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Пов'язані посилання", style = MaterialTheme.typography.titleMedium)

                if (relatedLinks.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = relatedLinks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (relatedLinks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(relatedLinks) { link ->
                        LinkItem(
                            link = link,
                            onRemove = { onRemoveLink(link.target) },
                            onClick = { },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ціль ще не має пов'язаних посилань",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            AddLinksButtons(
                onAddProjectLink = onAddLink,
                onAddWebLink = onAddWebLink,
                onAddObsidianLink = onAddObsidianLink,
            )
        }
    }
}

@Composable
private fun AddLinksButtons(
    onAddProjectLink: () -> Unit,
    onAddWebLink: () -> Unit,
    onAddObsidianLink: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isExpanded) {
            OutlinedButton(
                onClick = onAddProjectLink,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати проект")
            }

            OutlinedButton(
                onClick = onAddWebLink,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати веб-посилання")
            }

            OutlinedButton(
                onClick = onAddObsidianLink,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Note,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати Obsidian нотатку")
            }

            TextButton(
                onClick = { isExpanded = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ExpandLess, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Згорнути")
            }
        } else {
            OutlinedButton(
                onClick = { isExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати посилання")
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LinkItem(
    link: RelatedLink,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (link.type) {
            LinkType.PROJECT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            LinkType.URL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            null -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        },
        border = BorderStroke(
            1.dp,
            when (link.type) {
                LinkType.PROJECT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                LinkType.URL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                null -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = when (link.type) {
                        LinkType.PROJECT -> Icons.AutoMirrored.Filled.List
                        LinkType.URL -> Icons.Default.Language
                        LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                        null -> Icons.AutoMirrored.Filled.Note
                    },
                    contentDescription = null,
                    tint = when (link.type) {
                        LinkType.PROJECT -> MaterialTheme.colorScheme.primary
                        LinkType.URL -> MaterialTheme.colorScheme.secondary
                        LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary
                        null -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.displayName ?: link.target,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = when (link.type) {
                            LinkType.PROJECT -> "Проект"
                            LinkType.URL -> "Веб-посилання"
                            LinkType.OBSIDIAN -> "Obsidian нотатка"
                            null -> "broken"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Видалити посилання",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}