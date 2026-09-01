package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import kotlinx.coroutines.flow.Flow

/**
 * Local rebuildable projection cache for hashtag-routed Backlog Goal appearances.
 *
 * This DAO owns no Goal content and no explicit Backlog placement authority.
 */
@Dao
interface BacklogGoalAssociationLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<BacklogGoalAssociationLink>)

    @Query(
        """
        SELECT * FROM backlog_goal_association_links
        WHERE context_id = :contextId
        ORDER BY item_order ASC, projection_id ASC
        """,
    )
    fun observeForContext(contextId: String): Flow<List<BacklogGoalAssociationLink>>

    @Query(
        """
        SELECT * FROM backlog_goal_association_links
        WHERE context_id = :contextId
        ORDER BY item_order ASC, projection_id ASC
        """,
    )
    suspend fun getForContext(contextId: String): List<BacklogGoalAssociationLink>

    @Query(
        """
        SELECT * FROM backlog_goal_association_links
        WHERE goal_id = :goalId
        """,
    )
    suspend fun getLinksForGoal(goalId: String): List<BacklogGoalAssociationLink>

    @Query(
        """
        SELECT * FROM backlog_goal_association_links
        WHERE projection_id IN (:projectionIds)
        """,
    )
    suspend fun getByProjectionIds(
        projectionIds: Collection<String>,
    ): List<BacklogGoalAssociationLink>

    @Query(
        """
        DELETE FROM backlog_goal_association_links
        WHERE goal_id = :goalId
          AND context_id IN (:contextIds)
        """,
    )
    suspend fun deleteForGoalAndContexts(
        goalId: String,
        contextIds: List<String>,
    )

    @Query("DELETE FROM backlog_goal_association_links WHERE goal_id = :goalId")
    suspend fun deleteForGoal(goalId: String)

    @Query("DELETE FROM backlog_goal_association_links")
    suspend fun deleteAll()
}
