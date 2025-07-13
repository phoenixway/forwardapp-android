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

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Змінено для узгодженості
    suspend fun insert(goalList: GoalList)

    @Update
    suspend fun update(goalList: GoalList)

    @Delete // <-- ДОДАНО: Правильний метод для видалення
    suspend fun delete(goalList: GoalList)

    @Query("DELETE FROM goal_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)
}