package com.romankozak.forwardappmobile.ui.screens.common.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.screens.contextproperties.components.ParameterSlider
import com.romankozak.forwardappmobile.ui.screens.contextproperties.components.Scales
import kotlinx.coroutines.launch

@Composable
fun EvaluationTabContent(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
) {
    var isExpanded by remember { mutableStateOf(true) }

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column {
            Row(
                modifier =
                Modifier
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

                        if (uiState.scoringStatus == ScoringStatusValues.ASSESSED) {
                            val rawScore = uiState.rawScore
                            val balanceText = "Balance: ${if (rawScore >= 0) "+" else ""}" + "%.2f".format(rawScore)
                            val balanceColor =
                                when {
                                    rawScore > 0.2 -> Color(0xFF2E7D32)
                                    rawScore > -0.2 -> LocalContentColor.current
                                    else -> Color(0xFFC62828)
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
fun EvaluationTabs(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
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
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            userScrollEnabled = isEnabled,
        ) { page ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    0 -> {
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
                    1 -> {
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
                    2 -> {
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
fun ScoringStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = listOf(ScoringStatusValues.NOT_ASSESSED, ScoringStatusValues.ASSESSED, ScoringStatusValues.IMPOSSIBLE_TO_ASSESS)
    val labels =
        mapOf(
            ScoringStatusValues.NOT_ASSESSED to "Unset",
            ScoringStatusValues.ASSESSED to "Set",
            ScoringStatusValues.IMPOSSIBLE_TO_ASSESS to "Impossible",
        )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
            ) {
                Text(
                    text = labels[status] ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}