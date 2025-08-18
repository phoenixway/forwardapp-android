// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaledit/GoalEditScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goaledit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.components.FilterableListChooser
import com.romankozak.forwardappmobile.ui.components.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.LimitedMarkdownEditor
import com.romankozak.forwardappmobile.ui.components.SuggestionChipsRow
import com.romankozak.forwardappmobile.ui.components.formatDate
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val filteredListHierarchy by viewModel.filteredListHierarchy.collectAsState()
    val allContexts by viewModel.allContextNames.collectAsState()

    var showSuggestions by remember { mutableStateOf(value = false) }
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
        if (currentWord != null && currentWord.startsWith("@") && currentWord.length > 1) {
            val query = currentWord.substring(1)
            filteredContexts = allContexts.filter { it.startsWith(query, ignoreCase = true) }
            showSuggestions = filteredContexts.isNotEmpty()
        } else {
            showSuggestions = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalEditEvent.NavigateBack -> {
                    event.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    navController.popBackStack()
                }
            }
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
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                                    val newCursorPosition = wordStart + context.length + 2
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
                    AssociatedListsSection(
                        associatedLists = uiState.associatedLists,
                        onRemoveList = viewModel::onRemoveListAssociation,
                        onAddList = viewModel::onShowListChooser,
                    )
                }

                item {
                    EvaluationSection(uiState = uiState, viewModel = viewModel)
                }

                item {
                    val createdAt = uiState.createdAt
                    if (createdAt != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
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


    if (uiState.showListChooser) {
        FilterableListChooser(
            title = "Додати до списку",
            filterText = listChooserFilterText,
            onFilterTextChanged = viewModel::onListChooserFilterChanged,
            topLevelLists = filteredListHierarchy.topLevelLists,
            childMap = filteredListHierarchy.childMap,
            expandedIds = listChooserFinalExpandedIds,
            onToggleExpanded = viewModel::onListChooserToggleExpanded,
            onDismiss = viewModel::onDismissListChooser,
            onConfirm = { listId ->
                listId?.let { viewModel.onAddListAssociation(it) }
            },
            currentParentId = null,
            disabledIds = uiState.associatedLists.map { it.id }.toSet(),
            // ✨ ВИПРАВЛЕНО: Лямбда тепер відповідає оновленій сигнатурі
            onAddNewList = { id, parentId, name ->
                viewModel.addNewList(id, parentId, name)
            },
        )
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
private fun AssociatedListsSection(
    associatedLists: List<GoalList>,
    onRemoveList: (String) -> Unit,
    onAddList: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Пов'язані списки",
                style = MaterialTheme.typography.titleMedium,
            )

            if (associatedLists.isNotEmpty()) {
                FlowRow(
// WARNING: 'fun FlowRow(modifier: Modifier = ..., mainAxisSize: SizeMode = ..., mainAxisAlignment: MainAxisAlignment = ..., mainAxisSpacing: Dp = ..., crossAxisAlignment: FlowCrossAxisAlignment = ..., crossAxisSpacing: Dp = ..., lastLineMainAxisAlignment: MainAxisAlignment = ..., content: @Composable() ComposableFunction0<Unit>): Unit' is deprecated. accompanist/FlowRow is deprecated.
// For more migration information, please visit https://google.github.io/accompanist/flowlayout/.
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 4.dp,
                ) {
                    associatedLists.forEach { list ->
                        InputChip(
                            selected = false,
                            onClick = { /* Do nothing */ },
                            label = { Text(list.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Видалити зі списку",
                                    modifier = Modifier
                                        .size(InputChipDefaults.IconSize)
                                        .clickable { onRemoveList(list.id) },
                                )
                            },
                        )
                    }
                }
            } else {
                Text(
                    "Ціль ще не пов'язана з жодним списком.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onAddList,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Додати до списку")
            }
        }
    }
}


@Composable
private fun EvaluationSection(uiState: GoalEditUiState, viewModel: GoalEditViewModel) {
    var isExpanded by remember { mutableStateOf(value = false) }

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
                Text(
                    "Оцінка",
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ScoringStatusSelector(
                            selectedStatus = uiState.scoringStatus,
                            onStatusSelected = viewModel::onScoringStatusChange,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        val rawScore = uiState.rawScore
                        val balanceText = "Balance: ${if (rawScore >= 0) "+" else ""}" + "%.2f".format(rawScore)
                        val balanceColor = when {
                            rawScore > 0.2 -> Color(0xFF2E7D32) // Strong Green
                            rawScore > -0.2 -> LocalContentColor.current
                            else -> Color(0xFFC62828) // Strong Red
                        }

                        if (uiState.scoringStatus == ScoringStatus.ASSESSED) {
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
                            viewModel = viewModel,
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
    viewModel: GoalEditViewModel,
    isEnabled: Boolean,
) {
    val tabTitles = listOf("Gain", "Loss", "Weights")
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.alpha(if (isEnabled) 1.0f else 0.5f),
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            userScrollEnabled = isEnabled,
        ) { page ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    0 -> { // Користь
                        ParameterSlider(
                            label = "Value importance",
                            value = uiState.valueImportance,
                            onValueChange = viewModel::onValueImportanceChange,
                            scale = Scales.importance,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Value gain impact",
                            value = uiState.valueImpact,
                            onValueChange = viewModel::onValueImpactChange,
                            scale = Scales.impact,
                            enabled = isEnabled,
                        )
                    }
                    1 -> { // Витрати
                        ParameterSlider(
                            label = "Efforts",
                            value = uiState.effort,
                            onValueChange = viewModel::onEffortChange,
                            scale = Scales.effort,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Costs",
                            value = uiState.cost,
                            onValueChange = viewModel::onCostChange,
                            scale = Scales.cost,
                            valueLabels = Scales.costLabels,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Risk",
                            value = uiState.risk,
                            onValueChange = viewModel::onRiskChange,
                            scale = Scales.risk,
                            enabled = isEnabled,
                        )
                    }
                    2 -> { // Ваги
                        ParameterSlider(
                            label = "Efforts weight",
                            value = uiState.weightEffort,
                            onValueChange = viewModel::onWeightEffortChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Costs weight",
                            value = uiState.weightCost,
                            onValueChange = viewModel::onWeightCostChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Risk weight",
                            value = uiState.weightRisk,
                            onValueChange = viewModel::onWeightRiskChange,
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