// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaledit/GoalEditScreen.kt ---
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goaledit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.notesEditors.LimitedMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import com.romankozak.forwardappmobile.ui.utils.formatDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


private object Scales {
    val effort = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val importance = (1..12).map { it.toFloat() }
    val impact = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    val cost = (0..5).map { it.toFloat() }
    val risk = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val weights = (0..20).map { it * 0.1f }
    val costLabels = listOf("немає", "дуже низькі", "низькі", "середні", "високі", "дуже високі")
}

@Composable
fun GoalEditScreen(
    navController: NavController,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    // --- Основні стани та контекст ---
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Налаштування для отримання результатів від інших екранів ---
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val navGraphEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)


    // --- Обробка навігаційних подій від ViewModel ---
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                // ЗМІНЕНО: Тепер перед поверненням назад ми встановлюємо результат
                is GoalEditEvent.NavigateBack -> {
                    event.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    // Повідомляємо попередній екран, що потрібно оновити дані
                    resultViewModel.setResult("refresh_needed", true)
                    navController.popBackStack()
                }
                is GoalEditEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    // --- Отримання результату від екрана вибору списку ---
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Перевіряємо, чи повернувся ID обраного списку
            val selectedListId = resultViewModel.consumeResult<String>("selectedListId")
            if (selectedListId != null) {
                viewModel.onListChooserResult(selectedListId)
            }
        }
    }


    // --- Логіка для підказок контекстів (@...) ---
    val allContexts by viewModel.allContextNames.collectAsStateWithLifecycle()
    var showSuggestions by remember { mutableStateOf(false) }
    var filteredContexts by remember { mutableStateOf<List<String>>(emptyList()) }

    fun getCurrentWord(textValue: TextFieldValue): String? {
        val cursorPosition = textValue.selection.start
        if (cursorPosition == 0) return null
        val textUpToCursor = textValue.text.substring(0, cursorPosition)
        val lastSpaceIndex = textUpToCursor.lastIndexOf(' ')
        val startIndex = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1
        val currentWord = textUpToCursor.substring(startIndex)
        return currentWord.takeIf { it.startsWith("@") }
    }

    LaunchedEffect(uiState.goalText) {
        val currentWord = getCurrentWord(uiState.goalText)
        if (currentWord != null && currentWord.length > 1) {
            val query = currentWord.substring(1)
            filteredContexts = allContexts.filter { it.startsWith(query, ignoreCase = true) }
            showSuggestions = filteredContexts.isNotEmpty()
        } else {
            showSuggestions = false
        }
    }
    Scaffold(
        modifier = Modifier
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    Column {
                        OutlinedTextField(
                            value = uiState.goalText,
                            onValueChange = viewModel::onTextChange,
                            label = { Text("Назва цілі") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        SuggestionChipsRow(
                            visible = showSuggestions,
                            contexts = filteredContexts,
                            onContextClick = { context ->
                                val currentText = uiState.goalText.text
                                val cursorPosition = uiState.goalText.selection.start

                                val wordStart = currentText.substring(0, cursorPosition)
                                    .lastIndexOf(' ')
                                    .let { if (it == -1) 0 else it + 1 }
                                    .takeIf { currentText.substring(it, cursorPosition).startsWith("@") }
                                    ?: -1

                                if (wordStart != -1) {
                                    val textBefore = currentText.substring(0, wordStart)
                                    val textAfter = currentText.substring(cursorPosition)
                                    val newText = "$textBefore@$context $textAfter"
                                    val newCursorPosition = textBefore.length + 1 + context.length + 1

                                    viewModel.onTextChange(
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newCursorPosition),
                                        ),
                                    )
                                }
                                showSuggestions = false
                            },
                        )
                    }
                }

                item {
                    LimitedMarkdownEditor(
                        value = uiState.goalDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        maxHeight = 150.dp,
                        onExpandClick = { viewModel.openDescriptionEditor() },
                        modifier = Modifier.fillMaxWidth(),
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
                    EvaluationSection(uiState = uiState, onViewModelAction = viewModel)
                }

                item {
                    val createdAt = uiState.createdAt
                    if (createdAt != null) {
                        Column(
                            modifier = Modifier
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Пов'язані посилання", style = MaterialTheme.typography.titleMedium)

                if (relatedLinks.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = relatedLinks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (relatedLinks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(relatedLinks) { link ->
                        LinkItem(
                            link = link,
                            onRemove = { onRemoveLink(link.target) },
                            onClick = { /* TODO: Navigate based on link type */ }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ціль ще не має пов'язаних посилань",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AddLinksButtons(
                onAddListLink = onAddLink,
                onAddWebLink = onAddWebLink,
                onAddObsidianLink = onAddObsidianLink
            )
        }
    }
}

@Composable
private fun LinkItem(
    link: RelatedLink,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (link.type) {
            LinkType.GOAL_LIST -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            LinkType.URL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            when (link.type) {
                LinkType.GOAL_LIST -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                LinkType.URL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (link.type) {
                        LinkType.GOAL_LIST -> Icons.Default.List
                        LinkType.URL -> Icons.Default.Language
                        LinkType.OBSIDIAN -> Icons.Default.Note
                        else -> Icons.Default.Link
                    },
                    contentDescription = null,
                    tint = when (link.type) {
                        LinkType.GOAL_LIST -> MaterialTheme.colorScheme.primary
                        LinkType.URL -> MaterialTheme.colorScheme.secondary
                        LinkType.OBSIDIAN -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.displayName ?: link.target,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = when (link.type) {
                            LinkType.GOAL_LIST -> "Список цілей"
                            LinkType.URL -> "Веб-посилання"
                            LinkType.OBSIDIAN -> "Obsidian нотатка"
                            else -> "Посилання"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Видалити посилання",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddLinksButtons(
    onAddListLink: () -> Unit,
    onAddWebLink: () -> Unit,
    onAddObsidianLink: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isExpanded) {
            OutlinedButton(
                onClick = onAddListLink,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати список цілей")
            }

            OutlinedButton(
                onClick = onAddWebLink,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати веб-посилання")
            }

            OutlinedButton(
                onClick = onAddObsidianLink,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Note,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати Obsidian нотатку")
            }

            TextButton(
                onClick = { isExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExpandLess, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Згорнути")
            }
        } else {
            OutlinedButton(
                onClick = { isExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Додати посилання")
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


@Composable
private fun EvaluationSection(uiState: GoalEditUiState, onViewModelAction: GoalEditViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Оцінка", style = MaterialTheme.typography.titleLarge)
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ScoringStatusSelector(
                            selectedStatus = uiState.scoringStatus,
                            onStatusSelected = onViewModelAction::onScoringStatusChange,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        if (uiState.scoringStatus == ScoringStatus.ASSESSED) {
                            val rawScore = uiState.rawScore
                            val balanceText = "Balance: ${if (rawScore >= 0) "+" else ""}" + "%.2f".format(rawScore)
                            val balanceColor = when {
                                rawScore > 0.2 -> Color(0xFF2E7D32) // Strong Green
                                rawScore > -0.2 -> LocalContentColor.current
                                else -> Color(0xFFC62828) // Strong Red
                            }
                            Text(
                                text = balanceText,
                                color = balanceColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        EvaluationTabs(
                            uiState = uiState,
                            onViewModelAction = onViewModelAction,
                            isEnabled = uiState.isScoringEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringStatusSelector(
    selectedStatus: ScoringStatus,
    onStatusSelected: (ScoringStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = ScoringStatus.entries.toTypedArray()
    val labels = mapOf(
        ScoringStatus.NOT_ASSESSED to "Unset",
        ScoringStatus.ASSESSED to "Set",
        ScoringStatus.IMPOSSIBLE_TO_ASSESS to "Impossible",
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
            ) {
                Text(labels[status] ?: "")
            }
        }
    }
}

@Composable
private fun EvaluationTabs(
    uiState: GoalEditUiState,
    onViewModelAction: GoalEditViewModel,
    isEnabled: Boolean,
) {
    val tabTitles = listOf("Gain", "Loss", "Weights")
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.alpha(if (isEnabled) 1.0f else 0.5f)) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    enabled = isEnabled,
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            userScrollEnabled = isEnabled,
        ) { page ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    0 -> { // Gain
                        ParameterSlider(
                            label = "Value importance",
                            value = uiState.valueImportance,
                            onValueChange = onViewModelAction::onValueImportanceChange,
                            scale = Scales.importance,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Value gain impact",
                            value = uiState.valueImpact,
                            onValueChange = onViewModelAction::onValueImpactChange,
                            scale = Scales.impact,
                            enabled = isEnabled,
                        )
                    }
                    1 -> { // Loss
                        ParameterSlider(
                            label = "Efforts",
                            value = uiState.effort,
                            onValueChange = onViewModelAction::onEffortChange,
                            scale = Scales.effort,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Costs",
                            value = uiState.cost,
                            onValueChange = onViewModelAction::onCostChange,
                            scale = Scales.cost,
                            valueLabels = Scales.costLabels,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Risk",
                            value = uiState.risk,
                            onValueChange = onViewModelAction::onRiskChange,
                            scale = Scales.risk,
                            enabled = isEnabled,
                        )
                    }
                    2 -> { // Weights
                        ParameterSlider(
                            label = "Efforts weight",
                            value = uiState.weightEffort,
                            onValueChange = onViewModelAction::onWeightEffortChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Costs weight",
                            value = uiState.weightCost,
                            onValueChange = onViewModelAction::onWeightCostChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Risk weight",
                            value = uiState.weightRisk,
                            onValueChange = onViewModelAction::onWeightRiskChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    scale: List<Float>,
    enabled: Boolean,
    valueLabels: List<String>? = null,
) {
    val currentIndex = scale.indexOf(value).coerceAtLeast(0)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            val displayText = when {
                valueLabels != null -> valueLabels.getOrElse(currentIndex) { value.toString() }
                scale == Scales.weights -> "x${"%.1f".format(value)}"
                else -> value.toInt().toString()
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            enabled = enabled,
            value = currentIndex.toFloat(),
            onValueChange = { newIndex ->
                val roundedIndex = newIndex.roundToInt().coerceIn(0, scale.lastIndex)
                onValueChange(scale[roundedIndex])
            },
            valueRange = 0f..scale.lastIndex.toFloat(),
            steps = (scale.size - 2).coerceAtLeast(0),
        )
    }
}