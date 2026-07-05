package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

data class TacticalMissionSelectionState(
    val selectedMissionIds: Set<Long>,
    val selectionMode: Boolean,
)

data class TacticalMissionListCallbacks(
    val onMissionToggled: (TacticalMission) -> Unit,
    val onMissionSelectionToggle: (TacticalMission) -> Unit,
    val onMissionClick: (TacticalMission) -> Unit,
    val onMissionLongPress: (TacticalMission) -> Unit,
    val onMissionMoreClick: (TacticalMission) -> Unit,
    val onLinkedContextClick: (String) -> Unit,
    val onLinkedAttachmentClick: (String) -> Unit,
    val onMissionsReordered: (List<TacticalMission>) -> Unit,
)

data class TacticalMissionListLookups(
    val projectOptions: List<ProjectOption>,
    val attachmentOptions: List<AttachmentOption>,
    val missionStreamTitleById: Map<String, String> = emptyMap(),
)
