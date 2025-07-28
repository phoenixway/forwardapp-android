// GoalListDao.kt
package com.romankozak.forwardappmobile

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalListDao {
    @Query("SELECT * FROM goal_lists")
    fun getAllLists(): Flow<List<GoalList>>

    // --- Функції для синхронізації ---
    @Query("SELECT * FROM goal_lists")
    suspend fun getAll(): List<GoalList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<GoalList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
}