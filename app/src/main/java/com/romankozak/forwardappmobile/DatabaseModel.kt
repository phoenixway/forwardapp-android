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
    val associatedListIds: List<String>? = null
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

// --- ВИПРАВЛЕНО: Єдиний, правильний клас для представлення ієрархії ---
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

data class GoalIdListPair(
    val goalId: String,
    @Embedded
    val goalList: GoalList
)