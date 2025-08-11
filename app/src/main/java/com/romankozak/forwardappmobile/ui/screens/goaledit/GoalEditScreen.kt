@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goaledit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.components.FilterableListChooser
import com.romankozak.forwardappmobile.ui.components.MarkdownText
import com.romankozak.forwardappmobile.ui.components.formatDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- Визначення шкал для UI ---
private object Scales {
    val effort = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val importance = (1..12).map { it.toFloat() }
    val impact = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    val cost = (0..5).map { it.toFloat() }
    val risk = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val weights = (0..20).map { it * 0.1f } // Лінійна 0.0 -> 2.0 з кроком 0.1
    val costLabels = listOf("немає", "дуже низькі", "низькі", "середні", "високі", "дуже високі")
}

@Composable
fun GoalEditScreen(
    navController: NavController,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val listChooserExpandedIds by viewModel.listChooserExpandedIds.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val filteredListHierarchy by viewModel.filteredListHierarchy.collectAsState()

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
                        enabled = uiState.isReady && uiState.goalText.isNotBlank(),
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
                item { Spacer(Modifier.height(8.dp)) }

                item {
                    OutlinedTextField(
                        value = uiState.goalText,
                        onValueChange = viewModel::onTextChange,
                        label = { Text("Назва цілі") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.goalDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Notes (Markdown supported)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    )
                }

                if (uiState.goalDescription.isNotBlank()) {
                    item {
                        Column {
                            Text(
                                "Попередній перегляд опису:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                MarkdownText(
                                    text = uiState.goalDescription,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Пов'язані списки:", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisSpacing = 8.dp,
                        crossAxisSpacing = 4.dp,
                    ) {
                        uiState.associatedLists.forEach { list ->
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
                                            .clickable { viewModel.onRemoveListAssociation(list.id) },
                                    )
                                },
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.onShowListChooser() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add associated list")
                    }
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
                                .padding(top = 16.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Створено: ${formatDate(createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val updatedAt = uiState.updatedAt
                            if (updatedAt != null && updatedAt > createdAt + 1000) {
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
            expandedIds = listChooserExpandedIds,
            onToggleExpanded = viewModel::onListChooserToggleExpanded,
            onDismiss = viewModel::onDismissListChooser,
            onConfirm = viewModel::onAddListAssociation,
            disabledIds = uiState.associatedLists.map { it.id }.toSet()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvaluationSection(uiState: GoalEditUiState, viewModel: GoalEditViewModel) {
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
                Text(
                    "Assertion",
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScoringStatusSelector(
                        selectedStatus = uiState.scoringStatus,
                        onStatusSelected = viewModel::onScoringStatusChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    EvaluationTabs(
                        uiState = uiState,
                        viewModel = viewModel,
                        isEnabled = uiState.isScoringEnabled
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoringStatusSelector(
    selectedStatus: ScoringStatus,
    onStatusSelected: (ScoringStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = ScoringStatus.values()
    val labels = mapOf(
        ScoringStatus.NOT_ASSESSED to "Unset",
        ScoringStatus.ASSESSED to "Set",
        ScoringStatus.IMPOSSIBLE_TO_ASSESS to "Impossible"
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size)
            ) {
                Text(labels[status] ?: "")
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EvaluationTabs(
    uiState: GoalEditUiState,
    viewModel: GoalEditViewModel,
    isEnabled: Boolean
) {
    val tabTitles = listOf("Gain", "Loss", "Weights")
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.alpha(if (isEnabled) 1.0f else 0.5f)
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
            userScrollEnabled = isEnabled
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
                            enabled = isEnabled
                        )
                        ParameterSlider(
                            label = "Value gain impact",
                            value = uiState.valueImpact,
                            onValueChange = viewModel::onValueImpactChange,
                            scale = Scales.impact,
                            enabled = isEnabled
                        )
                    }
                    1 -> { // Витрати
                        ParameterSlider(
                            label = "Efforts",
                            value = uiState.effort,
                            onValueChange = viewModel::onEffortChange,
                            scale = Scales.effort,
                            enabled = isEnabled
                        )
                        ParameterSlider(
                            label = "Costs",
                            value = uiState.cost,
                            onValueChange = viewModel::onCostChange,
                            scale = Scales.cost,
                            valueLabels = Scales.costLabels,
                            enabled = isEnabled
                        )
                        ParameterSlider(
                            label = "Risk",
                            value = uiState.risk,
                            onValueChange = viewModel::onRiskChange,
                            scale = Scales.risk,
                            enabled = isEnabled
                        )
                    }
                    2 -> { // Ваги
                        ParameterSlider(
                            label = "Efforts weight",
                            value = uiState.weightEffort,
                            onValueChange = viewModel::onWeightEffortChange,
                            scale = Scales.weights,
                            enabled = isEnabled
                        )
                        ParameterSlider(
                            label = "Costs weight",
                            value = uiState.weightCost,
                            onValueChange = viewModel::onWeightCostChange,
                            scale = Scales.weights,
                            enabled = isEnabled
                        )
                        ParameterSlider(
                            label = "Risk weight",
                            value = uiState.weightRisk,
                            onValueChange = viewModel::onWeightRiskChange,
                            scale = Scales.weights,
                            enabled = isEnabled
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
    valueLabels: List<String>? = null
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
            steps = (scale.size - 2).coerceAtLeast(0)
        )
    }
}