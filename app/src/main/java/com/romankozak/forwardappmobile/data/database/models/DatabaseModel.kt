package com.romankozak.forwardappmobile.data.database.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

// --- ЛОКАЛЬНИЙ КОНВЕРТЕР (БЕЗ @ProvidedTypeConverter) ---
class PathSegmentsConverter {
    private val pathSeparator = " / "
    @TypeConverter fun fromPathSegments(pathSegments: List<String>?): String? = pathSegments?.joinToString(pathSeparator)
    @TypeConverter fun fromStringToPathSegments(data: String?): List<String>? = data?.split(pathSeparator)?.map { it.trim() }
}

// --- ГЛОБАЛЬНИЙ ОБ'ЄДНАНИЙ КОНВЕРТЕР (З @ProvidedTypeConverter) ---
@ProvidedTypeConverter
class Converters {
    private val gson = Gson()

    // Конвертери для List<String> (для `tags`)
    @TypeConverter fun fromStringList(value: String?): List<String>? = value?.let { gson.fromJson(it, object : TypeToken<List<String>>() {}.type) }
    @TypeConverter fun fromListToString(list: List<String>?): String? = gson.toJson(list)

    // Конвертери для ENUM-ів
    @TypeConverter fun fromScoringStatus(status: ScoringStatus?): String? = status?.name
    @TypeConverter fun toScoringStatus(value: String?): ScoringStatus? = value?.let { ScoringStatus.valueOf(it) }
    @TypeConverter fun fromListItemType(type: ListItemType?): String? = type?.name
    @TypeConverter fun toListItemType(value: String?): ListItemType? = value?.let { ListItemType.valueOf(it) }
    @TypeConverter fun fromProjectStatus(status: ProjectStatus?): String? = status?.name
    @TypeConverter fun toProjectStatus(value: String?): ProjectStatus? = value?.let { ProjectStatus.valueOf(it) }
    @TypeConverter fun fromProjectLogLevel(level: ProjectLogLevel?): String? = level?.name
    @TypeConverter fun toProjectLogLevel(value: String?): ProjectLogLevel? = value?.let { ProjectLogLevel.valueOf(it) }
    @TypeConverter fun fromProjectLogEntryType(type: ProjectLogEntryType?): String? = type?.name
    @TypeConverter fun toProjectLogEntryType(value: String?): ProjectLogEntryType? = value?.let { ProjectLogEntryType.valueOf(it) }

    // Конвертери для DayManagementEntities
    @TypeConverter fun fromDayStatus(status: DayStatus?): String? = status?.name
    @TypeConverter fun toDayStatus(value: String?): DayStatus? = value?.let { DayStatus.valueOf(it) }
    @TypeConverter fun fromTaskPriority(priority: TaskPriority?): String? = priority?.name
    @TypeConverter fun toTaskPriority(value: String?): TaskPriority? = value?.let { TaskPriority.valueOf(it) }
    @TypeConverter fun fromTaskStatus(status: TaskStatus?): String? = status?.name
    @TypeConverter fun toTaskStatus(value: String?): TaskStatus? = value?.let { TaskStatus.valueOf(it) }

    // Конвертери для складних типів (Gson)
    private val relatedLinkListType = object : TypeToken<List<RelatedLink>>() {}.type
    @TypeConverter fun fromRelatedLinkList(links: List<RelatedLink>?): String? = gson.toJson(links, relatedLinkListType)
    @TypeConverter fun toRelatedLinkList(json: String?): List<RelatedLink>? = json?.let { gson.fromJson(it, relatedLinkListType) }
    @TypeConverter fun fromRelatedLink(link: RelatedLink?): String? = gson.toJson(link)
    @TypeConverter fun toRelatedLink(json: String?): RelatedLink? = json?.let { gson.fromJson(it, RelatedLink::class.java) }
    @TypeConverter fun fromCustomMetrics(metrics: Map<String, Float>?): String? = gson.toJson(metrics)
    @TypeConverter fun toCustomMetrics(json: String?): Map<String, Float>? = json?.let { gson.fromJson(it, object : TypeToken<Map<String, Float>>() {}.type) }
}

// --- ENUMS ---
enum class ProjectViewMode { BACKLOG, INBOX, DASHBOARD }
enum class ListItemType { GOAL, SUBLIST, LINK_ITEM }
enum class LinkType { GOAL_LIST, URL, OBSIDIAN }
enum class ScoringStatus { NOT_ASSESSED, IMPOSSIBLE_TO_ASSESS, ASSESSED }
enum class ProjectLogLevel { DETAILED, NORMAL }
enum class ProjectLogEntryType { STATUS_CHANGE, COMMENT, AUTOMATIC, INSIGHT, MILESTONE }
enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }
enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL, NONE }
enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED }
enum class ProjectStatus(val displayName: String) { NO_PLAN("Без плану"), PLANNING("Планується"), IN_PROGRESS("В реалізації"), COMPLETED("Завершено"), ON_HOLD("Відкладено"), PAUSED("На паузі") }

