package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken



class PathSegmentsConverter {
    private val pathSeparator = " / "

    @TypeConverter
    fun fromPathSegments(pathSegments: List<String>?): String? {
        return pathSegments?.joinToString(pathSeparator)
    }

    @TypeConverter
    fun fromStringToPathSegments(data: String?): List<String>? {
        return data?.split(pathSeparator)?.map { it.trim() }
    }
}



@TypeConverters(Converters::class)
class Converters {
    private val gson = Gson()
    private val pathSeparator = " / "

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(pathSeparator)?.map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(pathSeparator)
    }

    @TypeConverter
    fun fromRelatedLinkList(value: List<RelatedLink>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRelatedLinkList(value: String?): List<RelatedLink>? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val listType = object : TypeToken<List<RelatedLink>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromRelatedLink(value: RelatedLink?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRelatedLink(value: String?): RelatedLink? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val type = object : TypeToken<RelatedLink>() {}.type
        return gson.fromJson(value, type)
    }
}



enum class ProjectViewMode { BACKLOG, INBOX, DASHBOARD }

enum class ListItemType { GOAL, SUBLIST, LINK_ITEM, NOTE, CUSTOM_LIST }

enum class LinkType { PROJECT, URL, OBSIDIAN }

enum class ScoringStatus { NOT_ASSESSED, IMPOSSIBLE_TO_ASSESS, ASSESSED }

enum class ProjectLogLevel { DETAILED, NORMAL }

enum class ProjectLogEntryType { STATUS_CHANGE, COMMENT, AUTOMATIC, INSIGHT, MILESTONE }

enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }

enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL, NONE;

    fun getDisplayName(): String {
        return when (this) {
            LOW -> "Низький"
            MEDIUM -> "Середній"
            HIGH -> "Високий"
            CRITICAL -> "Критичний"
            NONE -> "Немає"
        }
    }
}

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED }

enum class ProjectStatus(val displayName: String) {
    NO_PLAN("Без плану"),
    PLANNING("Планується"),
    IN_PROGRESS("В реалізації"),
    COMPLETED("Завершено"),
    ON_HOLD("Відкладено"),
    PAUSED("На паузі"),
}



data class RelatedLink(
    val type: LinkType?,
    val target: String,
    val displayName: String? = null,
)

@Entity(tableName = "link_items")
data class LinkItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "link_data")
    val linkData: RelatedLink,
    val createdAt: Long,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
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
    @ColumnInfo(name = "scoring_status", defaultValue = "'NOT_ASSESSED'") val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
    @ColumnInfo(defaultValue = "0.0") val parentValueImportance: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val impactOnParentGoal: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val timeCost: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val financialCost: Float? = null,
    @ColumnInfo(name = "reminder_time") val reminderTime: Long? = null,
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    @ColumnInfo(name = "is_expanded", defaultValue = "1") val isExpanded: Boolean = true,
    @ColumnInfo(name = "goal_order", defaultValue = "0") val order: Long = 0,
    @ColumnInfo(name = "is_attachments_expanded", defaultValue = "0") val isAttachmentsExpanded: Boolean = false,
    @ColumnInfo(name = "default_view_mode") val defaultViewModeName: String? = ProjectViewMode.BACKLOG.name,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "is_project_management_enabled") val isProjectManagementEnabled: Boolean? = false,
    @ColumnInfo(name = "project_status") val projectStatus: ProjectStatus? = ProjectStatus.NO_PLAN,
    @ColumnInfo(name = "project_status_text") val projectStatusText: String? = null,
    @ColumnInfo(name = "project_log_level") val projectLogLevel: ProjectLogLevel? = ProjectLogLevel.NORMAL,
    @ColumnInfo(name = "total_time_spent_minutes") val totalTimeSpentMinutes: Long? = 0,
    @ColumnInfo(name = "reminder_time") val reminderTime: Long? = null,
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
    @ColumnInfo(name = "scoring_status", defaultValue = "'NOT_ASSESSED'") val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
)

@Entity(
    tableName = "project_execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProjectExecutionLog(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val timestamp: Long,
    @ColumnInfo(name = "type") val type: ProjectLogEntryType,
    val description: String,
    val details: String? = null,
)

@Entity(
    tableName = "inbox_records",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InboxRecord(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val text: String,
    val createdAt: Long,
    @ColumnInfo(name = "item_order") val order: Long,
)

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ListItem(
    @PrimaryKey val id: String,
    @SerializedName(value = "projectId", alternate = ["listId"])
    @ColumnInfo(name = "project_id", index = true)
    val projectId: String,
    val itemType: ListItemType,
    val entityId: String,
    @ColumnInfo(name = "item_order") val order: Long,
)

@Fts4(contentEntity = Goal::class)
@Entity(tableName = "goals_fts")
data class GoalFts(
    val text: String,
    val description: String?,
)

@Fts4(contentEntity = Project::class)
@Entity(tableName = "projects_fts")
data class ProjectFts(
    val name: String,
    val description: String?,
)

data class GlobalGoalSearchResult(
    @Embedded
    val goal: Goal,
    val projectId: String,
    val projectName: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalLinkSearchResult(
    @Embedded
    val link: LinkItemEntity,
    val projectId: String,
    val projectName: String,
    val listItemId: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalSubprojectSearchResult(
    @Embedded
    val subproject: Project,
    val parentProjectId: String,
    val parentProjectName: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalProjectSearchResult(
    @Embedded
    val project: Project,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)



sealed class GlobalSearchResultItem {
    abstract val timestamp: Long
    abstract val uniqueId: String

    data class GoalItem(val searchResult: GlobalGoalSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.goal.updatedAt ?: searchResult.goal.createdAt
        override val uniqueId: String get() = "goal_${searchResult.goal.id}_${searchResult.projectId}"
    }

    data class LinkItem(val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.projectId}"
    }

    data class SublistItem(val searchResult: GlobalSubprojectSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.subproject.updatedAt ?: searchResult.subproject.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.subproject.id}_${searchResult.parentProjectId}"
    }

    data class ProjectItem(
        val searchResult: GlobalProjectSearchResult,
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.project.updatedAt ?: searchResult.project.createdAt
        override val uniqueId: String get() = "project_${searchResult.project.id}"
    }

    data class ActivityItem(val record: ActivityRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.startTime ?: record.createdAt
        override val uniqueId: String get() = "activity_${record.id}"
    }

    data class InboxItem(val record: InboxRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.createdAt
        override val uniqueId: String get() = "inbox_${record.id}"
    }
}
