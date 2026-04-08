package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

enum class LifeManagementLevelId(
    val label: String,
    val order: Int,
) {
    MAIN_BEACONS("main beacons", 0),
    REALIZATION_MODELS_OF_MAIN_BEACONS("realization models of main beacons", 1),
    MANDATORY_CORES_OF_MAIN_BEACONS("mandatory cores of main beacons", 2),
    STRATEGIC_PROJECTING_OF_MAIN_BEACONS("strategic projecting of main beacons", 3),
    LONG_TERM_STRATEGY("long term strategy", 4),
    MEDIUM_TERM_PROGRAM("medium term program", 5),
    WEEK("week", 6),
    DAY("day", 7),
}

enum class GeneralStatus {
    READY,
    CONDITIONAL,
    BLOCKED,
    DEFECTED,
}

enum class TransferStatus {
    NONE,
    PARTIAL,
    COMPLETE,
}

enum class FreshnessStatus {
    OUTDATED,
    NEEDS_REVIEW,
    FRESH,
    OUTDATED_BY_PARENT,
}

@Entity(tableName = "life_management_level_statuses")
data class LifeManagementLevelStatusEntity(
    @PrimaryKey
    @SerializedName("levelId")
    val levelId: LifeManagementLevelId,
    @SerializedName("generalStatus")
    val generalStatus: GeneralStatus,
    @SerializedName("transferStatus")
    val transferStatus: TransferStatus,
    @SerializedName("freshnessStatus")
    val freshnessStatus: FreshnessStatus,
    @SerializedName("blockerText")
    val blockerText: String? = null,
    @SerializedName("nextActionText")
    val nextActionText: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: Long,
)