// --- DATA CLASSES & ENTITIES ---
data class RelatedLink(val type: LinkType, val target: String, val displayName: String? = null)

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

@Entity(tableName = "goal_lists")
data class GoalList(
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

@Entity(tableName = "project_execution_logs", foreignKeys = [ForeignKey(entity = GoalList::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)])
data class ProjectExecutionLog(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val timestamp: Long,
    @ColumnInfo(name = "type") val type: ProjectLogEntryType,
    val description: String,
    val details: String? = null,
)

@Entity(tableName = "inbox_records", foreignKeys = [ForeignKey(entity = GoalList::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)])
data class InboxRecord(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val text: String,
    val createdAt: Long,
    @ColumnInfo(name = "item_order") val order: Long,
)

@Entity(tableName = "list_items", foreignKeys = [ForeignKey(entity = GoalList::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.CASCADE)])
data class ListItem(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val listId: String,
    val itemType: ListItemType,
    val entityId: String,
    @ColumnInfo(name = "item_order") val order: Long,
)

@Entity(tableName = "activity_records_fts")
@Fts4(contentEntity = ActivityRecord::class)
data class ActivityRecordFts(
    val text: String,
)

@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val name: String? = null,
    val status: DayStatus = DayStatus.PLANNED,
    val reflection: String? = null,
    val energyLevel: Int? = null,
    val mood: String? = null,
    val weatherConditions: String? = null,
    val totalPlannedMinutes: Long = 0,
    val totalCompletedMinutes: Long = 0,
    val completionPercentage: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
)

@Entity(
    tableName = "day_tasks",
    foreignKeys = [
        ForeignKey(entity = DayPlan::class, parentColumns = ["id"], childColumns = ["dayPlanId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = GoalList::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ActivityRecord::class, parentColumns = ["id"], childColumns = ["activityRecordId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("dayPlanId"), Index("goalId"), Index("projectId"), Index("activityRecordId"), Index("scheduledTime")]
)
data class DayTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val title: String,
    val description: String? = null,
    val goalId: String? = null,
    val projectId: String? = null,
    val activityRecordId: String? = null,
    val taskType: ListItemType? = null,
    val entityId: String? = null,
    val order: Long = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val completed: Boolean = false,
    val scheduledTime: Long? = null,
    val estimatedDurationMinutes: Long? = null,
    val actualDurationMinutes: Long? = null,
    val reminderTime: Long? = null,
    val dueTime: Long? = null,
    @ColumnInfo(defaultValue = "0.0") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val risk: Float = 0f,
    val location: String? = null,
    val tags: List<String>? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val completedAt: Long? = null
)

@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val date: Long,
    val tasksPlanned: Int = 0,
    val tasksCompleted: Int = 0,
    val completionRate: Float = 0f,
    val totalPlannedTime: Long = 0,
    val totalActiveTime: Long = 0,
    val totalBreakTime: Long = 0,
    val morningEnergyLevel: Int? = null,
    val eveningEnergyLevel: Int? = null,
    val overallMood: String? = null,
    val stressLevel: Int? = null,
    val customMetrics: Map<String, Float>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
)

// --- SEARCH RESULT AND UI MODELS ---

sealed class ListItemContent {
    abstract val item: ListItem
    data class GoalItem(val goal: Goal, override val item: ListItem) : ListItemContent()
    data class SublistItem(val sublist: GoalList, override val item: ListItem) : ListItemContent()
    data class LinkItem(val link: LinkItemEntity, override val item: ListItem) : ListItemContent()
}

data class GlobalSearchResult(
    @Embedded
    val goal: Goal,
    val listId: String,
    val listName: String,
    // Локально застосовуємо специфічний конвертер
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>
)

data class GlobalLinkSearchResult(
    @Embedded
    val link: LinkItemEntity,
    val listId: String,
    val listName: String,
    val listItemId: String,
)

data class GlobalSublistSearchResult(
    @Embedded
    val sublist: GoalList,
    val parentListId: String,
    val parentListName: String,
)

sealed class GlobalSearchResultItem {
    abstract val timestamp: Long
    abstract val uniqueId: String

    data class GoalItem(val searchResult: GlobalSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.goal.updatedAt ?: searchResult.goal.createdAt
        override val uniqueId: String get() = "goal_${searchResult.goal.id}_${searchResult.listId}"
    }

    data class LinkItem(val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.listId}"
    }

    data class SublistItem(val searchResult: GlobalSublistSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.sublist.updatedAt ?: searchResult.sublist.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.sublist.id}_${searchResult.parentListId}"
    }

    data class ListItem(val list: GoalList) : GlobalSearchResultItem() {
        override val timestamp: Long get() = list.updatedAt ?: list.createdAt
        override val uniqueId: String get() = "list_${list.id}"
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