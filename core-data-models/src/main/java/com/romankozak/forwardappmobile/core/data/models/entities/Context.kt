package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "contexts",
)
data class Context(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long?,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLink>? = null,
    @ColumnInfo(name = "is_expanded", defaultValue = "1") @SerializedName("isExpanded") val isExpanded: Boolean = true,
    @ColumnInfo(name = "goal_order", defaultValue = "0") @SerializedName("order") val order: Long = 0,
    @ColumnInfo(name = "is_attachments_expanded", defaultValue = "0") @SerializedName("isAttachmentsExpanded") val isAttachmentsExpanded: Boolean = false,
    @ColumnInfo(name = "default_view_mode") @SerializedName("defaultViewModeName") val defaultViewModeName: String? = null,
    @ColumnInfo(name = "is_completed", defaultValue = "0") @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @ColumnInfo(name = "is_context_management_enabled") @SerializedName("isContextManagementEnabled") val isContextManagementEnabled: Boolean? = false,
    @ColumnInfo(name = "context_status") @SerializedName("contextStatus") val contextStatus: String? = ContextStatusValues.NO_PLAN,
    @ColumnInfo(name = "context_status_text") @SerializedName("contextStatusText") val contextStatusText: String? = null,
    @ColumnInfo(name = "context_log_level") @SerializedName("contextLogLevel") val contextLogLevel: String? = ContextLogLevelValues.NORMAL,
    @ColumnInfo(name = "total_time_spent_minutes") @SerializedName("totalTimeSpentMinutes") val totalTimeSpentMinutes: Long? = 0,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImportance") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImpact") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("effort") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("cost") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("risk") val risk: Float = 0f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightEffort") val weightEffort: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightCost") val weightCost: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightRisk") val weightRisk: Float = 1f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("rawScore") val rawScore: Float = 0f,
    @ColumnInfo(defaultValue = "0") @SerializedName("displayScore") val displayScore: Int = 0,
    @ColumnInfo(name = "scoring_status") @SerializedName("scoringStatus") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @ColumnInfo(name = "show_checkboxes", defaultValue = "0") @SerializedName("showCheckboxes") val showCheckboxes: Boolean = false,
    @ColumnInfo(name = "role_code") @SerializedName("roleCode") val roleCode: String? = null,
)