package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues

@Composable
fun EvaluationSection(
    uiState: GoalEditUiState,
    onViewModelAction: GoalEditViewModel,
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
