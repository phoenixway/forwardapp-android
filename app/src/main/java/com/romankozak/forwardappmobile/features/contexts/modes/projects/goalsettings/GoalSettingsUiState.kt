package com.romankozak.forwardappmobile.features.contexts.modes.projects.goalsettings

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.models.ScoringStatusValues

data class GoalSettingsUiState(
    val title: TextFieldValue = TextFieldValue(""),
    val description: TextFieldValue = TextFieldValue(""),
    val relatedLinks: List<RelatedLink> = emptyList(),
    val tags: List<String> = emptyList(),
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
    val isScoringEnabled: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val isDescriptionEditorOpen: Boolean = false,
    val reminderTime: Long? = null,
    val selectedTabIndex: Int = 0,
)
