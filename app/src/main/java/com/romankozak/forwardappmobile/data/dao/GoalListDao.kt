package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.GoalList
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalListDao {
    @Query("SELECT * FROM goal_lists ORDER BY goal_order ASC")
    fun getAllLists(): Flow<List<GoalList>>

    // --- Функції для синхронізації ---
    @Query("SELECT * FROM goal_lists")
    suspend fun getAll(): List<GoalList>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertLists(lists: List<GoalList>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(goalList: GoalList)

    @Update
    suspend fun update(goalList: GoalList)

    @Delete
    suspend fun delete(goalList: GoalList)

    @Query("DELETE FROM goal_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)

    @Query("SELECT * FROM goal_lists WHERE id IN (:listIds)")
    suspend fun getListsByIds(listIds: List<String>): List<GoalList>

    // --- ДОДАНО ЦІ ДВА МЕТОДИ ---

    // Метод для одноразового отримання (використовується в init блоці ViewModel)
    @Query("SELECT * FROM goal_lists WHERE id = :id")
    suspend fun getGoalListById(id: String): GoalList?

    // Метод, що повертає Flow (для combine оператора)
    @Query("SELECT * FROM goal_lists WHERE id = :id")
    fun getGoalListByIdStream(id: String): Flow<GoalList?>

    @Query("UPDATE goal_lists SET goal_order = :order WHERE id = :listId")
    suspend fun updateOrder(listId: String, order: Long)


    @Query("SELECT * FROM goal_lists WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getListsByParentId(parentId: String): List<GoalList>

    @Query("SELECT * FROM goal_lists WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelLists(): List<GoalList>

    @Query("SELECT * FROM goal_lists WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getListsByTag(tag: String): List<GoalList>
}