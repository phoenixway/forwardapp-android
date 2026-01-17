package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.features.contexts.data.models.GlobalProjectSearchResult
import com.romankozak.forwardappmobile.features.contexts.data.models.GlobalSubprojectSearchResult
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY goal_order ASC")
    fun getAllProjectsForSync(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE is_deleted = 0 ORDER BY goal_order ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<Project>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project)

    @Update
    suspend fun update(project: Project)

    @Update
    suspend fun update(projects: List<Project>): Int

    @Query("DELETE FROM projects WHERE id = :id AND project_type = 'DEFAULT'")
    suspend fun delete(id: String)

    @Query("DELETE FROM projects WHERE id = :projectId AND project_type = 'DEFAULT'")
    suspend fun deleteProjectById(projectId: String)

    @Query("SELECT * FROM projects WHERE id IN (:projectIds)")
    suspend fun getProjectsByIds(projectIds: List<String>): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdStream(id: String): Flow<Project?>

    @Query("SELECT * FROM projects WHERE system_key = :systemKey")
    suspend fun getProjectBySystemKey(systemKey: String): Project?

    @Query("UPDATE projects SET goal_order = :order WHERE id = :projectId")
    suspend fun updateOrder(
        projectId: String,
        order: Long,
    )

    @Query("SELECT * FROM projects WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getProjectsByParentId(parentId: String): List<Project>

    @Query("SELECT * FROM projects WHERE parentId = :parentId AND role_code = :roleCode AND is_deleted = 0 LIMIT 1")
    suspend fun findChildByRole(
        parentId: String,
        roleCode: String
    ): Project?

    @Query("SELECT * FROM projects WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelProjects(): List<Project>

    @Query("SELECT * FROM projects WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getProjectsByTag(tag: String): List<Project>

    @Query("SELECT * FROM projects WHERE project_type = :projectType")
    suspend fun getProjectsByType(projectType: String): List<Project>

    @Query("SELECT * FROM projects WHERE reserved_group = :reservedGroup")
    suspend fun getProjectsByReservedGroup(reservedGroup: String): List<Project>

    @Query("SELECT id FROM projects WHERE tags LIKE '%' || :tag || '%' ORDER BY goal_order ASC, createdAt ASC")
    suspend fun getProjectIdsByTag(tag: String): List<String>

    @Transaction
    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM projects WHERE parentId IS NULL
        UNION ALL
        SELECT p.id, p.name, pct.path || ' / ' || p.name
        FROM projects p JOIN path_cte pct ON p.parentId = pct.id
    )
    SELECT
        subproject.*,
        parent_project.id as parentProjectId,
        parent_project.name as parentProjectName,
        pc.path as pathSegments
    FROM projects AS subproject
    INNER JOIN list_items AS li ON subproject.id = li.entityId
    INNER JOIN projects AS parent_project ON li.project_id = parent_project.id
    INNER JOIN path_cte pc ON subproject.id = pc.id
    WHERE li.itemType = 'SUBLIST' AND subproject.name LIKE :query
    """,
    )
    suspend fun searchSubprojectsGlobal(query: String): List<GlobalSubprojectSearchResult>

    @Query(
        """
    WITH RECURSIVE path_cte(id, name, path) AS (
        SELECT id, name, name as path FROM projects WHERE parentId IS NULL
        UNION ALL
        SELECT p.id, p.name, pct.path || ' / ' || p.name
        FROM projects p JOIN path_cte pct ON p.parentId = pct.id
    )
    SELECT p.*, pc.path as pathSegments
    FROM projects p
    JOIN path_cte pc ON p.id = pc.id
    WHERE p.name LIKE :query
""",
    )
    suspend fun searchProjectsGlobal(query: String): List<GlobalProjectSearchResult>

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("UPDATE projects SET default_view_mode = :viewModeName WHERE id = :projectId")
    suspend fun updateViewMode(
        projectId: String,
        viewModeName: String,
    )
}
