package com.romankozak.forwardappmobile

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

// --- КОНВЕРТЕР ДЛЯ СПИСКУ ТЕГІВ ---
// Room не знає, як зберігати список рядків, тому ми вказуємо,
// як перетворити його в JSON і назад.
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
}


// --- ОСНОВНІ КЛАСИ ДЛЯ БАЗИ ДАНИХ ---

@Entity(tableName = "goals")
@TypeConverters(Converters::class) // Ця анотація вже тут і обробить List<String>
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String?,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>?,
    // --- ДОДАНО: Поле для пов'язаних списків ---
    val associatedListIds: List<String>?
)
@Entity(tableName = "goal_lists")
data class GoalList(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    @ColumnInfo(name = "is_expanded", defaultValue = "1")
    var isExpanded: Boolean = true
)

@Entity(tableName = "goal_instances", primaryKeys = ["id"])
data class GoalInstance(
    val id: String,
    val goalId: String,
    val listId: String,
    val orderIndex: Int
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
    @Embedded
    val goal: Goal,
    val instanceId: String,
    val orderIndex: Int
)

// --- ДОДАНО: Клас для результатів глобального пошуку ---
data class GlobalSearchResult(
    @Embedded
    val goal: Goal,
    val listId: String,
    val listName: String
)