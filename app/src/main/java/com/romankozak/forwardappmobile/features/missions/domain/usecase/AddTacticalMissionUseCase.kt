package com.romankozak.forwardappmobile.features.missions.domain.usecase

import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import javax.inject.Inject

class AddTacticalMissionUseCase @Inject constructor(
    private val repository: MissionRepository
) {
    suspend operator fun invoke(mission: TacticalMission): Long {
        return repository.insertMission(mission)
    }
}
