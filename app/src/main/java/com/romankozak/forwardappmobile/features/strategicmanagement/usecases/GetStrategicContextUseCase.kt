package com.romankozak.forwardappmobile.features.strategicmanagement.usecases

import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedGroup
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import javax.inject.Inject

class GetStrategicContextUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(): List<Context> {
        return projectRepository.getProjectsByReservedGroup(ReservedGroup.Strategic.groupName)
    }
}
