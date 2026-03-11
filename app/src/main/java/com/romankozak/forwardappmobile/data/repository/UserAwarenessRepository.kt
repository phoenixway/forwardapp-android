package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.UserStateIntervalEntity
import com.romankozak.forwardappmobile.data.dao.UserStateIntervalDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.domain.userawareness.NudgePolicyFlags
import com.romankozak.forwardappmobile.domain.userawareness.QuotaPolicy
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateChange
import com.romankozak.forwardappmobile.domain.userawareness.UserStateInterval
import com.romankozak.forwardappmobile.domain.userawareness.UserStateSource
import com.romankozak.forwardappmobile.domain.userawareness.WeeklyReviewFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAwarenessRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val userStateIntervalDao: UserStateIntervalDao,
    ) {
        fun observeActiveState(): Flow<UserStateInterval> =
            userStateIntervalDao.observeActive().map { entity ->
                (
                    entity ?: UserStateIntervalEntity(
                        stateType = UserAwarenessStateType.NORMAL.name,
                        startedAt = System.currentTimeMillis(),
                    )
                ).toUserStateInterval()
            }

        suspend fun getActiveState(now: Long = System.currentTimeMillis()): UserStateInterval {
            return appDatabase.withTransaction {
                (userStateIntervalDao.getActive() ?: ensureDefaultStateLocked(now)).toUserStateInterval()
            }
        }

        suspend fun setStateManual(
            type: UserAwarenessStateType,
            level: Int? = null,
            label: String? = null,
        ): UserStateInterval =
            appDatabase.withTransaction {
                ensureDefaultStateLocked(System.currentTimeMillis())
                applyStateChangeLocked(
                    change = UserStateChange(type = type, crisisLevel = level, label = label),
                    source = UserStateSource.MANUAL,
                    createdFromActivityId = null,
                    now = System.currentTimeMillis(),
                )
            }

        suspend fun getStateTimeline(
            from: Long,
            to: Long,
        ): List<UserStateInterval> =
            userStateIntervalDao.getTimeline(
                fromInclusive = from,
                toInclusive = to,
            ).map { it.toUserStateInterval() }

        suspend fun applyStateChangeFromActivity(
            change: UserStateChange,
            activityId: String,
            now: Long,
        ): UserStateInterval =
            appDatabase.withTransaction {
                applyStateChangeFromActivityInTransaction(change, activityId, now)
            }

        suspend fun applyStateChangeFromActivityInTransaction(
            change: UserStateChange,
            activityId: String,
            now: Long,
        ): UserStateInterval {
            ensureDefaultStateLocked(now)
            return applyStateChangeLocked(
                change = change,
                source = UserStateSource.MANUAL,
                createdFromActivityId = activityId,
                now = now,
            )
        }

        fun getNudgePolicy(activeState: UserStateInterval): NudgePolicyFlags =
            when (activeState.type) {
                UserAwarenessStateType.CRISIS,
                UserAwarenessStateType.EXHAUSTION,
                ->
                    NudgePolicyFlags(
                        allowGuiltMessaging = false,
                        reduceNotifications = activeState.type == UserAwarenessStateType.EXHAUSTION,
                        enableMicroActions = activeState.type == UserAwarenessStateType.UNPRODUCTIVE,
                    )
                UserAwarenessStateType.UNPRODUCTIVE ->
                    NudgePolicyFlags(
                        allowGuiltMessaging = true,
                        reduceNotifications = false,
                        enableMicroActions = true,
                    )
                UserAwarenessStateType.NORMAL ->
                    NudgePolicyFlags(
                        allowGuiltMessaging = true,
                        reduceNotifications = false,
                        enableMicroActions = false,
                    )
            }

        fun getQuotaPolicy(activeState: UserStateInterval): QuotaPolicy =
            when (activeState.type) {
                UserAwarenessStateType.NORMAL ->
                    QuotaPolicy(
                        enforceTargetAndCap = true,
                        minimumIsSoft = false,
                        targetMultiplier = 1f,
                        disablePushes = false,
                    )
                UserAwarenessStateType.CRISIS ->
                    QuotaPolicy(
                        enforceTargetAndCap = false,
                        minimumIsSoft = true,
                        targetMultiplier = 0f,
                        disablePushes = true,
                    )
                UserAwarenessStateType.EXHAUSTION ->
                    QuotaPolicy(
                        enforceTargetAndCap = false,
                        minimumIsSoft = true,
                        targetMultiplier = 0.5f,
                        disablePushes = true,
                    )
                UserAwarenessStateType.UNPRODUCTIVE ->
                    QuotaPolicy(
                        enforceTargetAndCap = true,
                        minimumIsSoft = false,
                        targetMultiplier = 1f,
                        disablePushes = false,
                    )
            }

        suspend fun getWeeklyReviewFlags(
            from: Long,
            to: Long,
        ): WeeklyReviewFlags {
            val hasTurbulence =
                getStateTimeline(from, to).any {
                    it.type == UserAwarenessStateType.CRISIS || it.type == UserAwarenessStateType.EXHAUSTION
                }
            return WeeklyReviewFlags(turbulent = hasTurbulence)
        }

        suspend fun getStateAt(atMillis: Long): UserStateInterval {
            val found = userStateIntervalDao.getActiveAt(atMillis)
            return if (found != null) found.toUserStateInterval() else getActiveState(atMillis)
        }

        suspend fun ensureDefaultState(now: Long = System.currentTimeMillis()) {
            appDatabase.withTransaction { ensureDefaultStateLocked(now) }
        }

        suspend fun ensureDefaultStateInTransaction(now: Long = System.currentTimeMillis()): UserStateInterval =
            ensureDefaultStateLocked(now).toUserStateInterval()

        private suspend fun applyStateChangeLocked(
            change: UserStateChange,
            source: UserStateSource,
            createdFromActivityId: String?,
            now: Long,
        ): UserStateInterval {
            val normalized = normalizeChange(change)
            val active = userStateIntervalDao.getActive() ?: ensureDefaultStateLocked(now)

            if (isSameState(active, normalized)) {
                return active.toUserStateInterval()
            }

            userStateIntervalDao.closeActiveIntervals(now)
            val newInterval =
                UserStateIntervalEntity(
                    stateType = normalized.type.name,
                    crisisLevel = normalized.crisisLevel,
                    label = normalized.label,
                    source = source.name,
                    createdFromActivityId = createdFromActivityId,
                    startedAt = now,
                    endedAt = null,
                )
            val id = userStateIntervalDao.insert(newInterval)
            return newInterval.copy(id = id).toUserStateInterval()
        }

        private suspend fun ensureDefaultStateLocked(now: Long): UserStateIntervalEntity {
            val active = userStateIntervalDao.getActive()
            if (active != null) return active
            val default =
                UserStateIntervalEntity(
                    stateType = UserAwarenessStateType.NORMAL.name,
                    crisisLevel = null,
                    label = null,
                    source = UserStateSource.MANUAL.name,
                    createdFromActivityId = null,
                    startedAt = now,
                    endedAt = null,
                )
            val id = userStateIntervalDao.insert(default)
            return default.copy(id = id)
        }

        private fun normalizeChange(change: UserStateChange): UserStateChange {
            val normalizedLabel = change.label?.trim()?.take(80)?.ifBlank { null }
            return if (change.type == UserAwarenessStateType.CRISIS) {
                val level = (change.crisisLevel ?: 1).coerceIn(1, 3)
                UserStateChange(
                    type = UserAwarenessStateType.CRISIS,
                    crisisLevel = level,
                    label = normalizedLabel,
                )
            } else {
                UserStateChange(type = change.type, crisisLevel = null, label = null)
            }
        }

        private fun isSameState(
            active: UserStateIntervalEntity,
            change: UserStateChange,
        ): Boolean =
            active.stateType == change.type.name &&
                active.crisisLevel == change.crisisLevel &&
                normalizeLabel(active.label) == normalizeLabel(change.label)

        private fun normalizeLabel(label: String?): String? = label?.trim()?.ifBlank { null }
    }

private fun UserStateIntervalEntity.toUserStateInterval(): UserStateInterval =
    UserStateInterval(
        id = id,
        type = UserAwarenessStateType.valueOf(stateType),
        crisisLevel = crisisLevel,
        label = label,
        source = UserStateSource.valueOf(source),
        createdFromActivityId = createdFromActivityId,
        startedAt = startedAt,
        endedAt = endedAt,
    )
