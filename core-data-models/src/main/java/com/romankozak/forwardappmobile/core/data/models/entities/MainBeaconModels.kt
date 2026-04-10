package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class MainBeaconReadinessStatus {
    READY,
    CONDITIONAL,
    BLOCKED,
    DEFECTED,
}

enum class MainBeaconLevelType {
    MAIN_BEACON,
    REALIZATION_MODEL_OF_MAIN_BEACON,
    MANDATORY_CORE_OF_MAIN_BEACON,
    STRATEGIC_PROJECTING_OF_MAIN_BEACON,
    LONG_TERM_STRATEGY,
    MEDIUM_TERM_PROGRAM,
    WEEK,
    DAY,
}

enum class MainBeaconSyncStatus {
    UNSET,
    IN_SYNC,
    IN_PROCESS,
    NEEDS_REVIEW,
    OUTDATED_BY_PARENT,
}

@Entity(
    tableName = "main_beacons",
    indices = [
        Index(value = ["readiness_status"]),
    ],
)
@TypeConverters(Converters::class)
data class MainBeacon(
    @PrimaryKey
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @ColumnInfo(name = "why_it_matters")
    @SerializedName("whyItMatters")
    val whyItMatters: String? = null,
    @ColumnInfo(name = "success_shape")
    @SerializedName("successShape")
    val successShape: String? = null,
    @ColumnInfo(name = "failure_shape")
    @SerializedName("failureShape")
    val failureShape: String? = null,
    @ColumnInfo(name = "anti_goal")
    @SerializedName("antiGoal")
    val antiGoal: String? = null,
    @ColumnInfo(name = "decision_impact")
    @SerializedName("decisionImpact")
    val decisionImpact: String? = null,
    @ColumnInfo(name = "readiness_status")
    @SerializedName("readinessStatus")
    val readinessStatus: MainBeaconReadinessStatus = MainBeaconReadinessStatus.BLOCKED,
    @ColumnInfo(name = "blocker_text")
    @SerializedName("blockerText")
    val blockerText: String? = null,
    @ColumnInfo(name = "next_action_text")
    @SerializedName("nextActionText")
    val nextActionText: String? = null,
    @ColumnInfo(name = "beacon_order", defaultValue = "0")
    @SerializedName("order")
    val order: Long = 0L,
    @ColumnInfo(name = "updatedAt")
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "createdAt")
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "main_beacon_context_cross_ref",
    primaryKeys = ["beacon_id", "context_id"],
    foreignKeys = [
        ForeignKey(
            entity = MainBeacon::class,
            parentColumns = ["id"],
            childColumns = ["beacon_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["context_id"]),
    ],
)
data class MainBeaconContextCrossRef(
    @ColumnInfo(name = "beacon_id")
    @SerializedName("beaconId")
    val beaconId: String,
    @ColumnInfo(name = "context_id")
    @SerializedName("contextId")
    val contextId: String,
)

@Entity(
    tableName = "main_beacon_attachment_cross_ref",
    primaryKeys = ["beacon_id", "attachment_id"],
    foreignKeys = [
        ForeignKey(
            entity = MainBeacon::class,
            parentColumns = ["id"],
            childColumns = ["beacon_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["attachment_id"]),
    ],
)
data class MainBeaconAttachmentCrossRef(
    @ColumnInfo(name = "beacon_id")
    @SerializedName("beaconId")
    val beaconId: String,
    @ColumnInfo(name = "attachment_id")
    @SerializedName("attachmentId")
    val attachmentId: String,
)

@Entity(
    tableName = "main_beacon_level_statuses",
    foreignKeys = [
        ForeignKey(
            entity = MainBeacon::class,
            parentColumns = ["id"],
            childColumns = ["main_beacon_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["main_beacon_id"]),
        Index(value = ["main_beacon_id", "level_type"], unique = true),
    ],
)
@TypeConverters(Converters::class)
data class MainBeaconLevelStatus(
    @PrimaryKey
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "main_beacon_id")
    @SerializedName("mainBeaconId")
    val mainBeaconId: String,
    @ColumnInfo(name = "level_type")
    @SerializedName("levelType")
    val levelType: MainBeaconLevelType,
    @ColumnInfo(name = "general_status")
    @SerializedName("generalStatus")
    val generalStatus: MainBeaconReadinessStatus = MainBeaconReadinessStatus.BLOCKED,
    @ColumnInfo(name = "sync_status")
    @SerializedName("syncStatus")
    val syncStatus: MainBeaconSyncStatus = MainBeaconSyncStatus.IN_SYNC,
    @ColumnInfo(name = "blocker_text")
    @SerializedName("blockerText")
    val blockerText: String? = null,
    @ColumnInfo(name = "next_action_text")
    @SerializedName("nextActionText")
    val nextActionText: String? = null,
    @ColumnInfo(name = "updatedAt")
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
)
