/*package com.romankozak.forwardappmobile.features.projects.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.insertOrReplace
import com.romankozak.forwardappmobile.shared.features.projects.data.toModel
import com.romankozak.forwardappmobile.shared.logging.logd
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectLocalDataSource(
    private val database: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) {

    fun observeAll(): Flow<List<Project>> =
        database.projectsQueries
            .getAllProjects()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toModel() } }

    fun observeById(projectId: String): Flow<Project?> =
        database.projectsQueries
            .getProjectById(projectId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { row -> row?.toModel() }

    suspend fun getAll(): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getAllProjectsUnordered()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByIds(ids: List<String>): List<Project> =
        withContext(ioDispatcher) {
            if (ids.isEmpty()) return@withContext emptyList<Project>()
            database.projectsQueries
                .getProjectsByIds(ids)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getById(projectId: String): Project? =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectById(projectId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun upsert(project: Project) {
        withContext(ioDispatcher) {
            database.projectsQueries.insertOrReplace(project)
        }
    }

    suspend fun upsert(projects: List<Project>, useTransaction: Boolean = true) {
        if (projects.isEmpty()) return
        withContext(ioDispatcher) {
            if (useTransaction) {
                database.transaction {
                    projects.forEach { database.projectsQueries.insertOrReplace(it) }
                }
            } else {
                projects.forEach { database.projectsQueries.insertOrReplace(it) }
            }
        }
    }

    suspend fun delete(projectId: String) {
        withContext(ioDispatcher) {
            database.projectsQueries.deleteProject(projectId)
        }
    }

    suspend fun delete(projectIds: List<String>) {
        withContext(ioDispatcher) {
            database.transaction {
                projectIds.forEach { database.projectsQueries.deleteProject(it) }
            }
        }
    }

    suspend fun deleteDefault(projectId: String) {
        withContext(ioDispatcher) {
            database.projectsQueries.deleteProjectById(projectId)
        }
    }

    private fun deleteAllInternal() {
        logd("FullImportFlow", "ProjectLocalDataSource.deleteAllInternal() executing on ${Thread.currentThread().name}")
        database.projectsQueries.deleteProjectsForReset()
    }

    suspend fun deleteAll() {
        withContext<Unit>(ioDispatcher) {
            deleteAllInternal()
            logd("FullImportFlow", "ProjectLocalDataSource.deleteAll() done")
        }
    }

    fun deleteAllWithinTransaction() {
        deleteAllInternal()
        logd("FullImportFlow", "ProjectLocalDataSource.deleteAllWithinTransaction() done")
    }

    suspend fun getByParent(parentId: String): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectsByParentId(parentId)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getTopLevel(): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getTopLevelProjects()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByTag(tag: String): List<Project> =
        withContext(ioDispatcher) {
            val ids =
                database.projectsQueries
                    .getProjectIdsByTag(tag)
                    .executeAsList()
            if (ids.isEmpty()) {
                emptyList()
            } else {
                database.projectsQueries
                    .getProjectsByIds(ids)
                    .executeAsList()
                    .map { it.toModel() }
            }
        }

    suspend fun getIdsByTag(tag: String): List<String> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectIdsByTag(tag)
                .executeAsList()
        }

    suspend fun getByType(projectType: String): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectsByType(projectType)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getByReservedGroup(reservedGroup: String): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectsByReservedGroup(reservedGroup)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun updateOrder(projectId: String, order: Long) {
        withContext(ioDispatcher) {
            database.projectsQueries.updateProjectOrder(projectId, order)
        }
    }

    suspend fun updateDefaultViewMode(projectId: String, viewMode: String) {
        withContext(ioDispatcher) {
            database.projectsQueries.updateProjectViewMode(projectId, viewMode)
        }
    }

    suspend fun getByParentAndReservedGroup(
        parentId: String?,
        reservedGroup: String,
    ): Project? =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectByParentAndReservedGroup(parentId, reservedGroup)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun getByNameLike(query: String): List<Project> =
        withContext(ioDispatcher) {
            database.projectsQueries
                .getProjectsByNameLike(query)
                .executeAsList()
                .map { it.toModel() }
        }
}*/