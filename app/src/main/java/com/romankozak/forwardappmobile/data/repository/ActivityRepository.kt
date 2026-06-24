package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecordKind
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.domain.ai.events.ActivityFinishedEvent
import com.romankozak.forwardappmobile.domain.ai.events.ActivityLoggedEvent
import com.romankozak.forwardappmobile.domain.userawareness.ContextStateMinutes
import com.romankozak.forwardappmobile.domain.userawareness.StateSlashCommandParser
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateChange
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ActivityInputOutcome {
    LOGGED,
    STATE_CHANGED_ONLY,
    IGNORED_BLANK,
}

data class ActivityInputResult(
    val outcome: ActivityInputOutcome,
    val appliedStateChange: UserStateChange? = null,
)

private const val MINUTES_IN_MILLIS = 60_000L
private const val TARGET_TYPE_DAY = "DAY"

@Singleton
class ActivityRepository
    @Inject
    constructor(
        private val activityRecordDao: ActivityRecordDao,
        private val goalDao: GoalDao,
        private val contextDao: ContextDao,
        private val aiEventRepository: AiEventRepository,
        private val appDatabase: AppDatabase,
        private val userAwarenessRepository: UserAwarenessRepository,
        private val stateSlashCommandParser: StateSlashCommandParser,
    ) {
        fun getLogStream(): Flow<List<ActivityRecord>> = activityRecordDao.getAllRecordsStream()

        fun getRecentLogStream(limit: Int): Flow<List<ActivityRecord>> = activityRecordDao.getRecentRecordsStream(limit)

        suspend fun getOlderLogRecords(
            beforeCreatedAt: Long,
            limit: Int,
        ): List<ActivityRecord> = activityRecordDao.getOlderRecordsBefore(beforeCreatedAt, limit).asReversed()

        suspend fun addTimelessRecord(
            text: String,
            timestamp: Long = System.currentTimeMillis(),
        ): ActivityInputResult {
            if (text.isBlank()) {
                return ActivityInputResult(ActivityInputOutcome.IGNORED_BLANK)
            }
            val parsed = stateSlashCommandParser.parse(text)
            return when {
                parsed.cleanedText.isBlank() && parsed.detectedChange != null ->
                    applyStateOnlyTimelessRecord(
                        change = parsed.detectedChange,
                        timestamp = timestamp,
                    )

                else ->
                    insertTimelessRecord(
                        originalText = text,
                        cleanedText = parsed.cleanedText,
                        timestamp = timestamp,
                        detectedChange = parsed.detectedChange,
                    )
            }
        }

        suspend fun startActivity(
            text: String,
            startTime: Long,
        ): ActivityRecord {
            val parsed = stateSlashCommandParser.parse(text)
            val now = System.currentTimeMillis()
            val recordId = UUID.randomUUID().toString()
            val newRecord =
                ActivityRecord(
                    id = recordId,
                    text = parsed.cleanedText,
                    rawNoteText = text,
                    noteText = parsed.cleanedText,
                    recordKind = ActivityRecordKind.TIMED_ACTIVITY,
                    stateEventType = parsed.detectedChange?.type?.name,
                    stateEventCrisisLevel = parsed.detectedChange?.crisisLevel,
                    stateEventLabel = parsed.detectedChange?.label,
                    stateEventApplied = parsed.detectedChange != null,
                    createdAt = now,
                    startTime = startTime,
                    endTime = null,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(startTime)
                val ongoingActivity = activityRecordDao.findLastOngoingActivity()
                ongoingActivity?.let {
                    activityRecordDao.update(
                        it.copy(
                            endTime = startTime,
                            updatedAt = startTime,
                            syncedAt = null,
                            version = it.version + 1,
                        ),
                    )
                }
                activityRecordDao.insert(newRecord)
                parsed.detectedChange?.let {
                    userAwarenessRepository.applyStateChangeFromActivityInTransaction(
                        change = it,
                        activityId = recordId,
                        now = startTime,
                    )
                }
            }
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                ),
            )
            return newRecord
        }

        suspend fun endLastActivity(endTime: Long) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivity()
            ongoingActivity?.let {
                val finishedActivity =
                    it.copy(
                        endTime = endTime,
                        updatedAt = endTime,
                        syncedAt = null,
                        version = it.version + 1,
                    )
                activityRecordDao.update(finishedActivity)
            }
        }

        suspend fun startGoalActivity(goalId: String): ActivityRecord? {
            val goal = goalDao.getGoalById(goalId) ?: return null
            val now = System.currentTimeMillis()
            val newRecord =
                ActivityRecord(
                    text = goal.text,
                    rawNoteText = goal.text,
                    noteText = goal.text,
                    recordKind = ActivityRecordKind.TIMED_ACTIVITY,
                    startTime = now,
                    goalId = goalId,
                    createdAt = now,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(now)
                val ongoingActivity = activityRecordDao.findLastOngoingActivity()
                ongoingActivity?.let {
                    activityRecordDao.update(
                        it.copy(
                            endTime = now,
                            updatedAt = now,
                            syncedAt = null,
                            version = it.version + 1,
                        ),
                    )
                }
                activityRecordDao.insert(newRecord)
            }
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                ),
            )
            return newRecord
        }

        suspend fun endGoalActivity(goalId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForGoal(goalId)
            ongoingActivity?.let {
                val finishedActivity =
                    it.copy(
                        endTime = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncedAt = null,
                        version = it.version + 1,
                    )
                activityRecordDao.update(finishedActivity)
                val end = finishedActivity.endTime ?: finishedActivity.createdAt
                val duration =
                    ((end - (finishedActivity.startTime ?: end)) / MINUTES_IN_MILLIS)
                        .toInt()
                        .coerceAtLeast(0)
                aiEventRepository.emit(
                    ActivityFinishedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(end),
                        durationMinutes = duration,
                        xp = finishedActivity.xpGained ?: 0,
                        antiXp = finishedActivity.antyXp ?: 0,
                    ),
                )
            }
        }

        suspend fun startContextActivity(contextId: String): ActivityRecord? {
            val context = contextDao.getContextById(contextId) ?: return null
            val now = System.currentTimeMillis()
            val newRecord =
                ActivityRecord(
                    text = context.name,
                    rawNoteText = context.name,
                    noteText = context.name,
                    recordKind = ActivityRecordKind.TIMED_ACTIVITY,
                    startTime = now,
                    contextId = contextId,
                    createdAt = now,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(now)
                val ongoingActivity = activityRecordDao.findLastOngoingActivity()
                ongoingActivity?.let {
                    activityRecordDao.update(
                        it.copy(
                            endTime = now,
                            updatedAt = now,
                            syncedAt = null,
                            version = it.version + 1,
                        ),
                    )
                }
                activityRecordDao.insert(newRecord)
            }
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                ),
            )
            return newRecord
        }

        suspend fun addCompletedActivity(
            text: String,
            xpGained: Int?,
            antyXp: Int?,
        ) {
            if (text.isBlank()) return
            val now = System.currentTimeMillis()
            val parsed = stateSlashCommandParser.parse(text)
            val recordId = UUID.randomUUID().toString()
            val record =
                ActivityRecord(
                    id = recordId,
                    text = parsed.cleanedText,
                    rawNoteText = text,
                    noteText = parsed.cleanedText,
                    recordKind = ActivityRecordKind.EVENT,
                    stateEventType = parsed.detectedChange?.type?.name,
                    stateEventCrisisLevel = parsed.detectedChange?.crisisLevel,
                    stateEventLabel = parsed.detectedChange?.label,
                    stateEventApplied = parsed.detectedChange != null,
                    createdAt = now,
                    startTime = now,
                    endTime = now,
                    xpGained = xpGained,
                    antyXp = antyXp,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(now)
                activityRecordDao.insert(record)
                parsed.detectedChange?.let {
                    userAwarenessRepository.applyStateChangeFromActivityInTransaction(
                        change = it,
                        activityId = recordId,
                        now = now,
                    )
                }
            }
            aiEventRepository.emit(
                ActivityFinishedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = xpGained ?: 0,
                    antiXp = antyXp ?: 0,
                ),
            )
        }

        suspend fun endContextActivity(contextId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForContext(contextId)
            ongoingActivity?.let {
                val now = System.currentTimeMillis()
                val finishedActivity =
                    it.copy(
                        endTime = now,
                        updatedAt = now,
                        syncedAt = null,
                        version = it.version + 1,
                    )
                activityRecordDao.update(finishedActivity)
                val duration =
                    ((now - (finishedActivity.startTime ?: now)) / MINUTES_IN_MILLIS)
                        .toInt()
                        .coerceAtLeast(0)
                aiEventRepository.emit(
                    ActivityFinishedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(now),
                        durationMinutes = duration,
                        xp = finishedActivity.xpGained ?: 0,
                        antiXp = finishedActivity.antyXp ?: 0,
                    ),
                )
            }
        }

        suspend fun updateRecord(record: ActivityRecord) {
            activityRecordDao.update(record.bumpSync())
        }

        suspend fun upsertTodaySummary(text: String) {
            if (text.isBlank()) return
            val now = System.currentTimeMillis()
            val dayId = LocalDate.now().toString()
            val existing =
                activityRecordDao.findByKindAndTarget(
                    recordKind = ActivityRecordKind.DAY_SUMMARY,
                    targetType = TARGET_TYPE_DAY,
                    targetId = dayId,
                )
            val record =
                existing?.copy(
                    text = text.trim(),
                    rawNoteText = text,
                    noteText = text.trim(),
                    updatedAt = now,
                    syncedAt = null,
                    version = existing.version + 1,
                ) ?: ActivityRecord(
                    text = text.trim(),
                    rawNoteText = text,
                    noteText = text.trim(),
                    recordKind = ActivityRecordKind.DAY_SUMMARY,
                    createdAt = now,
                    startTime = null,
                    endTime = null,
                    targetId = dayId,
                    targetType = TARGET_TYPE_DAY,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(record)
        }

        suspend fun clearLog() {
            activityRecordDao.clearAll()
        }

        suspend fun deleteRecord(record: ActivityRecord) {
            // Changed the logic here to correctly delete an activity
            activityRecordDao.deleteById(record.id)
        }

        suspend fun searchActivities(query: String): List<ActivityRecord> = activityRecordDao.search(query)

        suspend fun getAllActivitiesForSearch(): List<ActivityRecord> =
            activityRecordDao.getAllRaw().filterNot { it.isDeleted }

        suspend fun getCompletedActivitiesForContext(
            contextId: String,
            goalIds: List<String>,
            startTime: Long,
            endTime: Long,
        ): List<ActivityRecord> =
            activityRecordDao.getCompletedActivitiesForContext(
                contextId = contextId,
                goalIds = goalIds,
                startTime = startTime,
                endTime = endTime,
            )

        suspend fun getAllCompletedActivitiesForContext(
            contextId: String,
            goalIds: List<String>,
        ): List<ActivityRecord> = activityRecordDao.getAllCompletedActivitiesForContext(contextId, goalIds)

        suspend fun getActivityRecordById(recordId: String): ActivityRecord? {
            return activityRecordDao.findById(recordId)
        }

        fun findLastOngoingActivityFlow(): Flow<ActivityRecord?> {
            return activityRecordDao.findLastOngoingActivityFlow()
        }

        fun getActivitiesForContextStream(contextId: String): Flow<List<ActivityRecord>> {
            return activityRecordDao.getRecordsForContextStream(contextId)
        }

        suspend fun getTrackedMinutesByContextAndState(
            fromTimestamp: Long,
            toTimestamp: Long,
        ): List<ContextStateMinutes> {
            val records = activityRecordDao.getCompletedContextActivitiesBetween(fromTimestamp, toTimestamp)
            if (records.isEmpty()) return emptyList()

            val buckets = linkedMapOf<String, ContextStateMinutes>()
            records.forEach { record ->
                val contextId = record.contextId ?: return@forEach
                val durationMinutes =
                    (((record.endTime ?: return@forEach) - (record.startTime ?: return@forEach)) / MINUTES_IN_MILLIS)
                        .toInt()
                        .coerceAtLeast(0)
                if (durationMinutes == 0) return@forEach

                val stateType = userAwarenessRepository.getStateAt(record.startTime ?: record.createdAt).type
                val current = buckets[contextId] ?: ContextStateMinutes(contextId = contextId)
                val updated =
                    when (stateType) {
                        UserAwarenessStateType.NORMAL ->
                            current.copy(normalMinutes = current.normalMinutes + durationMinutes)

                        UserAwarenessStateType.CRISIS ->
                            current.copy(crisisMinutes = current.crisisMinutes + durationMinutes)

                        UserAwarenessStateType.EXHAUSTION ->
                            current.copy(exhaustionMinutes = current.exhaustionMinutes + durationMinutes)
                        UserAwarenessStateType.UNPRODUCTIVE ->
                            current.copy(unproductiveMinutes = current.unproductiveMinutes + durationMinutes)
                    }
                buckets[contextId] = updated
            }
            return buckets.values.toList()
        }

        private suspend fun applyStateOnlyTimelessRecord(
            change: UserStateChange,
            timestamp: Long,
        ): ActivityInputResult {
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(timestamp)
                userAwarenessRepository.applyStateChangeFromActivityInTransaction(
                    change = change,
                    activityId = UUID.randomUUID().toString(),
                    now = timestamp,
                )
            }
            return ActivityInputResult(
                outcome = ActivityInputOutcome.STATE_CHANGED_ONLY,
                appliedStateChange = change,
            )
        }

        private suspend fun insertTimelessRecord(
            originalText: String,
            cleanedText: String,
            timestamp: Long,
            detectedChange: UserStateChange?,
        ): ActivityInputResult {
            val recordId = UUID.randomUUID().toString()
            val record =
                ActivityRecord(
                    id = recordId,
                    text = cleanedText,
                    rawNoteText = originalText,
                    noteText = cleanedText,
                    recordKind = ActivityRecordKind.COMMENT,
                    stateEventType = detectedChange?.type?.name,
                    stateEventCrisisLevel = detectedChange?.crisisLevel,
                    stateEventLabel = detectedChange?.label,
                    stateEventApplied = detectedChange != null,
                    createdAt = timestamp,
                    startTime = null,
                    endTime = null,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = timestamp,
                    syncedAt = null,
                    version = 1,
                )
            appDatabase.withTransaction {
                userAwarenessRepository.ensureDefaultStateInTransaction(timestamp)
                activityRecordDao.insert(record)
                detectedChange?.let {
                    userAwarenessRepository.applyStateChangeFromActivityInTransaction(
                        change = it,
                        activityId = recordId,
                        now = timestamp,
                    )
                }
            }
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(timestamp),
                    durationMinutes = 0,
                    xp = record.xpGained ?: 0,
                    antiXp = record.antyXp ?: 0,
                    isOngoing = false,
                ),
            )
            return ActivityInputResult(
                outcome = ActivityInputOutcome.LOGGED,
                appliedStateChange = detectedChange,
            )
        }
    }
