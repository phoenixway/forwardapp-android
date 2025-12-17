package com.romankozak.forwardappmobile.features.missions.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMissionAttachmentCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalMissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: TacticalMission): Long

    @Update
    suspend fun updateMission(mission: TacticalMission)

    @Query("DELETE FROM tactical_missions WHERE id = :missionId")
    suspend fun deleteMissionById(missionId: Long)

    @Query("SELECT * FROM tactical_missions WHERE projectId = :projectId ORDER BY deadline DESC")
    fun getMissionsForProject(projectId: String): Flow<List<TacticalMission>>
    
    @Query("SELECT * FROM tactical_missions ORDER BY deadline DESC")
    fun getAllMissions(): Flow<List<TacticalMission>>

    @Query("SELECT * FROM tactical_missions WHERE id = :missionId")
    suspend fun getMissionById(missionId: Long): TacticalMission?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionAttachmentCrossRef(crossRef: TacticalMissionAttachmentCrossRef)

    @Query("DELETE FROM tactical_mission_attachment_cross_ref WHERE missionId = :missionId AND attachmentId = :attachmentId")
    suspend fun deleteMissionAttachmentCrossRef(missionId: Long, attachmentId: String)

    @Query(
        """
        SELECT attachmentId FROM tactical_mission_attachment_cross_ref
        WHERE missionId = :missionId
        """
    )
    suspend fun getAttachmentIdsForMission(missionId: Long): List<String>
}
