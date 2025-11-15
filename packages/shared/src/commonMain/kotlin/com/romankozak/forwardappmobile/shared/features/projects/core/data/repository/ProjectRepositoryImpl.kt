package com.romankozak.forwardappmobile.shared.features.projects.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.core.platform.Platform
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> {
        return db.projectsQueries.getAllProjects()
            .asFlow()
            .mapToList(dispatcher)
            .map { projects -> projects.map { it.toDomain() } }
    }

    override fun getProjectById(id: String): Flow<Project?> {
        return db.projectsQueries.getProjectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }

    override fun searchProjects(query: String): Flow<List<Project>> {
        val projects = if (Platform.isAndroid) {
            db.projectsQueries.searchProjectsFts(query)
        } else {
            db.projectsQueries.searchProjectsFallback(query)
        }
        return projects.asFlow()
            .mapToList(dispatcher)
            .map { projects -> projects.map { it.toDomain() } }
    }

    override suspend fun upsertProject(project: Project) = withContext(dispatcher) {
        db.projectsQueries.insertProject(
            id = project.id,
            name = project.name,
            description = project.description,
            parentId = project.parentId,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            tags = project.tags,
            relatedLinks = project.relatedLinks,
            isExpanded = project.isExpanded,
            goalOrder = project.goalOrder,
            isAttachmentsExpanded = project.isAttachmentsExpanded,
            defaultViewMode = project.defaultViewMode,
            isCompleted = project.isCompleted,
            isProjectManagementEnabled = project.isProjectManagementEnabled ?: false,
            projectStatus = project.projectStatus,
            projectStatusText = project.projectStatusText,
            projectLogLevel = project.projectLogLevel,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes,
            valueImportance = project.valueImportance,
            valueImpact = project.valueImpact,
            effort = project.effort,
            cost = project.cost,
            risk = project.risk,
            weightEffort = project.weightEffort,
            weightCost = project.weightCost,
            weightRisk = project.weightRisk,
            rawScore = project.rawScore,
            displayScore = project.displayScore,
            scoringStatus = project.scoringStatus,
            showCheckboxes = project.showCheckboxes,
            projectType = project.projectType,
            reservedGroup = project.reservedGroup
        )
    }

    override suspend fun deleteProject(id: String) = withContext(dispatcher) {
        db.projectsQueries.deleteProject(id)
    }
}
