package com.romankozak.forwardappmobile.ui.screens.strategicmanagement.usecases

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.core.database.models.ReservedGroup
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import javax.inject.Inject

class GetStrategicProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(): List<Project> {
        return projectRepository.getProjectsByReservedGroup(ReservedGroup.Strategic.groupName)
    }
}
