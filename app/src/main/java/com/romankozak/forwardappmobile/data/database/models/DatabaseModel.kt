package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ✨ ДОДАНО: Enum для статусу оцінки
 */
enum class ScoringStatus {
    NOT_ASSESSED,
    IMPOSSIBLE_TO_ASSESS,
    ASSESSED
}

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null) {
            return null
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        return Gson().toJson(list)
    }

    /**
     * ✨ ДОДАНО: Конвертер для ScoringStatus
     */
    @TypeConverter
    fun fromScoringStatus(status: ScoringStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toScoringStatus(value: String?): ScoringStatus? {
        return value?.let { ScoringStatus.valueOf(it) }
    }
}


@Entity(tableName = "goals")
@TypeConverters(Converters::class)
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val associatedListIds: List<String>? = null,

    // --- Поля для нової Системи Б ---
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

    // --- Вагові коефіцієнти ---
    @ColumnInfo(defaultValue = "1.0")
    val weightEffort: Float = 1f,

    @ColumnInfo(defaultValue = "1.0")
    val weightCost: Float = 1f,

    @ColumnInfo(defaultValue = "1.0")
    val weightRisk: Float = 1f,

    // --- Розраховані поля для зберігання ---
    @ColumnInfo(defaultValue = "0.0")
    val rawScore: Float = 0f,

    @ColumnInfo(defaultValue = "0")
    val displayScore: Int = 0,

    // ✨ ДОДАНО: Нове поле для статусу оцінки
    @ColumnInfo(name = "scoring_status", defaultValue = "'NOT_ASSESSED'")
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,

    // --- Старі поля, які будуть використовуватися для міграції, а потім можуть бути видалені ---
    // Важливо, щоб їхні назви збігалися з тими, що були у вашій БД до міграції.
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
    val order: Long = 0,
)

@Entity(tableName = "goal_instances")
data class GoalInstance(
    @PrimaryKey
    @ColumnInfo(name = "instance_id")
    val instanceId: String,

    val goalId: String,
    val listId: String,

    @ColumnInfo(name = "goal_order")
    val order: Long
)

data class GoalListWithGoals(
    @Embedded val goalList: GoalList,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GoalInstance::class,
            parentColumn = "listId",
            entityColumn = "goalId"
        )
    )
    val goals: List<Goal>
)

data class GoalWithInstanceInfo(
    @Embedded val goal: Goal,
    @ColumnInfo(name = "instance_id") val instanceId: String,
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "goal_order") val order: Long
)

fun GoalWithInstanceInfo.toGoalInstance(): GoalInstance {
    return GoalInstance(
        instanceId = this.instanceId,
        goalId = this.goal.id,
        listId = this.listId,
        order = this.order
    )
}

/**
 * Представляє повну ієрархію списків цілей для зручного доступу в UI.
 */
data class ListHierarchyData(
    val allLists: List<GoalList> = emptyList(),
    val topLevelLists: List<GoalList> = emptyList(),
    val childMap: Map<String, List<GoalList>> = emptyMap()
)

data class GlobalSearchResult(
    @Embedded
    val goal: Goal,
    val listId: String,
    val listName: String
)