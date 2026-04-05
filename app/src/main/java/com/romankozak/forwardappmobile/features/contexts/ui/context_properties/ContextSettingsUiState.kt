package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

data class ContextSettingsUiState(
    val contextId: String? = null,
    val title: TextFieldValue = TextFieldValue(""),
    val description: TextFieldValue = TextFieldValue(""),
    val relatedLinks: List<RelatedLink> = emptyList(),
    val availableContexts: List<ProjectOption> = emptyList(),
    val availableAttachments: List<AttachmentOption> = emptyList(),
    val selectedAttachmentIds: Set<String> = emptySet(),
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
    val isBeaconProgressExpanded: Boolean = true,
    val isRelativeSizeExpanded: Boolean = true,
    val isDescriptionEditorOpen: Boolean = false,
    val reminderTime: Long? = null,
    val selectedTabIndex: Int = 0,
    val showCheckboxes: Boolean = true,
    val isProjectManagementEnabled: Boolean = false,
    val currentPresetLabel: String? = null,
    val basePresetCode: String? = null,
    val capabilityApplyMode: String = "ADDITIVE",
    val enabledCapabilityIds: Set<CapabilityId> = emptySet(),
    val experimentalCapabilityIds: List<CapabilityId> = emptyList(),
    val availablePresets: List<ContextRoleProfile> = emptyList(),
    val autoLinkSubprojects: Boolean = true,
    val features: Map<String, Boolean> = emptyMap(),
)
