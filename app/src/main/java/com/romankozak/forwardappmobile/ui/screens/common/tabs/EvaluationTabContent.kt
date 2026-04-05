package com.romankozak.forwardappmobile.ui.screens.common.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
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
private const val MAX_RELATIVE_SIZE = 5

@Composable
fun EvaluationTabContent(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BeaconProgressSection(
            uiState = uiState,
            onViewModelAction = onViewModelAction,
        )
        if (uiState.showRelativeSizeSection) {
            RelativeSizeSection(
                uiState = uiState,
                onViewModelAction = onViewModelAction,
            )
        }
    }
}

@Composable
private fun BeaconProgressSection(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionHeader(
                title = "Beacon progress",
                isExpanded = uiState.isBeaconProgressExpanded,
                onToggle = { onViewModelAction.onBeaconProgressExpandedChange(!uiState.isBeaconProgressExpanded) },
            )

            AnimatedVisibility(visible = uiState.isBeaconProgressExpanded) {
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
private fun RelativeSizeSection(
    uiState: EvaluationTabUiState,
    onViewModelAction: EvaluationTabActions,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionHeader(
                title = "Relative size",
                isExpanded = uiState.isRelativeSizeExpanded,
                onToggle = { onViewModelAction.onRelativeSizeExpandedChange(!uiState.isRelativeSizeExpanded) },
            )

            AnimatedVisibility(visible = uiState.isRelativeSizeExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Умовні одиниці масштабу. 0 = не задано, 1..5 = від малої до великої цілі.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        RelativeSizePicker(
                            value = uiState.relativeSize,
                            onValueChange = onViewModelAction::onRelativeSizeChange,
                        )
                        Text(
                            text = relativeSizeLabel(uiState.relativeSize),
                            style = MaterialTheme.typography.labelLarge,
                            color =
                                if (uiState.relativeSize == 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
        )
    }
}

@Composable
private fun RelativeSizePicker(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (index in 1..MAX_RELATIVE_SIZE) {
                IconButton(
                    onClick = {
                        onValueChange(if (value == index) 0 else index)
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    DiamondGlyph(
                        filled = index <= value,
                        tint =
                            if (index <= value) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Text(
            text = if (value == 0) "Unset" else "$value/$MAX_RELATIVE_SIZE",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(56.dp),
            color =
                if (value == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

@Composable
private fun DiamondGlyph(
    filled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.rotate(45f),
        shape = RoundedCornerShape(3.dp),
        color = if (filled) tint else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.4.dp, tint),
    ) {}
}

private fun relativeSizeLabel(value: Int): String =
    when (value) {
        0 -> "Unset"
        1 -> "1/5 · Дуже мала"
        2 -> "2/5 · Мала"
        3 -> "3/5 · Середня"
        4 -> "4/5 · Велика"
        else -> "5/5 · Дуже велика"
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
