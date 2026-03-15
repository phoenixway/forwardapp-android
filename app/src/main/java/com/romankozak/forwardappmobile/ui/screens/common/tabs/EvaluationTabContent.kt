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
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.components.ParameterSlider
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.components.ParameterSliderConfig
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.components.Scales
import kotlinx.coroutines.launch

private const val BALANCE_POSITIVE_THRESHOLD = 0.2
private const val BALANCE_NEUTRAL_THRESHOLD = -0.2
private const val DISABLED_CONTENT_ALPHA = 0.5f

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
                                    rawScore > BALANCE_POSITIVE_THRESHOLD -> MaterialTheme.colorScheme.tertiary
                                    rawScore > BALANCE_NEUTRAL_THRESHOLD -> LocalContentColor.current
                                    else -> MaterialTheme.colorScheme.error
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

    Column(modifier = Modifier.alpha(if (isEnabled) 1.0f else DISABLED_CONTENT_ALPHA)) {
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
                EvaluationTabPage(
                    page = page,
                    uiState = uiState,
                    onViewModelAction = onViewModelAction,
                    isEnabled = isEnabled,
                )
            }
        }
    }
}

@Composable
private fun EvaluationTabPage(
    page: Int,
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
    isEnabled: Boolean,
) {
    when (page) {
        0 -> GainTab(uiState = uiState, onViewModelAction = onViewModelAction, isEnabled = isEnabled)
        1 -> LossTab(uiState = uiState, onViewModelAction = onViewModelAction, isEnabled = isEnabled)
        2 -> WeightsTab(uiState = uiState, onViewModelAction = onViewModelAction, isEnabled = isEnabled)
    }
}

@Composable
private fun GainTab(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
    isEnabled: Boolean,
) {
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Value importance",
                value = uiState.valueImportance,
                scale = Scales.importance,
            ),
        onValueChange = onViewModelAction::onValueImportanceChange,
        enabled = isEnabled,
    )
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Value gain impact",
                value = uiState.valueImpact,
                scale = Scales.impact,
            ),
        onValueChange = onViewModelAction::onValueImpactChange,
        enabled = isEnabled,
    )
}

@Composable
private fun LossTab(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
    isEnabled: Boolean,
) {
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Efforts",
                value = uiState.effort,
                scale = Scales.effort,
            ),
        onValueChange = onViewModelAction::onEffortChange,
        enabled = isEnabled,
    )
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Costs",
                value = uiState.cost,
                scale = Scales.cost,
                valueLabels = Scales.costLabels,
            ),
        onValueChange = onViewModelAction::onCostChange,
        enabled = isEnabled,
    )
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Risk",
                value = uiState.risk,
                scale = Scales.risk,
            ),
        onValueChange = onViewModelAction::onRiskChange,
        enabled = isEnabled,
    )
}

@Composable
private fun WeightsTab(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
    isEnabled: Boolean,
) {
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Efforts weight",
                value = uiState.weightEffort,
                scale = Scales.weights,
            ),
        onValueChange = onViewModelAction::onWeightEffortChange,
        enabled = isEnabled,
    )
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Costs weight",
                value = uiState.weightCost,
                scale = Scales.weights,
            ),
        onValueChange = onViewModelAction::onWeightCostChange,
        enabled = isEnabled,
    )
    ParameterSlider(
        state =
            ParameterSliderConfig(
                label = "Risk weight",
                value = uiState.weightRisk,
                scale = Scales.weights,
            ),
        onValueChange = onViewModelAction::onWeightRiskChange,
        enabled = isEnabled,
    )
}

@Composable
fun ScoringStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses =
        listOf(
            ScoringStatusValues.NOT_ASSESSED,
            ScoringStatusValues.ASSESSED,
            ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
        )
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
