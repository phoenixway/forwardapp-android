package com.romankozak.forwardappmobile.features.missions.domain.repository

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMissionAttachmentCrossRef
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepository
    @Inject
    constructor(
        private val tacticalMissionDao: TacticalMissionDao,
    ) {
        private val tag = "MissionRepository"

        fun getMissionsForProject(projectId: String): Flow<List<TacticalMission>> {
            return tacticalMissionDao.getMissionsForProject(projectId)
        }

        fun getAllMissions(): Flow<List<TacticalMission>> {
            return tacticalMissionDao.getAllMissions()
        }

        fun observeBacklogMissionIdsForWeek(weekKey: String): Flow<List<String>> =
            tacticalMissionDao.observeBacklogMissionIdsForWeek(weekKey)

        suspend fun getMissionById(missionId: Long): TacticalMission? {
            return tacticalMissionDao.getMissionById(missionId)
        }

        suspend fun getMissionForBacklogItemInWeek(
            backlogItemId: String,
            weekKey: String,
        ): TacticalMission? = tacticalMissionDao.getMissionForBacklogItemInWeek(backlogItemId, weekKey)

        suspend fun insertMission(mission: TacticalMission): Long {
            return tacticalMissionDao.insertMission(mission)
        }

        suspend fun insertMissionWithAutoOrder(mission: TacticalMission): Long {
            val topOrder = tacticalMissionDao.getMinMissionOrder() - 1
            val now = System.currentTimeMillis()
            return tacticalMissionDao.insertMission(
                mission.copy(
                    order = topOrder,
                    orderInWeek = topOrder,
                    createdAt = mission.createdAt.takeIf { it > 0L } ?: now,
                    updatedAt = now,
                    syncedAt = null,
                    version = mission.version + 1,
                ),
            )
        }

        suspend fun updateMission(mission: TacticalMission) {
            tacticalMissionDao.updateMission(
                mission.copy(
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = mission.version + 1,
                ),
            )
        }

        suspend fun deleteMissionById(missionId: Long) {
            tacticalMissionDao.deleteMissionById(missionId)
        }

        suspend fun setAttachments(
            missionId: Long,
            attachmentIds: List<String>,
        ) {
            val sanitizedIncoming = mutableSetOf<String>()
            attachmentIds.forEach { rawId ->
                val id = rawId.trim()
                if (id.isNotBlank() && tacticalMissionDao.attachmentExists(id)) {
                    sanitizedIncoming.add(id)
                }
            }
            val existing = tacticalMissionDao.getAttachmentIdsForMission(missionId).toSet()
            val toAdd = sanitizedIncoming - existing
            val toDelete = existing - sanitizedIncoming
            toAdd.forEach { id ->
                val crossRef = TacticalMissionAttachmentCrossRef(missionId = missionId, attachmentId = id)
                runCatching {
                    tacticalMissionDao.insertMissionAttachmentCrossRef(crossRef)
                }.onFailure { error ->
                    Log.w(tag, "Skip invalid mission attachment link missionId=$missionId attachmentId=$id", error)
                }
            }
            toDelete.forEach { id ->
                tacticalMissionDao.deleteMissionAttachmentCrossRef(missionId, id)
            }
        }

        suspend fun linkAttachmentToMission(
            missionId: Long,
            attachmentId: String,
        ) {
            val current = tacticalMissionDao.getAttachmentIdsForMission(missionId).toMutableSet()
            if (current.add(attachmentId)) {
                setAttachments(missionId, current.toList())
            }
        }

        suspend fun unlinkAttachmentFromMission(
            missionId: Long,
            attachmentId: String,
        ) {
            val current = tacticalMissionDao.getAttachmentIdsForMission(missionId).toMutableSet()
            if (current.remove(attachmentId)) {
                setAttachments(missionId, current.toList())
            }
        }

        suspend fun reorderMissions(missions: List<TacticalMission>) {
            missions.forEachIndexed { index, mission ->
                tacticalMissionDao.updateMissionOrder(missionId = mission.id, order = index.toLong())
            }
        }
    }
