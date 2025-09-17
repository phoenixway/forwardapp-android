package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.GlobalListSearchResult
import com.romankozak.forwardappmobile.data.database.models.GlobalSublistSearchResult
import com.romankozak.forwardappmobile.data.database.models.GoalList
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalListDao {
    @Query("SELECT * FROM goal_lists ORDER BY goal_order ASC")
    fun getAllLists(): Flow<List<GoalList>>

    @Query("SELECT * FROM goal_lists")
    suspend fun getAll(): List<GoalList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<GoalList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goalList: GoalList)

    @Update
    suspend fun update(goalList: GoalList)

    @Update
    suspend fun update(lists: List<GoalList>): Int

    @Delete
    suspend fun delete(goalList: GoalList)

    @Query("DELETE FROM goal_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)

    @Query("SELECT * FROM goal_lists WHERE id IN (:listIds)")
    suspend fun getListsByIds(listIds: List<String>): List<GoalList>

    @Query("SELECT * FROM goal_lists WHERE id = :id")
    suspend fun getGoalListById(id: String): GoalList?

    @Query("SELECT * FROM goal_lists WHERE id = :id")
    fun getGoalListByIdStream(id: String): Flow<GoalList?>

    @Query("UPDATE goal_lists SET goal_order = :order WHERE id = :listId")
    suspend fun updateOrder(
        listId: String,
        order: Long,
    )

    @Query("SELECT * FROM goal_lists WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getListsByParentId(parentId: String): List<GoalList>

    @Query("SELECT * FROM goal_lists WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelLists(): List<GoalList>

    @Query("SELECT * FROM goal_lists WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getListsByTag(tag: String): List<GoalList>

    @Query("SELECT id FROM goal_lists WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getListIdsByTag(tag: String): List<String>

    @Transaction
    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM goal_lists WHERE parentId IS NULL
        UNION ALL
        SELECT gl.id, gl.name, p.path || ' / ' || gl.name
        FROM goal_lists gl JOIN path_cte p ON gl.parentId = p.id
    )
    SELECT
        sublist.*,
        parent_list.id as parentListId,
        parent_list.name as parentListName,
        pc.path as pathSegments
    FROM goal_lists AS sublist
    INNER JOIN list_items AS li ON sublist.id = li.entityId
    INNER JOIN goal_lists AS parent_list ON li.listId = parent_list.id
    INNER JOIN path_cte pc ON sublist.id = pc.id
    WHERE li.itemType = 'SUBLIST' AND sublist.name LIKE :query
    """
    )
    suspend fun searchSublistsGlobal(query: String): List<GlobalSublistSearchResult>


    @Query("""
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM goal_lists WHERE parentId IS NULL
        UNION ALL
        SELECT gl.id, gl.name, p.path || ' / ' || gl.name
        FROM goal_lists gl JOIN path_cte p ON gl.parentId = p.id
    )
    SELECT gl.*, pc.path as pathSegments
    FROM goal_lists gl
    JOIN path_cte pc ON gl.id = pc.id
    WHERE gl.name LIKE :query AND gl.parentId IS NOT NULL 
""")
    suspend fun searchListsGlobal(query: String): List<GlobalListSearchResult>

    @Query("DELETE FROM goal_lists")
    suspend fun deleteAll()

    @Query("UPDATE goal_lists SET default_view_mode = :viewModeName WHERE id = :listId")
    suspend fun updateViewMode(listId: String, viewModeName: String)


}
