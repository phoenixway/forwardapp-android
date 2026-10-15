package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY goal_order ASC")
    fun getAllContextsForSync(): Flow<List<Context>>

    @Query("SELECT * FROM contexts WHERE is_deleted = 0 ORDER BY goal_order ASC, id ASC")
    fun getAllContexts(): Flow<List<Context>>

    @Query("SELECT * FROM contexts WHERE is_deleted = 0 ORDER BY goal_order ASC")
    suspend fun getAllActiveOrdered(): List<Context>

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

    @Query(
        """
        UPDATE contexts SET
            valueImportance = :importance,
            valueImpact = :impact,
            effort = :effort,
            cost = :cost,
            risk = :risk,
            weightEffort = :effortWeight,
            weightCost = :costWeight,
            weightRisk = :riskWeight,
            rawScore = :rawScore,
            displayScore = :displayScore,
            scoring_status = :scoringStatus
        WHERE id = :contextId
        """,
    )
    suspend fun projectCanonicalScoring(
        contextId: String,
        importance: Float,
        impact: Float,
        effort: Float,
        cost: Float,
        risk: Float,
        effortWeight: Float,
        costWeight: Float,
        riskWeight: Float,
        rawScore: Float,
        displayScore: Int,
        scoringStatus: String,
    )

    @Query("UPDATE contexts SET goal_order = :order WHERE id = :contextId")
    suspend fun updateOrder(
        contextId: String,
        order: Long,
    )

    @Query("SELECT * FROM contexts WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getContextsByParentId(parentId: String): List<Context>

    @Query("SELECT * FROM contexts WHERE parentId = :parentId AND is_deleted = 0 ORDER BY goal_order ASC")
    suspend fun getActiveContextsByParentId(parentId: String): List<Context>

    @Query("SELECT * FROM contexts WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelContexts(): List<Context>

    @Query("DELETE FROM contexts")
    suspend fun deleteAll()

    @Query("SELECT * FROM contexts")
    fun getAllContextsFlow(): Flow<List<Context>>

    @Query("SELECT * FROM contexts")
    suspend fun getAllRaw(): List<Context>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contexts: List<Context>)

}
