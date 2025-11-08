package com.romankozak.forwardappmobile.ui.screens.strategicmanagement.usecases

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import javax.inject.Inject

class GetStrategicProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepositoryCore
) {
    suspend operator fun invoke(): List<Project> {
        return projectRepository.getProjectsByReservedGroup(ReservedGroup.Strategic.groupName)
    }
}
