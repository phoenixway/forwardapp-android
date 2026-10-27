package com.romankozak.forwardappmobile.features.missions.presentation

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

internal data class TacticalCarryForwardDestination(
    val iterationId: String?,
    val weekKey: String,
    val deadline: Long,
    val slotOrder: Long?,
)

internal sealed interface TacticalCarryForwardResult {
    data class Created(val missionId: Long) : TacticalCarryForwardResult
    data class AlreadyPresent(val mission: TacticalMission) : TacticalCarryForwardResult
    data object Busy : TacticalCarryForwardResult
    data object Unavailable : TacticalCarryForwardResult
}

/** UI command boundary only; canonical persistence/semantic admission remains in the repository. */
internal class TacticalMissionCarryForward(private val repository: MissionRepository) {
    private val mutex = Mutex()

    suspend fun carry(
        mission: TacticalMission,
        destination: TacticalCarryForwardDestination,
        existingMissions: List<TacticalMission>,
    ): TacticalCarryForwardResult {
        if (!mutex.tryLock()) return TacticalCarryForwardResult.Busy
        try {
            val existing = existingMissions.firstOrNull { candidate ->
                !candidate.isDeleted &&
                    candidate.isInCurrentIteration(destination.iterationId, destination.weekKey) &&
                    (candidate.id == mission.id || candidate.carriedFromMissionId == mission.id)
            }
            if (existing != null) return TacticalCarryForwardResult.AlreadyPresent(existing)

            val now = System.currentTimeMillis()
            val carriedMission = mission.copy(
                id = 0,
                weekKey = destination.weekKey,
                iterationId = destination.iterationId,
                carriedFromMissionId = mission.id,
                status = MissionStatus.ACTIVE,
                startTime = now,
                deadline = destination.deadline,
                orderInSlot = destination.slotOrder,
                sourceType = MissionSourceType.PREVIOUS_WEEK,
                createdAt = now,
                updatedAt = now,
                syncedAt = null,
                version = 0L,
            )
            val id = repository.insertMissionWithAutoOrder(carriedMission)
            repository.setAttachments(id, carriedMission.linkedAttachmentIds.orEmpty())
            return TacticalCarryForwardResult.Created(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TACTICAL_MISSION_DEBUG_TAG, "Carry-forward failed for mission ${mission.id}", error)
            return TacticalCarryForwardResult.Unavailable
        } finally {
            mutex.unlock()
        }
    }
}
