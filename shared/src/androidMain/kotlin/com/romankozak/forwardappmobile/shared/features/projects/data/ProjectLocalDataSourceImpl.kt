package com.romankozak.forwardappmobile.shared.features.projects.data

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.logging.logd
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.romankozak.forwardappmobile.shared.logging.logd as androidLogd


class ProjectLocalDataSourceImpl(
    private val database: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ProjectLocalDataSource {

    private val projectsQueries = database.projectsQueries

    override fun observeAll(): Flow<List<Project>> =
        projectsQueries.getAllProjects()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toModel() } }

    override fun observeById(projectId: String): Flow<Project?> =
        projectsQueries.getProjectById(projectId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toModel() }

    override suspend fun getAll(): List<Project> = withContext(ioDispatcher) {
        projectsQueries.getAllProjectsUnordered()
            .executeAsList()
            .map { it.toModel() }
    }

    override suspend fun getByIds(ids: List<String>): List<Project> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        projectsQueries.getProjectsByIds(ids)
            .executeAsList()
            .map { it.toModel() }
    }

    override suspend fun getById(projectId: String): Project? =
        withContext(ioDispatcher) {
            projectsQueries.getProjectById(projectId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    override suspend fun upsert(project: Project) = withContext(ioDispatcher) {
        projectsQueries.insertProject(
            id = project.id,
            name = project.name,
            description = project.description,
            parentId = project.parentId,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            tags = project.tags,
            relatedLinks = project.relatedLinks,
            isExpanded = project.isExpanded,
            goalOrder = project.order,
            isAttachmentsExpanded = project.isAttachmentsExpanded,
            defaultViewMode = project.defaultViewModeName,
            isCompleted = project.isCompleted,
            isProjectManagementEnabled = project.isProjectManagementEnabled,
            projectStatus = project.projectStatus,
            projectStatusText = project.projectStatusText,
            projectLogLevel = project.projectLogLevel,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
            valueImportance = project.valueImportance.toDouble(),
            valueImpact = project.valueImpact.toDouble(),
            effort = project.effort.toDouble(),
            cost = project.cost.toDouble(),
            risk = project.risk.toDouble(),
            weightEffort = project.weightEffort.toDouble(),
            weightCost = project.weightCost.toDouble(),
            weightRisk = project.weightRisk.toDouble(),
            rawScore = project.rawScore.toDouble(),
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType,
            reservedGroup = project.reservedGroup?.let { ReservedGroup.valueOf(it) }
        )
    }

    override suspend fun upsert(projects: List<Project>, useTransaction: Boolean) {
        if (projects.isEmpty()) return
        withContext(ioDispatcher) {
            if (useTransaction) {
                database.transaction {
                    projects.forEach { project ->
                        projectsQueries.insertProject(
                            id = project.id,
                            name = project.name,
                            description = project.description,
                            parentId = project.parentId,
                            createdAt = project.createdAt,
                            updatedAt = project.updatedAt,
                            tags = project.tags,
                            relatedLinks = project.relatedLinks,
                            isExpanded = project.isExpanded,
                            goalOrder = project.order,
                            isAttachmentsExpanded = project.isAttachmentsExpanded,
                            defaultViewMode = project.defaultViewModeName,
                            isCompleted = project.isCompleted,
                            isProjectManagementEnabled = project.isProjectManagementEnabled,
                            projectStatus = project.projectStatus,
                            projectStatusText = project.projectStatusText,
                            projectLogLevel = project.projectLogLevel,
                            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
                            valueImportance = project.valueImportance.toDouble(),
                            valueImpact = project.valueImpact.toDouble(),
                            effort = project.effort.toDouble(),
                            cost = project.cost.toDouble(),
                            risk = project.risk.toDouble(),
                            weightEffort = project.weightEffort.toDouble(),
                            weightCost = project.weightCost.toDouble(),
                            weightRisk = project.weightRisk.toDouble(),
                            rawScore = project.rawScore.toDouble(),
                            displayScore = project.displayScore,
                            scoringStatus = project.scoringStatus,
                            showCheckboxes = project.showCheckboxes,
                            projectType = project.projectType,
                            reservedGroup = project.reservedGroup?.let { ReservedGroup.valueOf(it) }
                        )
                    }
                }
            } else {
                projects.forEach { project ->
                    projectsQueries.insertProject(
                        id = project.id,
                        name = project.name,
                        description = project.description,
                        parentId = project.parentId,
                        createdAt = project.createdAt,
                        updatedAt = project.updatedAt,
                        tags = project.tags,
                        relatedLinks = project.relatedLinks,
                        isExpanded = project.isExpanded,
                        goalOrder = project.order,
                        isAttachmentsExpanded = project.isAttachmentsExpanded,
                        defaultViewMode = project.defaultViewModeName,
                        isCompleted = project.isCompleted,
                        isProjectManagementEnabled = project.isProjectManagementEnabled,
                        projectStatus = project.projectStatus,
                        projectStatusText = project.projectStatusText,
                        projectLogLevel = project.projectLogLevel,
                        totalTimeSpentMinutes = project.totalTimeSpentMinutes,
                        valueImportance = project.valueImportance.toDouble(),
                        valueImpact = project.valueImpact.toDouble(),
                        effort = project.effort.toDouble(),
                        cost = project.cost.toDouble(),
                        risk = project.risk.toDouble(),
                        weightEffort = project.weightEffort.toDouble(),
                        weightCost = project.weightCost.toDouble(),
                        weightRisk = project.weightRisk.toDouble(),
                        rawScore = project.rawScore.toDouble(),
                        displayScore = project.displayScore,
                        scoringStatus = project.scoringStatus,
                        showCheckboxes = project.showCheckboxes,
                        projectType = project.projectType,
                        reservedGroup = project.reservedGroup?.let { ReservedGroup.valueOf(it) }
                    )
                }
            }
        }
    }

    override suspend fun delete(projectId: String) {
        withContext(ioDispatcher) { projectsQueries.deleteProject(projectId) }
    }

    override suspend fun delete(projectIds: List<String>) {
        withContext(ioDispatcher) {
            database.transaction {
                projectIds.forEach { projectsQueries.deleteProject(it) }
            }
        }
    }

    override suspend fun deleteDefault(projectId: String) {
        withContext(ioDispatcher) { projectsQueries.deleteProjectById(projectId) }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            projectsQueries.deleteProjectsForReset()
        }
        Log.d("FullImportFlow", "deleteAll() done")

    }

    override fun deleteAllWithinTransaction() {
        projectsQueries.deleteProjectsForReset()
        Log.d("FullImportFlow", "deleteAllWithinTransaction() done")
    }

    override suspend fun getByParent(parentId: String): List<Project> =
        withContext(ioDispatcher) {
            projectsQueries.getProjectsByParentId(parentId)
                .executeAsList()
                .map { it.toModel() }
        }

    override suspend fun getTopLevel(): List<Project> =
        withContext(ioDispatcher) {
            projectsQueries.getTopLevelProjects()
                .executeAsList().map { it.toModel() }
        }

    override suspend fun getByTag(tag: String): List<Project> =
        withContext(ioDispatcher) {
            val ids = projectsQueries.getProjectIdsByTag(tag).executeAsList()
            if (ids.isEmpty()) emptyList()
            else projectsQueries.getProjectsByIds(ids)
                .executeAsList().map { it.toModel() }
        }

    override suspend fun getIdsByTag(tag: String): List<String> =
        withContext(ioDispatcher) {
            projectsQueries.getProjectIdsByTag(tag).executeAsList()
        }

    override suspend fun getByType(projectType: String): List<Project> =
        withContext(ioDispatcher) {
            projectsQueries.getProjectsByType(projectType)
                .executeAsList().map { it.toModel() }
        }

    override suspend fun getByReservedGroup(reservedGroup: String): List<Project> =
        withContext(ioDispatcher) {
            projectsQueries.getProjectsByReservedGroup(reservedGroup)
                .executeAsList().map { it.toModel() }
        }

    override suspend fun getByParentAndReservedGroup(parentId: String?, reservedGroup: String): Project? =
        withContext(ioDispatcher) {
            projectsQueries
                .getProjectByParentAndReservedGroup(parentId, reservedGroup)
                .executeAsOneOrNull()?.toModel()
        }

    override suspend fun getByNameLike(query: String): List<Project> =
        withContext(ioDispatcher) {
            projectsQueries.getProjectsByNameLike(query)
                .executeAsList().map { it.toModel() }
        }

    override suspend fun updateOrder(projectId: String, order: Long) {
        withContext(ioDispatcher) {
            projectsQueries.updateProjectOrder(order, projectId)
        }
    }

    override suspend fun updateDefaultViewMode(projectId: String, viewMode: String) {
        withContext(ioDispatcher) {
            projectsQueries.updateProjectViewMode(viewMode, projectId)
        }
    }
}