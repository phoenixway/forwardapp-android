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

    suspend fun setAttachments(missionId: Long, attachmentIds: List<String>) {
        val existing = tacticalMissionDao.getAttachmentIdsForMission(missionId).toSet()
        val incoming = attachmentIds.toSet()
        val toAdd = incoming - existing
        val toDelete = existing - incoming
        toAdd.forEach { id ->
            val crossRef = TacticalMissionAttachmentCrossRef(missionId = missionId, attachmentId = id)
            tacticalMissionDao.insertMissionAttachmentCrossRef(crossRef)
        }
        toDelete.forEach { id ->
            tacticalMissionDao.deleteMissionAttachmentCrossRef(missionId, id)
        }
    }

    suspend fun linkAttachmentToMission(missionId: Long, attachmentId: String) {
        val current = tacticalMissionDao.getAttachmentIdsForMission(missionId).toMutableSet()
        if (current.add(attachmentId)) {
            setAttachments(missionId, current.toList())
        }
    }

    suspend fun unlinkAttachmentFromMission(missionId: Long, attachmentId: String) {
        val current = tacticalMissionDao.getAttachmentIdsForMission(missionId).toMutableSet()
        if (current.remove(attachmentId)) {
            setAttachments(missionId, current.toList())
        }
    }
}
