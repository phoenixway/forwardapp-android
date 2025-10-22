package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ProjectActionsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun addNewProject(scope: CoroutineScope, name: String, parentId: String?, allProjects: List<com.romankozak.forwardappmobile.data.database.models.Project>) {
        if (name.isBlank()) {
            // Handle error or throw exception, or pass it back to ViewModel
            return
        }

        val newProjectId = UUID.randomUUID().toString()

        scope.launch(ioDispatcher) {
            projectRepository.addNewProject(
                id = newProjectId,
                name = name,
                parentId = parentId,
                allProjects = allProjects,
            )
        }
    }
}