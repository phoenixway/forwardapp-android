package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.GlobalProjectSearchResult
import com.romankozak.forwardappmobile.data.database.models.GlobalSubprojectSearchResult
import com.romankozak.forwardappmobile.data.database.models.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY goal_order ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Update
    suspend fun update(projects: List<ProjectEntity>): Int

    @Query("DELETE FROM projects WHERE id = :id AND project_type = 'DEFAULT'")
    suspend fun delete(id: String)

    @Query("DELETE FROM projects WHERE id = :projectId AND project_type = 'DEFAULT'")
    suspend fun deleteProjectById(projectId: String)

    @Query("SELECT * FROM projects WHERE id IN (:projectIds)")
    suspend fun getProjectsByIds(projectIds: List<String>): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdStream(id: String): Flow<ProjectEntity?>

    @Query("UPDATE projects SET goal_order = :order WHERE id = :projectId")
    suspend fun updateOrder(
        projectId: String,
        order: Long,
    )

    @Query("SELECT * FROM projects WHERE parentId = :parentId ORDER BY goal_order ASC")
    suspend fun getProjectsByParentId(parentId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE parentId IS NULL ORDER BY goal_order ASC")
    suspend fun getTopLevelProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getProjectsByTag(tag: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE project_type = :projectType")
    suspend fun getProjectsByType(projectType: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE reserved_group = :reservedGroup")
    suspend fun getProjectsByReservedGroup(reservedGroup: String): List<ProjectEntity>

    @Query("SELECT id FROM projects WHERE tags LIKE '%' || :tag || '%'")
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
