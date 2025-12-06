package com.romankozak.forwardappmobile.features.missions.domain.usecase

import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import javax.inject.Inject

class DeleteTacticalMissionUseCase @Inject constructor(
    private val repository: MissionRepository
) {
    suspend operator fun invoke(missionId: Long) {
        repository.deleteMissionById(missionId)
    }
}
