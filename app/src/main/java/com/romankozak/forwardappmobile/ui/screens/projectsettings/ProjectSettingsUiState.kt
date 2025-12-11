package com.romankozak.forwardappmobile.ui.screens.projectsettings

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues

data class ProjectSettingsUiState(
    val title: TextFieldValue = TextFieldValue(""),
    val description: TextFieldValue = TextFieldValue(""),
    val tags: List<String> = emptyList(),
    val isReady: Boolean = false,
    val isNewProject: Boolean = true,
    val isScoringEnabled: Boolean = true,
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
    val showCheckboxes: Boolean = true,
    val isProjectManagementEnabled: Boolean = false,
)
