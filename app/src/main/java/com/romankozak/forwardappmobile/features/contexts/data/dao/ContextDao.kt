package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.GlobalContextSearchResult
import com.romankozak.forwardappmobile.core.data.models.GlobalSubcontextSearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY goal_order ASC")
    fun getAllContextsForSync(): Flow<List<Context>>

    @Query("SELECT * FROM contexts WHERE is_deleted = 0 ORDER BY goal_order ASC")
    fun getAllContexts(): Flow<List<Context>>

    @Query("SELECT * FROM contexts")
    suspend fun getAll(): List<Context>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContexts(contexts: List<Context>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: Context)

    @Update
    suspend fun update(context: Context)

    @Update
    suspend fun update(contexts: List<Context>): Int

    @Query("DELETE FROM contexts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM contexts WHERE id = :contextId")
    suspend fun deleteContextById(contextId: String)

    @Query("SELECT * FROM contexts WHERE id IN (:contextIds)")
    suspend fun getContextsByIds(contextIds: List<String>): List<Context>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getContextById(id: String): Context?

    @Query("SELECT * FROM contexts WHERE id = :id")
    fun getContextByIdStream(id: String): Flow<Context?>



    @Query("UPDATE contexts SET goal_order = :order WHERE id = :contextId")
    suspend fun updateOrder(
        contextId: String,
        order: Long,
    )

    @Query("SELECT * FROM contexts WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getContextsByParentId(parentId: String): List<Context>

    @Query("SELECT * FROM contexts WHERE parentId = :parentId AND role_code = :roleCode AND is_deleted = 0 LIMIT 1")
    suspend fun findChildByRole(
        parentId: String,
        roleCode: String,
    ): Context?

    @Query("SELECT * FROM contexts WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelContexts(): List<Context>

    @Query("SELECT * FROM contexts WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getContextsByTag(tag: String): List<Context>



    @Query("SELECT id FROM contexts WHERE tags LIKE '%' || :tag || '%' ORDER BY goal_order ASC, createdAt ASC")
    suspend fun getContextIdsByTag(tag: String): List<String>

    @Transaction
    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM contexts WHERE parentId IS NULL
        UNION ALL
        SELECT p.id, p.name, pct.path || ' / ' || p.name
        FROM contexts p JOIN path_cte pct ON p.parentId = pct.id
    )
    SELECT
        subproject.*,
        parent_project.id as parentContextId,
        parent_project.name as parentContextName,
        pc.path as pathSegments
    FROM contexts AS subproject
    INNER JOIN list_items AS li ON subproject.id = li.entityId
    INNER JOIN contexts AS parent_project ON li.context_id = parent_project.id
    INNER JOIN path_cte pc ON subproject.id = pc.id
    WHERE li.itemType = 'SUBLIST' AND subproject.name LIKE :query
    """,
    )
    suspend fun searchSubprojectsGlobal(query: String): List<GlobalSubcontextSearchResult>

    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM contexts WHERE parentId IS NULL
        UNION ALL
        SELECT p.id, p.name, pct.path || ' / ' || p.name
        FROM contexts p JOIN path_cte pct ON p.parentId = pct.id
    )
    SELECT p.*, pc.path as pathSegments
    FROM contexts p
    JOIN path_cte pc ON p.id = pc.id
    WHERE p.name LIKE :query
""",
    )
    suspend fun searchContextsGlobal(query: String): List<GlobalContextSearchResult>

    @Query("DELETE FROM contexts")
    suspend fun deleteAll()

    @Query("UPDATE contexts SET default_view_mode = :viewModeName WHERE id = :contextId")
    suspend fun updateViewMode(
        contextId: String,
        viewModeName: String,
    )

    @Query("SELECT * FROM contexts")
    fun getAllContextsFlow(): Flow<List<Context>>

    @Query("SELECT * FROM contexts")
    suspend fun getAllRaw(): List<Context>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contexts: List<Context>)
}
