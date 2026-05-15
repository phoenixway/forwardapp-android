package com.romankozak.forwardappmobile.features.daymanagement.runtime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementPhase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeCommand
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeDecision
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeEvent
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.runtime.engine.DayManagementRuntime
import com.romankozak.forwardappmobile.features.daymanagement.runtime.platform.DayManagementRuntimeNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dayManagementRuntimeDataStore: DataStore<Preferences> by preferencesDataStore(name = "day_management_runtime")

@Singleton
class DayManagementRuntimeRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val runtime: DayManagementRuntime,
        private val notifier: DayManagementRuntimeNotifier,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val gson = Gson()

        val state: Flow<DayManagementRuntimeState> =
            context.dayManagementRuntimeDataStore.data.map { prefs ->
                DayManagementRuntimeState(
                    sessionId = prefs[sessionIdKey],
                    calendarAnchorDate = prefs[calendarAnchorDateKey],
                    wokeAt = prefs[wokeAtKey],
                    sleepAt = prefs[sleepAtKey],
                    currentPhase = prefs[currentPhaseKey]?.let(DayManagementPhase::valueOf) ?: DayManagementPhase.CLOSED,
                    phaseStartedAt = prefs[phaseStartedAtKey],
                    dayPlanFinalizedAt = prefs[dayPlanFinalizedAtKey],
                    activeAlarmIds = prefs[activeAlarmIdsKey] ?: emptySet(),
                    riskFlags = prefs[riskFlagsKey] ?: emptySet(),
                    updatedAt = prefs[updatedAtKey],
                )
            }

        suspend fun apply(command: DayManagementRuntimeCommand) {
            withContext(ioDispatcher) {
                val currentState = state.first()
                val decision = runtime.handle(currentState, command)
                persistDecision(decision)
            }
        }

        private suspend fun persistDecision(decision: DayManagementRuntimeDecision) {
            context.dayManagementRuntimeDataStore.edit { prefs ->
                writeState(prefs, decision.newState)
            }
            appendEvents(decision.events)
            notifier.sync(decision.newState)
        }

        private fun writeState(
            prefs: androidx.datastore.preferences.core.MutablePreferences,
            state: DayManagementRuntimeState,
        ) {
            writeNullableString(prefs, sessionIdKey, state.sessionId)
            writeNullableLong(prefs, calendarAnchorDateKey, state.calendarAnchorDate)
            writeNullableLong(prefs, wokeAtKey, state.wokeAt)
            writeNullableLong(prefs, sleepAtKey, state.sleepAt)
            prefs[currentPhaseKey] = state.currentPhase.name
            writeNullableLong(prefs, phaseStartedAtKey, state.phaseStartedAt)
            writeNullableLong(prefs, dayPlanFinalizedAtKey, state.dayPlanFinalizedAt)
            prefs[activeAlarmIdsKey] = state.activeAlarmIds
            prefs[riskFlagsKey] = state.riskFlags
            writeNullableLong(prefs, updatedAtKey, state.updatedAt)
        }

        private fun appendEvents(events: List<DayManagementRuntimeEvent>) {
            if (events.isEmpty()) return
            val logFile = eventLogFile()
            logFile.parentFile?.mkdirs()
            logFile.appendText(
                buildString {
                    events.forEach { event ->
                        append(gson.toJson(event))
                        append('\n')
                    }
                },
            )
        }

        private fun eventLogFile(): File = File(context.filesDir, "day_management/runtime_events.jsonl")

        private fun writeNullableString(
            prefs: androidx.datastore.preferences.core.MutablePreferences,
            key: Preferences.Key<String>,
            value: String?,
        ) {
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }

        private fun writeNullableLong(
            prefs: androidx.datastore.preferences.core.MutablePreferences,
            key: Preferences.Key<Long>,
            value: Long?,
        ) {
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }

        companion object {
            private val sessionIdKey = stringPreferencesKey("session_id")
            private val calendarAnchorDateKey = longPreferencesKey("calendar_anchor_date")
            private val wokeAtKey = longPreferencesKey("woke_at")
            private val sleepAtKey = longPreferencesKey("sleep_at")
            private val currentPhaseKey = stringPreferencesKey("current_phase")
            private val phaseStartedAtKey = longPreferencesKey("phase_started_at")
            private val dayPlanFinalizedAtKey = longPreferencesKey("day_plan_finalized_at")
            private val activeAlarmIdsKey = stringSetPreferencesKey("active_alarm_ids")
            private val riskFlagsKey = stringSetPreferencesKey("risk_flags")
            private val updatedAtKey = longPreferencesKey("updated_at")
        }
    }
