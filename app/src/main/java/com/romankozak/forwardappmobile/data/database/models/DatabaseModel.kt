package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
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
        val objectType = object : TypeToken<RelatedLink>() {}.type
        return gson.fromJson(value, objectType)
    }
}

object ProjectStatusValues {
    const val NO_PLAN = "NO_PLAN"
    const val PLANNING = "PLANNING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val PAUSED = "PAUSED"
    
    fun getDisplayName(status: String?): String {
        return when (status) {
            NO_PLAN -> "Без плану"
            PLANNING -> "Планується"
            IN_PROGRESS -> "В реалізації"
            COMPLETED -> "Завершено"
            ON_HOLD -> "Відкладено"
            PAUSED -> "На паузі"
            else -> "Без плану"
        }
    }
}

object ScoringStatusValues {
    const val NOT_ASSESSED = "NOT_ASSESSED"
    const val IMPOSSIBLE_TO_ASSESS = "IMPOSSIBLE_TO_ASSESS"
    const val ASSESSED = "ASSESSED"
}

object ProjectLogLevelValues {
    const val DETAILED = "DETAILED"
    const val NORMAL = "NORMAL"
}

object ProjectLogEntryTypeValues {
    const val STATUS_CHANGE = "STATUS_CHANGE"
    const val COMMENT = "COMMENT"
    const val AUTOMATIC = "AUTOMATIC"
    const val INSIGHT = "INSIGHT"
    const val MILESTONE = "MILESTONE"
}



object ListItemTypeValues {
    const val GOAL = "GOAL"
    const val SUBLIST = "SUBLIST"
    const val LINK_ITEM = "LINK_ITEM"
    const val NOTE = "NOTE"
    const val NOTE_DOCUMENT = "NOTE_DOCUMENT"
    const val CHECKLIST = "CHECKLIST"
    const val SCRIPT = "SCRIPT"
}

@Entity(
    tableName = "backlog_orders",
    indices = [
        Index("list_id"),
        Index(value = ["list_id", "item_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BacklogOrder(
    @PrimaryKey val id: String,
    @SerializedName("listId")
    @ColumnInfo(name = "list_id")
    val listId: String,
    @SerializedName("itemId")
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "item_order") val order: Long,
    @ColumnInfo(name = "order_version", defaultValue = "0") val orderVersion: Long = 0,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
)



enum class ProjectViewMode { BACKLOG, INBOX, ADVANCED, ATTACHMENTS }

enum class LinkType { PROJECT, URL, OBSIDIAN }

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
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
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
    @ColumnInfo(name = "scoring_status") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @ColumnInfo(defaultValue = "0.0") val parentValueImportance: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val impactOnParentGoal: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val timeCost: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val financialCost: Float? = null,
)



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

class ProjectTypeConverter {
    @TypeConverter
    fun fromProjectType(projectType: ProjectType?): String {
        return (projectType ?: ProjectType.DEFAULT).name
    }

    @TypeConverter
    fun toProjectType(value: String?): ProjectType {
        return ProjectType.fromString(value)
    }
}

class ReservedGroupConverter {
    @TypeConverter
    fun fromReservedGroup(reservedGroup: ReservedGroup?): String? {
        return reservedGroup?.groupName
    }

    @TypeConverter
    fun toReservedGroup(groupName: String?): ReservedGroup? {
        return ReservedGroup.fromString(groupName)
    }
}

@Entity(
    tableName = "projects",
    indices = [
        Index("system_key", unique = true, name = "idx_projects_systemkey_unique")
    ]
)
@TypeConverters(ProjectTypeConverter::class, ReservedGroupConverter::class)
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
    @ColumnInfo(name = "reserved_group") val reservedGroup: ReservedGroup? = null
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
    @ColumnInfo(name = "type") val type: String,
    val description: String,
    val details: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
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
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
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
    val itemType: String,
    val entityId: String,
    @ColumnInfo(name = "item_order") val order: Long,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
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

    data class GoalItem(
        val goal: Goal,
        val listItem: ListItem,
        val projectName: String,
        val pathSegments: List<String>
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = goal.updatedAt ?: goal.createdAt
        override val uniqueId: String get() = "goal_${goal.id}_${listItem.projectId}"
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
