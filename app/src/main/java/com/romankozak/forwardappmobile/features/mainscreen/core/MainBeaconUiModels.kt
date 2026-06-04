package com.romankozak.forwardappmobile.features.mainscreen.core

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus

data class MainBeaconCardUi(
    val id: String,
    val title: String,
    val readinessStatus: MainBeaconReadinessStatus,
    val highestCompletedLevel: String,
    val breakPointLevel: String,
    val blockReason: String,
    val nextRequiredAction: String,
    val relatedContextIds: List<String>,
    val relatedAttachmentIds: List<String>,
    val groupIds: List<String>,
    val parentBeaconId: String?,
)

data class MainBeaconGroupUi(
    val id: String,
    val title: String,
    val description: String?,
)

data class MainBeaconCardLinkUi(
    val id: String,
    val title: String,
)

data class MainBeaconEditorState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val whyItMatters: String = "",
    val successShape: String = "",
    val failureShape: String = "",
    val antiGoal: String = "",
    val decisionImpact: String = "",
    val readinessStatus: MainBeaconReadinessStatus = MainBeaconReadinessStatus.BLOCKED,
    val blockerText: String = "",
    val nextActionText: String = "",
    val relatedContextIds: Set<String> = emptySet(),
    val relatedAttachmentIds: Set<String> = emptySet(),
    val groupIds: Set<String> = emptySet(),
    val parentBeaconId: String? = null,
    val levelStatuses: List<MainBeaconLevelEditorState> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
)

data class MainBeaconLevelSummaryUi(
    val levelType: MainBeaconLevelType,
    val label: String,
    val generalStatus: MainBeaconReadinessStatus,
    val syncStatus: MainBeaconSyncStatus,
)

data class MainBeaconLevelEditorState(
    val levelType: MainBeaconLevelType,
    val generalStatus: MainBeaconReadinessStatus = MainBeaconReadinessStatus.BLOCKED,
    val syncStatus: MainBeaconSyncStatus = MainBeaconSyncStatus.IN_SYNC,
    val blockerText: String = "",
    val nextActionText: String = "",
)

data class MainBeaconOption(
    val id: String,
    val title: String,
)

fun MainBeaconLevelType.displayLabel(): String =
    when (this) {
        MainBeaconLevelType.MAIN_BEACON -> "Main beacon"
        MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON -> "Realization model"
        MainBeaconLevelType.MANDATORY_CORE_OF_MAIN_BEACON -> "Mandatory core"
        MainBeaconLevelType.STRATEGIC_PROJECTING_OF_MAIN_BEACON -> "Strategic projecting"
        MainBeaconLevelType.LONG_TERM_STRATEGY -> "Long term strategy"
        MainBeaconLevelType.MEDIUM_TERM_PROGRAM -> "Medium term program"
        MainBeaconLevelType.WEEK -> "Week"
        MainBeaconLevelType.DAY -> "Day"
    }

fun MainBeaconLevelStatus.toEditorState(): MainBeaconLevelEditorState =
    MainBeaconLevelEditorState(
        levelType = levelType,
        generalStatus = generalStatus,
        syncStatus = syncStatus,
        blockerText = blockerText.orEmpty(),
        nextActionText = nextActionText.orEmpty(),
    )
