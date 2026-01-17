package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStatusValues
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedGroup
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.models.ScoringStatusValues


enum class ProjectType {
    DEFAULT,
    RESERVED,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ProjectType {
            return try {
                if (value == null) ProjectType.DEFAULT else valueOf(value)
            } catch (e: IllegalArgumentException) {
                ProjectType.DEFAULT
            }
        }
    }
}

@Entity(
    tableName = "projects",
    indices = [
        Index("system_key", unique = true, name = "idx_projects_systemkey_unique")
    ]
)
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    @ColumnInfo(name = "system_key") val systemKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    @ColumnInfo(name = "is_expanded", defaultValue = "1") val isExpanded: Boolean = true,
    @ColumnInfo(name = "goal_order", defaultValue = "0") val order: Long = 0,
    @ColumnInfo(name = "is_attachments_expanded", defaultValue = "0") val isAttachmentsExpanded: Boolean = false,
    @ColumnInfo(name = "default_view_mode") val defaultViewModeName: String? = null,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "is_project_management_enabled") val isProjectManagementEnabled: Boolean? = false,
    @ColumnInfo(name = "project_status") val projectStatus: String? = ProjectStatusValues.NO_PLAN,
    @ColumnInfo(name = "project_status_text") val projectStatusText: String? = null,
    @ColumnInfo(name = "project_log_level") val projectLogLevel: String? = ProjectLogLevelValues.NORMAL,
    @ColumnInfo(name = "total_time_spent_minutes") val totalTimeSpentMinutes: Long? = 0,
    @ColumnInfo(defaultValue = "0.0") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val risk: Float = 0f,
    @ColumnInfo(defaultValue = "1.0") val weightEffort: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") val weightCost: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") val weightRisk: Float = 1f,
    @ColumnInfo(defaultValue = "0.0") val rawScore: Float = 0f,
    @ColumnInfo(defaultValue = "0") val displayScore: Int = 0,
    @ColumnInfo(name = "scoring_status") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @ColumnInfo(name = "show_checkboxes", defaultValue = "0") val showCheckboxes: Boolean = false,
    @ColumnInfo(name = "project_type", defaultValue = "'DEFAULT'") val projectType: ProjectType = ProjectType.DEFAULT,
    @ColumnInfo(name = "reserved_group") val reservedGroup: ReservedGroup? = null,
    @ColumnInfo(name = "role_code") val roleCode: String? = null,
)