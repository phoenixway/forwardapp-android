// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/database/models/DatabaseModel.kt ---
package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ✨ ДОДАНО: Enum для типів елементів у списку
enum class ListItemType {
    GOAL,
    NOTE,
    LIST_LINK
}

// ✨ ДОДАНО: Enum для типів пов'язаних посилань
enum class LinkType {
    GOAL_LIST,
    NOTE,
    URL,
    OBSIDIAN
}

// ✨ ДОДАНО: Клас для представлення універсального посилання
data class RelatedLink(
    val type: LinkType,
    val target: String, // ID, URL-адреса, або Obsidian URI
    val displayName: String? = null
)

enum class ScoringStatus {
    NOT_ASSESSED,
    IMPOSSIBLE_TO_ASSESS,
    ASSESSED
}

@TypeConverters(Converters::class) // ✨ НЕ ЗАБУДЬТЕ ДОДАТИ ЦЕЙ АНОТАЦІЮ, ЯКЩО CONVERTERS ВИЗНАЧЕНО В ОКРЕМОМУ ФАЙЛІ
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

    // ✨ ДОДАНО: Конвертери для RelatedLink
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
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    // ✨ ЗАМІНА: associatedListIds -> relatedLinks
    val relatedLinks: List<RelatedLink>? = null,

    // ... решта полів Goal без змін ...
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
    val financialCost: Float? = null
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
    val order: Long = 0
)

// ✨ ДОДАНО: Нова сутність для нотаток
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val title: String?,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long?
)

// ✨ ЗАМІНА: GoalInstance замінено на універсальний ListItem
@Entity(tableName = "list_items",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class ListItem(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true)
    val listId: String,
    val itemType: ListItemType,
    val entityId: String,
    @ColumnInfo(name = "item_order")
    val order: Long
)

// ✨ ЗАМІНА: Sealed class для представлення контенту в UI
sealed class ListItemContent {
    abstract val item: ListItem
    data class GoalItem(val goal: Goal, override val item: ListItem) : ListItemContent()
    data class NoteItem(val note: Note, override val item: ListItem) : ListItemContent()
    data class SublistItem(val sublist: GoalList, override val item: ListItem) : ListItemContent()
}

// Сутності ActivityRecord та RecentListEntry залишаються без змін

/*@Entity(tableName = "activity_records")
data class ActivityRecord(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val startTime: Long?,
    val endTime: Long?
)*/

@Entity(
    tableName = "recent_list_entries",
    primaryKeys = ["list_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)


data class GlobalSearchResult(
    @Embedded
    val goal: Goal,
    val listId: String,
    val listName: String
)