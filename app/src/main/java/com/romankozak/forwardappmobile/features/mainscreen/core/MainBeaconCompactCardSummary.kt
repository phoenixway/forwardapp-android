package com.romankozak.forwardappmobile.features.mainscreen.core

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus

data class MainBeaconCompactCardSummary(
    val highestCompletedLevel: String,
    val breakPointLevel: String,
    val blockReason: String,
    val nextRequiredAction: String,
)

private enum class MainBeaconChainState {
    USABLE,
    READY,
    ROOT_UNSET,
    IN_PROCESS,
    CONDITIONAL,
    BLOCKED,
    OUTDATED,
    DEFECTED,
    NOT_STARTED,
    NEEDS_REVIEW,
}

fun deriveMainBeaconCompactCardSummary(
    levelStatuses: List<MainBeaconLevelStatus>,
): MainBeaconCompactCardSummary {
    val orderedLevels =
        MainBeaconRepository.DefaultLevels.map { levelType ->
            levelStatuses.firstOrNull { it.levelType == levelType }
        }

    val highestCompletedIndex =
        orderedLevels.indexOfFirst { status ->
            status.toChainState() !in setOf(MainBeaconChainState.READY, MainBeaconChainState.USABLE)
        }
            .let { firstNonReadyIndex ->
                when {
                    firstNonReadyIndex == -1 -> orderedLevels.lastIndex
                    firstNonReadyIndex == 0 -> -1
                    else -> firstNonReadyIndex - 1
                }
            }

    val highestCompletedLevel =
        orderedLevels.getOrNull(highestCompletedIndex)?.levelType?.displayLabel().orEmpty()
    val breakLevel =
        when {
            orderedLevels.isEmpty() -> null
            highestCompletedIndex == orderedLevels.lastIndex -> null
            else -> orderedLevels.getOrNull(highestCompletedIndex + 1)
        }
    val breakPointLevel = breakLevel?.levelType?.displayLabel().orEmpty()

    return MainBeaconCompactCardSummary(
        highestCompletedLevel = highestCompletedLevel,
        breakPointLevel = breakPointLevel,
        blockReason = breakLevel.deriveWhy(),
        nextRequiredAction = breakLevel.deriveNext(),
    )
}

private fun MainBeaconLevelStatus?.toChainState(): MainBeaconChainState =
    when {
        this == null -> MainBeaconChainState.NOT_STARTED
        levelType == com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType.MAIN_BEACON &&
            generalStatus == MainBeaconReadinessStatus.CONDITIONAL &&
            syncStatus == MainBeaconSyncStatus.UNSET -> MainBeaconChainState.USABLE
        levelType == com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType.MAIN_BEACON &&
            generalStatus == MainBeaconReadinessStatus.READY &&
            syncStatus == MainBeaconSyncStatus.UNSET -> MainBeaconChainState.READY
        levelType == com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType.MAIN_BEACON &&
            syncStatus == MainBeaconSyncStatus.UNSET -> MainBeaconChainState.ROOT_UNSET
        generalStatus == MainBeaconReadinessStatus.CONDITIONAL && syncStatus == MainBeaconSyncStatus.IN_SYNC -> MainBeaconChainState.USABLE
        generalStatus == MainBeaconReadinessStatus.READY && syncStatus == MainBeaconSyncStatus.IN_SYNC -> MainBeaconChainState.READY
        syncStatus == MainBeaconSyncStatus.IN_PROCESS -> MainBeaconChainState.IN_PROCESS
        generalStatus == MainBeaconReadinessStatus.READY && syncStatus == MainBeaconSyncStatus.OUTDATED_BY_PARENT -> MainBeaconChainState.OUTDATED
        generalStatus == MainBeaconReadinessStatus.READY && syncStatus == MainBeaconSyncStatus.NEEDS_REVIEW -> MainBeaconChainState.NEEDS_REVIEW
        generalStatus == MainBeaconReadinessStatus.CONDITIONAL -> MainBeaconChainState.CONDITIONAL
        generalStatus == MainBeaconReadinessStatus.BLOCKED -> MainBeaconChainState.BLOCKED
        generalStatus == MainBeaconReadinessStatus.DEFECTED -> MainBeaconChainState.DEFECTED
        else -> MainBeaconChainState.NOT_STARTED
    }

private fun MainBeaconLevelStatus?.deriveWhy(): String {
    this ?: return ""
    blockerText?.takeIf { it.isNotBlank() }?.let { return it }
    return when (toChainState()) {
        MainBeaconChainState.USABLE -> ""
        MainBeaconChainState.ROOT_UNSET -> ""
        MainBeaconChainState.IN_PROCESS -> "level is in process"
        MainBeaconChainState.CONDITIONAL -> "level is incomplete"
        MainBeaconChainState.BLOCKED -> "level is blocked"
        MainBeaconChainState.OUTDATED -> "level is outdated"
        MainBeaconChainState.DEFECTED -> "level has an open defect"
        MainBeaconChainState.NOT_STARTED -> "level is not started"
        MainBeaconChainState.NEEDS_REVIEW -> "level needs review"
        MainBeaconChainState.READY -> ""
    }
}

private fun MainBeaconLevelStatus?.deriveNext(): String {
    this ?: return ""
    nextActionText?.takeIf { it.isNotBlank() }?.let { return it }
    return when (toChainState()) {
        MainBeaconChainState.USABLE -> ""
        MainBeaconChainState.ROOT_UNSET -> ""
        MainBeaconChainState.IN_PROCESS -> "continue this level"
        MainBeaconChainState.CONDITIONAL -> "complete this level"
        MainBeaconChainState.BLOCKED -> "unblock this level"
        MainBeaconChainState.OUTDATED -> "resync this level from parent"
        MainBeaconChainState.DEFECTED -> "resolve the defect affecting this level"
        MainBeaconChainState.NOT_STARTED -> "create this level"
        MainBeaconChainState.NEEDS_REVIEW -> "review this level"
        MainBeaconChainState.READY -> ""
    }
}
