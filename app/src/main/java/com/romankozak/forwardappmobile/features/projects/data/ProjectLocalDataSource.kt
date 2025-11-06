package com.romankozak.forwardappmobile.features.projects.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.database.ProjectQueriesQueries
import com.romankozak.forwardappmobile.shared.features.projects.data.insertOrReplace
import com.romankozak.forwardappmobile.shared.features.projects.data.toModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProjectLocalDataSource
@Inject
constructor(
    private val projectQueries: ProjectQueriesQueries,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun observeAll(): Flow<List<Project>> =
        projectQueries
            .getAllProjects()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toModel() } }

    fun observeById(projectId: String): Flow<Project?> =
        projectQueries
            .getProjectById(projectId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { row -> row?.toModel() }

    suspend fun getAll(): List<Project> =
        withContext(ioDispatcher) {
            projectQueries
                .getAllProjectsUnordered()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByIds(ids: List<String>): List<Project> =
        withContext(ioDispatcher) {
            if (ids.isEmpty()) return@withContext emptyList<Project>()
            projectQueries
                .getProjectsByIds(ids)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getById(projectId: String): Project? =
        withContext(ioDispatcher) {
            projectQueries
                .getProjectById(projectId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun upsert(project: Project) {
        withContext(ioDispatcher) {
            projectQueries.insertOrReplace(project)
        }
    }

    suspend fun upsert(projects: List<Project>) {
        if (projects.isEmpty()) return
        withContext(ioDispatcher) {
            projectQueries.transaction {
                projects.forEach { projectQueries.insertOrReplace(it) }
            }
        }
    }

    suspend fun delete(projectId: String) {
        withContext(ioDispatcher) {
            projectQueries.deleteProject(projectId)
        }
    }

    suspend fun deleteDefault(projectId: String) {
        withContext(ioDispatcher) {
            projectQueries.deleteProjectById(projectId)
        }
    }

    suspend fun deleteAll() {
        withContext(ioDispatcher) {
            projectQueries.deleteAllProjects()
        }
    }

    suspend fun getByParent(parentId: String): List<Project> =
        withContext(ioDispatcher) {
            projectQueries
                .getProjectsByParentId(parentId)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getTopLevel(): List<Project> =
        withContext(ioDispatcher) {
            projectQueries
                .getTopLevelProjects()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByTag(tag: String): List<Project> =
        withContext(ioDispatcher) {
            val ids =
                projectQueries
                    .getProjectIdsByTag(tag)
                    .executeAsList()
            if (ids.isEmpty()) {
                emptyList()
            } else {
                projectQueries
                    .getProjectsByIds(ids)
                    .executeAsList()
                    .map { it.toModel() }
            }
        }

    suspend fun getIdsByTag(tag: String): List<String> =
        withContext(ioDispatcher) {
            projectQueries
                .getProjectIdsByTag(tag)
                .executeAsList()
        }

    suspend fun getByType(projectType: String): List<Project> =
        withContext(ioDispatcher) {
            projectQueries
                .getProjectsByType(projectType)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByReservedGroup(reservedGroup: String): List<Project> =
        withContext(ioDispatcher) {
            projectQueries
                .getProjectsByReservedGroup(reservedGroup)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun updateOrder(projectId: String, order: Long) {
        withContext(ioDispatcher) {
            projectQueries.updateProjectOrder(projectId, order)
        }
    }

    suspend fun updateDefaultViewMode(projectId: String, viewMode: String) {
        withContext(ioDispatcher) {
            projectQueries.updateProjectViewMode(projectId, viewMode)
        }
    }

}
