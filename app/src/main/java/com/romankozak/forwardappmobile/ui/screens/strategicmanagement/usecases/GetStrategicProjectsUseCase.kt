package com.romankozak.forwardappmobile.ui.screens.strategicmanagement.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import javax.inject.Inject

class GetStrategicProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(): List<Project> {
        return projectRepository.getProjectsByReservedGroup(ReservedGroup.Strategic.groupName)
    }
}
