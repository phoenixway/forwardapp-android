// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/database/models/DatabaseModel.kt ---
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
import com.google.gson.reflect.TypeToken

// --- ПОЧАТОК ЗМІНИ: Додано enum для режиму перегляду ---
enum class ProjectViewMode {
    BACKLOG, INBOX, ADDONS
}
// --- КІНЕЦЬ ЗМІНИ ---

enum class ListItemType {
    GOAL,
    // NOTE, // Видалено
    SUBLIST,
    LINK_ITEM
}

enum class LinkType {
    GOAL_LIST,
    // NOTE, // Видалено
    URL,
    OBSIDIAN
}

data class RelatedLink(
    val type: LinkType,
    val target: String,
    val displayName: String? = null,
)

enum class ScoringStatus {
    NOT_ASSESSED,
    IMPOSSIBLE_TO_ASSESS,
    ASSESSED
}

@TypeConverters(Converters::class)
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromScoringStatus(status: ScoringStatus?): String? = status?.name

    @TypeConverter
    fun toScoringStatus(value: String?): ScoringStatus? = value?.let { ScoringStatus.valueOf(it) }

    @TypeConverter
    fun fromListItemType(type: ListItemType?): String? = type?.name

    @TypeConverter
    fun toListItemType(value: String?): ListItemType? = value?.let { ListItemType.valueOf(it) }

    private val relatedLinkListType = object : TypeToken<List<RelatedLink>>() {}.type

    @TypeConverter
    fun fromRelatedLinkList(links: List<RelatedLink>?): String? {
        if (links == null) return null
        return gson.toJson(links, relatedLinkListType)
    }

    @TypeConverter
    fun toRelatedLinkList(json: String?): List<RelatedLink>? {
        if (json == null) return null
        return gson.fromJson(json, relatedLinkListType)
    }

    @TypeConverter
    fun fromRelatedLink(link: RelatedLink?): String? {
        if (link == null) return null
        return gson.toJson(link, RelatedLink::class.java)
    }

    @TypeConverter
    fun toRelatedLink(json: String?): RelatedLink? {
        if (json == null) return null
        return gson.fromJson(json, RelatedLink::class.java)
    }
}

@Entity(tableName = "link_items")
data class LinkItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "link_data")
    val linkData: RelatedLink,
    // ВИПРАВЛЕНО: Додано поле createdAt для можливості сортування
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
    @ColumnInfo(defaultValue = "0.0")
    val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0")
    val risk: Float = 0f,
    @ColumnInfo(defaultValue = "1.0")
    val weightEffort: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val weightCost: Float = 1f,
    @ColumnInfo(defaultValue = "1.0")
    val weightRisk: Float = 1f,
    @ColumnInfo(defaultValue = "0.0")
    val rawScore: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val displayScore: Int = 0,
    @ColumnInfo(name = "scoring_status", defaultValue = "'NOT_ASSESSED'")
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
    @ColumnInfo(defaultValue = "0.0")
    val parentValueImportance: Float? = null,
    @ColumnInfo(defaultValue = "0.0")
    val impactOnParentGoal: Float? = null,
    @ColumnInfo(defaultValue = "0.0")
    val timeCost: Float? = null,
    @ColumnInfo(defaultValue = "0.0")
    val financialCost: Float? = null,
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,
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
    @ColumnInfo(name = "is_expanded", defaultValue = "1")
    val isExpanded: Boolean = true,
    @ColumnInfo(name = "goal_order", defaultValue = "0")
    val order: Long = 0,
    @ColumnInfo(name = "is_attachments_expanded", defaultValue = "0")
    val isAttachmentsExpanded: Boolean = false,
    @ColumnInfo(name = "default_view_mode", defaultValue = "'BACKLOG'")
    val defaultViewModeName: String = ProjectViewMode.BACKLOG.name,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
)

@Entity(
    tableName = "inbox_records",
    foreignKeys = [
        ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InboxRecord(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true)
    val projectId: String, // Зв'язок з GoalList (проєктом)
    val text: String,
    val createdAt: Long,
    @ColumnInfo(name = "item_order")
    val order: Long,
)

// --- ПОЧАТОК ВИПРАВЛЕННЯ: Анотація @Entity переміщена до свого класу ListItem ---
@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ListItem(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true)
    val listId: String,
    val itemType: ListItemType,
    val entityId: String,
    @ColumnInfo(name = "item_order")
    val order: Long,
)
// --- КІНЕЦЬ ВИПРАВЛЕННЯ ---

@Entity(tableName = "activity_records_fts")
@Fts4(contentEntity = ActivityRecord::class)
data class ActivityRecordFts(
    // Поля, за якими буде здійснюватися пошук
    val text: String,
)

sealed class ListItemContent {
    abstract val item: ListItem
    data class GoalItem(val goal: Goal, override val item: ListItem) : ListItemContent()
    // NoteItem видалено
    data class SublistItem(val sublist: GoalList, override val item: ListItem) : ListItemContent()
    data class LinkItem(val link: LinkItemEntity, override val item: ListItem) : ListItemContent()
}

data class GlobalSearchResult(
    @Embedded
    val goal: Goal,
    val listId: String,
    val listName: String,
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
    /**
     * Спільна властивість для сортування всіх результатів за часом (новіші спочатку).
     */
    abstract val timestamp: Long

    /**
     * Унікальний ідентифікатор для кожного елемента, необхідний для стабільної роботи UI (LazyColumn keys).
     */
    abstract val uniqueId: String

    data class GoalItem(val searchResult: GlobalSearchResult) : GlobalSearchResultItem() {
        // ВИПРАВЛЕНО: Використовуємо createdAt, якщо updatedAt == null
        override val timestamp: Long get() = searchResult.goal.updatedAt ?: searchResult.goal.createdAt
        override val uniqueId: String get() = "goal_${searchResult.goal.id}_${searchResult.listId}"
    }

    data class LinkItem(val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        // ВИПРАВЛЕНО: Тепер поле існує в LinkItemEntity
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.listId}"
    }

    data class SublistItem(val searchResult: GlobalSublistSearchResult) : GlobalSearchResultItem() {
        // ВИПРАВЛЕНО: Використовуємо createdAt, якщо updatedAt == null
        override val timestamp: Long get() = searchResult.sublist.updatedAt ?: searchResult.sublist.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.sublist.id}_${searchResult.parentListId}"
    }

    data class ListItem(val list: GoalList) : GlobalSearchResultItem() {
        // ВИПРАВЛЕНО: Використовуємо createdAt, якщо updatedAt == null
        override val timestamp: Long get() = list.updatedAt ?: list.createdAt
        override val uniqueId: String get() = "list_${list.id}"
    }

    data class ActivityItem(val record: ActivityRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.createdAt
        override val uniqueId: String get() = "activity_${record.id}"
    }
}