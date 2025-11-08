package com.romankozak.forwardappmobile.shared.features.projects.data

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import kotlinx.coroutines.flow.Flow

interface ProjectLocalDataSource {

    fun observeAll(): Flow<List<Project>>
    fun observeById(projectId: String): Flow<Project?>

    suspend fun getAll(): List<Project>
    suspend fun getByIds(ids: List<String>): List<Project>
    suspend fun getById(projectId: String): Project?

    suspend fun upsert(project: Project)
    suspend fun upsert(projects: List<Project>, useTransaction: Boolean = true)

    suspend fun delete(projectId: String)
    suspend fun delete(projectIds: List<String>)
    suspend fun deleteDefault(projectId: String)

    suspend fun deleteAll()
    fun deleteAllWithinTransaction()

    suspend fun getByParent(parentId: String): List<Project>
    suspend fun getTopLevel(): List<Project>
    suspend fun getByTag(tag: String): List<Project>
    suspend fun getIdsByTag(tag: String): List<String>
    suspend fun getByType(projectType: String): List<Project>
    suspend fun getByReservedGroup(reservedGroup: String): List<Project>
    suspend fun getByParentAndReservedGroup(parentId: String?, reservedGroup: String): Project?
    suspend fun getByNameLike(query: String): List<Project>

    suspend fun updateOrder(projectId: String, order: Long)
    suspend fun updateDefaultViewMode(projectId: String, viewMode: String)
}

