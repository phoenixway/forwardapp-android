package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun EvaluationTabs(
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
