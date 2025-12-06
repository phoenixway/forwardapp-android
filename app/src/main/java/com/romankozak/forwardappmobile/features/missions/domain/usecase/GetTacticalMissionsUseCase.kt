package com.romankozak.forwardappmobile.features.missions.domain.usecase

import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTacticalMissionsUseCase @Inject constructor(
    private val repository: MissionRepository
) {
    operator fun invoke(projectId: String? = null): Flow<List<TacticalMission>> {
        return if (projectId != null) {
            repository.getMissionsForProject(projectId)
        } else {
            repository.getAllMissions()
        }
    }
}
