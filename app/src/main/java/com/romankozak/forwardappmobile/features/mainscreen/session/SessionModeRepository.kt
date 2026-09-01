package com.romankozak.forwardappmobile.features.mainscreen.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecordKind
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_mode")

@Singleton
class SessionModeRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val activityRecordDao: ActivityRecordDao,
        private val contextLogRepository: ContextLogRepository,
        private val systemContextEnsurer: SystemContextEnsurer,
    ) {
        companion object {
            private val currentModeKey = stringPreferencesKey("current_mode")
            private val currentModeStartedAtKey = longPreferencesKey("current_mode_started_at")
            private const val SESSION_EVENT_TARGET_TYPE = "SESSION_MODE_EVENT"
            private const val SESSION_RESULT_TARGET_TYPE = "SESSION_MODE_RESULT"
            private const val SESSION_EVENT_KEEP_COUNT = 120
            private const val SESSION_RESULT_KEEP_COUNT = 80
        }

        val sessionModeState: Flow<SessionModeState> =
            context.sessionModeDataStore.data.map { prefs ->
                val mode = SessionMode.fromStorage(prefs[currentModeKey])
                val startedAt = prefs[currentModeStartedAtKey]
                SessionModeState(
                    mode = mode,
                    startedAt = startedAt?.takeIf { mode != SessionMode.UNSET },
                )
            }

        suspend fun setMode(newMode: SessionMode): SessionModeChangeResult {
            ensureSessionSystemContextsExist()
            val previousState = sessionModeState.first()
            if (previousState.mode == newMode) {
                return SessionModeChangeResult(previousMode = null, newMode = newMode)
            }

            val now = System.currentTimeMillis()

            if (previousState.isActive) {
                val previousMode = previousState.mode
                val startedAt = previousState.startedAt ?: now
                logModeFinished(previousMode, startedAt, now)
            }

            if (newMode == SessionMode.UNSET) {
                context.sessionModeDataStore.edit { prefs ->
                    prefs.clearActiveMode()
                }
            } else {
                context.sessionModeDataStore.edit { prefs ->
                    prefs[currentModeKey] = newMode.name
                    prefs[currentModeStartedAtKey] = now
                }
                logModeStarted(newMode, now)
            }

            return SessionModeChangeResult(
                previousMode = previousState.mode.takeIf { it != SessionMode.UNSET },
                newMode = newMode,
            )
        }

        suspend fun reportModeResults(
            mode: SessionMode,
            text: String,
        ) {
            ensureSessionSystemContextsExist()
            val trimmed = text.trim()
            if (trimmed.isEmpty() || mode == SessionMode.UNSET) return

            insertActivityRecord(
                text = "Підсумок ${mode.title}: $trimmed",
                targetType = SESSION_RESULT_TARGET_TYPE,
                mode = mode,
            )
            mode.systemContextId?.let { contextId ->
                contextLogRepository.addSystemContextLogEntry(
                    contextId = contextId,
                    type = "COMMENT",
                    description = "Підсумок сесії ${mode.title}: $trimmed",
                )
            }
            activityRecordDao.deleteByTargetTypeKeepingNewest(
                targetType = SESSION_RESULT_TARGET_TYPE,
                keepCount = SESSION_RESULT_KEEP_COUNT,
            )
        }

        suspend fun reportModeChangeReason(
            mode: SessionMode,
            text: String,
        ) {
            ensureSessionSystemContextsExist()
            val trimmed = text.trim()
            if (trimmed.isEmpty() || mode == SessionMode.UNSET) return

            val fullText = "Причина переходу в ${mode.title}: $trimmed"
            insertActivityRecord(
                text = fullText,
                targetType = SESSION_EVENT_TARGET_TYPE,
                mode = mode,
            )
            mode.systemContextId?.let { contextId ->
                contextLogRepository.addSystemContextLogEntry(
                    contextId = contextId,
                    type = "SESSION_REASON",
                    description = fullText,
                )
            }
            activityRecordDao.deleteByTargetTypeKeepingNewest(
                targetType = SESSION_EVENT_TARGET_TYPE,
                keepCount = SESSION_EVENT_KEEP_COUNT,
            )
        }

        private suspend fun logModeStarted(
            mode: SessionMode,
            timestamp: Long,
        ) {
            val text = "Режим ${mode.title} увімкнено о ${formatDateTime(timestamp)}"
            insertActivityRecord(
                text = text,
                targetType = SESSION_EVENT_TARGET_TYPE,
                mode = mode,
            )
            mode.systemContextId?.let { contextId ->
                contextLogRepository.addSystemContextLogEntry(
                    contextId = contextId,
                    type = "SESSION_START",
                    description = text,
                )
            }
            activityRecordDao.deleteByTargetTypeKeepingNewest(
                targetType = SESSION_EVENT_TARGET_TYPE,
                keepCount = SESSION_EVENT_KEEP_COUNT,
            )
        }

        private suspend fun logModeFinished(
            mode: SessionMode,
            startedAt: Long,
            endedAt: Long,
        ) {
            val text =
                "Режим ${mode.title} завершено. " +
                    "${formatDateTime(startedAt)} - ${formatDateTime(endedAt)}"
            insertActivityRecord(
                text = text,
                targetType = SESSION_EVENT_TARGET_TYPE,
                mode = mode,
            )
            mode.systemContextId?.let { contextId ->
                contextLogRepository.addSystemContextLogEntry(
                    contextId = contextId,
                    type = "SESSION_END",
                    description = text,
                    details = "Тривалість: ${formatDuration(endedAt - startedAt)}",
                )
            }
            activityRecordDao.deleteByTargetTypeKeepingNewest(
                targetType = SESSION_EVENT_TARGET_TYPE,
                keepCount = SESSION_EVENT_KEEP_COUNT,
            )
        }

        private suspend fun insertActivityRecord(
            text: String,
            targetType: String,
            mode: SessionMode,
        ) {
            val now = System.currentTimeMillis()
            activityRecordDao.insert(
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    rawNoteText = text,
                    noteText = text,
                    recordKind = ActivityRecordKind.EVENT,
                    createdAt = now,
                    updatedAt = now,
                    targetType = targetType,
                    targetId = mode.name,
                    contextId = mode.systemContextId,
                    syncedAt = null,
                    version = 1,
                ),
            )
        }

        private suspend fun ensureSessionSystemContextsExist() {
            systemContextEnsurer.ensureAllSystemContextsExist()
        }

        private fun MutablePreferences.clearActiveMode() {
            remove(currentModeKey)
            remove(currentModeStartedAtKey)
        }

        private fun formatDateTime(timestamp: Long): String =
            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(timestamp))

        private fun formatDuration(durationMillis: Long): String {
            val totalMinutes = (durationMillis / 60000L).coerceAtLeast(0)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return buildString {
                if (hours > 0) append("${hours}г ")
                append("${minutes}хв")
            }
        }
    }
