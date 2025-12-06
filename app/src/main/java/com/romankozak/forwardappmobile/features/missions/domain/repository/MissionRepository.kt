package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMissionAttachmentCrossRef
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepository @Inject constructor(
    private val tacticalMissionDao: TacticalMissionDao
) {
    fun getMissionsForProject(projectId: String): Flow<List<TacticalMission>> {
        return tacticalMissionDao.getMissionsForProject(projectId)
    }

    fun getAllMissions(): Flow<List<TacticalMission>> {
        return tacticalMissionDao.getAllMissions()
    }

    suspend fun getMissionById(missionId: Long): TacticalMission? {
        return tacticalMissionDao.getMissionById(missionId)
    }

    suspend fun insertMission(mission: TacticalMission): Long {
        return tacticalMissionDao.insertMission(mission)
    }

    suspend fun updateMission(mission: TacticalMission) {
        tacticalMissionDao.updateMission(mission)
    }

    suspend fun deleteMissionById(missionId: Long) {
        tacticalMissionDao.deleteMissionById(missionId)
    }

    suspend fun linkAttachmentToMission(missionId: Long, attachmentId: String) {
        val crossRef = TacticalMissionAttachmentCrossRef(missionId = missionId, attachmentId = attachmentId)
        tacticalMissionDao.insertMissionAttachmentCrossRef(crossRef)
    }

    suspend fun unlinkAttachmentFromMission(missionId: Long, attachmentId: String) {
        tacticalMissionDao.deleteMissionAttachmentCrossRef(missionId, attachmentId)
    }
}
