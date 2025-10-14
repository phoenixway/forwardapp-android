@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goaledit

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.components.SuggestionUtils
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditor
import com.romankozak.forwardappmobile.ui.utils.copyToClipboard
import com.romankozak.forwardappmobile.ui.utils.formatDate

object Scales {
    val effort = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val importance = (1..12).map { it.toFloat() }
    val impact = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    val cost = (0..5).map { it.toFloat() }
    val risk = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val weights = (0..20).map { it * 0.1f }
    val costLabels = listOf("немає", "дуже низькі", "низькі", "середні", "високі", "дуже високі")
}

private enum class SuggestionType {
    CONTEXT,
    TAG,
}

@Composable
fun GoalEditScreen(
    navController: NavController,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalEditEvent.NavigateBack -> {
                    event.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_needed", true)
                    navController.popBackStack()
                }
                is GoalEditEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    val navBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(navBackStackEntry) {
        val resultFlow =
            navBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<String>("list_chooser_result")

        resultFlow?.observe(navBackStackEntry) { result ->
            if (result != null) {
                viewModel.onListChooserResult(result)
                navBackStackEntry.savedStateHandle.remove<String>("list_chooser_result")
            }
        }
    }

    val allContexts by viewModel.allContextNames.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewGoal) "Нова ціль" else "Редагувати ціль") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onSave() },
                        enabled = uiState.isReady && uiState.goalText.text.isNotBlank(),
                    ) {
                        Text("Зберегти")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (!uiState.isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            GoalEditScreenContent(
                uiState = uiState,
                viewModel = viewModel,
                allContexts = allContexts,
                allTags = allTags,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }

    if (uiState.isDescriptionEditorOpen) {
        FullScreenMarkdownEditor(
            initialValue = uiState.goalDescription,
            onDismiss = { viewModel.closeDescriptionEditor() },
            onSave = { newText -> viewModel.onDescriptionChangeAndCloseEditor(newText) },
        )
    }
}

@Composable
private fun GoalEditScreenContent(
    uiState: GoalEditUiState,
    viewModel: GoalEditViewModel,
    allContexts: List<String>,
    allTags: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            EnhancedTextInputSection(
                goalText = uiState.goalText,
                onTextChange = viewModel::onTextChange,
                allContexts = allContexts,
                allTags = allTags,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = {
                    copyToClipboard(context, uiState.goalText.text, "Goal Text")
                    Toast.makeText(context, "Goal text copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy goal text")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        }

        
        item {
            LimitedMarkdownEditor(
                value = uiState.goalDescription,
                onValueChange = viewModel::onDescriptionChange,
                maxHeight = 150.dp,
                onExpandClick = { viewModel.openDescriptionEditor() },
                modifier = Modifier.fillMaxWidth(),
                onCopy = {
                    copyToClipboard(context, uiState.goalDescription.text, "Goal Description")
                    Toast.makeText(context, "Goal description copied", Toast.LENGTH_SHORT).show()
                },
            )
        }

        
        item {
            RelatedLinksSection(
                relatedLinks = uiState.relatedLinks,
                onRemoveLink = viewModel::onRemoveLinkAssociation,
                onAddLink = viewModel::onAddLinkRequest,
                onAddWebLink = viewModel::onAddWebLinkRequest,
                onAddObsidianLink = viewModel::onAddObsidianLinkRequest,
            )
        }

        
        item {
            ReminderSection(
                reminderTime = uiState.reminderTime,
                onSetReminder = viewModel::onSetReminder,
                onClearReminder = viewModel::onClearReminder,
            )
        }

        
        item {
            EvaluationSection(uiState = uiState, onViewModelAction = viewModel)
        }

        
        item {
            if (allTags.isNotEmpty()) {
                TagStatistics(
                    allTags = allTags,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        
        item {
            val createdAt = uiState.createdAt
            if (createdAt != null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Створено: ${formatDate(createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val updatedAt = uiState.updatedAt
                    if (updatedAt != null && (updatedAt > createdAt + 1000)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Оновлено: ${formatDate(updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedTextInputSection(
    goalText: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    allContexts: List<String>,
    allTags: List<String>,
    modifier: Modifier = Modifier,
) {
    var showSuggestions by remember { mutableStateOf(value = false) }
    var filteredContexts by remember { mutableStateOf<List<String>>(emptyList()) }
    var filteredTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSuggestionType by remember { mutableStateOf<SuggestionType?>(null) }

    
    LaunchedEffect(goalText) {
        val suggestionState =
            processSuggestions(
                text = goalText.text,
                cursorPosition = goalText.selection.start,
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
            value = goalText,
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
                handleContextSelection(goalText, onTextChange, context)
                showSuggestions = false
            },
            onTagClick = { tag ->
                handleTagSelection(goalText, onTextChange, tag)
                showSuggestions = false
            },
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

private fun handleContextSelection(
    goalText: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    context: String,
) {
    val replacementResult =
        SuggestionUtils.replaceCurrentWord(
            goalText.text,
            goalText.selection.start,
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

private fun handleTagSelection(
    goalText: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    tag: String,
) {
    val tagToInsert = if (tag.startsWith("#")) tag else "#$tag"
    val replacementResult =
        SuggestionUtils.replaceCurrentWord(
            goalText.text,
            goalText.selection.start,
            tagToInsert,
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
