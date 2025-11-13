package com.romankozak.forwardappmobile.shared.features.projects.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
